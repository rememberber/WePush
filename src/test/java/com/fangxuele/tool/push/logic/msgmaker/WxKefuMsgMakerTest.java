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
    public void shouldBuildVoiceMessage() {
        TMsgKefu config = new TMsgKefu();
        config.setKefuMsgType("语音消息");
        config.setMediaId("VOICE_$var1");

        JSONObject json = JSON.parseObject(makeMessage(
                JSON.toJSONString(config), "openid", "MEDIA_ID").toJson());

        assertEquals("voice", json.getString("msgtype"));
        assertEquals("VOICE_MEDIA_ID", json.getJSONObject("voice").getString("media_id"));
    }

    @Test
    public void shouldBuildVideoMessage() {
        TMsgKefu config = new TMsgKefu();
        config.setKefuMsgType("视频消息");
        config.setMediaId("VIDEO_MEDIA_ID");
        config.setThumbMediaId("VIDEO_THUMB_ID");
        config.setTitle("视频$var1");
        config.setDescribe("视频描述$var1");

        JSONObject json = JSON.parseObject(makeMessage(
                JSON.toJSONString(config), "openid", "1001").toJson());
        JSONObject video = json.getJSONObject("video");

        assertEquals("video", json.getString("msgtype"));
        assertEquals("VIDEO_MEDIA_ID", video.getString("media_id"));
        assertEquals("VIDEO_THUMB_ID", video.getString("thumb_media_id"));
        assertEquals("视频1001", video.getString("title"));
        assertEquals("视频描述1001", video.getString("description"));
    }

    @Test
    public void shouldBuildMusicMessage() {
        TMsgKefu config = new TMsgKefu();
        config.setKefuMsgType("音乐消息");
        config.setTitle("音乐标题");
        config.setDescribe("音乐描述");
        config.setMusicUrl("https://example.com/music/${var1}.mp3");
        config.setHqMusicUrl("https://example.com/music/${var1}-hq.mp3");
        config.setThumbMediaId("MUSIC_THUMB_ID");

        JSONObject json = JSON.parseObject(makeMessage(
                JSON.toJSONString(config), "openid", "1001").toJson());
        JSONObject music = json.getJSONObject("music");

        assertEquals("music", json.getString("msgtype"));
        assertEquals("https://example.com/music/1001.mp3", music.getString("musicurl"));
        assertEquals("https://example.com/music/1001-hq.mp3", music.getString("hqmusicurl"));
        assertEquals("MUSIC_THUMB_ID", music.getString("thumb_media_id"));
    }

    @Test
    public void shouldRejectBlankRequiredMediaFields() {
        TMsgKefu image = new TMsgKefu();
        image.setKefuMsgType("图片消息");
        image.setMediaId("$var1");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(image), "openid", ""));

        TMsgKefu voice = new TMsgKefu();
        voice.setKefuMsgType("语音消息");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(voice), "openid"));

        TMsgKefu video = new TMsgKefu();
        video.setKefuMsgType("视频消息");
        video.setMediaId("VIDEO_MEDIA_ID");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(video), "openid"));

        TMsgKefu music = new TMsgKefu();
        music.setKefuMsgType("音乐消息");
        music.setTitle("音乐标题");
        music.setMusicUrl("https://example.com/music.mp3");
        music.setThumbMediaId("MUSIC_THUMB_ID");
        assertThrows(IllegalArgumentException.class,
                () -> makeMessage(JSON.toJSONString(music), "openid"));

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
        priority.setKefuMsgType("音乐消息");
        priority.setTitle("优先音乐");
        priority.setDescribe("优先音乐描述");
        priority.setMusicUrl("https://example.com/priority.mp3");
        priority.setHqMusicUrl("https://example.com/priority-hq.mp3");
        priority.setThumbMediaId("PRIORITY_MUSIC_THUMB_ID");

        WxMpKefuMessage priorityMessage = makeMessage(JSON.toJSONString(priority), "openid");
        assertEquals("https://example.com/priority.mp3", priorityMessage.getMusicUrl());
        assertEquals("https://example.com/priority-hq.mp3", priorityMessage.getHqMusicUrl());

        TMsgKefu legacy = JSON.parseObject(
                "{\"kefuMsgType\":\"文本消息\",\"content\":\"hello\"}", TMsgKefu.class);
        assertEquals("文本消息", legacy.getKefuMsgType());
        assertEquals("hello", legacy.getContent());
        assertNull(legacy.getMediaId());
        assertNull(legacy.getMusicUrl());
        assertNull(legacy.getHqMusicUrl());
    }

    private static WxMpKefuMessage makeMessage(String content, String... messageData) {
        TMsg message = new TMsg();
        message.setContent(content);
        return new WxKefuMsgMaker(message).makeMsg(messageData);
    }
}
