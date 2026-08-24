package com.fangxuele.tool.push.logic.carriersms;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.cmpp.wap.LongMessageFrameHolder;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitResponseMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitRespMessage;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import com.zx.sms.codec.smpp.msg.SubmitSmResp;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPClientEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipClientEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPClientEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPClientEndpointEntity;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CarrierSmsProtocolSupportTest {

    @After
    public void tearDown() {
        CarrierSmsSessionRegistry.resetForTests();
    }

    @Test
    public void createsEndpointForEveryProtocol() {
        assertTrue(CarrierSmsEndpointFactory.create(1, config(CarrierSmsProtocol.CMPP)) instanceof CMPPClientEndpointEntity);
        assertTrue(CarrierSmsEndpointFactory.create(1, config(CarrierSmsProtocol.SMGP)) instanceof SMGPClientEndpointEntity);
        assertTrue(CarrierSmsEndpointFactory.create(1, config(CarrierSmsProtocol.SGIP)) instanceof SgipClientEndpointEntity);
        assertTrue(CarrierSmsEndpointFactory.create(1, config(CarrierSmsProtocol.SMPP)) instanceof SMPPClientEndpointEntity);

        EndpointEntity endpoint = CarrierSmsEndpointFactory.create(42, config(CarrierSmsProtocol.CMPP));
        assertEquals("wepush-carrier-42", endpoint.getId());
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals(Integer.valueOf(7890), endpoint.getPort());
        assertEquals(2, endpoint.getWindow());
        assertEquals(1, endpoint.getMaxChannels());
        assertEquals(30, endpoint.getIdleTimeSec());
        assertEquals(0, endpoint.getMaxRetryCnt());
        assertEquals(EndpointEntity.SupportLongMessage.SEND, endpoint.getSupportLongmsg());
        assertFalse(endpoint.toString().contains("secret"));
    }

    @Test
    public void appliesProtocolSpecificDefaults() {
        CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
        config.setProtocol(CarrierSmsProtocol.SMPP);
        config.applyDefaults();
        assertEquals(2775, config.getPort());
        assertEquals("3.4", config.getVersion());
        assertEquals(30, config.getHeartbeatIntervalSeconds());
    }

    @Test
    public void validatesAndRoundTripsProtocolSpecificConfiguration() {
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            CarrierSmsAccountConfig source = config(protocol);
            CarrierSmsAccountConfig restored = JSON.parseObject(JSON.toJSONString(source), CarrierSmsAccountConfig.class);
            assertEquals(protocol, restored.getProtocol());
            assertTrue(restored.validate().toString(), restored.validate().isEmpty());
            assertEquals(source.connectionFingerprint(), restored.connectionFingerprint());
            assertFalse(restored.toString().contains("secret"));
        }

        CarrierSmsAccountConfig invalidSgip = config(CarrierSmsProtocol.SGIP);
        invalidSgip.setCorpId("");
        assertTrue(invalidSgip.validate().stream().anyMatch(error -> error.contains("CorpId")));

        CarrierSmsAccountConfig invalidSmpp = config(CarrierSmsProtocol.SMPP);
        invalidSmpp.setPassword("123456789");
        assertTrue(invalidSmpp.validate().stream().anyMatch(error -> error.contains("SMPP 登录密码")));

        String legacyJson = JSON.toJSONString(config(CarrierSmsProtocol.CMPP));
        legacyJson = legacyJson.substring(0, legacyJson.length() - 1) + ",\"registeredDelivery\":true}";
        CarrierSmsAccountConfig legacy = JSON.parseObject(legacyJson, CarrierSmsAccountConfig.class);
        try {
            CmppSubmitRequestMessage request = (CmppSubmitRequestMessage) CarrierSmsRequestFactory.create(
                    legacy, "13800138000", "兼容旧配置");
            assertEquals(0, request.getRegisteredDelivery());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void mapsSubmitFieldsForEveryProtocol() throws Exception {
        CarrierSmsAccountConfig cmppConfig = config(CarrierSmsProtocol.CMPP);
        CmppSubmitRequestMessage cmpp = (CmppSubmitRequestMessage) CarrierSmsRequestFactory.create(cmppConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, cmpp.getDestterminalId());
        assertEquals("10690000", cmpp.getSrcId());
        assertEquals("service", cmpp.getServiceId());
        assertEquals("corp", cmpp.getMsgsrc());
        assertEquals(0, cmpp.getRegisteredDelivery());
        assertEquals("你好", cmpp.getMsgContent());

        CarrierSmsAccountConfig smgpConfig = config(CarrierSmsProtocol.SMGP);
        SMGPSubmitMessage smgp = (SMGPSubmitMessage) CarrierSmsRequestFactory.create(smgpConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, smgp.getDestTermIdArray());
        assertEquals("10690000", smgp.getSrcTermId());
        assertEquals("service", smgp.getServiceId());
        assertFalse(smgp.isNeedReport());
        assertEquals("你好", smgp.getMsgContent());

        CarrierSmsAccountConfig sgipConfig = config(CarrierSmsProtocol.SGIP);
        SgipSubmitRequestMessage sgip = (SgipSubmitRequestMessage) CarrierSmsRequestFactory.create(sgipConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, sgip.getUsernumber());
        assertEquals("10690000", sgip.getSpnumber());
        assertEquals("corp", sgip.getCorpid());
        assertEquals("service", sgip.getServicetype());
        assertEquals(0, sgip.getReportflag());
        assertEquals("你好", sgip.getMsgContent());

        CarrierSmsAccountConfig smppConfig = config(CarrierSmsProtocol.SMPP);
        SubmitSm smpp = (SubmitSm) CarrierSmsRequestFactory.create(smppConfig, "13800138000", "你好");
        assertEquals("10690000", smpp.getSourceAddress().getAddress());
        assertEquals("13800138000", smpp.getDestAddress().getAddress());
        assertEquals("svc", smpp.getServiceType());
        assertEquals(0, smpp.getRegisteredDelivery());
        assertEquals("你好", smpp.getMsgContent());
    }

    @Test
    public void splitsLongTextWithoutExcludedWapDependencies() throws Exception {
        String content = "这是一条需要分片的中文短信".repeat(20);
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            CarrierSmsAccountConfig config = config(protocol);
            BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", content);
            List<BaseMessage> fragments = LongMessageFrameHolder.INS.splitLongSmsMessage(
                    CarrierSmsEndpointFactory.create(1, config), request);
            assertTrue(protocol + " should split a long message", fragments.size() > 1);
        }
    }

    @Test
    public void parsesSuccessAndFailureResponses() {
        CmppSubmitResponseMessage cmpp = new CmppSubmitResponseMessage(1);
        cmpp.setResult(0);
        assertTrue(CarrierSmsResponseParser.parse(CarrierSmsProtocol.CMPP, List.of(cmpp)).success());
        cmpp.setResult(8);
        CarrierSmsSubmitResult cmppFailure = CarrierSmsResponseParser.parse(CarrierSmsProtocol.CMPP, List.of(cmpp));
        assertFalse(cmppFailure.success());
        assertTrue(cmppFailure.info().contains("流量控制"));

        SMGPSubmitRespMessage smgp = new SMGPSubmitRespMessage();
        smgp.setStatus(0);
        assertTrue(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMGP, List.of(smgp)).success());
        smgp.setStatus(1);
        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMGP, List.of(smgp)).success());

        SgipSubmitResponseMessage sgip = new SgipSubmitResponseMessage();
        sgip.setResult((short) 0);
        assertTrue(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SGIP, List.of(sgip)).success());
        sgip.setResult((short) 1);
        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SGIP, List.of(sgip)).success());

        SubmitSmResp smpp = new SubmitSmResp();
        smpp.setCommandStatus(0);
        assertTrue(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMPP, List.of(smpp)).success());
        smpp.setCommandStatus(5);
        CarrierSmsSubmitResult smppFailure = CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMPP, List.of(smpp));
        assertFalse(smppFailure.success());
        assertTrue(smppFailure.info().contains("0x5"));

        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMPP, List.of(cmpp)).success());
        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.CMPP, List.of()).success());
    }

    @Test
    public void sharesOneClientPerAccountUnderConcurrency() throws Exception {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        CarrierSmsSessionRegistry.setClientFactoryForTests((accountId, config) -> {
            created.incrementAndGet();
            return new FakeClient(closed);
        });
        CarrierSmsAccountConfig config = config(CarrierSmsProtocol.CMPP);
        BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", "你好");
        int workers = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        for (int i = 0; i < workers; i++) {
            executor.submit(() -> {
                start.await();
                CarrierSmsSessionRegistry.submit(7, config, request);
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, created.get());

        CarrierSmsSessionRegistry.invalidate(7);
        assertEquals(1, closed.get());

        CarrierSmsSessionRegistry.submit(7, config, request);
        assertEquals(2, created.get());
        config.setWindowSize(3);
        CarrierSmsSessionRegistry.submit(7, config, request);
        assertEquals(3, created.get());
        assertEquals(2, closed.get());

        CarrierSmsSessionRegistry.shutdown();
        assertEquals(3, closed.get());
    }

    @Test
    public void invalidatesFailedClientAndDoesNotCloseDuringInflightSubmit() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CarrierSmsSessionRegistry.setClientFactoryForTests((accountId, config) ->
                new BlockingClient(closed, entered, release));
        CarrierSmsAccountConfig config = config(CarrierSmsProtocol.CMPP);
        BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", "并发关闭测试");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<BaseMessage>> submitting = executor.submit(() ->
                    CarrierSmsSessionRegistry.submit(8, config, request));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Future<?> invalidating = executor.submit(() -> CarrierSmsSessionRegistry.invalidate(8));
            assertThrows(TimeoutException.class, () -> invalidating.get(100, TimeUnit.MILLISECONDS));
            assertEquals(0, closed.get());

            release.countDown();
            assertFalse(submitting.get(2, TimeUnit.SECONDS).isEmpty());
            invalidating.get(2, TimeUnit.SECONDS);
            assertEquals(1, closed.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        AtomicInteger failedClosed = new AtomicInteger();
        CarrierSmsSessionRegistry.setClientFactoryForTests((accountId, ignored) -> new CarrierSmsGatewayClient() {
            @Override
            public void connect(int timeoutMillis) {
            }

            @Override
            public List<BaseMessage> submit(BaseMessage ignoredRequest, int timeoutMillis) throws Exception {
                throw new IOException("network failed");
            }

            @Override
            public void close() {
                failedClosed.incrementAndGet();
            }
        });
        assertThrows(IOException.class, () -> CarrierSmsSessionRegistry.submit(9, config, request));
        assertEquals(1, failedClosed.get());
    }

    @Test
    public void keepsExistingClientWhenReplacementCreationFails() throws Exception {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        CarrierSmsSessionRegistry.setClientFactoryForTests((accountId, config) -> {
            if (created.incrementAndGet() == 2) {
                throw new IllegalStateException("create failed");
            }
            return new FakeClient(closed);
        });
        CarrierSmsAccountConfig config = config(CarrierSmsProtocol.CMPP);
        BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", "替换失败测试");
        CarrierSmsSessionRegistry.submit(10, config, request);

        config.setWindowSize(3);
        assertThrows(IllegalStateException.class, () -> CarrierSmsSessionRegistry.submit(10, config, request));
        assertEquals(0, closed.get());

        config.setWindowSize(2);
        CarrierSmsSessionRegistry.submit(10, config, request);
        assertEquals(2, created.get());
        assertEquals(0, closed.get());
        CarrierSmsSessionRegistry.invalidate(10);
        assertEquals(1, closed.get());
    }

    @Test(timeout = 15000)
    public void realClientHonorsLoginTimeoutWithoutJvmOpens() throws Exception {
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            ExecutorService acceptor = Executors.newSingleThreadExecutor();
            try (ServerSocket server = new ServerSocket(0)) {
                FutureSocket accepted = new FutureSocket(acceptor.submit(server::accept));
                CarrierSmsAccountConfig config = config(protocol);
                config.setPort(server.getLocalPort());
                config.setRequestTimeoutMillis(300);
                SmsClientCarrierGatewayClient client = new SmsClientCarrierGatewayClient(100 + protocol.ordinal(), config);
                BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", "超时测试");
                long startedAt = System.nanoTime();
                try {
                    client.submit(request, config.getRequestTimeoutMillis());
                    fail("expected timeout for " + protocol);
                } catch (TimeoutException expected) {
                    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                    assertTrue(protocol + " timeout took too long: " + elapsedMillis, elapsedMillis < 3000);
                } finally {
                    accepted.close();
                    client.close();
                }
            } finally {
                acceptor.shutdownNow();
            }
        }
    }

    private static CarrierSmsAccountConfig config(CarrierSmsProtocol protocol) {
        CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
        config.setProtocol(protocol);
        config.setHost("127.0.0.1");
        config.setPort(protocol.getDefaultPort());
        config.setUsername("client");
        config.setPassword("secret");
        config.setVersion(protocol.getDefaultVersion());
        config.setMaxChannels(1);
        config.setWindowSize(2);
        config.setRequestTimeoutMillis(1000);
        config.setSourceAddress("10690000");
        config.setServiceId(protocol == CarrierSmsProtocol.SMPP ? "svc" : "service");
        config.setMsgSrc("corp");
        config.setCorpId("corp");
        config.setNodeId(123456789L);
        return config;
    }

    private static final class FakeClient implements CarrierSmsGatewayClient {
        private final AtomicInteger closed;

        private FakeClient(AtomicInteger closed) {
            this.closed = closed;
        }

        @Override
        public void connect(int timeoutMillis) {
        }

        @Override
        public List<BaseMessage> submit(BaseMessage request, int timeoutMillis) {
            CmppSubmitResponseMessage response = new CmppSubmitResponseMessage(1);
            response.setResult(0);
            return List.of(response);
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }

    private static final class BlockingClient implements CarrierSmsGatewayClient {
        private final AtomicInteger closed;
        private final CountDownLatch entered;
        private final CountDownLatch release;

        private BlockingClient(AtomicInteger closed, CountDownLatch entered, CountDownLatch release) {
            this.closed = closed;
            this.entered = entered;
            this.release = release;
        }

        @Override
        public void connect(int timeoutMillis) {
        }

        @Override
        public List<BaseMessage> submit(BaseMessage request, int timeoutMillis) throws Exception {
            entered.countDown();
            assertTrue(release.await(2, TimeUnit.SECONDS));
            CmppSubmitResponseMessage response = new CmppSubmitResponseMessage(1);
            response.setResult(0);
            return List.of(response);
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }

    private record FutureSocket(java.util.concurrent.Future<Socket> future) {
        private void close() throws Exception {
            Socket socket = future.get(2, TimeUnit.SECONDS);
            socket.close();
        }
    }
}
