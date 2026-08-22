package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;

@FunctionalInterface
public interface AgentControlGateway {
    boolean send(String agentId, AgentFrames.ServicePayload payload);
}
