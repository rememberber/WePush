package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

final class AgentHttpAuthenticatorTest {
    @Test
    void permitsAnonymousDevelopmentOnlyOnLoopback() {
        AgentIdentityService identities = mock(AgentIdentityService.class);
        AgentLeaseRepository leases = mock(AgentLeaseRepository.class);

        assertEquals("", new AgentHttpAuthenticator(
                identities, leases, "", "127.0.0.1").authenticate(null, null));
        assertThrows(ResponseStatusException.class, () -> new AgentHttpAuthenticator(
                identities, leases, "", "0.0.0.0").authenticate(null, null));
    }
}
