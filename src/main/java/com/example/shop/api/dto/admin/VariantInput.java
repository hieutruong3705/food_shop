package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record VariantInput(
        @Positive Long id,
        @NotBlank @Size(max = 80) String sku,
        @NotNull @DecimalMin("0") @DecimalMax("1000000000") @Digits(integer = 10, fraction = 0) Double price,
        @NotNull @Min(0) @Max(1000000) Long quantity,
        Boolean active,
        @Size(max = 500) String imageUrl,
        @NotEmpty @Size(max = 6) Map<@NotBlank @Size(max = 80) String, @NotBlank @Size(max = 100) String> options) {
}
