import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Source-file-mode utility used by package-carrier-provider.sh; requires only Java 21+. */
final class PluginPackager {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 4) throw new IllegalArgumentException(
                "Usage: PluginPackager <package-directory> <archive> <protocol> <version>");
        Path root = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path archive = Path.of(arguments[1]).toAbsolutePath().normalize();
        String protocol = token(arguments[2], "protocol");
        String version = version(arguments[3]);
        String keyId = token(requiredEnvironment("WEPUSH_PLUGIN_SIGNING_KEY_ID"), "key id");
        byte[] privateKey = Base64.getDecoder().decode(
                requiredEnvironment("WEPUSH_PLUGIN_SIGNING_KEY_PKCS8_BASE64"));

        Map<String, String> digests = new LinkedHashMap<>();
        for (Path file : files(root)) {
            String name = root.relativize(file).toString().replace('\\', '/');
            if (!name.equals("plugin.json") && !name.equals("signature.ed25519")) {
                digests.put(name, HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
            }
        }
        byte[] manifest = manifest(protocol, version, keyId, digests).getBytes(StandardCharsets.UTF_8);
        Files.write(root.resolve("plugin.json"), manifest);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey)));
        signer.update(manifest);
        Files.writeString(root.resolve("signature.ed25519"),
                Base64.getEncoder().encodeToString(signer.sign()), StandardCharsets.US_ASCII);

        Files.createDirectories(archive.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Path file : files(root)) {
                String name = root.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L);
                output.putNextEntry(entry);
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        String archiveDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive)));
        Files.writeString(Path.of(archive + ".sha256"), archiveDigest + "  " + archive.getFileName() + "\n",
                StandardCharsets.US_ASCII);
        System.out.println(archive);
    }

    private static List<Path> files(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .toList();
        }
    }

    private static String manifest(String protocol, String version, String keyId, Map<String, String> files) {
        List<String> entries = new ArrayList<>();
        files.forEach((name, digest) -> entries.add(quote(name) + ":" + quote(digest)));
        return "{\"pluginId\":" + quote("wepush-provider-" + protocol)
                + ",\"version\":" + quote(version) + ",\"spiMajor\":1,\"signatureKeyId\":" + quote(keyId)
                + ",\"files\":{" + String.join(",", entries) + "}}";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String token(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,120}")) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return value;
    }

    private static String version(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._+-]{1,80}")) {
            throw new IllegalArgumentException("version is invalid");
        }
        return value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value.trim();
    }
}
