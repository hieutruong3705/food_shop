package com.example.shop.api.controller;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/account") @Transactional
public class AccountApiController {
    private final UserRepository users; private final PasswordEncoder encoder;
    public AccountApiController(UserRepository users,PasswordEncoder encoder) {this.users=users;this.encoder=encoder;}
    @GetMapping public Profile get(Authentication a) { var u=users.findByEmail(a.getName()); return new Profile(u.getFullName(),u.getPhone(),u.getAddress()); }
    @PutMapping public Profile update(Authentication a,@Valid @RequestBody Profile p) { var u=users.lockByEmail(a.getName()); u.setFullName(p.fullName());u.setPhone(p.phone());u.setAddress(p.address());return p; }
    @PostMapping("/password") public java.util.Map<String,String> password(Authentication a,@Valid @RequestBody Password p,jakarta.servlet.http.HttpServletRequest req) {
        var u=users.lockByEmail(a.getName()); if(!encoder.matches(p.currentPassword(),u.getPassword())) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        u.setPassword(encoder.encode(p.password())); u.setResetPasswordToken(null);
        req.getSession().invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return java.util.Map.of("message","Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
    }
}
