package com.fangxuele.tool.push.logic.carriersms;

import com.zx.sms.BaseMessage;

import java.util.List;

/** 隔离 Classic 业务层与具体协议库，便于测试和后续替换实现。 */
public interface CarrierSmsGatewayClient extends AutoCloseable {
    void connect(int timeoutMillis) throws Exception;

    List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception;

    @Override
    void close() throws Exception;
}
