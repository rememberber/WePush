package com.fangxuele.tool.push.util;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.fangxuele.tool.push.App;
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
    private TimedCache<String, String> timedCache = CacheUtil.newTimedCache(2000);

    private int count;

    @Override
    public String getAccessToken(boolean forceRefresh) throws WxErrorException {
        if (!this.getWxMpConfigStorage().isAccessTokenExpired() && !forceRefresh) {
            return this.getWxMpConfigStorage().getAccessToken();
        }

        Lock lock = this.getWxMpConfigStorage().getAccessTokenLock();
        lock.lock();

        try {
            System.err.println("hello");
            if (timedCache.get("count") != null && Integer.parseInt(timedCache.get("count")) > 10) {
                WxError wxError = WxError.builder().errorCode(98).errorMsg("短时间内大量获取AccessToken失败").errorMsgEn("Fail to get AccessToken in a shot period").json("").build();
                count = 0;
                throw new WxErrorException(wxError);

            } else {
                count++;
                timedCache.put("count", String.valueOf(count));
            }

            try {
                WxAccessToken accessToken;

                if (App.config.isMpUseOutSideAt() && App.config.isMpManualAt()) {
                    accessToken = new WxAccessToken();
                    accessToken.setAccessToken(App.config.getMpAt());
                    accessToken.setExpiresIn(Integer.parseInt(App.config.getMpAtExpiresIn()));
                } else {
                    String url = String.format(WxMpService.GET_ACCESS_TOKEN_URL,
                            this.getWxMpConfigStorage().getAppId(), this.getWxMpConfigStorage().getSecret());
                    if (App.config.isMpUseOutSideAt() && App.config.isMpApiAt()) {
                        url = App.config.getMpAtApiUrl();
                    }
                    HttpGet httpGet = new HttpGet(url);
                    if (this.getRequestHttpProxy() != null) {
                        RequestConfig config = RequestConfig.custom().setProxy(this.getRequestHttpProxy()).build();
                        httpGet.setConfig(config);
                    }
                    try (CloseableHttpResponse response = getRequestHttpClient().execute(httpGet)) {
                        String resultContent = new BasicResponseHandler().handleResponse(response);
                        WxError error = WxError.fromJson(resultContent, WxType.MP);
                        if (error.getErrorCode() != 0) {
                            if (App.config.isMpUseOutSideAt() && App.config.isMpApiAt()) {
                                error = WxError.builder().errorCode(99).errorMsg("通过接口" + url + "获取AccessToken失败").errorMsgEn("Fail to get AccessToken from:" + url).json(resultContent).build();
                                throw new WxErrorException(error);
                            } else {
                                throw new WxErrorException(error);
                            }
                        }
                        accessToken = WxAccessToken.fromJson(resultContent);
                    } finally {
                        httpGet.releaseConnection();
                    }
                }

                this.getWxMpConfigStorage().updateAccessToken(accessToken.getAccessToken(), accessToken.getExpiresIn());
                return this.getWxMpConfigStorage().getAccessToken();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }

    }
}
