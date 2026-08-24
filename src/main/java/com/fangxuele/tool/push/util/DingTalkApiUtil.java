package com.fangxuele.tool.push.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;

/**
 * <pre>
 * 钉钉开放平台简单HTTP客户端
 * 接口文档：https://open.dingtalk.com/document/orgapp-server/
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 */
public class DingTalkApiUtil {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private DingTalkApiUtil() {
    }

    /**
     * GET请求，返回响应JSON
     *
     * @param url 完整url（含查询参数）
     * @return 响应JSON
     */
    public static JSONObject get(String url) throws IOException {
        return get(HttpClientRegistry.get(MessageTypeEnum.DING_CODE, 0), url);
    }

    public static JSONObject get(OkHttpClient httpClient, String url) throws IOException {
        return getResponse(httpClient, url).body();
    }

    public static JsonResponse getResponse(OkHttpClient httpClient, String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
        ensureSuccessful(response);
        return jsonResponse(response);
    }

    /**
     * POST JSON请求，返回响应JSON
     *
     * @param url  完整url（含查询参数）
     * @param body 请求体JSON
     * @return 响应JSON
     */
    public static JSONObject postJson(String url, JSONObject body) throws IOException {
        return postJson(HttpClientRegistry.get(MessageTypeEnum.DING_CODE, 0), url, body);
    }

    public static JSONObject postJson(OkHttpClient httpClient, String url, JSONObject body) throws IOException {
        return postJsonResponse(httpClient, url, body).body();
    }

    public static JsonResponse postJsonResponse(OkHttpClient httpClient, String url, JSONObject body) throws IOException {
        Request request = new Request.Builder().url(url)
                .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE)).build();
        OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(httpClient, request);
        ensureSuccessful(response);
        return jsonResponse(response);
    }

    private static JsonResponse jsonResponse(OkHttpRequestUtil.ResponseData response) {
        JSONObject body = StringUtils.isBlank(response.body()) ? new JSONObject() : JSON.parseObject(response.body());
        return new JsonResponse(body, response.statusCode(), response.retryAfterMillis());
    }

    private static void ensureSuccessful(OkHttpRequestUtil.ResponseData response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("钉钉 HTTP 请求失败（" + response.statusCode() + "）：" + response.body());
        }
    }

    public record JsonResponse(JSONObject body, int statusCode, Long retryAfterMillis) {
    }
}
