package com.fangxuele.wepush.next.provider.standard;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import org.junit.jupiter.api.Test;
import jakarta.mail.MessagingException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpProviderFactoryTest {
    private final SmtpProviderFactory factory = new SmtpProviderFactory();

    @Test
    void sendsUtf8MultipartEmailThroughLocalSmtpServer() throws Exception {
        try (MiniSmtpServer server = new MiniSmtpServer()) {
            ConfigDocument account = account(server.port());
            ConfigDocument message = json("message", """
                    {
                      "subjectTemplate":"Welcome {{name}}",
                      "textBodyTemplate":"Hello {{name}}",
                      "htmlBodyTemplate":"<strong>Hello {{name}}</strong>",
                      "replyTo":"support@example.com"
                    }
                    """);

            ProviderResult result = send(account, message, recipient(), false);
            String source = server.message().get(5, TimeUnit.SECONDS);

            assertEquals(ItemState.SUCCEEDED, result.outcome());
            assertEquals("SMTP_ACCEPTED", result.code());
            assertTrue(source.contains("Subject: Welcome Alice"));
            assertTrue(source.contains("alice@example.com"));
            assertTrue(source.contains("multipart/alternative"));
        }
    }

    @Test
    void connectionTestUsesSmtpHandshakeWithoutSending() throws Exception {
        try (MiniSmtpServer server = new MiniSmtpServer()) {
            var result = factory.testConnection(account(server.port()), ref -> {
                throw new AssertionError("No secret expected");
            }, Duration.ofSeconds(5));

            assertTrue(result.successful());
            assertEquals("SMTP_CONNECTED", result.code());
        }
    }

    @Test
    void dryRunValidatesRecipientWithoutOpeningNetworkConnection() throws Exception {
        ProviderResult result = send(account(9), json("message", """
                {"subjectTemplate":"test","textBodyTemplate":"hello"}
                """), recipient(), true);

        assertEquals(ItemState.SUCCEEDED, result.outcome());
        assertEquals("DRY_RUN", result.code());
    }

    @Test
    void reportsStructuredAccountAndMessageValidation() {
        var account = factory.validateAccount(json("account", """
                {"host":"smtp.example.com","fromAddress":"not-an-email"}
                """));
        var message = factory.validateMessage(json("message", """
                {"subjectTemplate":"empty body"}
                """));

        assertFalse(account.validResult());
        assertEquals("INVALID_EMAIL", account.violations().getFirst().code());
        assertFalse(message.validResult());
        assertEquals("MESSAGE_BODY_REQUIRED", message.violations().getFirst().code());
    }

    @Test
    void distinguishesPreSubmissionTimeoutFromUnknownSubmissionOutcome() {
        MessagingException timeout = new MessagingException("timeout", new SocketTimeoutException());

        ProviderResult beforeSubmission = SmtpProviderSession.messagingFailure(timeout, false);
        ProviderResult duringSubmission = SmtpProviderSession.messagingFailure(timeout, true);

        assertEquals(ItemState.FAILED, beforeSubmission.outcome());
        assertEquals(ErrorCategory.TIMEOUT, beforeSubmission.category());
        assertTrue(beforeSubmission.retryable());
        assertEquals(ItemState.UNKNOWN, duringSubmission.outcome());
        assertFalse(duringSubmission.retryable());
    }

    private ProviderResult send(ConfigDocument account, ConfigDocument message,
                                RecipientRecord recipient, boolean dryRun) throws Exception {
        RunExecutionSpec spec = new RunExecutionSpec("run-smtp",
                new ProviderRef(SmtpProviderFactory.PROVIDER_ID, SmtpProviderFactory.VERSION),
                account, message, ExecutionPolicies.defaults(), Map.of(), dryRun, Instant.now());
        try (ProviderSession session = factory.open(new ProviderOpenContext(spec, ref -> {
            throw new AssertionError("No secret expected");
        }, ExecutionClock.system()))) {
            return session.send(new ProviderSendRequest(spec.runId(), recipient.itemId(), 1,
                    recipient, message, "smtp-key", Instant.now().plusSeconds(5)), () -> false);
        }
    }

    private static ConfigDocument account(int port) {
        return json("account", """
                {"host":"127.0.0.1","port":%d,"security":"NONE","fromAddress":"sender@example.com"}
                """.formatted(port));
    }

    private static RecipientRecord recipient() {
        return new RecipientRecord("mail-1", 0, Map.of(
                "email", new RecipientValue.TextValue("alice@example.com"),
                "name", new RecipientValue.TextValue("Alice")));
    }

    private static ConfigDocument json(String schema, String value) {
        return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MiniSmtpServer implements AutoCloseable {
        private final ServerSocket socket;
        private final CompletableFuture<String> message = new CompletableFuture<>();
        private final Thread worker;

        private MiniSmtpServer() throws IOException {
            socket = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
            worker = Thread.ofPlatform().daemon().start(this::serve);
        }

        int port() { return socket.getLocalPort(); }

        CompletableFuture<String> message() { return message; }

        private void serve() {
            try (Socket connection = socket.accept();
                 BufferedReader input = new BufferedReader(new InputStreamReader(
                         connection.getInputStream(), StandardCharsets.US_ASCII));
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                         connection.getOutputStream(), StandardCharsets.US_ASCII))) {
                reply(output, "220 localhost ESMTP ready");
                StringBuilder data = new StringBuilder();
                boolean body = false;
                String line;
                while ((line = input.readLine()) != null) {
                    if (body) {
                        if (line.equals(".")) {
                            message.complete(data.toString());
                            body = false;
                            reply(output, "250 2.0.0 accepted");
                        } else {
                            data.append(line).append("\r\n");
                        }
                    } else if (line.regionMatches(true, 0, "EHLO", 0, 4)) {
                        output.write("250-localhost\r\n250-8BITMIME\r\n250 SMTPUTF8\r\n");
                        output.flush();
                    } else if (line.regionMatches(true, 0, "DATA", 0, 4)) {
                        body = true;
                        reply(output, "354 end with <CRLF>.<CRLF>");
                    } else if (line.regionMatches(true, 0, "QUIT", 0, 4)) {
                        reply(output, "221 bye");
                        return;
                    } else {
                        reply(output, "250 ok");
                    }
                }
            } catch (IOException problem) {
                if (!socket.isClosed()) message.completeExceptionally(problem);
            }
        }

        private static void reply(BufferedWriter output, String line) throws IOException {
            output.write(line);
            output.write("\r\n");
            output.flush();
        }

        @Override
        public void close() throws Exception {
            socket.close();
            worker.join(2000);
        }
    }
}
