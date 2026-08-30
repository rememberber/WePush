package com.fangxuele.wepush.next.plugin.carriersms;

import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitResponseMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitRespMessage;
import com.zx.sms.codec.smpp.msg.SubmitSmResp;

import java.util.ArrayList;
import java.util.List;

final class CarrierSmsResponseParser {
    private CarrierSmsResponseParser() { }

    static CarrierSmsSubmitResult parse(CarrierProtocol protocol, List<BaseMessage> responses) {
        if (responses == null || responses.isEmpty()) return failed(protocol, "NO_SUBMIT_RESPONSE",
                ErrorCategory.UNKNOWN, false, "Gateway returned no SUBMIT_RESP");
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            CarrierSmsSubmitResult part = one(protocol, responses.get(index), index + 1);
            if (!part.success()) {
                if (index > 0) return failed(protocol, "PARTIAL_SUBMIT_UNKNOWN", ErrorCategory.UNKNOWN,
                        false, "A prior long-message fragment was accepted before fragment " + (index + 1) + " failed");
                return part;
            }
            if (part.messageId() != null && !part.messageId().isBlank()) ids.add(part.messageId());
        }
        return new CarrierSmsSubmitResult(true, protocol + "_ACCEPTED", ErrorCategory.NONE,
                false, String.join(",", ids), "Gateway accepted all " + responses.size() + " fragment(s)");
    }

    private static CarrierSmsSubmitResult one(CarrierProtocol protocol, BaseMessage response, int part) {
        if (response == null) return failed(protocol, "EMPTY_SUBMIT_RESPONSE", ErrorCategory.UNKNOWN,
                false, "Fragment " + part + " response was empty");
        long status;
        String messageId;
        switch (protocol) {
            case CMPP -> {
                if (!(response instanceof CmppSubmitResponseMessage value)) return wrong(protocol, part, response);
                status = value.getResult(); messageId = String.valueOf(value.getMsgId());
            }
            case SMGP -> {
                if (!(response instanceof SMGPSubmitRespMessage value)) return wrong(protocol, part, response);
                status = value.getStatus(); messageId = String.valueOf(value.getMsgId());
            }
            case SGIP -> {
                if (!(response instanceof SgipSubmitResponseMessage value)) return wrong(protocol, part, response);
                status = value.getResult(); messageId = "sequence=" + value.getSequenceNo();
            }
            case SMPP -> {
                if (!(response instanceof SubmitSmResp value)) return wrong(protocol, part, response);
                status = value.getCommandStatus(); messageId = value.getMessageId();
            }
            default -> throw new IllegalStateException("Unexpected protocol " + protocol);
        }
        if (status == 0) return new CarrierSmsSubmitResult(true, protocol + "_ACCEPTED",
                ErrorCategory.NONE, false, messageId, "");
        ErrorCategory category = status == 8 || status == 88 ? ErrorCategory.RATE_LIMITED
                : status == 3 || status == 4 || status == 5 || status == 13 || status == 14
                ? ErrorCategory.AUTHENTICATION : status == 10 || status == 11 || status == 12
                ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.PERMANENT_REMOTE;
        boolean retryable = category == ErrorCategory.RATE_LIMITED;
        return failed(protocol, "SUBMIT_REJECTED_" + Long.toUnsignedString(status), category,
                retryable, "Gateway rejected fragment " + part + " with status 0x" + Long.toHexString(status));
    }

    private static CarrierSmsSubmitResult wrong(CarrierProtocol protocol, int part, BaseMessage response) {
        return failed(protocol, "UNEXPECTED_RESPONSE", ErrorCategory.UNKNOWN, false,
                "Fragment " + part + " returned unexpected " + response.getClass().getSimpleName());
    }

    private static CarrierSmsSubmitResult failed(CarrierProtocol protocol, String code,
                                                 ErrorCategory category, boolean retryable,
                                                 String diagnostic) {
        return new CarrierSmsSubmitResult(false, protocol + "_" + code, category,
                retryable, "", diagnostic);
    }
}
