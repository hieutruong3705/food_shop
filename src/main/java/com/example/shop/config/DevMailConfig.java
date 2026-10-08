package com.example.shop.config;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.*;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.*;
/** Writes e-mail to disk instead of sending it. Set shop.dev-mail-enabled=false (MAIL_DEV=false) to use the real SMTP server. */
@Configuration @Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="shop.dev-mail-enabled", havingValue="true", matchIfMissing=true)
public class DevMailConfig {
    @org.springframework.beans.factory.annotation.Value("${shop.dev-mail-dir:data/dev-mail}") private String mailDirectory;
    @Bean @Primary JavaMailSender devMailSender() {
        return new JavaMailSenderImpl() {
            @Override public void send(MimeMessage message) {
                try {
                    Path directory=Path.of(mailDirectory);Files.createDirectories(directory);
                    try(var stream=Files.newOutputStream(directory.resolve(java.util.UUID.randomUUID()+".eml"))) {message.writeTo(stream);}
                } catch(Exception e) {throw new org.springframework.mail.MailSendException("Cannot write local development email",e);}
            }
        };
    }
}
