package com.fangxuele.wepush.next.plugin.carriersms;

import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPClientEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipClientEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPClientEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPClientEndpointEntity;

final class CarrierSmsEndpointFactory {
    private CarrierSmsEndpointFactory() { }

    static EndpointEntity create(String sessionId, CarrierSmsConfig config, String password) {
        EndpointEntity endpoint = switch (config.protocol()) {
            case CMPP -> cmpp(config, password);
            case SMGP -> smgp(config, password);
            case SGIP -> sgip(config, password);
            case SMPP -> smpp(config, password);
        };
        endpoint.setId("wepush-next-" + config.protocol().name().toLowerCase() + "-" + safe(sessionId));
        endpoint.setHost(config.host());
        endpoint.setPort(config.port());
        endpoint.setValid(true);
        endpoint.setChannelType(EndpointEntity.ChannelType.DUPLEX);
        endpoint.setMaxChannels((short) config.maxChannels());
        endpoint.setWindow(config.windowSize());
        endpoint.setIdleTimeSec((short) config.heartbeatIntervalSeconds());
        endpoint.setSupportLongmsg(EndpointEntity.SupportLongMessage.SEND);
        endpoint.setReSendFailMsg(false);
        endpoint.setMaxRetryCnt((short) 0);
        return endpoint;
    }

    private static CMPPClientEndpointEntity cmpp(CarrierSmsConfig config, String password) {
        CMPPClientEndpointEntity endpoint = new CMPPClientEndpointEntity();
        endpoint.setUserName(config.username()); endpoint.setPassword(password);
        endpoint.setVersion((short) config.protocol().versionCode(config.version()));
        endpoint.setSpCode(config.sourceAddress()); endpoint.setServiceId(config.serviceId());
        endpoint.setMsgSrc(config.msgSrc());
        return endpoint;
    }

    private static SMGPClientEndpointEntity smgp(CarrierSmsConfig config, String password) {
        SMGPClientEndpointEntity endpoint = new SMGPClientEndpointEntity();
        endpoint.setClientID(config.username()); endpoint.setPassword(password);
        endpoint.setClientVersion((byte) config.protocol().versionCode(config.version()));
        return endpoint;
    }

    private static SgipClientEndpointEntity sgip(CarrierSmsConfig config, String password) {
        SgipClientEndpointEntity endpoint = new SgipClientEndpointEntity();
        endpoint.setLoginName(config.username()); endpoint.setLoginPassowrd(password);
        endpoint.setNodeId(config.nodeId());
        return endpoint;
    }

    private static SMPPClientEndpointEntity smpp(CarrierSmsConfig config, String password) {
        SMPPClientEndpointEntity endpoint = new SMPPClientEndpointEntity();
        endpoint.setSystemId(config.username()); endpoint.setPassword(password);
        endpoint.setSystemType(config.systemType());
        endpoint.setInterfaceVersion((byte) config.protocol().versionCode(config.version()));
        endpoint.setAddZeroByte(config.addZeroByte());
        return endpoint;
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "-").substring(0, Math.min(80, value.length()));
    }
}
