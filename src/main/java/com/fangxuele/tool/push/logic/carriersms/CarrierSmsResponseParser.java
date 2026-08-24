package com.fangxuele.tool.push.logic.carriersms;

import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitResponseMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitRespMessage;
import com.zx.sms.codec.smpp.msg.SubmitSmResp;

import java.util.ArrayList;
import java.util.List;

/** 对 SUBMIT_RESP 做严格的协议类型和结果码检查。 */
public final class CarrierSmsResponseParser {
    private CarrierSmsResponseParser() {
    }

    public static CarrierSmsSubmitResult parse(CarrierSmsProtocol protocol, List<BaseMessage> responses) {
        if (responses == null || responses.isEmpty()) {
            return new CarrierSmsSubmitResult(false, protocol + " 网关未返回提交应答");
        }
        List<String> messageIds = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            CarrierSmsSubmitResult result = parseOne(protocol, responses.get(i), i + 1);
            if (!result.success()) {
                return result;
            }
            if (result.info() != null && !result.info().isBlank()) {
                messageIds.add(result.info());
            }
        }
        String suffix = messageIds.isEmpty() ? "" : "，messageId=" + String.join(",", messageIds);
        return new CarrierSmsSubmitResult(true, protocol + " 网关已受理" + suffix);
    }

    private static CarrierSmsSubmitResult parseOne(CarrierSmsProtocol protocol, BaseMessage response, int part) {
        if (response == null) {
            return fail(protocol, part, "应答为空");
        }
        return switch (protocol) {
            case CMPP -> parseCmpp(response, part);
            case SMGP -> parseSmgp(response, part);
            case SGIP -> parseSgip(response, part);
            case SMPP -> parseSmpp(response, part);
        };
    }

    private static CarrierSmsSubmitResult parseCmpp(BaseMessage response, int part) {
        if (!(response instanceof CmppSubmitResponseMessage submitResponse)) {
            return unexpected(CarrierSmsProtocol.CMPP, response, part);
        }
        if (submitResponse.getResult() != 0) {
            return fail(CarrierSmsProtocol.CMPP, part,
                    CarrierSmsErrorTranslator.submitStatus(CarrierSmsProtocol.CMPP, submitResponse.getResult()));
        }
        return new CarrierSmsSubmitResult(true, String.valueOf(submitResponse.getMsgId()));
    }

    private static CarrierSmsSubmitResult parseSmgp(BaseMessage response, int part) {
        if (!(response instanceof SMGPSubmitRespMessage submitResponse)) {
            return unexpected(CarrierSmsProtocol.SMGP, response, part);
        }
        if (submitResponse.getStatus() != 0) {
            return fail(CarrierSmsProtocol.SMGP, part,
                    CarrierSmsErrorTranslator.submitStatus(CarrierSmsProtocol.SMGP, submitResponse.getStatus()));
        }
        return new CarrierSmsSubmitResult(true, String.valueOf(submitResponse.getMsgId()));
    }

    private static CarrierSmsSubmitResult parseSgip(BaseMessage response, int part) {
        if (!(response instanceof SgipSubmitResponseMessage submitResponse)) {
            return unexpected(CarrierSmsProtocol.SGIP, response, part);
        }
        if (submitResponse.getResult() != 0) {
            return fail(CarrierSmsProtocol.SGIP, part,
                    CarrierSmsErrorTranslator.submitStatus(CarrierSmsProtocol.SGIP, submitResponse.getResult()));
        }
        return new CarrierSmsSubmitResult(true, "sequence=" + submitResponse.getSequenceNo());
    }

    private static CarrierSmsSubmitResult parseSmpp(BaseMessage response, int part) {
        if (!(response instanceof SubmitSmResp submitResponse)) {
            return unexpected(CarrierSmsProtocol.SMPP, response, part);
        }
        if (submitResponse.getCommandStatus() != 0) {
            return fail(CarrierSmsProtocol.SMPP, part,
                    CarrierSmsErrorTranslator.submitStatus(CarrierSmsProtocol.SMPP, submitResponse.getCommandStatus()));
        }
        return new CarrierSmsSubmitResult(true, submitResponse.getMessageId());
    }

    private static CarrierSmsSubmitResult unexpected(CarrierSmsProtocol protocol, BaseMessage response, int part) {
        return fail(protocol, part, "应答类型错误: " + response.getClass().getSimpleName());
    }

    private static CarrierSmsSubmitResult fail(CarrierSmsProtocol protocol, int part, String reason) {
        return new CarrierSmsSubmitResult(false, protocol + " 第 " + part + " 个分片提交失败，" + reason);
    }
}
