package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record SpecInput(
        @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 500) String value) {
}
