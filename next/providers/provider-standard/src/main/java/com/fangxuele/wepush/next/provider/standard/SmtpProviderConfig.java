package com.fangxuele.wepush.next.provider.standard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.time.Duration;
import java.util.List;

final class SmtpProviderConfig {
    private SmtpProviderConfig() { }

    static Account account(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "SMTP account");
        String host = StandardProviderSupport.requiredText(root, "host");
        if (!host.matches("(?i)[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?")) {
            throw StandardProviderSupport.invalid("host", "INVALID_HOST", "host must be a DNS name or IP address");
        }
        int port = StandardProviderSupport.integer(root, "port", 587, 1, 65535);
        Security security = StandardProviderSupport.enumeration(root, "security", Security.class, Security.STARTTLS);
        String username = StandardProviderSupport.optionalText(root, "username", "").trim();
        SecretRef password = StandardProviderSupport.optionalSecret(root, "password");
        if (username.isBlank() != (password == null)) {
            throw StandardProviderSupport.invalid("password", "SMTP_AUTH_INCOMPLETE",
                    "username and password SecretRef must either both be configured or both be omitted");
        }
        String fromAddress = email(StandardProviderSupport.requiredText(root, "fromAddress"), "fromAddress");
        String fromName = StandardProviderSupport.optionalText(root, "fromName", "").trim();
        Duration connectionTimeout = StandardProviderSupport.duration(root, "connectionTimeout",
                Duration.ofSeconds(10), Duration.ofMinutes(2));
        Duration readTimeout = StandardProviderSupport.duration(root, "readTimeout",
                Duration.ofSeconds(30), Duration.ofMinutes(5));
        return new Account(host, port, security, username, password, fromAddress, fromName,
                connectionTimeout, readTimeout);
    }

    static Message message(ConfigDocument document) {
        JsonNode root = StandardProviderSupport.object(document, "SMTP message");
        String subject = StandardProviderSupport.requiredText(root, "subjectTemplate");
        if (subject.indexOf('\r') >= 0 || subject.indexOf('\n') >= 0) {
            throw StandardProviderSupport.invalid("subjectTemplate", "HEADER_INJECTION",
                    "subjectTemplate must not contain line breaks");
        }
        String text = StandardProviderSupport.optionalText(root, "textBodyTemplate", "");
        String html = StandardProviderSupport.optionalText(root, "htmlBodyTemplate", "");
        if (text.isBlank() && html.isBlank()) {
            throw StandardProviderSupport.invalid("textBodyTemplate", "MESSAGE_BODY_REQUIRED",
                    "At least one of textBodyTemplate or htmlBodyTemplate is required");
        }
        String replyTo = StandardProviderSupport.optionalText(root, "replyTo", "").trim();
        if (!replyTo.isBlank()) replyTo = email(replyTo, "replyTo");
        List<String> cc = emails(StandardProviderSupport.stringList(root, "cc"), "cc");
        List<String> bcc = emails(StandardProviderSupport.stringList(root, "bcc"), "bcc");
        return new Message(subject, text, html, replyTo, cc, bcc);
    }

    static String email(String value, String path) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            if (!address.getAddress().equals(value)) {
                throw new AddressException("personal name is not allowed");
            }
            return address.getAddress();
        } catch (AddressException problem) {
            throw StandardProviderSupport.invalid(path, "INVALID_EMAIL", path + " must be one valid email address");
        }
    }

    private static List<String> emails(List<String> input, String path) {
        return input.stream().map(value -> email(value, path)).toList();
    }

    enum Security { NONE, STARTTLS, TLS }

    record Account(String host, int port, Security security, String username, SecretRef password,
                   String fromAddress, String fromName, Duration connectionTimeout, Duration readTimeout) { }

    record Message(String subjectTemplate, String textBodyTemplate, String htmlBodyTemplate,
                   String replyTo, List<String> cc, List<String> bcc) { }
}
