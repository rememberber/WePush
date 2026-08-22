package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.agent.protocol.AgentFrames;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import com.fangxuele.wepush.next.service.application.AgentControlGateway;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class AgentStreamGateway implements AgentControlGateway {
    private final AgentApplicationService agents;
    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    AgentStreamGateway(AgentApplicationService agents) {
        this.agents = agents;
    }

    void register(AgentApplicationService.Connection connection,
                  Consumer<AgentFrames.ServiceToAgent> sender,
                  Runnable replaced) {
        Channel channel = new Channel(connection, sender, replaced);
        Channel previous = channels.put(connection.registration().id(), channel);
        if (previous != null && previous != channel) previous.replaced.run();
    }

    void unregister(AgentApplicationService.Connection connection) {
        if (connection != null) {
            channels.computeIfPresent(connection.registration().id(), (_id, current) ->
                    current.connection == connection ? null : current);
        }
    }

    @Override
    public boolean send(String agentId, AgentFrames.ServicePayload payload) {
        Channel channel = channels.get(agentId);
        if (channel == null) return false;
        try {
            channel.send(agents.next(channel.connection, payload));
            return true;
        } catch (RuntimeException problem) {
            return false;
        }
    }

    private static final class Channel {
        private final AgentApplicationService.Connection connection;
        private final Consumer<AgentFrames.ServiceToAgent> sender;
        private final Runnable replaced;

        private Channel(AgentApplicationService.Connection connection,
                        Consumer<AgentFrames.ServiceToAgent> sender,
                        Runnable replaced) {
            this.connection = connection;
            this.sender = sender;
            this.replaced = replaced;
        }

        private synchronized void send(AgentFrames.ServiceToAgent frame) {
            sender.accept(frame);
        }
    }
}
