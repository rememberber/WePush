package com.fangxuele.tool.push.logic.carriersms;

import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smpp.Address;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import org.apache.commons.lang3.StringUtils;

/** 四种协议的 SUBMIT 请求字段映射。 */
public final class CarrierSmsRequestFactory {
    private CarrierSmsRequestFactory() {
    }

    public static BaseMessage create(CarrierSmsAccountConfig config, String mobile, String content) throws Exception {
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("接收号码不能为空");
        }
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("短信内容不能为空");
        }
        String target = mobile.trim();
        return switch (config.getProtocol()) {
            case CMPP -> createCmpp(config, target, content);
            case SMGP -> createSmgp(config, target, content);
            case SGIP -> createSgip(config, target, content);
            case SMPP -> createSmpp(config, target, content);
        };
    }

    private static CmppSubmitRequestMessage createCmpp(CarrierSmsAccountConfig config, String mobile, String content) {
        CmppSubmitRequestMessage request = new CmppSubmitRequestMessage();
        request.setRegisteredDelivery((short) (config.isRegisteredDelivery() ? 1 : 0));
        request.setServiceId(value(config.getServiceId()));
        request.setMsgsrc(value(config.getMsgSrc()));
        request.setSrcId(value(config.getSourceAddress()));
        request.setDestterminalId(mobile);
        request.setMsgContent(content);
        setCmppFee(config, request);
        return request;
    }

    private static void setCmppFee(CarrierSmsAccountConfig config, CmppSubmitRequestMessage request) {
        if (StringUtils.isNotBlank(config.getFeeType())) {
            request.setFeeType(config.getFeeType().trim());
        }
        if (StringUtils.isNotBlank(config.getFeeCode())) {
            request.setFeeCode(config.getFeeCode().trim());
        }
    }

    private static SMGPSubmitMessage createSmgp(CarrierSmsAccountConfig config, String mobile, String content) {
        SMGPSubmitMessage request = new SMGPSubmitMessage();
        request.setNeedReport(config.isRegisteredDelivery());
        request.setServiceId(value(config.getServiceId()));
        request.setSrcTermId(value(config.getSourceAddress()));
        request.setDestTermIdArray(mobile);
        request.setMsgContent(content);
        if (StringUtils.isNotBlank(config.getMsgSrc())) {
            request.setMsgSrc(config.getMsgSrc().trim());
        }
        if (StringUtils.isNotBlank(config.getFeeType())) {
            request.setFeeType(config.getFeeType().trim());
        }
        if (StringUtils.isNotBlank(config.getFeeCode())) {
            request.setFeeCode(config.getFeeCode().trim());
        }
        if (StringUtils.isNotBlank(config.getFixedFee())) {
            request.setFixedFee(config.getFixedFee().trim());
        }
        return request;
    }

    private static SgipSubmitRequestMessage createSgip(CarrierSmsAccountConfig config, String mobile, String content) {
        SgipSubmitRequestMessage request = new SgipSubmitRequestMessage();
        request.setSpnumber(value(config.getSourceAddress()));
        request.setChargenumber(value(config.getChargeNumber()));
        request.setUsernumber(mobile);
        request.setCorpid(value(config.getCorpId()));
        request.setServicetype(value(config.getServiceId()));
        request.setReportflag((short) (config.isRegisteredDelivery() ? 1 : 0));
        request.setMsgContent(content);
        if (StringUtils.isNotBlank(config.getFeeType())) {
            request.setFeetype(Short.parseShort(config.getFeeType().trim()));
        }
        if (StringUtils.isNotBlank(config.getFeeValue())) {
            request.setFeevalue(config.getFeeValue().trim());
        }
        return request;
    }

    private static SubmitSm createSmpp(CarrierSmsAccountConfig config, String mobile, String content) {
        SubmitSm request = new SubmitSm();
        request.setServiceType(value(config.getServiceId()));
        request.setSourceAddress(new Address((byte) config.getSourceTon(), (byte) config.getSourceNpi(), value(config.getSourceAddress())));
        request.setDestAddress(new Address((byte) config.getDestinationTon(), (byte) config.getDestinationNpi(), mobile));
        request.setRegisteredDelivery((byte) (config.isRegisteredDelivery() ? 1 : 0));
        request.setSmsMsg(content);
        return request;
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
