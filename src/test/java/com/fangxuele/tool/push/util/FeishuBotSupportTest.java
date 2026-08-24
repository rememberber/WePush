package com.fangxuele.tool.push.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FeishuBotSupportTest {

    @Test
    public void shouldValidateOfficialWebhook() {
        FeishuBotSupport.validateWebhook("https://open.feishu.cn/open-apis/bot/v2/hook/abc-123");
        FeishuBotSupport.validateWebhook("https://open.larksuite.com/open-apis/bot/v2/hook/abc-123");

        assertThrows(IllegalArgumentException.class,
                () -> FeishuBotSupport.validateWebhook("http://open.feishu.cn/open-apis/bot/v2/hook/abc"));
        assertThrows(IllegalArgumentException.class,
                () -> FeishuBotSupport.validateWebhook("https://example.com/open-apis/bot/v2/hook/abc"));
    }

    @Test
    public void shouldGenerateFeishuSignature() {
        assertEquals("wSds2BzzFIIGf/WrhUO+NI1q/9j+FRJd3JNHKAq0NZY=",
                FeishuBotSupport.sign(1599360473L, "test-secret"));
    }

    @Test
    public void shouldBuildTextPayloadWithKeywordAndMention() {
        JSONObject payload = FeishuBotSupport.buildPayload(
                FeishuBotSupport.TYPE_TEXT, "", "服务恢复", "告警", "ou_123");

        assertEquals("text", payload.getString("msg_type"));
        assertEquals("<at user_id=\"ou_123\">用户</at> 告警 服务恢复",
                payload.getJSONObject("content").getString("text"));

        assertThrows(IllegalArgumentException.class, () -> FeishuBotSupport.buildPayload(
                FeishuBotSupport.TYPE_TEXT, "", "服务恢复", "", "ou_bad\"id"));
    }

    @Test
    public void shouldBuildPostPayloadAndAtAll() {
        JSONObject payload = FeishuBotSupport.buildPayload(
                FeishuBotSupport.TYPE_POST, "日报", "今日无异常", "", "@all");

        JSONObject locale = payload.getJSONObject("content").getJSONObject("post").getJSONObject("zh_cn");
        assertEquals("日报", locale.getString("title"));
        JSONArray paragraph = locale.getJSONArray("content").getJSONArray(0);
        assertEquals("all", paragraph.getJSONObject(0).getString("user_id"));
        assertEquals("今日无异常", paragraph.getJSONObject(1).getString("text"));
    }

    @Test
    public void shouldBuildCardAndRawPayload() {
        JSONObject cardPayload = FeishuBotSupport.buildPayload(
                FeishuBotSupport.TYPE_CARD, "", "{\"header\":{},\"elements\":[]}", "", "");
        assertEquals("interactive", cardPayload.getString("msg_type"));
        assertTrue(cardPayload.getJSONObject("card").containsKey("elements"));

        JSONObject rawPayload = FeishuBotSupport.buildPayload(
                FeishuBotSupport.TYPE_RAW_JSON, "", "{\"msg_type\":\"image\",\"content\":{\"image_key\":\"img_x\"}}", "", "");
        assertEquals("image", rawPayload.getString("msg_type"));
    }

    @Test
    public void shouldValidateBusinessResponse() {
        FeishuBotSupport.validateResponse("{\"code\":0,\"msg\":\"success\"}");
        FeishuBotSupport.validateResponse("{\"StatusCode\":0,\"StatusMessage\":\"success\"}");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> FeishuBotSupport.validateResponse("{\"code\":19001,\"msg\":\"keyword not found\"}"));
        assertTrue(error.getMessage().contains("19001"));
    }

    @Test
    public void shouldRejectPayloadOverTwentyKilobytes() {
        String oversized = "x".repeat(FeishuBotSupport.MAX_PAYLOAD_BYTES + 1);
        assertThrows(IllegalArgumentException.class,
                () -> FeishuBotSupport.validatePayloadSize(oversized));
    }
}
