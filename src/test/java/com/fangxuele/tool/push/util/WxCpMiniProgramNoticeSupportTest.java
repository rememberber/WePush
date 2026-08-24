package com.fangxuele.tool.push.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fangxuele.tool.push.domain.TMsgWxCp;
import com.fangxuele.tool.push.domain.WxCpMiniProgramContentItem;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WxCpMiniProgramNoticeSupportTest {

    @Test
    public void shouldValidateAndPreserveContentItemOrder() {
        List<WxCpMiniProgramContentItem> items = List.of(
                new WxCpMiniProgramContentItem("会议室", "402"),
                new WxCpMiniProgramContentItem("会议时间", "今天 15:00"));

        LinkedHashMap<String, String> result = WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                "wx1234567890abcdef", "pages/index?id=1", "会议预订成功", "今天下午三点",
                items, true, 1800);

        assertEquals(List.of("会议室", "会议时间"), new ArrayList<>(result.keySet()));
        assertEquals("402", result.get("会议室"));
    }

    @Test
    public void shouldRejectProtocolLimitViolations() {
        List<WxCpMiniProgramContentItem> duplicateItems = List.of(
                new WxCpMiniProgramContentItem("状态", "成功"),
                new WxCpMiniProgramContentItem("状态", "完成"));
        assertThrows(IllegalArgumentException.class, () -> validMessage(duplicateItems, false, 1800));

        List<WxCpMiniProgramContentItem> tooManyItems = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tooManyItems.add(new WxCpMiniProgramContentItem("键" + i, "值" + i));
        }
        assertThrows(IllegalArgumentException.class, () -> validMessage(tooManyItems, false, 1800));
        assertThrows(IllegalArgumentException.class, () ->
                WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                        "invalid", "", "会议预订成功", "", List.of(), false, 1800));
        assertThrows(IllegalArgumentException.class, () ->
                WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                        "wx1234567890abcdef", "", "短", "", List.of(), false, 1800));
        assertThrows(IllegalArgumentException.class, () ->
                WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                        "wx1234567890abcdef", "pages/" + "x".repeat(1024), "会议预订成功", "",
                        List.of(), false, 1800));
        assertThrows(IllegalArgumentException.class, () -> validMessage(List.of(), true, 14401));
        assertThrows(IllegalArgumentException.class, () -> WxCpMiniProgramNoticeSupport.validateRecipient("@all"));
        assertThrows(IllegalArgumentException.class, () -> WxCpMiniProgramNoticeSupport.validateRecipient(""));
        WxCpMiniProgramNoticeSupport.validateRecipient("zhangsan");
    }

    @Test
    public void shouldSerializeSdkMiniProgramNoticePayload() {
        LinkedHashMap<String, String> items = new LinkedHashMap<>();
        items.put("会议室", "402");
        items.put("会议时间", "今天 15:00");
        WxCpMessage message = WxCpMessage.newMiniProgramNoticeBuilder()
                .agentId(1000002)
                .toUser("zhangsan")
                .appId("wx1234567890abcdef")
                .page("pages/index?id=1")
                .title("会议预订成功")
                .description("今天下午三点")
                .contentItems(items)
                .emphasisFirstItem(true)
                .build();
        message.setEnableIdTrans(true);
        message.setEnableDuplicateCheck(true);
        message.setDuplicateCheckInterval(1800);

        JSONObject json = JSON.parseObject(message.toJson());
        assertEquals("miniprogram_notice", json.getString("msgtype"));
        assertEquals("zhangsan", json.getString("touser"));
        JSONObject notice = json.getJSONObject("miniprogram_notice");
        assertEquals("wx1234567890abcdef", notice.getString("appid"));
        assertTrue(notice.getBooleanValue("emphasis_first_item"));
        JSONArray contentItems = notice.getJSONArray("content_item");
        assertEquals("会议室", contentItems.getJSONObject(0).getString("key"));
        assertEquals("会议时间", contentItems.getJSONObject(1).getString("key"));
        assertTrue(json.getBooleanValue("enable_id_trans"));
        assertTrue(json.getBooleanValue("enable_duplicate_check"));
        assertEquals(1800, json.getIntValue("duplicate_check_interval"));
    }

    @Test
    public void shouldKeepLegacyWxCpMessageJsonCompatible() {
        TMsgWxCp legacy = JSON.parseObject(
                "{\"cpMsgType\":\"文本消息\",\"content\":\"hello\"}", TMsgWxCp.class);

        assertEquals("文本消息", legacy.getCpMsgType());
        assertEquals("hello", legacy.getContent());
        assertNull(legacy.getMiniProgramAppId());
        assertNull(legacy.getMiniProgramContentItems());
        assertFalse(Boolean.TRUE.equals(legacy.getEnableDuplicateCheck()));
    }

    private static LinkedHashMap<String, String> validMessage(
            List<WxCpMiniProgramContentItem> items, boolean duplicateCheck, int interval) {
        return WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                "wx1234567890abcdef", "", "会议预订成功", "", items, duplicateCheck, interval);
    }
}
