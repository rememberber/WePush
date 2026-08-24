package com.fangxuele.tool.push.logic.carriersms;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        assertFalse(endpoint.toString().contains("secret"));
    }

    @Test
    public void appliesProtocolSpecificDefaults() {
        CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
        config.setProtocol(CarrierSmsProtocol.SMPP);
        config.applyDefaults();
        assertEquals(2775, config.getPort());
        assertEquals("3.4", config.getVersion());
    }

    @Test
    public void mapsSubmitFieldsForEveryProtocol() throws Exception {
        CarrierSmsAccountConfig cmppConfig = config(CarrierSmsProtocol.CMPP);
        CmppSubmitRequestMessage cmpp = (CmppSubmitRequestMessage) CarrierSmsRequestFactory.create(cmppConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, cmpp.getDestterminalId());
        assertEquals("10690000", cmpp.getSrcId());
        assertEquals("service", cmpp.getServiceId());
        assertEquals("corp", cmpp.getMsgsrc());
        assertEquals(1, cmpp.getRegisteredDelivery());
        assertEquals("你好", cmpp.getMsgContent());

        CarrierSmsAccountConfig smgpConfig = config(CarrierSmsProtocol.SMGP);
        SMGPSubmitMessage smgp = (SMGPSubmitMessage) CarrierSmsRequestFactory.create(smgpConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, smgp.getDestTermIdArray());
        assertEquals("10690000", smgp.getSrcTermId());
        assertEquals("service", smgp.getServiceId());
        assertTrue(smgp.isNeedReport());
        assertEquals("你好", smgp.getMsgContent());

        CarrierSmsAccountConfig sgipConfig = config(CarrierSmsProtocol.SGIP);
        SgipSubmitRequestMessage sgip = (SgipSubmitRequestMessage) CarrierSmsRequestFactory.create(sgipConfig, "13800138000", "你好");
        assertArrayEquals(new String[]{"13800138000"}, sgip.getUsernumber());
        assertEquals("10690000", sgip.getSpnumber());
        assertEquals("corp", sgip.getCorpid());
        assertEquals("service", sgip.getServicetype());
        assertEquals(1, sgip.getReportflag());
        assertEquals("你好", sgip.getMsgContent());

        CarrierSmsAccountConfig smppConfig = config(CarrierSmsProtocol.SMPP);
        SubmitSm smpp = (SubmitSm) CarrierSmsRequestFactory.create(smppConfig, "13800138000", "你好");
        assertEquals("10690000", smpp.getSourceAddress().getAddress());
        assertEquals("13800138000", smpp.getDestAddress().getAddress());
        assertEquals("service", smpp.getServiceType());
        assertEquals(1, smpp.getRegisteredDelivery());
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
        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.CMPP, List.of(cmpp)).success());

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
        assertFalse(CarrierSmsResponseParser.parse(CarrierSmsProtocol.SMPP, List.of(smpp)).success());

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
        config.setServiceId("service");
        config.setMsgSrc("corp");
        config.setCorpId("corp");
        config.setNodeId(123456789L);
        config.setRegisteredDelivery(true);
        return config;
    }

    private static final class FakeClient implements CarrierSmsGatewayClient {
        private final AtomicInteger closed;

        private FakeClient(AtomicInteger closed) {
            this.closed = closed;
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

    private record FutureSocket(java.util.concurrent.Future<Socket> future) {
        private void close() throws Exception {
            Socket socket = future.get(2, TimeUnit.SECONDS);
            socket.close();
        }
    }
}
