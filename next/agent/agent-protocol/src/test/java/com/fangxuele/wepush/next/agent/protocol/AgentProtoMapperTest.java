package com.fangxuele.wepush.next.agent.protocol;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentProtoMapperTest {
    @Test
    void roundTripsHelloHeartbeatAndWelcomeFrames() {
        LeaseFence recovered = new LeaseFence("lease_1", "run_1", 2, "secret-fence");
        AgentFrames.AgentToService hello = new AgentFrames.AgentToService(new AgentId("agent_1"), 7,
                new AgentFrames.Hello("0.1.0", 1, 1, "Linux", "amd64", "21", 4, 3, 6,
                        List.of(new ProviderCapability("wepush.http", "0.1.0", 1, 32)),
                        "x25519-public-key", List.of(recovered)));
        AgentFrames.AgentToService heartbeat = new AgentFrames.AgentToService(new AgentId("agent_1"), 8,
                new AgentFrames.Heartbeat("READY", 1, 3,
                        List.of(recovered)));
        AgentFrames.ServiceToAgent welcome = new AgentFrames.ServiceToAgent(4,
                new AgentFrames.Welcome(1, Instant.parse("2026-08-22T10:00:00Z"), 10,
                        1_048_576, 8, List.of(recovered)));

        assertEquals(hello, AgentProtoMapper.fromProto(AgentProtoMapper.toProto(hello)));
        assertEquals(heartbeat, AgentProtoMapper.fromProto(AgentProtoMapper.toProto(heartbeat)));
        assertEquals(welcome, AgentProtoMapper.fromProto(AgentProtoMapper.toProto(welcome)));
    }
}
