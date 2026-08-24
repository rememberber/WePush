package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class WxKefuMsgMakerTest {

    @Test
    public void shouldBuildImageMessageAndRenderMediaId() {
        TMsgKefu config = new TMsgKefu();
        config.setKefuMsgType("图片消息");
        config.setMediaId("$var1");

        WxMpKefuMessage message = makeMessage(JSON.toJSONString(config), "openid", "IMAGE_MEDIA_ID");
        message.setToUser("openid");

        JSONObject json = JSON.parseObject(message.toJson());
        assertEquals("image", json.getString("msgtype"));
        assertEquals("openid", json.getString("touser"));
        assertEquals("IMAGE_MEDIA_ID", json.getJSONObject("image").getString("media_id"));
    }

    @Test
    public void shouldBuildMiniProgramCardAndRenderRequiredFields() {
        TMsgKefu config = new TMsgKefu();
        config.setKefuMsgType("小程序卡片消息");
        config.setTitle("订单$var1");
        config.setAppId("wx1234567890abcdef");
        config.setPagePath("pages/order?id=$var1");
        config.setThumbMediaId("THUMB_MEDIA_ID");

        WxMpKefuMessage message = makeMessage(JSON.toJSONString(config), "openid", "1001");
        JSONObject json = JSON.parseObject(message.toJson());

        assertEquals("miniprogrampage", json.getString("msgtype"));
        JSONObject miniProgramPage = json.getJSONObject("miniprogrampage");
        assertEquals("订单1001", miniProgramPage.getString("title"));
        assertEquals("pages/order?id=1001", miniProgramPage.getString("pagepath"));
        assertEquals("THUMB_MEDIA_ID", miniProgramPage.getString("thumb_media_id"));
    }

    @Test
    public void shouldRejectBlankRequiredMediaFields() {
        TMsgKefu image = new TMsgKefu();
        image.setKefuMsgType("图片消息");
        image.setMediaId("$var1");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(image), "openid", ""));

        TMsgKefu miniProgram = new TMsgKefu();
        miniProgram.setKefuMsgType("小程序卡片消息");
        miniProgram.setTitle("订单详情");
        miniProgram.setAppId("wx1234567890abcdef");
        miniProgram.setPagePath("pages/order");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(miniProgram), "openid"));
    }

    @Test
    public void shouldSupportPriorityAndLegacyJson() {
        TMsgKefuPriority priority = new TMsgKefuPriority();
        priority.setKefuMsgType("图片消息");
        priority.setMediaId("PRIORITY_IMAGE_MEDIA_ID");

        WxMpKefuMessage priorityMessage = makeMessage(JSON.toJSONString(priority), "openid");
        assertEquals("PRIORITY_IMAGE_MEDIA_ID", priorityMessage.getMediaId());

        TMsgKefu legacy = JSON.parseObject(
                "{\"kefuMsgType\":\"文本消息\",\"content\":\"hello\"}", TMsgKefu.class);
        assertEquals("文本消息", legacy.getKefuMsgType());
        assertEquals("hello", legacy.getContent());
        assertNull(legacy.getMediaId());
    }

    private static WxMpKefuMessage makeMessage(String content, String... messageData) {
        TMsg message = new TMsg();
        message.setContent(content);
        return new WxKefuMsgMaker(message).makeMsg(messageData);
    }
}
