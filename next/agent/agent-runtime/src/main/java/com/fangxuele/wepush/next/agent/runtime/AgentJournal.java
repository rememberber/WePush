package com.fangxuele.wepush.next.agent.runtime;

public interface AgentJournal {
    AgentJournalState load();

    void save(AgentJournalState state);
}
