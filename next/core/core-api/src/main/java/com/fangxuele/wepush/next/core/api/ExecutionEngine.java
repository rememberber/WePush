package com.fangxuele.wepush.next.core.api;

public interface ExecutionEngine extends AutoCloseable {
    RunHandle start(RunExecutionSpec spec, ExecutionPorts ports);

    @Override
    void close();
}
