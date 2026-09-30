package com.example.shop.api.dto.admin;

import java.util.List;

public interface ScopeInput {
    String scopeType();
    List<Long> categoryIds();
    List<Long> brandIds();
    List<Long> productIds();
    Boolean all();
}
