package com.example.shop;

import com.example.shop.domain.Role;
import com.example.shop.domain.User;
import com.example.shop.repository.RoleRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.support.MovableClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The limits are switched off in the shared test profile; this class turns them on. */
@SpringBootTest(properties = "shop.rate-limit.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitTests {
    private static final String PASSWORD = "Test-password-2026";
    private static final AtomicInteger NEXT_ADDRESS = new AtomicInteger();

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary MovableClock movableClock() { return new MovableClock(Instant.now()); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MovableClock clock;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mail;

    @BeforeEach
    void mailSender() {
        doNothing().when(mail).send(any(MimeMessage.class));
        when(mail.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    /** A remote address no other test in this class uses, so windows never overlap between tests. */
    private static String freshAddress() {
        int n = NEXT_ADDRESS.incrementAndGet();
        return "10.9." + (n / 250) + "." + (n % 250 + 1);
    }

    private ResultActions send(String path, String address, Object body) throws Exception {
        return mvc.perform(post(path).with(csrf()).with(request -> {
            request.setRemoteAddr(address);
            return request;
        }).contentType("application/json").content(json.writeValueAsString(body)));
    }

    private String newEmail() {
        return UUID.randomUUID() + "@limit.test";
    }

    private User verifiedUser() {
        Role role = roles.findByName("USER");
        if (role == null) {
            role = new Role();
            role.setName("USER");
            role = roles.save(role);
        }
        var user = new User();
        user.setEmail(newEmail());
        user.setFullName("Limit User");
        user.setPassword(encoder.encode(PASSWORD));
        user.setEnabled(true);
        user.setRole(role);
        return users.save(user);
    }

    @Test
    void chatIsLimitedToTwelveRequestsAMinutePerAddressAndRecovers() throws Exception {
        String address = freshAddress();
        var body = Map.of("message", "laptop", "history", List.of());
        for (int i = 0; i < 12; i++) send("/api/chat", address, body).andExpect(status().isOk());

        send("/api/chat", address, body).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("[1-9][0-9]?")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("thử lại sau")));
        send("/api/v1/chat", address, body).andExpect(status().isTooManyRequests());
        send("/api/chat", freshAddress(), body).andExpect(status().isOk());

        clock.advance(Duration.ofSeconds(61));
        send("/api/chat", address, body).andExpect(status().isOk());
    }

    @Test
    void anAddressCannotStartMoreThanTenLoginsAMinuteEvenWithDifferentAccounts() throws Exception {
        String address = freshAddress();
        for (int i = 0; i < 10; i++) {
            send("/api/v1/auth/login", address, Map.of("email", newEmail(), "password", "wrong")).andExpect(status().isUnauthorized());
        }
        send("/api/v1/auth/login", address, Map.of("email", newEmail(), "password", "wrong")).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofSeconds(61));
        send("/api/v1/auth/login", address, Map.of("email", newEmail(), "password", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void fiveWrongPasswordsLockTheAccountForFiveMinutesEvenForTheRightPassword() throws Exception {
        var user = verifiedUser();
        for (int i = 0; i < 5; i++) {
            send("/api/v1/auth/login", freshAddress(), Map.of("email", user.getEmail(), "password", "wrong")).andExpect(status().isUnauthorized());
        }
        send("/api/v1/auth/login", freshAddress(), Map.of("email", user.getEmail(), "password", PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5 phút")));
        // The same lock protects addresses that have no account, so it does not reveal which exist.
        String unknown = newEmail();
        for (int i = 0; i < 5; i++) {
            send("/api/v1/auth/login", freshAddress(), Map.of("email", unknown, "password", "wrong")).andExpect(status().isUnauthorized());
        }
        send("/api/v1/auth/login", freshAddress(), Map.of("email", unknown, "password", "wrong")).andExpect(status().isTooManyRequests());

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        send("/api/v1/auth/login", freshAddress(), Map.of("email", user.getEmail(), "password", PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void aSuccessfulLoginResetsTheFailureCount() throws Exception {
        var user = verifiedUser();
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 4; i++) {
                send("/api/v1/auth/login", freshAddress(), Map.of("email", user.getEmail(), "password", "wrong")).andExpect(status().isUnauthorized());
            }
            send("/api/v1/auth/login", freshAddress(), Map.of("email", user.getEmail(), "password", PASSWORD)).andExpect(status().isOk());
        }
    }

    @Test
    void registerForgotPasswordAndResendAreEachLimitedToFiveEveryTenMinutes() throws Exception {
        String address = freshAddress();
        for (int i = 0; i < 5; i++) {
            String email = newEmail();
            send("/api/v1/auth/register", address, Map.of("firstName", "A", "lastName", "B", "email", email,
                    "password", PASSWORD, "confirmPassword", PASSWORD)).andExpect(status().isCreated());
            send("/api/v1/auth/forgot-password", address, Map.of("email", email)).andExpect(status().isOk());
            send("/api/v1/auth/resend-verification", address, Map.of("email", email)).andExpect(status().isOk());
        }
        send("/api/v1/auth/register", address, Map.of("firstName", "A", "lastName", "B", "email", newEmail(),
                "password", PASSWORD, "confirmPassword", PASSWORD)).andExpect(status().isTooManyRequests());
        send("/api/v1/auth/forgot-password", address, Map.of("email", newEmail())).andExpect(status().isTooManyRequests());
        send("/api/v1/auth/resend-verification", address, Map.of("email", newEmail())).andExpect(status().isTooManyRequests());
        // Other addresses are unaffected.
        send("/api/v1/auth/forgot-password", freshAddress(), Map.of("email", newEmail())).andExpect(status().isOk());

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));
        send("/api/v1/auth/resend-verification", address, Map.of("email", newEmail())).andExpect(status().isOk());
    }

    @Test
    void chatHistoryOverTheLimitsIsRejectedInVietnamese() throws Exception {
        String address = freshAddress();
        var turn = "x".repeat(900);
        var tooLong = List.of(Map.of("role", "user", "text", turn), Map.of("role", "assistant", "text", turn),
                Map.of("role", "user", "text", turn), Map.of("role", "assistant", "text", turn), Map.of("role", "user", "text", turn));
        send("/api/chat", address, Map.of("message", "hi", "history", tooLong)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("4.000 ký tự")));

        var oneTurnTooLong = List.of(Map.of("role", "user", "text", "x".repeat(1001)));
        send("/api/chat", address, Map.of("message", "hi", "history", oneTurnTooLong)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['history[0].text']").value("Mỗi lượt hội thoại tối đa 1.000 ký tự"));

        var exactlyAtTheLimit = List.of(Map.of("role", "user", "text", "x".repeat(1000)), Map.of("role", "assistant", "text", "x".repeat(1000)),
                Map.of("role", "user", "text", "x".repeat(1000)), Map.of("role", "assistant", "text", "x".repeat(1000)));
        send("/api/chat", address, Map.of("message", "hi", "history", exactlyAtTheLimit)).andExpect(status().isOk());
    }
}
