package com.example.shop.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class ApiSecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http,
            @Value("${shop.client-url:http://localhost:5173}") String clientUrl, com.example.shop.repository.UserRepository users) throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(clientUrl));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        http.securityMatcher("/api/**")
            .addFilterBefore(new AccountStatusFilter(users), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .cors(c -> c.configurationSource(source))
            .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/chat","/api/v1/chat").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/chat/status","/api/v1/chat/status").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/catalog/menu","/api/v1/offers").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/payments/vnpay/ipn").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/catalog/facets", "/api/v1/categories", "/api/v1/brands", "/api/v1/banners", "/api/v1/policies/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Vui lòng đăng nhập\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Không đủ quyền hoặc phiên bảo mật hết hạn\"}"); }))
            .requestCache(c -> c.disable());
        return http.build();
    }
}
