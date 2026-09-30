package com.example.shop.support;

import jakarta.mail.internet.MimeMessage;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.regex.Pattern;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/** Reads the one-time token out of the last e-mail a mocked {@link JavaMailSender} was asked to send. */
public final class MailLinks {
    private MailLinks() {}

    /** Value of the named query parameter in the link of the most recent e-mail. */
    public static String lastParameter(JavaMailSender mail, String name) throws Exception {
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail, atLeastOnce()).send(captor.capture());
        var all = captor.getAllValues();
        String body = String.valueOf(all.get(all.size() - 1).getContent());
        var matcher = Pattern.compile("[?&]" + Pattern.quote(name) + "=([A-Za-z0-9_-]+)").matcher(body);
        if (!matcher.find()) throw new AssertionError("No " + name + " parameter in the e-mail body");
        return matcher.group(1);
    }
}
