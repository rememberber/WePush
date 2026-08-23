package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileArtifactStoreTest {
    @TempDir
    Path directory;

    @Test
    void writesAtomicallyChecksumsSupportsRangesAndDeletesIdempotently() throws Exception {
        LocalFileArtifactStore store = new LocalFileArtifactStore(directory, "standalone");
        ArtifactStore.ObjectPlan plan = store.plan(new WorkspaceId("ws_default"),
                "artifact_1", "RUN_RESULTS_CSV", Instant.parse("2026-08-22T10:00:00Z"));
        assertEquals("standalone/ws_default/run-results-csv/2026/08/artifact_1", plan.location());

        ArtifactStore.StoredObject stored = store.write(plan,
                output -> output.write("0123456789".getBytes(StandardCharsets.UTF_8)));
        assertEquals(10, stored.size());
        assertEquals("84d89877f0d4041efb6bf91a16f0248f2fd573e6af05c19f96bedb9f882f7882",
                stored.sha256());
        assertEquals(stored, store.inspect(plan.location()));
        try (InputStream range = store.open(plan.location(), 2, 4)) {
            assertEquals("2345", new String(range.readAllBytes(), StandardCharsets.UTF_8));
        }

        Path object = directory.resolve(plan.location());
        assertTrue(Files.isRegularFile(object));
        if (Files.getFileAttributeView(object, PosixFileAttributeView.class) != null) {
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(object));
        }
        assertThrows(IllegalArgumentException.class, () -> store.open("../outside", 0, 1));

        store.delete(plan.location());
        store.delete(plan.location());
        assertThrows(java.nio.file.NoSuchFileException.class, () -> store.open(plan.location(), 0, 1));
    }
}
