package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ApiAccessServiceTest {
    @Test
    void ordinaryTokensDoNotSuppressBootstrapAdministratorInitialization() {
        InMemoryAccess access = new InMemoryAccess();
        ApiAccessService service = new ApiAccessService(access, null, prefix -> prefix + "_1",
                new DirectTransactions(),
                Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneOffset.UTC),
                new SecureRandom());

        service.initializeBootstrap("bootstrap-token-that-is-at-least-32-characters");

        assertNotNull(access.principal);
        assertEquals(ApiAccessRepository.SystemRole.SYSTEM_ADMIN,
                access.principal.systemRole());
        assertEquals("principal_bootstrap", access.binding.principalId());
        assertEquals("token_bootstrap", access.token.id());
    }

    private static final class InMemoryAccess implements ApiAccessRepository {
        private Principal principal;
        private RoleBinding binding;
        private ApiToken token;

        @Override
        public Optional<AccessIdentity> authenticate(String tokenHash, Instant now) {
            return Optional.empty();
        }

        @Override
        public boolean hasSystemAdministrator() {
            return principal != null && principal.systemRole() == SystemRole.SYSTEM_ADMIN;
        }

        @Override
        public void createPrincipal(Principal principal) {
            this.principal = principal;
        }

        @Override
        public void createToken(ApiToken token) {
            this.token = token;
        }

        @Override
        public void bindRole(RoleBinding binding) {
            this.binding = binding;
        }

        @Override
        public void touchToken(String tokenId, Instant usedAt) {
        }

        @Override
        public List<ApiToken> listTokens(String workspaceId) {
            return List.of();
        }

        @Override
        public boolean revokeToken(String tokenId, String workspaceId, Instant revokedAt) {
            return false;
        }
    }

    private static final class DirectTransactions implements TransactionRunner {
        @Override
        public <T> T required(Supplier<T> work) {
            return work.get();
        }
    }
}
