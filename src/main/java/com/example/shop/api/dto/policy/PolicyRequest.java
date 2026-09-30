package com.example.shop.api.dto.policy;

import jakarta.validation.constraints.NotBlank;

public record PolicyRequest(
        @NotBlank(message = "Tên chính sách không được để trống") String name,
        @NotBlank(message = "Nội dung chính sách không được để trống") String content,
        Boolean active, Boolean technologyCatalog) {
}
