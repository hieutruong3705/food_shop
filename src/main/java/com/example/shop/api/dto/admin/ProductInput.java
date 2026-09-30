package com.example.shop.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

public record ProductInput(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 150) @Pattern(regexp = "[a-z0-9-]+") String slug,
        @NotNull @DecimalMin("0") @DecimalMax("1000000000") Double price,
        @NotNull @Min(0) @Max(1000000) Long quantity,
        @Min(0) @Max(120) Integer warrantyMonths,
        @NotNull @Positive Long categoryId,
        @NotNull @Positive Long brandId,
        @Size(max = 2000) String shortDesc,
        @Size(max = 20000) String detailDesc,
        @Size(max = 12) List<@NotNull @Size(max = 500) String> images,
        @Size(max = 40) List<@NotNull @Valid SpecInput> specs,
        @Size(max = 100) List<@NotNull @Valid VariantInput> variants,
        Boolean active,
        Boolean technologyCatalog) {
}
