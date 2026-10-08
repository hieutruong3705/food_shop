package com.example.shop.config;

import com.example.shop.domain.User;
import com.example.shop.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Turns a finished Google sign-in into the same session the password form produces: the stored
 * authentication is a plain username/password token holding the e-mail, so {@link AccountStatusFilter},
 * {@code /api/v1/auth/me} and every controller that reads {@code authentication.getName()} keep working
 * without knowing a second login method exists.
 */
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginSuccessHandler.class);

    private final UserService userService;
    private final String clientUrl;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public GoogleLoginSuccessHandler(UserService userService, String clientUrl) {
        this.userService = userService;
        this.clientUrl = clientUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        /* Google only vouches for a verified address; an unverified one could belong to somebody else. */
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(principal.getAttribute("email_verified"))) {
            redirect(response, "/login?error=google-email");
            return;
        }
        User user;
        try {
            user = userService.loginWithGoogle(email, principal.getAttribute("name"));
        } catch (IllegalStateException e) {
            redirect(response, "/login?error=google-locked");
            return;
        } catch (RuntimeException e) {
            log.warn("Google sign-in failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            redirect(response, "/login?error=google");
            return;
        }
        var token = UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        request.getSession();
        request.changeSessionId();
        contexts.saveContext(context, request, response);
        redirect(response, "/");
    }

    private void redirect(HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(clientUrl + path);
    }
}
