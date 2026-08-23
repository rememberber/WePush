package com.fangxuele.wepush.next.service.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DeploymentValidationTest {
    @Test
    void allowsLocalStandaloneWithoutApiSecurity() {
        assertDoesNotThrow(() -> ServiceComposition.validateDeployment(
                "standalone", "sqlite", "local", false, false, "127.0.0.1"));
    }

    @Test
    void rejectsUnauthenticatedHttpOnANetworkInterface() {
        assertThrows(IllegalStateException.class, () -> ServiceComposition.validateDeployment(
                "standalone", "sqlite", "local", false, false, "0.0.0.0"));
    }

    @Test
    void requiresTheCompleteSharedServerBaseline() {
        assertThrows(IllegalStateException.class, () -> ServiceComposition.validateDeployment(
                "server", "postgresql", "local", true, true, "0.0.0.0"));
        assertDoesNotThrow(() -> ServiceComposition.validateDeployment(
                "server", "postgresql", "s3", true, true, "0.0.0.0"));
    }
}
