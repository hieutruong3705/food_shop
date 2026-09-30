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

/** Strict binding for legacy voucher updates and scope previews. */
@Component
public class AdminInputReader {
    private final ObjectMapper mapper;
    private final Validator validator;

    public AdminInputReader(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper.copy().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        this.validator = validator;
    }

    public <T> T readValidated(JsonNode body,Class<T> type) {
        T input=read(body,type);
        var violations=validator.validate(input);
        if(!violations.isEmpty()) {
            String fields=violations.stream().map(v->v.getPropertyPath().toString()).distinct().sorted().collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Dữ liệu không hợp lệ: "+fields);
        }
        return input;
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
