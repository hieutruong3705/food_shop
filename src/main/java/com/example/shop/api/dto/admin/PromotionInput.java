package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record PromotionInput(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String imageUrl,
        @NotNull @DecimalMin("1") @DecimalMax("100") Double discountRate,
        @NotBlank String startDate,
        @NotBlank String endDate,
        Boolean active,
        String scopeType,
        @Size(max = 500) List<@NotNull @Positive Long> categoryIds,
        @Size(max = 500) List<@NotNull @Positive Long> brandIds,
        @Size(max = 500) List<@NotNull @Positive Long> productIds,
        Boolean all) implements ScopeInput {
}
