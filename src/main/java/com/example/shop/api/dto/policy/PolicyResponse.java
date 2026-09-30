package com.example.shop.api.dto.policy;

import com.example.shop.domain.Policy;

public record PolicyResponse(Long id, String name, String content, boolean active, boolean technologyCatalog) {
    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(policy.getId(), policy.getName(), policy.getContent(), policy.isActive(),policy.isTechnologyCatalog());
    }
}
