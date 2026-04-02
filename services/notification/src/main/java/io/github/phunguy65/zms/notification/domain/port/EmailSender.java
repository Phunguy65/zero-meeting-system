package io.github.phunguy65.zms.notification.domain.port;

public interface EmailSender {

    void send(String toEmail, String subject, String html);
}
