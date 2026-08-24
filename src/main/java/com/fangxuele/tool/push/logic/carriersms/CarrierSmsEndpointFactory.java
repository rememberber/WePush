package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPClientEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipClientEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPClientEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPClientEndpointEntity;

/** 将 WePush 账号配置映射为 sms-client 连接端点。 */
public final class CarrierSmsEndpointFactory {
    private CarrierSmsEndpointFactory() {
    }

    public static EndpointEntity create(int accountId, CarrierSmsAccountConfig config) {
        config.applyDefaults();
        EndpointEntity entity = switch (config.getProtocol()) {
            case CMPP -> createCmpp(config);
            case SMGP -> createSmgp(config);
            case SGIP -> createSgip(config);
            case SMPP -> createSmpp(config);
        };
        entity.setId("wepush-carrier-" + accountId);
        entity.setHost(config.getHost().trim());
        entity.setPort(config.getPort());
        entity.setValid(true);
        entity.setChannelType(EndpointEntity.ChannelType.DUPLEX);
        entity.setMaxChannels((short) config.getMaxChannels());
        entity.setWindow(config.getWindowSize());
        // 提交结果不明时不自动重发，避免重复短信。
        entity.setReSendFailMsg(false);
        return entity;
    }

    private static CMPPClientEndpointEntity createCmpp(CarrierSmsAccountConfig config) {
        CMPPClientEndpointEntity entity = new CMPPClientEndpointEntity();
        entity.setUserName(config.getUsername());
        entity.setPassword(config.getPassword());
        entity.setVersion((short) config.getProtocol().versionCode(config.getVersion()));
        entity.setSpCode(value(config.getSourceAddress()));
        entity.setServiceId(value(config.getServiceId()));
        entity.setMsgSrc(value(config.getMsgSrc()));
        return entity;
    }

    private static SMGPClientEndpointEntity createSmgp(CarrierSmsAccountConfig config) {
        SMGPClientEndpointEntity entity = new SMGPClientEndpointEntity();
        entity.setClientID(config.getUsername());
        entity.setPassword(config.getPassword());
        entity.setClientVersion((byte) config.getProtocol().versionCode(config.getVersion()));
        return entity;
    }

    private static SgipClientEndpointEntity createSgip(CarrierSmsAccountConfig config) {
        SgipClientEndpointEntity entity = new SgipClientEndpointEntity();
        entity.setLoginName(config.getUsername());
        entity.setLoginPassowrd(config.getPassword());
        entity.setNodeId(config.getNodeId());
        return entity;
    }

    private static SMPPClientEndpointEntity createSmpp(CarrierSmsAccountConfig config) {
        SMPPClientEndpointEntity entity = new SMPPClientEndpointEntity();
        entity.setSystemId(config.getUsername());
        entity.setPassword(config.getPassword());
        entity.setSystemType(value(config.getSystemType()));
        entity.setInterfaceVersion((byte) config.getProtocol().versionCode(config.getVersion()));
        entity.setAddZeroByte(config.isAddZeroByte());
        return entity;
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
