package com.fangxuele.wepush.next.plugin.carriersms;

import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;
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
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarrierSmsProviderTest {
    @Test
    void validatesProtocolSpecificSchemasAndMapsSubmitFields() {
        for (CarrierProtocol protocol : CarrierProtocol.values()) {
            CarrierSmsProviderFactory factory = new CarrierSmsProviderFactory(protocol);
            ValidationResult account = factory.validateAccount(account(protocol, protocol.defaultPort()));
            assertTrue(account.validResult(), () -> protocol + ": " + account.violations());
            assertTrue(factory.validateMessage(document("message", "{\"content\":\"您好，{{name}}\"}")).validResult());
            CarrierSmsConfig config = CarrierSmsConfig.parse(protocol, account(protocol, protocol.defaultPort()));
            BaseMessage request = CarrierSmsRequestFactory.create(config, "13800138000", "中文短信");
            switch (protocol) {
                case CMPP -> assertInstanceOf(CmppSubmitRequestMessage.class, request);
                case SMGP -> assertInstanceOf(SMGPSubmitMessage.class, request);
                case SGIP -> assertInstanceOf(SgipSubmitRequestMessage.class, request);
                case SMPP -> assertInstanceOf(SubmitSm.class, request);
            }
            assertTrue(factory.descriptor().accountSchema().canonicalContent().length > 100);
        }
    }

    @Test
    void logsInAndSubmitsShortAndLongTextThroughLocalGateways() throws Exception {
        for (CarrierProtocol protocol : CarrierProtocol.values()) {
            try (LocalGateway server = LocalGateway.start(protocol)) {
                CarrierSmsConfig config = CarrierSmsConfig.parse(protocol, account(protocol, server.port));
                try (SmsClientGateway gateway = new SmsClientGateway("test-" + protocol, config, "secret")) {
                    gateway.connect(5_000);
                    CarrierSmsSubmitResult shortResult = CarrierSmsResponseParser.parse(protocol,
                            gateway.submit(CarrierSmsRequestFactory.create(config, "13800138000", "中文短信"), 5_000));
                    assertTrue(shortResult.success(), shortResult.diagnostic());
                    List<BaseMessage> longResponses = gateway.submit(CarrierSmsRequestFactory.create(config,
                            "13800138000", "这是一条用于验证长短信分片的中文短信".repeat(20)), 10_000);
                    assertTrue(CarrierSmsResponseParser.parse(protocol, longResponses).success());
                    assertTrue(longResponses.size() > 1, protocol + " did not split long content");
                }
                assertTrue(server.submits.get() > 2);
                assertFalse(server.reportRequested.get());
            }
        }
    }

    private static ConfigDocument account(CarrierProtocol protocol, int port) {
        String extra = switch (protocol) {
            case CMPP -> "\"msgSrc\":\"corp\",";
            case SGIP -> "\"nodeId\":123456789,\"corpId\":\"corp\",";
            default -> "";
        };
        return document("account", "{\"host\":\"127.0.0.1\",\"port\":" + port
                + ",\"username\":\"client\",\"password\":{\"namespace\":\"carrier\",\"name\":\"password\",\"version\":\"v1\"},"
                + "\"sourceAddress\":\"10690000\",\"serviceId\":\""
                + (protocol == CarrierProtocol.SMPP ? "svc" : "service") + "\"," + extra
                + "\"windowSize\":4}");
    }

    private static ConfigDocument document(String kind, String json) {
        return new ConfigDocument("test/" + kind, "1", json.getBytes(StandardCharsets.UTF_8));
    }

    private static final class LocalGateway implements AutoCloseable {
        private final String parentId;
        private final String childId;
        private final int port;
        private final AtomicInteger submits;
        private final AtomicBoolean reportRequested;

        private LocalGateway(String parentId, String childId, int port,
                             AtomicInteger submits, AtomicBoolean reportRequested) {
            this.parentId = parentId; this.childId = childId; this.port = port;
            this.submits = submits; this.reportRequested = reportRequested;
        }

        static LocalGateway start(CarrierProtocol protocol) throws Exception {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
            String suffix = protocol.name().toLowerCase() + "-" + UUID.randomUUID();
            String parentId = "wepush-next-test-parent-" + suffix;
            String childId = "wepush-next-test-child-" + suffix;
            AtomicInteger submits = new AtomicInteger();
            AtomicBoolean reportRequested = new AtomicBoolean();
            EndpointEntity parent;
            EndpointEntity child;
            switch (protocol) {
                case CMPP -> {
                    CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
                    CMPPServerChildEndpointEntity account = new CMPPServerChildEndpointEntity();
                    account.setUserName("client"); account.setPassword("secret"); account.setVersion((short) 0x30);
                    server.addchild(account); parent = server; child = account;
                }
                case SMGP -> {
                    SMGPServerEndpointEntity server = new SMGPServerEndpointEntity();
                    SMGPServerChildEndpointEntity account = new SMGPServerChildEndpointEntity();
                    account.setClientID("client"); account.setPassword("secret"); account.setClientVersion((byte) 0x30);
                    server.addchild(account); parent = server; child = account;
                }
                case SGIP -> {
                    SgipServerEndpointEntity server = new SgipServerEndpointEntity();
                    SgipServerChildEndpointEntity account = new SgipServerChildEndpointEntity();
                    account.setLoginName("client"); account.setLoginPassowrd("secret"); account.setNodeId(123456789L);
                    server.addchild(account); parent = server; child = account;
                }
                case SMPP -> {
                    SMPPServerEndpointEntity server = new SMPPServerEndpointEntity();
                    SMPPServerChildEndpointEntity account = new SMPPServerChildEndpointEntity();
                    account.setSystemId("client"); account.setPassword("secret"); account.setSystemType("");
                    account.setInterfaceVersion((byte) 0x34); server.addchild(account); parent = server; child = account;
                }
                default -> throw new IllegalStateException();
            }
            AbstractBusinessHandler handler = responder(protocol, submits, reportRequested);
            child.setId(childId); child.setValid(true); child.setChannelType(EndpointEntity.ChannelType.DUPLEX);
            child.setMaxChannels((short) 2); child.setWindow(16);
            child.setSupportLongmsg(EndpointEntity.SupportLongMessage.NONE);
            child.setBusinessHandlerSet(new ArrayList<>(List.of(handler)));
            parent.setId(parentId); parent.setHost("127.0.0.1"); parent.setPort(port);
            parent.setValid(true); parent.setMaxChannels((short) 16);
            EndpointManager.INS.openEndpoint(parent);
            return new LocalGateway(parentId, childId, port, submits, reportRequested);
        }

        private static AbstractBusinessHandler responder(CarrierProtocol protocol, AtomicInteger submits,
                                                         AtomicBoolean reportRequested) {
            return new AbstractBusinessHandler() {
                @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    boolean submit = switch (protocol) {
                        case CMPP -> message instanceof CmppSubmitRequestMessage;
                        case SMGP -> message instanceof SMGPSubmitMessage;
                        case SGIP -> message instanceof SgipSubmitRequestMessage;
                        case SMPP -> message instanceof SubmitSm;
                    };
                    if (!submit) { context.fireChannelRead(message); return; }
                    BaseMessage response = switch (protocol) {
                        case CMPP -> {
                            CmppSubmitRequestMessage request = (CmppSubmitRequestMessage) message;
                            reportRequested.compareAndSet(false, request.getRegisteredDelivery() != 0);
                            yield new CmppSubmitResponseMessage(request.getHeader().getSequenceId());
                        }
                        case SMGP -> {
                            SMGPSubmitMessage request = (SMGPSubmitMessage) message;
                            reportRequested.compareAndSet(false, request.isNeedReport());
                            SMGPSubmitRespMessage value = new SMGPSubmitRespMessage(); value.setSequenceNo(request.getSequenceNo()); yield value;
                        }
                        case SGIP -> {
                            SgipSubmitRequestMessage request = (SgipSubmitRequestMessage) message;
                            reportRequested.compareAndSet(false, request.getReportflag() != 0);
                            SgipSubmitResponseMessage value = new SgipSubmitResponseMessage(request.getHeader());
                            value.setTimestamp(request.getTimestamp()); yield value;
                        }
                        case SMPP -> {
                            SubmitSm request = (SubmitSm) message;
                            reportRequested.compareAndSet(false, request.getRegisteredDelivery() != 0);
                            SubmitSmResp value = request.createResponse(); value.setMessageId("wepush-test-" + submits.get()); yield value;
                        }
                    };
                    submits.incrementAndGet(); context.writeAndFlush(response);
                }

                @Override public String name() { return "wepush-next-carrier-test-" + protocol.name().toLowerCase(); }
            };
        }

        @Override public void close() { EndpointManager.INS.remove(childId); EndpointManager.INS.remove(parentId); }
    }
}
