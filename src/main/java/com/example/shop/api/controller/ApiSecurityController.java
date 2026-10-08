package com.example.shop.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the CSRF token for the session-cookie based API used by the Vue client. */
@RestController
@RequestMapping("/api/v1/auth")
public class ApiSecurityController {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    /** Lets the sign-in page hide the Google button on deployments without Google credentials. */
    @GetMapping("/providers")
    public Map<String, Boolean> providers() {
        return Map.of("google", !googleClientId.isBlank());
    }
}
