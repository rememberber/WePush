import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;

/** Generates an Ed25519 key pair in the encodings consumed by Agent plugin tooling. */
final class GeneratePluginSigningKey {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) throw new IllegalArgumentException(
                "Usage: GeneratePluginSigningKey <private-pkcs8-base64-file> <public-x509-base64-file>");
        Path privateFile = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path publicFile = Path.of(arguments[1]).toAbsolutePath().normalize();
        if (privateFile.equals(publicFile)) throw new IllegalArgumentException("Key outputs must differ");
        if (privateFile.getParent() != null) Files.createDirectories(privateFile.getParent());
        if (publicFile.getParent() != null) Files.createDirectories(publicFile.getParent());

        var pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Files.writeString(privateFile, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                StandardCharsets.US_ASCII);
        Files.writeString(publicFile, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                StandardCharsets.US_ASCII);
        try {
            Files.setPosixFilePermissions(privateFile,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            Files.setPosixFilePermissions(publicFile,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // ACL-based platforms retain their normal user-owned file permissions.
        }
        System.out.println("Ed25519 key files created");
    }
}
