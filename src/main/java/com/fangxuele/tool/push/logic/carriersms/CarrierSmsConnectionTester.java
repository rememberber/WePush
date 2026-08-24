package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** 使用临时客户端仅测试 TCP 连接和协议登录，不发送短信。 */
public final class CarrierSmsConnectionTester {
    private static final AtomicInteger TEST_ID = new AtomicInteger(-1);

    private CarrierSmsConnectionTester() {
    }

    public static CarrierSmsConnectionTestResult test(CarrierSmsAccountConfig config) {
        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            return new CarrierSmsConnectionTestResult(false, String.join("；", errors), 0);
        }

        long startedAt = System.nanoTime();
        try (SmsClientCarrierGatewayClient client = new SmsClientCarrierGatewayClient(TEST_ID.getAndDecrement(), config)) {
            client.connect(config.getRequestTimeoutMillis());
            return new CarrierSmsConnectionTestResult(true, config.getProtocol() + " 网关登录成功", elapsedMillis(startedAt));
        } catch (Exception e) {
            return new CarrierSmsConnectionTestResult(false,
                    CarrierSmsErrorTranslator.connectionFailure(config.getProtocol(), e), elapsedMillis(startedAt));
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
