package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitResponseMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitRespMessage;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import com.zx.sms.codec.smpp.msg.SubmitSmResp;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.cmpp.CMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipServerChildEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipServerEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPServerChildEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPServerEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPServerEndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 使用 sms-core 本地服务端完成真实编解码、登录、分片和 SUBMIT_RESP 往返。 */
public class CarrierSmsLocalGatewayIntegrationTest {

    @Test(timeout = 60000)
    public void logsInAndSubmitsShortAndLongTextForAllProtocols() throws Exception {
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            try (LocalGateway gateway = LocalGateway.start(protocol)) {
                CarrierSmsAccountConfig config = config(protocol, gateway.port());
                CarrierSmsConnectionTestResult connectionResult = CarrierSmsConnectionTester.test(config);
                assertTrue(protocol + " test login failed: " + connectionResult.info(), connectionResult.success());
                try (SmsClientCarrierGatewayClient client = new SmsClientCarrierGatewayClient(200 + protocol.ordinal(), config)) {
                    client.connect(5000);

                    BaseMessage shortRequest = CarrierSmsRequestFactory.create(config, "13800138000", "中文短信");
                    CarrierSmsSubmitResult shortResult = CarrierSmsResponseParser.parse(protocol, client.submit(shortRequest, 5000));
                    assertTrue(protocol + " short submit failed: " + shortResult.info(), shortResult.success());

                    BaseMessage longRequest = CarrierSmsRequestFactory.create(config, "13800138000",
                            "这是一条用于验证真实编解码和长短信分片的中文短信".repeat(20));
                    List<BaseMessage> longResponses = client.submit(longRequest, 10000);
                    CarrierSmsSubmitResult longResult = CarrierSmsResponseParser.parse(protocol, longResponses);
                    assertTrue(protocol + " long submit failed: " + longResult.info(), longResult.success());
                    assertTrue(protocol + " did not split long text", longResponses.size() > 1);
                }
                assertTrue(protocol + " server did not receive all submits", gateway.submitCount() > 2);
                assertFalse(protocol + " requested DLR", gateway.reportRequested());
            }
        }
    }

    @Test(timeout = 30000)
    public void rejectsInvalidCredentialsWithoutLeakingSecrets() throws Exception {
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            try (LocalGateway gateway = LocalGateway.start(protocol)) {
                CarrierSmsAccountConfig config = config(protocol, gateway.port());
                config.setPassword("badpass");
                CarrierSmsConnectionTestResult result = CarrierSmsConnectionTester.test(config);
                assertFalse(protocol + " unexpectedly accepted invalid credentials", result.success());
                assertFalse(result.info().contains("badpass"));
                assertTrue(result.info().contains("登录失败") || result.info().contains("拒绝登录"));
            }
        }
    }

    private static CarrierSmsAccountConfig config(CarrierSmsProtocol protocol, int port) {
        CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
        config.setProtocol(protocol);
        config.setHost("127.0.0.1");
        config.setPort(port);
        config.setUsername("client");
        config.setPassword("secret");
        config.setVersion(protocol.getDefaultVersion());
        config.setMaxChannels(1);
        config.setWindowSize(4);
        config.setRequestTimeoutMillis(5000);
        config.setHeartbeatIntervalSeconds(30);
        config.setSourceAddress("10690000");
        config.setServiceId(protocol == CarrierSmsProtocol.SMPP ? "svc" : "service");
        config.setMsgSrc("corp");
        config.setCorpId("corp");
        config.setNodeId(123456789L);
        return config;
    }

    private static final class LocalGateway implements AutoCloseable {
        private final String parentId;
        private final String childId;
        private final int port;
        private final AtomicInteger submitCount;
        private final AtomicBoolean reportRequested;

        private LocalGateway(String parentId, String childId, int port, AtomicInteger submitCount,
                             AtomicBoolean reportRequested) {
            this.parentId = parentId;
            this.childId = childId;
            this.port = port;
            this.submitCount = submitCount;
            this.reportRequested = reportRequested;
        }

        private static LocalGateway start(CarrierSmsProtocol protocol) throws Exception {
            int port = freePort();
            String suffix = protocol.name().toLowerCase() + "-" + UUID.randomUUID();
            String parentId = "wepush-test-parent-" + suffix;
            String childId = "wepush-test-child-" + suffix;
            AtomicInteger submits = new AtomicInteger();
            AtomicBoolean requestedReport = new AtomicBoolean();
            AbstractBusinessHandler handler = submitResponder(protocol, submits, requestedReport);

            EndpointEntity parent;
            EndpointEntity child;
            switch (protocol) {
                case CMPP -> {
                    CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
                    CMPPServerChildEndpointEntity account = new CMPPServerChildEndpointEntity();
                    account.setUserName("client");
                    account.setPassword("secret");
                    account.setVersion((short) 0x30);
                    server.addchild(account);
                    parent = server;
                    child = account;
                }
                case SMGP -> {
                    SMGPServerEndpointEntity server = new SMGPServerEndpointEntity();
                    SMGPServerChildEndpointEntity account = new SMGPServerChildEndpointEntity();
                    account.setClientID("client");
                    account.setPassword("secret");
                    account.setClientVersion((byte) 0x30);
                    server.addchild(account);
                    parent = server;
                    child = account;
                }
                case SGIP -> {
                    SgipServerEndpointEntity server = new SgipServerEndpointEntity();
                    SgipServerChildEndpointEntity account = new SgipServerChildEndpointEntity();
                    account.setLoginName("client");
                    account.setLoginPassowrd("secret");
                    account.setNodeId(123456789L);
                    server.addchild(account);
                    parent = server;
                    child = account;
                }
                case SMPP -> {
                    SMPPServerEndpointEntity server = new SMPPServerEndpointEntity();
                    SMPPServerChildEndpointEntity account = new SMPPServerChildEndpointEntity();
                    account.setSystemId("client");
                    account.setPassword("secret");
                    account.setSystemType("");
                    account.setInterfaceVersion((byte) 0x34);
                    server.addchild(account);
                    parent = server;
                    child = account;
                }
                default -> throw new IllegalStateException("Unsupported protocol " + protocol);
            }

            child.setId(childId);
            child.setValid(true);
            child.setChannelType(EndpointEntity.ChannelType.DUPLEX);
            child.setMaxChannels((short) 2);
            child.setWindow(16);
            child.setSupportLongmsg(EndpointEntity.SupportLongMessage.NONE);
            child.setBusinessHandlerSet(new ArrayList<>(List.of(handler)));

            parent.setId(parentId);
            parent.setHost("127.0.0.1");
            parent.setPort(port);
            parent.setValid(true);
            parent.setMaxChannels((short) 16);
            EndpointManager.INS.openEndpoint(parent);
            return new LocalGateway(parentId, childId, port, submits, requestedReport);
        }

        private static AbstractBusinessHandler submitResponder(CarrierSmsProtocol protocol, AtomicInteger submits,
                                                               AtomicBoolean reportRequested) {
            return new AbstractBusinessHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
                    boolean submitMessage = switch (protocol) {
                        case CMPP -> message instanceof CmppSubmitRequestMessage;
                        case SMGP -> message instanceof SMGPSubmitMessage;
                        case SGIP -> message instanceof SgipSubmitRequestMessage;
                        case SMPP -> message instanceof SubmitSm;
                    };
                    if (!submitMessage) {
                        ctx.fireChannelRead(message);
                        return;
                    }
                    BaseMessage response = switch (protocol) {
                        case CMPP -> {
                            CmppSubmitRequestMessage request = (CmppSubmitRequestMessage) message;
                            reportRequested.compareAndSet(false, request.getRegisteredDelivery() != 0);
                            yield new CmppSubmitResponseMessage(request.getHeader().getSequenceId());
                        }
                        case SMGP -> {
                            SMGPSubmitMessage request = (SMGPSubmitMessage) message;
                            reportRequested.compareAndSet(false, request.isNeedReport());
                            SMGPSubmitRespMessage result = new SMGPSubmitRespMessage();
                            result.setSequenceNo(request.getSequenceNo());
                            yield result;
                        }
                        case SGIP -> {
                            SgipSubmitRequestMessage request = (SgipSubmitRequestMessage) message;
                            reportRequested.compareAndSet(false, request.getReportflag() != 0);
                            SgipSubmitResponseMessage result = new SgipSubmitResponseMessage(request.getHeader());
                            result.setTimestamp(request.getTimestamp());
                            yield result;
                        }
                        case SMPP -> {
                            SubmitSm request = (SubmitSm) message;
                            reportRequested.compareAndSet(false, request.getRegisteredDelivery() != 0);
                            SubmitSmResp result = request.createResponse();
                            result.setMessageId("wepush-test-" + submits.get());
                            yield result;
                        }
                    };
                    submits.incrementAndGet();
                    ctx.writeAndFlush(response);
                }

                @Override
                public String name() {
                    return "wepush-test-submit-responder-" + protocol.name().toLowerCase();
                }
            };
        }

        private static int freePort() throws Exception {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }

        private int port() {
            return port;
        }

        private int submitCount() {
            return submitCount.get();
        }

        private boolean reportRequested() {
            return reportRequested.get();
        }

        @Override
        public void close() {
            EndpointManager.INS.remove(childId);
            EndpointManager.INS.remove(parentId);
        }
    }
}
