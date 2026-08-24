package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsProtocol;
import com.fangxuele.tool.push.logic.msgmaker.CarrierSmsMsgMaker;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CarrierSmsMsgSenderTest {

    @Test
    public void dryRunRendersTemplateWithoutOpeningNetworkConnection() {
        CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
        config.setProtocol(CarrierSmsProtocol.CMPP);
        config.setHost("invalid.invalid");
        config.setUsername("client");
        config.setPassword("secret");
        config.setSourceAddress("10690000");
        config.setServiceId("service");
        config.setMsgSrc("corp");

        TMsgSms sms = new TMsgSms();
        sms.setContent("您好，${var1}");
        TMsg message = new TMsg();
        message.setContent(JSON.toJSONString(sms));

        CarrierSmsMsgSender sender = new CarrierSmsMsgSender(1, 1, config, new CarrierSmsMsgMaker(message));
        SendResult result = sender.send(new String[]{"13800138000", "张三"});
        assertTrue(result.getInfo(), result.isSuccess());
        assertTrue(result.getInfo().contains("未建立网络连接"));
    }
}
