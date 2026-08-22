package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.core.api.SecretRef;
import com.fangxuele.wepush.next.core.api.SecretValue;
import com.fangxuele.wepush.next.service.application.SecretMetadata;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalEnvelopeSecretStoreTest {
    @TempDir
    Path directory;

    @Test
    void encryptsAtRestUpdatesVersionAndFailsClosedWithoutMasterKey() throws Exception {
        Path database = directory.resolve("wepush.db");
        Path keyFile = directory.resolve("secrets/master-key.json");
        WorkspaceId workspace = new WorkspaceId("ws_default");
        SecretRef ref = new SecretRef("http", "authorization", "v1");

        try (HikariDataSource dataSource = SQLiteDatabase.create(database)) {
            Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration/sqlite").load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            try (LocalEnvelopeSecretStore store = new LocalEnvelopeSecretStore(
                    jdbc, keyFile, "", true, Clock.systemUTC())) {
                SecretMetadata created = store.put(workspace, ref, "first-value".toCharArray());
                SecretMetadata replaced = store.put(workspace, ref, "second-value".toCharArray());

                assertEquals(1, created.recordVersion());
                assertEquals(2, replaced.recordVersion());
                try (SecretValue resolved = store.resolve(workspace, ref)) {
                    char[] clear = resolved.copyChars();
                    try {
                        assertArrayEquals("second-value".toCharArray(), clear);
                    } finally {
                        Arrays.fill(clear, '\0');
                    }
                }

                byte[] ciphertext = jdbc.queryForObject("""
                        SELECT ciphertext FROM secret_record
                        WHERE workspace_id = ? AND secret_namespace = ? AND secret_name = ?
                          AND secret_version = ?
                        """, byte[].class, workspace.value(), ref.namespace(), ref.name(), ref.version());
                assertFalse(new String(ciphertext, StandardCharsets.UTF_8).contains("second-value"));
            }

            PosixFileAttributeView posix = Files.getFileAttributeView(keyFile, PosixFileAttributeView.class);
            if (posix != null) {
                assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                        Files.getPosixFilePermissions(keyFile));
            }

            Files.delete(keyFile);
            assertThrows(IllegalStateException.class, () -> new LocalEnvelopeSecretStore(
                    jdbc, keyFile, "", true, Clock.systemUTC()));
        }
    }
}
