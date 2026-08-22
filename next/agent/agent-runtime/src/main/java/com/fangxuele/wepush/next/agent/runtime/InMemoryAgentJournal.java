package com.fangxuele.wepush.next.agent.runtime;

import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryAgentJournal implements AgentJournal {
    private final AtomicReference<AgentJournalState> state = new AtomicReference<>(AgentJournalState.empty());

    @Override
    public AgentJournalState load() {
        return state.get();
    }

    @Override
    public void save(AgentJournalState state) {
        this.state.set(state);
    }
}
