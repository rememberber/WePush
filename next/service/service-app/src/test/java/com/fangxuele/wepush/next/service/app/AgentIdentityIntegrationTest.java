package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.agent.protocol.AgentId;
import com.fangxuele.wepush.next.agent.protocol.AgentProtoMapper;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentControlServiceGrpc;
import com.fangxuele.wepush.next.agent.protocol.v1.AgentToService;
import com.fangxuele.wepush.next.agent.protocol.v1.ServiceToAgent;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.io.StringWriter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgentIdentityIntegrationTest {
    private static final Path ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-agent-identity-" + UUID.randomUUID());

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        createServerCertificate();
        registry.add("wepush.database.path", () -> ROOT.resolve("service.db").toString());
        registry.add("wepush.secret.master-key-path", () -> ROOT.resolve("master-key.json").toString());
        registry.add("wepush.agent.identity.ca-key-path", () -> ROOT.resolve("ca-key.pem").toString());
        registry.add("wepush.agent.identity.ca-certificate-path", () -> ROOT.resolve("ca.pem").toString());
        registry.add("wepush.agent.grpc.port", () -> "0");
        registry.add("wepush.agent.grpc.token", () -> "legacy-test-token");
        registry.add("wepush.agent.grpc.tls.enabled", () -> "true");
        registry.add("wepush.agent.grpc.tls.certificate-chain",
                () -> ROOT.resolve("server-cert.pem").toString());
        registry.add("wepush.agent.grpc.tls.private-key",
                () -> ROOT.resolve("server-key.pem").toString());
        registry.add("wepush.agent.grpc.tls.trust-certificates", () -> ROOT.resolve("ca.pem").toString());
        registry.add("wepush.agent.grpc.tls.require-client-certificate", () -> "true");
        registry.add("server.shutdown", () -> "immediate");
    }

    @Autowired
    private AgentGrpcServer grpcServer;

    @LocalServerPort
    private int httpPort;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void enrollsOnceBindsGrpcIdentityAndRotatesCredential() throws Exception {
        JsonNode issued = post("/api/v1/workspaces/ws_default/agent-enrollment-tokens", null,
                "{\"name\":\"test-edge\",\"ttl\":\"PT10M\"}", 201);
        String enrollmentToken = issued.get("token").textValue();
        var keys = KeyPairGenerator.getInstance("EC").generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
        String request = json.writeValueAsString(new EnrollmentRequest("enrolled-agent", publicKey));

        JsonNode enrollment = post("/internal/agent/v1/enroll",
                "Enrollment " + enrollmentToken, request, 201);
        String firstCredential = enrollment.get("credential").textValue();
        X509Certificate clientCertificate = certificate(enrollment.get("certificatePem").textValue());
        X509Certificate caCertificate = certificate(enrollment.get("caCertificatePem").textValue());
        clientCertificate.verify(caCertificate.getPublicKey());
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), clientCertificate.getExtendedKeyUsage());
        Files.writeString(ROOT.resolve("client-cert.pem"), enrollment.get("certificatePem").textValue());
        Files.writeString(ROOT.resolve("client-key.pem"), privateKeyPem(keys.getPrivate().getEncoded()));

        postRaw("/internal/agent/v1/enroll", "Enrollment " + enrollmentToken, request, 401);
        assertEquals(Status.Code.OK, connect(firstCredential, "enrolled-agent"));
        assertEquals(Status.Code.FAILED_PRECONDITION, connect(firstCredential, "different-agent"));

        JsonNode rotated = post("/internal/agent/v1/credentials/rotate",
                "Agent " + firstCredential,
                json.writeValueAsString(new RotationRequest(publicKey)), 200);
        String replacement = rotated.get("credential").textValue();
        assertNotEquals(firstCredential, replacement);
        assertEquals(Status.Code.UNAUTHENTICATED, connect(firstCredential, "enrolled-agent"));
        Files.writeString(ROOT.resolve("client-cert.pem"), rotated.get("certificatePem").textValue());
        assertEquals(Status.Code.OK, connect(replacement, "enrolled-agent"));
    }

    private Status.Code connect(String credential, String agentId) throws Exception {
        ManagedChannel channel = NettyChannelBuilder.forAddress("127.0.0.1", grpcServer.localPort())
                .sslContext(GrpcSslContexts.forClient()
                        .trustManager(ROOT.resolve("server-cert.pem").toFile())
                        .keyManager(ROOT.resolve("client-cert.pem").toFile(),
                                ROOT.resolve("client-key.pem").toFile())
                        .build())
                .build();
        try {
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER),
                    credential);
            var stub = AgentControlServiceGrpc.newStub(channel).withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(metadata));
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<Status.Code> status = new AtomicReference<>();
            AtomicReference<StreamObserver<AgentToService>> requests = new AtomicReference<>();
            requests.set(stub.connect(new StreamObserver<ServiceToAgent>() {
                @Override
                public void onNext(ServiceToAgent value) {
                    status.set(Status.Code.OK);
                    requests.get().onCompleted();
                    done.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    status.set(Status.fromThrowable(throwable).getCode());
                    done.countDown();
                }

                @Override
                public void onCompleted() {
                    if (status.get() == null) status.set(Status.Code.OK);
                    done.countDown();
                }
            }));
            requests.get().onNext(AgentProtoMapper.toProto(new AgentFrames.AgentToService(
                    new AgentId(agentId), 1, new AgentFrames.Hello("test", 1, 1,
                    "test", "test", "21", 1, 0, 0, List.of()))));
            assertTrue(done.await(5, TimeUnit.SECONDS));
            return status.get();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private JsonNode post(String path, String authorization, String body, int expected) throws Exception {
        HttpResponse<String> response = postRaw(path, authorization, body, expected);
        return json.readTree(response.body());
    }

    private HttpResponse<String> postRaw(String path, String authorization, String body,
                                         int expected) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + httpPort + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) request.header("Authorization", authorization);
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), response.body());
        return response;
    }

    private static X509Certificate certificate(String pem) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    private static void createServerCertificate() {
        try {
            Files.createDirectories(ROOT);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            KeyPair pair = generator.generateKeyPair();
            X500Name name = new X500Name("CN=127.0.0.1");
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                    new BigInteger(160, new SecureRandom()).abs().max(BigInteger.ONE),
                    Date.from(now.minusSeconds(60)), Date.from(now.plusSeconds(3600)),
                    name, pair.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            builder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1")));
            var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(pair.getPrivate());
            X509Certificate certificate = new JcaX509CertificateConverter()
                    .getCertificate(builder.build(signer));
            Files.writeString(ROOT.resolve("server-cert.pem"), pem(certificate));
            Files.writeString(ROOT.resolve("server-key.pem"), privateKeyPem(pair.getPrivate().getEncoded()));
        } catch (Exception problem) {
            throw new IllegalStateException("Cannot prepare gRPC TLS test identity", problem);
        }
    }

    private static String pem(Object value) throws Exception {
        StringWriter text = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(text)) {
            writer.writeObject(value);
        }
        return text.toString();
    }

    private static String privateKeyPem(byte[] encoded) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded)
                + "\n-----END PRIVATE KEY-----\n";
    }

    private record EnrollmentRequest(String requestedAgentId, String publicKeyBase64) {
    }

    private record RotationRequest(String publicKeyBase64) {
    }
}
