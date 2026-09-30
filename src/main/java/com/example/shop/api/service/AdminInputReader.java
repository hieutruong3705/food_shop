package com.example.shop.api.service;

import com.example.shop.api.dto.admin.*;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.stream.Collectors;

/** Temporary binding adapter for the resource router; individual controllers use the same DTOs. */
@Component
public class AdminInputReader {
    private final ObjectMapper mapper;
    private final Validator validator;

    public AdminInputReader(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper.copy().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        this.validator = validator;
    }

    public void validate(String resource, JsonNode body) {
        Class<?> type = switch (resource) {
            case "products" -> ProductInput.class;
            case "categories" -> CategoryInput.class;
            case "brands" -> BrandInput.class;
            case "banners" -> BannerInput.class;
            case "vouchers" -> VoucherInput.class;
            case "promotions" -> PromotionInput.class;
            case "users" -> UserInput.class;
            default -> throw new ResourceNotFoundException(resource, 0);
        };
        Object input = read(body, type);
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            String fields = violations.stream().map(v -> v.getPropertyPath().toString()).distinct().sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Dữ liệu không hợp lệ: " + fields);
        }
    }

    public OfferScopeInput scope(JsonNode body) {
        return read(body, OfferScopeInput.class);
    }

    private <T> T read(JsonNode body, Class<T> type) {
        try {
            return mapper.readerFor(type).readValue(body);
        } catch (IOException e) {
            throw new IllegalArgumentException("Định dạng dữ liệu không hợp lệ");
        }
    }
}
