package com.example.shop.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max=50) String firstName,
        @NotBlank @Size(max=50) String lastName,
        @Email @NotBlank @Size(max=254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu phải có ít nhất 8 ký tự") String password,
        @NotBlank String confirmPassword) {}
