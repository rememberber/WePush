package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.AgentCertificateAuthority;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

public final class LocalAgentCertificateAuthority implements AgentCertificateAuthority {
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final X500Name CA_SUBJECT = new X500Name("CN=WePush Next Agent CA");

    private final Path privateKeyPath;
    private final Path certificatePath;
    private final Clock clock;
    private final SecureRandom random;
    private final Duration caValidity;
    private final PrivateKey privateKey;
    private final X509Certificate certificate;
    private final String certificatePem;

    public LocalAgentCertificateAuthority(Path privateKeyPath, Path certificatePath,
                                          Clock clock, SecureRandom random, Duration caValidity) {
        this.privateKeyPath = absolute(privateKeyPath, "Agent CA private key");
        this.certificatePath = absolute(certificatePath, "Agent CA certificate");
        this.clock = clock;
        this.random = random;
        if (caValidity == null || caValidity.compareTo(Duration.ofDays(365)) < 0) {
            throw new IllegalArgumentException("Agent CA validity must be at least one year");
        }
        this.caValidity = caValidity;
        AuthorityMaterial material = loadOrCreate();
        this.privateKey = material.privateKey();
        this.certificate = material.certificate();
        this.certificatePem = pem(certificate);
    }

    @Override
    public synchronized IssuedCertificate issueClientCertificate(
            String agentId, java.security.PublicKey publicKey, Instant notBefore, Instant notAfter) {
        if (agentId == null || !agentId.matches("[A-Za-z0-9._-]{1,120}")) {
            throw new IllegalArgumentException("Agent ID is invalid");
        }
        if (publicKey == null || notBefore == null || notAfter == null || !notAfter.isAfter(notBefore)) {
            throw new IllegalArgumentException("Agent certificate request is invalid");
        }
        Instant maximum = certificate.getNotAfter().toInstant().minus(1, ChronoUnit.MINUTES);
        Instant effectiveNotAfter = notAfter.isBefore(maximum) ? notAfter : maximum;
        if (!effectiveNotAfter.isAfter(notBefore)) {
            throw new IllegalStateException("Agent CA expires before the requested client certificate");
        }
        try {
            X500Name subject = new X500Name("CN=" + agentId);
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    certificate, serial(), Date.from(notBefore), Date.from(effectiveNotAfter),
                    subject, publicKey);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            builder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier,
                            "spiffe://wepush/agents/" + agentId)));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
            X509Certificate issued = new JcaX509CertificateConverter()
                    .getCertificate(builder.build(signer));
            issued.verify(certificate.getPublicKey());
            String fingerprint = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(issued.getEncoded()));
            return new IssuedCertificate(pem(issued), certificatePem, fingerprint, effectiveNotAfter);
        } catch (Exception problem) {
            throw new IllegalStateException("Agent client certificate cannot be issued", problem);
        }
    }

    public X509Certificate caCertificate() {
        return certificate;
    }

    private AuthorityMaterial loadOrCreate() {
        boolean keyExists = Files.exists(privateKeyPath);
        boolean certificateExists = Files.exists(certificatePath);
        if (keyExists != certificateExists) {
            throw new IllegalStateException("Agent CA key and certificate must either both exist or both be absent");
        }
        return keyExists ? load() : create();
    }

    private AuthorityMaterial load() {
        try {
            verifyOwnerOnly(privateKeyPath);
            PrivateKey key;
            try (PEMParser parser = new PEMParser(Files.newBufferedReader(privateKeyPath,
                    StandardCharsets.US_ASCII))) {
                Object object = parser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (object instanceof PEMKeyPair pair) {
                    key = pair.getPublicKeyInfo() == null
                            ? converter.getPrivateKey(pair.getPrivateKeyInfo())
                            : converter.getKeyPair(pair).getPrivate();
                }
                else if (object instanceof PrivateKeyInfo info) key = converter.getPrivateKey(info);
                else throw new IllegalStateException("Agent CA private key PEM is invalid");
            }
            X509Certificate loaded;
            try (PEMParser parser = new PEMParser(Files.newBufferedReader(certificatePath,
                    StandardCharsets.US_ASCII))) {
                Object object = parser.readObject();
                if (!(object instanceof X509CertificateHolder holder)) {
                    throw new IllegalStateException("Agent CA certificate PEM is invalid");
                }
                loaded = new JcaX509CertificateConverter().getCertificate(holder);
            }
            loaded.checkValidity(Date.from(clock.instant()));
            if (loaded.getBasicConstraints() < 0) throw new IllegalStateException("Agent CA certificate is not a CA");
            if (!KeyFactory.getInstance(key.getAlgorithm()).generatePublic(
                    new java.security.spec.X509EncodedKeySpec(loaded.getPublicKey().getEncoded()))
                    .equals(loaded.getPublicKey())) {
                throw new IllegalStateException("Agent CA key algorithm is inconsistent");
            }
            verifyKeyMatchesCertificate(key, loaded);
            return new AuthorityMaterial(key, loaded);
        } catch (Exception problem) {
            throw new IllegalStateException("Agent CA cannot be loaded", problem);
        }
    }

    private AuthorityMaterial create() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), random);
            KeyPair keys = generator.generateKeyPair();
            Instant now = clock.instant();
            Instant notBefore = now.minus(5, ChronoUnit.MINUTES);
            Instant notAfter = now.plus(caValidity);
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    CA_SUBJECT, serial(), Date.from(notBefore), Date.from(notAfter),
                    CA_SUBJECT, keys.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(keys.getPrivate());
            X509Certificate ca = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
            ca.verify(keys.getPublic());
            writeSecure(privateKeyPath, privateKeyPem(keys.getPrivate().getEncoded()));
            writeSecure(certificatePath, pem(ca));
            return new AuthorityMaterial(keys.getPrivate(), ca);
        } catch (Exception problem) {
            throw new IllegalStateException("Agent CA cannot be initialized", problem);
        }
    }

    private BigInteger serial() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        bytes[0] &= 0x7f;
        return new BigInteger(1, bytes).max(BigInteger.ONE);
    }

    private static void verifyKeyMatchesCertificate(PrivateKey key, X509Certificate ca) throws Exception {
        byte[] challenge = "wepush-agent-ca-key-check".getBytes(StandardCharsets.US_ASCII);
        java.security.Signature signature = java.security.Signature.getInstance("SHA256withECDSA");
        signature.initSign(key);
        signature.update(challenge);
        byte[] signed = signature.sign();
        signature.initVerify(ca.getPublicKey());
        signature.update(challenge);
        if (!signature.verify(signed)) throw new IllegalStateException("Agent CA key does not match certificate");
    }

    private static String pem(Object value) {
        try {
            StringWriter text = new StringWriter();
            try (JcaPEMWriter writer = new JcaPEMWriter(text)) {
                writer.writeObject(value);
            }
            return text.toString();
        } catch (IOException problem) {
            throw new IllegalStateException("PEM cannot be encoded", problem);
        }
    }

    private static String privateKeyPem(byte[] encoded) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded)
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static void writeSecure(Path path, String value) throws IOException {
        Path parent = path.getParent();
        if (parent == null) throw new IllegalArgumentException("Agent CA path has no parent");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".wepush-agent-ca-", ".tmp");
        try {
            secureOwnerOnly(temporary);
            Files.writeString(temporary, value, StandardCharsets.US_ASCII);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path);
            }
            secureOwnerOnly(path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void secureOwnerOnly(Path path) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            Files.setPosixFilePermissions(path, OWNER_ONLY);
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            AclEntry owner = AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
            acl.setAcl(List.of(owner));
        }
    }

    private static void verifyOwnerOnly(Path path) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null && Files.getPosixFilePermissions(path).stream().anyMatch(permission ->
                permission.name().startsWith("GROUP_") || permission.name().startsWith("OTHERS_"))) {
            throw new IllegalStateException("Agent CA private key permissions are unsafe");
        }
    }

    private static Path absolute(Path path, String label) {
        if (path == null) throw new IllegalArgumentException(label + " path is required");
        return path.toAbsolutePath().normalize();
    }

    private record AuthorityMaterial(PrivateKey privateKey, X509Certificate certificate) {
    }
}
