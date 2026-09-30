package com.example.shop.api.controller;

import java.util.Map;
import java.util.UUID;

import com.example.shop.api.dto.auth.LoginRequest;
import com.example.shop.api.dto.auth.RegisterRequest;
import com.example.shop.api.dto.auth.ResetPasswordRequest;
import com.example.shop.api.dto.auth.UserResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.User;
import com.example.shop.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {
    @org.springframework.beans.factory.annotation.Value("${shop.client-url:http://localhost:5173}")
    private String clientUrl;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthApiController(UserService userService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    static final String REGISTERED = "Đăng ký thành công. Vui lòng kiểm tra email để xác minh. Nếu không nhận được email, hãy yêu cầu gửi lại.";
    /** Identical for every outcome so the response never reveals whether an e-mail address is registered. */
    static final String VERIFICATION_RESENT = "Nếu tài khoản đang chờ xác minh, chúng tôi đã gửi lại email xác minh. Nếu không nhận được email, hãy thử lại sau ít phút.";

    /** The account is stored before the e-mail is sent, so a mail outage must not turn into a 500. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        if (!request.password().equals(request.confirmPassword())) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        var issued = userService.register(request.firstName(), request.lastName(), request.email(), request.password());
        userService.sendVerificationQuietly(issued, clientBaseUrl(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", REGISTERED));
    }

    public record ResendRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 254) String email) {}
    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(@Valid @RequestBody ResendRequest request, HttpServletRequest servletRequest) {
        var issued = userService.issueVerificationResend(request.email());
        if (issued != null) userService.sendVerificationQuietly(issued, clientBaseUrl(servletRequest));
        return Map.of("message", VERIFICATION_RESENT);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        servletRequest.getSession();
        servletRequest.changeSessionId();
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return UserResponse.from(requireUser(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResourceNotFoundException("Phiên đăng nhập", "current");
        }
        return UserResponse.from(requireUser(authentication.getName()));
    }

    public record Verification(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100) String code) {}
    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody Verification request) {
        String code=request.code();
        if(code.isBlank())throw new IllegalArgumentException("Mã xác minh là bắt buộc");
        return Map.of("verified", userService.verify(code));
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest servletRequest) throws Exception {
        String email = body.get("email");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email là bắt buộc");
        String token = UUID.randomUUID().toString();
        if (!userService.checkEmailExists(email)) return Map.of("message", "Nếu email tồn tại, hướng dẫn đã được gửi.");
        userService.updateResetPasswordToken(token, email);
        userService.sendEmail(email, clientBaseUrl(servletRequest) + "/reset-password?token=" + token);
        return Map.of("message", "Nếu email tồn tại, hướng dẫn đã được gửi.");
    }

    @PostMapping("/reset-password")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        User user = userService.getByResetPasswordToken(request.token());
        if (user == null) throw new ResourceNotFoundException("Token đặt lại mật khẩu", "invalid-or-expired");
        userService.updatePassword(user, passwordEncoder.encode(request.password()));
        return Map.of("message", "Đặt lại mật khẩu thành công.");
    }

    private User requireUser(String email) {
        User user = userService.getUserByEmail(email);
        if (user == null) throw new ResourceNotFoundException("Người dùng", email);
        return user;
    }

    private String clientBaseUrl(HttpServletRequest request) {
        return clientUrl;
    }
}
