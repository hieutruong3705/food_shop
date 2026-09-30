package com.example.shop.config;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.*;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.*;
@Configuration @Profile("dev")
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
