package com.fangxuele.tool.push.util;

import me.chanjar.weixin.common.WxType;
import me.chanjar.weixin.common.bean.WxAccessToken;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

/**
 * <pre>
 * Description
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/21.
 */
public class WeWxMpServiceImpl extends WxMpServiceImpl {
    @Override
    public String getAccessToken() throws WxErrorException {
        String accessToken = super.getAccessToken();
        System.err.println(accessToken);
        return accessToken;
    }

    @Override
    public String getAccessToken(boolean forceRefresh) throws WxErrorException {
        if (!this.getWxMpConfigStorage().isAccessTokenExpired() && !forceRefresh) {
            return this.getWxMpConfigStorage().getAccessToken();
        }

        Lock lock = this.getWxMpConfigStorage().getAccessTokenLock();
        lock.lock();
        try {
            String url = String.format(WxMpService.GET_ACCESS_TOKEN_URL,
                    this.getWxMpConfigStorage().getAppId(), this.getWxMpConfigStorage().getSecret());
            try {
                HttpGet httpGet = new HttpGet(url);
                if (this.getRequestHttpProxy() != null) {
                    RequestConfig config = RequestConfig.custom().setProxy(this.getRequestHttpProxy()).build();
                    httpGet.setConfig(config);
                }
                try (CloseableHttpResponse response = getRequestHttpClient().execute(httpGet)) {
                    String resultContent = new BasicResponseHandler().handleResponse(response);
                    WxError error = WxError.fromJson(resultContent, WxType.MP);
                    if (error.getErrorCode() != 0) {
                        throw new WxErrorException(error);
                    }
                    WxAccessToken accessToken = WxAccessToken.fromJson(resultContent);
                    this.getWxMpConfigStorage().updateAccessToken(accessToken.getAccessToken(), accessToken.getExpiresIn());
                    return this.getWxMpConfigStorage().getAccessToken();
                } finally {
                    httpGet.releaseConnection();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }
}
