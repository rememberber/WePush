package com.fangxuele.wepush.next.service.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
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
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class LocalMasterKeyRing implements AutoCloseable {
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final String activeVersion;
    private final Map<String, byte[]> keys;

    private LocalMasterKeyRing(String activeVersion, Map<String, byte[]> keys) {
        this.activeVersion = activeVersion;
        this.keys = keys;
    }

    static LocalMasterKeyRing open(Path keyFile, String injectedBase64,
                                   boolean allowGeneration, boolean encryptedRecordsExist) {
        if (injectedBase64 != null && !injectedBase64.isBlank()) {
            return injected(injectedBase64);
        }
        Path path = keyFile.toAbsolutePath().normalize();
        if (Files.exists(path)) {
            verifyPermissions(path);
            return read(path);
        }
        if (!allowGeneration || encryptedRecordsExist) {
            throw new IllegalStateException("Secret Store master key is unavailable");
        }
        return generate(path);
    }

    String activeVersion() {
        return activeVersion;
    }

    SecretKey activeKey() {
        return key(activeVersion);
    }

    SecretKey key(String version) {
        byte[] encoded = keys.get(version);
        if (encoded == null) {
            throw new IllegalStateException("Secret Store key version is unavailable");
        }
        return new SecretKeySpec(encoded.clone(), "AES");
    }

    @Override
    public void close() {
        keys.values().forEach(value -> Arrays.fill(value, (byte) 0));
    }

    private static LocalMasterKeyRing injected(String base64) {
        byte[] key = decodeKey(base64);
        return new LocalMasterKeyRing("env-v1", new LinkedHashMap<>(Map.of("env-v1", key)));
    }

    private static LocalMasterKeyRing generate(Path path) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String version = "mk-1";
        KeyFile document = new KeyFile(version, Map.of(version, Base64.getEncoder().encodeToString(key)));
        writeSecurely(path, document);
        return new LocalMasterKeyRing(version, new LinkedHashMap<>(Map.of(version, key)));
    }

    private static LocalMasterKeyRing read(Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            KeyFile file = mapper.readValue(Files.readString(path, StandardCharsets.UTF_8), KeyFile.class);
            if (file.activeVersion() == null || file.activeVersion().isBlank()
                    || file.keys() == null || !file.keys().containsKey(file.activeVersion())) {
                throw new IllegalStateException("Secret Store key file is invalid");
            }
            Map<String, byte[]> decoded = new LinkedHashMap<>();
            file.keys().forEach((version, value) -> decoded.put(version, decodeKey(value)));
            return new LocalMasterKeyRing(file.activeVersion(), decoded);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Secret Store key file cannot be loaded", exception);
        }
    }

    private static byte[] decodeKey(String encoded) {
        try {
            byte[] key = Base64.getDecoder().decode(encoded);
            if (key.length != 32) {
                Arrays.fill(key, (byte) 0);
                throw new IllegalArgumentException("key length");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Secret Store master key is invalid", exception);
        }
    }

    private static void writeSecurely(Path path, KeyFile document) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalStateException("Secret Store key path has no parent directory");
        }
        try {
            Files.createDirectories(parent);
            Path temporary = createSecureTemporary(parent);
            try {
                byte[] bytes = new ObjectMapper().writeValueAsBytes(document);
                Files.write(temporary, bytes);
                Arrays.fill(bytes, (byte) 0);
                securePermissions(temporary);
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, path);
                }
                securePermissions(path);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Secret Store master key cannot be initialized", exception);
        }
    }

    private static Path createSecureTemporary(Path parent) throws IOException {
        if (Files.getFileAttributeView(parent, PosixFileAttributeView.class) != null) {
            return Files.createTempFile(parent, ".wepush-master-key-", ".tmp",
                    PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        }
        Path temporary = Files.createTempFile(parent, ".wepush-master-key-", ".tmp");
        securePermissions(temporary);
        return temporary;
    }

    private static void securePermissions(Path path) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            Files.setPosixFilePermissions(path, OWNER_ONLY);
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            UserPrincipal owner = Files.getOwner(path);
            AclEntry ownerOnly = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(owner)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            acl.setAcl(List.of(ownerOnly));
        }
    }

    private static void verifyPermissions(Path path) {
        try {
            PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (posix != null) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
                boolean unsafe = permissions.stream().anyMatch(permission ->
                        permission.name().startsWith("GROUP_") || permission.name().startsWith("OTHERS_"));
                if (unsafe || !permissions.contains(PosixFilePermission.OWNER_READ)) {
                    throw new IllegalStateException("Secret Store key file permissions are unsafe");
                }
                return;
            }
            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (acl != null) {
                String owner = Files.getOwner(path).getName().toLowerCase(Locale.ROOT);
                for (AclEntry entry : acl.getAcl()) {
                    String principal = entry.principal().getName().toLowerCase(Locale.ROOT);
                    boolean trusted = principal.equals(owner) || principal.endsWith("\\administrators")
                            || principal.endsWith("\\system");
                    if (entry.type() == AclEntryType.ALLOW && !trusted
                            && entry.permissions().contains(AclEntryPermission.READ_DATA)) {
                        throw new IllegalStateException("Secret Store key file permissions are unsafe");
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Secret Store key file permissions cannot be verified", exception);
        }
    }

    private record KeyFile(String activeVersion, Map<String, String> keys) {
    }
}
