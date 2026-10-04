package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.bean.account.KsYunAccountConfig;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.logic.msgmaker.KsYunMsgMaker;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class KsYunMsgSenderTest {
    @Test
    public void signatureMatchesIndependentPythonVectorIncludingChineseAndReservedCharacters() throws Exception {
        Map<String, String> parameters = new TreeMap<>(Map.of(
                "Action", "SendSms", "Accesskey", "test-id", "Mobile", "13800138000",
                "Region", "cn-beijing-6", "Service", "ksms", "SignName", "测试 +*~/",
                "SignatureMethod", "HMAC-SHA256", "SignatureVersion", "1.0",
                "Timestamp", "2026-10-04T00:00:00Z", "TplId", "123"));
        parameters.put("TplParams", "{\"name\":\"张三 &+*~/\"}");
        parameters.put("Version", "2019-05-01");
        assertEquals("9cc1e48b5cc82618440a0d0e1a0d4b9987ad9d868e4d16af1900ce52d4df5fac",
                KsYunMsgSender.signature(parameters, "test-secret"));
    }

    @Test
    public void rendersNamedVariablesSignsPostAndHandlesFailuresWithoutFalseSuccess() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> response = new AtomicReference<>("{\"Sid\":\"sid-1\",\"RequestId\":\"req-1\"}");
        AtomicReference<Map<String, String>> posted = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            Map<String, String> parameters = new TreeMap<>();
            for (String pair : new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).split("&")) {
                String[] parts = pair.split("=", 2);
                parameters.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
            posted.set(parameters);
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            OkHttpClient client = new OkHttpClient();
            KsYunMsgSender sender = new KsYunMsgSender(account(), maker(), false, client,
                    "http://127.0.0.1:" + server.getAddress().getPort());
            SendResult success = sender.send(new String[]{"13800138000", "张三 &+*~/"});
            assertTrue(success.getInfo(), success.isSuccess());
            Map<String, String> parameters = posted.get();
            String signature = parameters.remove("Signature");
            assertEquals(KsYunMsgSender.signature(parameters, "test-secret"), signature);
            assertEquals("ksms", parameters.get("Service"));
            assertEquals("123", parameters.get("TplId"));
            assertEquals("张三 &+*~/", JSON.parseObject(parameters.get("TplParams")).getString("name"));
            response.set("{\"Error\":{\"Code\":\"InvalidMobile\"}}");
            assertFalse(sender.send(new String[]{"13800138000", "张三"}).isSuccess());
            response.set("{}");
            assertFalse(sender.send(new String[]{"13800138000", "张三"}).isSuccess());
            response.set("not-json");
            assertFalse(sender.send(new String[]{"13800138000", "张三"}).isSuccess());
            int before = calls.get();
            KsYunMsgSender dry = new KsYunMsgSender(account(), maker(), true, client,
                    "http://127.0.0.1:" + server.getAddress().getPort());
            assertTrue(dry.send(new String[]{"13800138000", "张三"}).isSuccess());
            assertFalse(dry.send(new String[]{"13800138000,13900139000", "张三"}).isSuccess());
            assertEquals(before, calls.get());
        } finally { server.stop(0); }
    }

    private static KsYunAccountConfig account() {
        KsYunAccountConfig config = new KsYunAccountConfig();
        config.setAccessKeyId("test-id");
        config.setAccessKeySecret("test-secret");
        config.setSign("测试签名");
        return config;
    }

    private static KsYunMsgMaker maker() {
        TemplateData parameter = new TemplateData();
        parameter.setName("name");
        parameter.setValue("${var1}");
        TMsgSms sms = new TMsgSms();
        sms.setTemplateId("123");
        sms.setTemplateDataList(List.of(parameter));
        TMsg message = new TMsg();
        message.setContent(JSON.toJSONString(sms));
        return new KsYunMsgMaker(message);
    }
}
