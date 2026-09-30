package com.example.shop.api.controller;
import com.example.shop.api.service.AccountService;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/account") 
public class AccountApiController {
    private final AccountService service;
    public AccountApiController(AccountService service) {
        this.service = service;
    }

    @GetMapping public Profile get(Authentication a) {
        return service.get(a);
    }
    @PutMapping public Profile update(Authentication a,@Valid @RequestBody Profile p) {
        return service.update(a, p);
    }
    @PostMapping("/password") public java.util.Map<String,String> password(Authentication a,@Valid @RequestBody Password p,jakarta.servlet.http.HttpServletRequest req) {
        return service.password(a, p, req);
    }
}
