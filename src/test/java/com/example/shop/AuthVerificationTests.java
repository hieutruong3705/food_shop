package com.example.shop;

import com.example.shop.domain.Role;
import com.example.shop.domain.User;
import com.example.shop.repository.RoleRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.service.Tokens;
import com.example.shop.support.MailLinks;
import com.example.shop.support.MovableClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthVerificationTests {
    private static final String PASSWORD = "Test-password-2026";

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary MovableClock movableClock() { return new MovableClock(Instant.now()); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MovableClock clock;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @MockitoBean JavaMailSender mail;

    @BeforeEach
    void mailSender() {
        clearInvocations(mail);
        doNothing().when(mail).send(any(MimeMessage.class));
        when(mail.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private int registerStatus(String email, String firstName, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("firstName", firstName, "lastName", "User", "email", email,
                        "password", password, "confirmPassword", password))))
                .andReturn().getResponse().getStatus();
    }

    private String resend(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/resend-verification").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String newEmail() {
        return UUID.randomUUID() + "@verify.test";
    }

    @Test
    void registrationSurvivesAMailOutageAndTheLinkCanBeRequestedAgain(CapturedOutput output) throws Exception {
        doThrow(new MailSendException("SMTP is down")).when(mail).send(any(MimeMessage.class));
        String email = newEmail();

        assertThat(registerStatus(email, "First", PASSWORD)).isEqualTo(201);
        var user = users.findByEmail(email);
        assertThat(user.isEnabled()).isFalse();
        String firstCode = MailLinks.lastParameter(mail, "code");
        assertThat(user.getVerificationCode()).isEqualTo(Tokens.hash(firstCode));
        assertThat(output.getOut()).contains("Could not send the verification e-mail")
                .doesNotContain(firstCode).doesNotContain(user.getVerificationCode());
        assertThat(user.getVerificationSentAt()).as("a failed send must not consume the resend slot").isNull();

        // The mail server is back: the user asks again right away and can verify with the new link.
        doNothing().when(mail).send(any(MimeMessage.class));
        resend(email);
        verify(mail, times(2)).send(any(MimeMessage.class));
        String secondCode = MailLinks.lastParameter(mail, "code");
        assertThat(secondCode).isNotEqualTo(firstCode);
        assertThat(users.findByEmail(email).getVerificationCode()).isEqualTo(Tokens.hash(secondCode));
        mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("code", secondCode))))
                .andExpect(jsonPath("$.verified").value(true));
        assertThat(users.findByEmail(email).isEnabled()).isTrue();
    }

    @Test
    void secondResendWithinSixtySecondsSendsNoMail() throws Exception {
        String email = newEmail();
        assertThat(registerStatus(email, "First", PASSWORD)).isEqualTo(201);
        verify(mail, times(1)).send(any(MimeMessage.class));

        clock.advance(Duration.ofSeconds(30));
        resend(email);
        verify(mail, times(1)).send(any(MimeMessage.class));

        clock.advance(Duration.ofSeconds(31));
        resend(email);
        verify(mail, times(2)).send(any(MimeMessage.class));
        resend(email);
        verify(mail, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void resendGivesTheSameAnswerWhetherOrNotTheEmailExists() throws Exception {
        String pending = newEmail();
        registerStatus(pending, "First", PASSWORD);
        clock.advance(Duration.ofSeconds(61));
        var verified = createUser(true, null);
        clearInvocations(mail);

        String forUnknown = resend(newEmail());
        verify(mail, never()).send(any(MimeMessage.class));
        String forVerified = resend(verified.getEmail());
        verify(mail, never()).send(any(MimeMessage.class));
        String forPending = resend(pending);
        verify(mail, times(1)).send(any(MimeMessage.class));

        assertThat(forUnknown).isEqualTo(forVerified).isEqualTo(forPending);
        mvc.perform(post("/api/v1/auth/resend-verification").with(csrf()).contentType("application/json")
                .content("{\"email\":\"not-an-email\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredUnverifiedRegistrationCanBeRegisteredAgain() throws Exception {
        String email = newEmail();
        assertThat(registerStatus(email, "Old", PASSWORD)).isEqualTo(201);
        String oldCode = MailLinks.lastParameter(mail, "code");

        // Still inside the 24 hours: the address is taken.
        clock.advance(Duration.ofHours(23));
        assertThat(registerStatus(email, "New", "Another-password-2026")).isEqualTo(400);
        assertThat(users.findByEmail(email).getFullName()).isEqualTo("Old User");

        clock.advance(Duration.ofHours(2));
        assertThat(registerStatus(email, "New", "Another-password-2026")).isEqualTo(201);
        var renewed = users.findByEmail(email);
        assertThat(renewed.getFullName()).isEqualTo("New User");
        String renewedCode = MailLinks.lastParameter(mail, "code");
        assertThat(renewedCode).isNotEqualTo(oldCode);
        assertThat(renewed.getVerificationCode()).isEqualTo(Tokens.hash(renewedCode));
        assertThat(renewed.getVerificationExpiresAt()).isAfter(clock.instant());
        assertThat(renewed.isEnabled()).isFalse();
        mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("code", oldCode)))).andExpect(jsonPath("$.verified").value(false));
        mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("code", renewedCode)))).andExpect(jsonPath("$.verified").value(true));
        // The renewed password is the one that works.
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", "Another-password-2026")))).andExpect(status().isOk());
    }

    @Test
    void accountsThatWereNotStartedByRegistrationAreNeverTakenOverOrReVerified() throws Exception {
        // Disabled by an administrator: no verification code, so it is not a pending registration.
        var disabled = createUser(false, null);
        clock.advance(Duration.ofDays(2));
        assertThat(registerStatus(disabled.getEmail(), "Hijack", "Another-password-2026")).isEqualTo(400);
        resend(disabled.getEmail());
        verify(mail, never()).send(any(MimeMessage.class));
        var unchanged = users.findByEmail(disabled.getEmail());
        assertThat(unchanged.isEnabled()).isFalse();
        assertThat(unchanged.getVerificationCode()).isNull();
        assertThat(unchanged.getFullName()).isEqualTo("Existing User");

        // A pending registration that an administrator has locked is not restarted either.
        String email = newEmail();
        registerStatus(email, "First", PASSWORD);
        var locked = users.findByEmail(email);
        locked.setIsLocked(true);
        users.save(locked);
        clock.advance(Duration.ofDays(2));
        assertThat(registerStatus(email, "Hijack", "Another-password-2026")).isEqualTo(400);
        clearInvocations(mail);
        resend(email);
        verify(mail, never()).send(any(MimeMessage.class));
    }

    private User createUser(boolean enabled, String code) {
        Role role = roles.findByName("USER");
        if (role == null) {
            role = new Role();
            role.setName("USER");
            role = roles.save(role);
        }
        var user = new User();
        user.setEmail(newEmail());
        user.setFullName("Existing User");
        user.setPassword("{noop}unused");
        user.setEnabled(enabled);
        user.setRole(role);
        user.setVerificationCode(code);
        return users.save(user);
    }
}
