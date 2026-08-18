package com.fangxuele.tool.push.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <pre>
 * 钉钉开放平台简单HTTP客户端
 * 接口文档：https://open.dingtalk.com/document/orgapp-server/
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 */
public class DingTalkApiUtil {

    private DingTalkApiUtil() {
    }

    /**
     * GET请求，返回响应JSON
     *
     * @param url 完整url（含查询参数）
     * @return 响应JSON
     */
    public static JSONObject get(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(RequestBuilder.create("GET").setUri(url).build());
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            return StringUtils.isBlank(responseBody) ? new JSONObject() : JSON.parseObject(responseBody);
        }
    }

    /**
     * POST JSON请求，返回响应JSON
     *
     * @param url  完整url（含查询参数）
     * @param body 请求体JSON
     * @return 响应JSON
     */
    public static JSONObject postJson(String url, JSONObject body) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(RequestBuilder.create("POST")
                    .setUri(url)
                    .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                    .setEntity(new StringEntity(body.toJSONString(), Charset.forName("UTF-8"))).build());
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            return StringUtils.isBlank(responseBody) ? new JSONObject() : JSON.parseObject(responseBody);
        }
    }
}
