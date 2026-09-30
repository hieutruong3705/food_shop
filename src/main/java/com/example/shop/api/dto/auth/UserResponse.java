package com.example.shop.api.dto.auth;

import com.example.shop.domain.User;

public record UserResponse(long id, String email, String fullName, String role, boolean enabled) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole() == null ? null : user.getRole().getName(), user.isEnabled());
    }
}
