package com.example.shop.config;

import com.example.shop.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Google sign-in, active only once a client id is configured, so a checkout without Google credentials
 * still starts. The chain covers just the two OAuth endpoints; everything else keeps its existing rules.
 */
@Configuration
@ConditionalOnProperty("spring.security.oauth2.client.registration.google.client-id")
public class OAuthSecurityConfig {

    @Bean @Order(2)
    SecurityFilterChain oauthFilterChain(HttpSecurity http, UserService userService,
            @Value("${shop.client-url:http://localhost:5173}") String clientUrl) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .oauth2Login(o -> o
                .successHandler(new GoogleLoginSuccessHandler(userService, clientUrl))
                .failureHandler((request, response, exception) -> response.sendRedirect(clientUrl + "/login?error=google")))
            .logout(l -> l.disable());
        return http.build();
    }
}
