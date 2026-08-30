package com.fangxuele.wepush.next.plugin.carriersms;

import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smpp.Address;
import com.zx.sms.codec.smpp.msg.SubmitSm;

final class CarrierSmsRequestFactory {
    private CarrierSmsRequestFactory() { }

    static BaseMessage create(CarrierSmsConfig config, String phoneNumber, String content) {
        String phone = phoneNumber == null ? "" : phoneNumber.trim();
        if (!phone.matches("[0-9]{5,32}")) {
            throw CarrierProviderSupport.invalid("recipient.phoneNumber", "INVALID_PHONE_NUMBER",
                    "phoneNumber must contain only 5 to 32 digits");
        }
        int maximum = switch (config.protocol()) {
            case CMPP -> config.protocol().versionCode(config.version()) == 0x20 ? 21 : 32;
            case SMGP, SGIP -> 21;
            case SMPP -> 20;
        };
        if (phone.length() > maximum) throw CarrierProviderSupport.invalid("recipient.phoneNumber",
                "PHONE_NUMBER_TOO_LONG", config.protocol() + " phoneNumber exceeds " + maximum + " digits");
        if (content == null || content.isBlank()) throw CarrierProviderSupport.invalid("content",
                "EMPTY_RENDERED_CONTENT", "rendered SMS content must not be blank");
        return switch (config.protocol()) {
            case CMPP -> cmpp(config, phone, content);
            case SMGP -> smgp(config, phone, content);
            case SGIP -> sgip(config, phone, content);
            case SMPP -> smpp(config, phone, content);
        };
    }

    private static CmppSubmitRequestMessage cmpp(CarrierSmsConfig config, String phone, String content) {
        CmppSubmitRequestMessage request = new CmppSubmitRequestMessage();
        request.setRegisteredDelivery((short) 0); request.setServiceId(config.serviceId());
        request.setMsgsrc(config.msgSrc()); request.setSrcId(config.sourceAddress());
        request.setDestterminalId(phone); request.setMsgContent(content);
        if (!config.feeType().isBlank()) request.setFeeType(config.feeType());
        if (!config.feeCode().isBlank()) request.setFeeCode(config.feeCode());
        return request;
    }

    private static SMGPSubmitMessage smgp(CarrierSmsConfig config, String phone, String content) {
        SMGPSubmitMessage request = new SMGPSubmitMessage();
        request.setNeedReport(false); request.setServiceId(config.serviceId());
        request.setSrcTermId(config.sourceAddress()); request.setDestTermIdArray(phone);
        request.setMsgContent(content);
        if (!config.msgSrc().isBlank()) request.setMsgSrc(config.msgSrc());
        if (!config.feeType().isBlank()) request.setFeeType(config.feeType());
        if (!config.feeCode().isBlank()) request.setFeeCode(config.feeCode());
        if (!config.fixedFee().isBlank()) request.setFixedFee(config.fixedFee());
        return request;
    }

    private static SgipSubmitRequestMessage sgip(CarrierSmsConfig config, String phone, String content) {
        SgipSubmitRequestMessage request = new SgipSubmitRequestMessage();
        request.setSpnumber(config.sourceAddress()); request.setChargenumber(config.chargeNumber());
        request.setUsernumber(phone); request.setCorpid(config.corpId());
        request.setServicetype(config.serviceId()); request.setReportflag((short) 0);
        request.setMsgContent(content);
        if (!config.feeType().isBlank()) request.setFeetype(Short.parseShort(config.feeType()));
        if (!config.feeValue().isBlank()) request.setFeevalue(config.feeValue());
        return request;
    }

    private static SubmitSm smpp(CarrierSmsConfig config, String phone, String content) {
        SubmitSm request = new SubmitSm();
        request.setServiceType(config.serviceId());
        request.setSourceAddress(new Address((byte) config.sourceTon(), (byte) config.sourceNpi(), config.sourceAddress()));
        request.setDestAddress(new Address((byte) config.destinationTon(), (byte) config.destinationNpi(), phone));
        request.setRegisteredDelivery((byte) 0); request.setSmsMsg(content);
        return request;
    }
}
