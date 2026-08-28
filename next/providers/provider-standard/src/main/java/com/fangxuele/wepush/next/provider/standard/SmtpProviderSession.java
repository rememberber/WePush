package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.CancellationToken;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

final class SmtpProviderSession implements ProviderSession {
    private final SmtpProviderConfig.Account account;
    private final SmtpProviderConfig.Message message;
    private final SecretResolver secrets;
    private final ExecutionClock clock;
    private final boolean dryRun;
    private final Session session;
    private Transport transport;

    SmtpProviderSession(SmtpProviderConfig.Account account, SmtpProviderConfig.Message message,
                        SecretResolver secrets, ExecutionClock clock, boolean dryRun) {
        this.account = account;
        this.message = message;
        this.secrets = secrets;
        this.clock = clock;
        this.dryRun = dryRun;
        session = mailSession(account, account.readTimeout());
    }

    static Session mailSession(SmtpProviderConfig.Account account, Duration timeout) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", account.host());
        properties.setProperty("mail.smtp.port", Integer.toString(account.port()));
        properties.setProperty("mail.smtp.auth", Boolean.toString(account.password() != null));
        properties.setProperty("mail.smtp.connectiontimeout",
                Long.toString(Math.min(timeout.toMillis(), account.connectionTimeout().toMillis())));
        properties.setProperty("mail.smtp.timeout", Long.toString(Math.min(timeout.toMillis(), account.readTimeout().toMillis())));
        properties.setProperty("mail.smtp.writetimeout", Long.toString(Math.min(timeout.toMillis(), account.readTimeout().toMillis())));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.3 TLSv1.2");
        if (account.security() == SmtpProviderConfig.Security.STARTTLS) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.starttls.required", "true");
        } else if (account.security() == SmtpProviderConfig.Security.TLS) {
            properties.setProperty("mail.smtp.ssl.enable", "true");
        }
        return Session.getInstance(properties);
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) {
            return ProviderResult.failure("CANCELLED", ErrorCategory.CANCELLED, false,
                    "Email was cancelled before SMTP submission");
        }
        if (!request.deadline().isAfter(clock.now())) {
            return ProviderResult.failure("ITEM_TIMEOUT", ErrorCategory.TIMEOUT, false,
                    "Email deadline elapsed before SMTP submission");
        }
        boolean submitting = false;
        try {
            MimeMessage mail = build(request);
            if (dryRun) return ProviderResult.success("DRY_RUN", "");
            ensureConnected();
            submitting = true;
            transport.sendMessage(mail, mail.getAllRecipients());
            return new ProviderResult(ItemState.SUCCEEDED, "SMTP_ACCEPTED", ErrorCategory.NONE,
                    false, null, "", "", Map.of("recipientCount",
                    Integer.toString(mail.getAllRecipients().length)));
        } catch (ProviderConfigException problem) {
            return ProviderResult.failure(problem.code(), ErrorCategory.RECIPIENT_INVALID, false,
                    problem.getMessage());
        } catch (AuthenticationFailedException problem) {
            closeQuietly();
            return ProviderResult.failure("SMTP_AUTHENTICATION_FAILED", ErrorCategory.AUTHENTICATION,
                    false, "SMTP authentication failed");
        } catch (SendFailedException problem) {
            return ProviderResult.failure("SMTP_RECIPIENT_REJECTED", ErrorCategory.RECIPIENT_INVALID,
                    false, "SMTP rejected one or more recipients");
        } catch (MessagingException problem) {
            closeQuietly();
            return messagingFailure(problem, submitting);
        } catch (Exception problem) {
            closeQuietly();
            return ProviderResult.failure("SMTP_INTERNAL_ERROR", ErrorCategory.INTERNAL, false,
                    problem.getClass().getSimpleName());
        }
    }

    private MimeMessage build(ProviderSendRequest request) throws Exception {
        String toAddress = SmtpProviderConfig.email(
                StandardProviderSupport.recipientText(request.recipient(), "email", true), "recipient.email");
        String toName = StandardProviderSupport.recipientText(request.recipient(), "name", false);
        String subject = StandardProviderSupport.renderText(message.subjectTemplate(), request.recipient());
        if (subject.indexOf('\r') >= 0 || subject.indexOf('\n') >= 0) {
            throw StandardProviderSupport.invalid("subjectTemplate", "HEADER_INJECTION",
                    "Rendered subject must not contain line breaks");
        }

        MimeMessage mail = new MimeMessage(session);
        mail.setFrom(address(account.fromAddress(), account.fromName()));
        mail.setRecipient(Message.RecipientType.TO, address(toAddress, toName));
        add(mail, Message.RecipientType.CC, message.cc());
        add(mail, Message.RecipientType.BCC, message.bcc());
        if (!message.replyTo().isBlank()) mail.setReplyTo(new InternetAddress[]{new InternetAddress(message.replyTo())});
        mail.setSubject(subject, StandardCharsets.UTF_8.name());
        String text = StandardProviderSupport.renderText(message.textBodyTemplate(), request.recipient());
        String html = StandardProviderSupport.renderText(message.htmlBodyTemplate(), request.recipient());
        if (!text.isBlank() && !html.isBlank()) {
            MimeMultipart alternative = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text, StandardCharsets.UTF_8.name());
            alternative.addBodyPart(textPart);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");
            alternative.addBodyPart(htmlPart);
            mail.setContent(alternative);
        } else if (!html.isBlank()) {
            mail.setContent(html, "text/html; charset=UTF-8");
        } else {
            mail.setText(text, StandardCharsets.UTF_8.name());
        }
        mail.saveChanges();
        return mail;
    }

    private static InternetAddress address(String email, String name) throws Exception {
        return name == null || name.isBlank() ? new InternetAddress(email)
                : new InternetAddress(email, name, StandardCharsets.UTF_8.name());
    }

    private static void add(MimeMessage mail, Message.RecipientType type, Iterable<String> values)
            throws MessagingException {
        for (String value : values) mail.addRecipient(type, new InternetAddress(value));
    }

    private void ensureConnected() throws Exception {
        if (transport != null && transport.isConnected()) return;
        transport = session.getTransport("smtp");
        SmtpProviderFactory.connect(transport, account, secrets);
    }

    static String connectionCode(Exception problem) {
        if (problem instanceof AuthenticationFailedException) return "SMTP_AUTHENTICATION_FAILED";
        if (causedBy(problem, SocketTimeoutException.class)) return "SMTP_TIMEOUT";
        if (causedBy(problem, ConnectException.class)) return "SMTP_CONNECT_FAILED";
        return "SMTP_CONNECTION_TEST_FAILED";
    }

    static ProviderResult messagingFailure(MessagingException problem, boolean submitting) {
        if (causedBy(problem, SocketTimeoutException.class)) {
            return submitting
                    ? unknown("SMTP_TIMEOUT", ErrorCategory.TIMEOUT,
                    "SMTP outcome is unknown after timeout during submission")
                    : ProviderResult.failure("SMTP_TIMEOUT", ErrorCategory.TIMEOUT, true,
                    "SMTP connection timed out before submission");
        }
        if (causedBy(problem, ConnectException.class)) {
            return submitting
                    ? unknown("SMTP_CONNECT_UNKNOWN", ErrorCategory.NETWORK,
                    "SMTP outcome is unknown after connection failure during submission")
                    : ProviderResult.failure("SMTP_CONNECT_FAILED", ErrorCategory.NETWORK, true,
                    "SMTP connection failed before submission");
        }
        return submitting
                ? unknown("SMTP_SUBMISSION_UNKNOWN", ErrorCategory.UNKNOWN,
                "SMTP outcome is unknown after a messaging failure")
                : ProviderResult.failure("SMTP_CONNECTION_FAILED", ErrorCategory.NETWORK, true,
                "SMTP connection failed before submission");
    }

    private static boolean causedBy(Throwable problem, Class<? extends Throwable> type) {
        Throwable current = problem;
        while (current != null) {
            if (type.isInstance(current)) return true;
            if (current instanceof MessagingException messaging && messaging.getNextException() != null
                    && messaging.getNextException() != current.getCause()) {
                if (causedBy(messaging.getNextException(), type)) return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ProviderResult unknown(String code, ErrorCategory category, String diagnostic) {
        return new ProviderResult(ItemState.UNKNOWN, code, category, false, null,
                diagnostic, "", Map.of());
    }

    private void closeQuietly() {
        if (transport != null) try { transport.close(); } catch (Exception ignored) { }
        transport = null;
    }

    @Override
    public void close() { closeQuietly(); }
}
