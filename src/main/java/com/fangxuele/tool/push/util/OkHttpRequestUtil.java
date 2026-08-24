package com.fangxuele.tool.push.util;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** 安全执行 OkHttp 请求并完整读取、关闭响应。 */
public final class OkHttpRequestUtil {
    private OkHttpRequestUtil() {
    }

    public static ResponseData execute(OkHttpClient client, Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            return new ResponseData(response.code(), body, response.headers(), response.protocol());
        }
    }

    public record ResponseData(int statusCode, String body, Headers headers, Protocol protocol) {
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public Long retryAfterMillis() {
            String retryAfter = headers.get("Retry-After");
            if (retryAfter == null) {
                return null;
            }
            try {
                return TimeUnit.SECONDS.toMillis(Long.parseLong(retryAfter.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
