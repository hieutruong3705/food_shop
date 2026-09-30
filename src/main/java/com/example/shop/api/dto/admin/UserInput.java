package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record UserInput(
        @NotBlank @Pattern(regexp = "USER|ADMIN") String role, Boolean enabled, Boolean locked) {
}
