package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record BrandInput(
        @NotBlank @Size(max = 100) String name, @Size(max = 5000) String description, @Size(max = 500) String logoUrl) {
}
