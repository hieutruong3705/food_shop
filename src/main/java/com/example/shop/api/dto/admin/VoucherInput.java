package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record VoucherInput(
        @NotBlank @Size(max = 50) String code,
        @NotNull @DecimalMin("1") @DecimalMax("100") Double discount,
        @Size(max = 500) String imageUrl,
        Boolean active,
        String startDate,
        String endDate,
        @Min(0) @Max(100000000000L) Long minimumOrder,
        @DecimalMin("1") @DecimalMax("100000000000") @Digits(integer = 12, fraction = 0) Double maximumDiscount,
        @Min(1) @Max(1000000000) Long usageLimit,
        String scopeType,
        @Size(max = 500) List<@NotNull @Positive Long> categoryIds,
        @Size(max = 500) List<@NotNull @Positive Long> brandIds,
        @Size(max = 500) List<@NotNull @Positive Long> productIds,
        Boolean all) implements ScopeInput {
}
