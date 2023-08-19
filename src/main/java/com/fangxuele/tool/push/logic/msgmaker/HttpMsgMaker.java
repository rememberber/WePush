package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fangxuele.tool.push.bean.msg.HttpMsg;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgHttp;
import com.fangxuele.tool.push.ui.form.msg.HttpMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.velocity.VelocityContext;

import java.net.HttpCookie;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

/**
 * <pre>
 * http消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/16.
 */
@Slf4j
@Getter
public class HttpMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String method;
    private String url;
    private String body;
    private String bodyType;
    private List<HttpMsgForm.NameValueObject> paramList;
    private List<HttpMsgForm.NameValueObject> headerList;
    private List<HttpMsgForm.CookieObject> cookieList;

    public HttpMsgMaker(TMsg tMsg) {
        TMsgHttp tMsgHttp = JSON.parseObject(tMsg.getContent(), TMsgHttp.class);

        method = tMsgHttp.getMethod();
        url = tMsgHttp.getUrl();
        body = tMsgHttp.getBody();
        bodyType = tMsgHttp.getBodyType();

        paramList = JSON.parseObject(tMsgHttp.getParams(), new TypeReference<List<HttpMsgForm.NameValueObject>>() {
        });
        headerList = JSON.parseObject(tMsgHttp.getHeaders(), new TypeReference<List<HttpMsgForm.NameValueObject>>() {
        });
        cookieList = JSON.parseObject(tMsgHttp.getCookies(), new TypeReference<List<HttpMsgForm.CookieObject>>() {
        });
    }

    @Override
    public HttpMsg makeMsg(String[] msgData) {
        HttpMsg httpMsg = new HttpMsg();

        VelocityContext velocityContext = getVelocityContext(msgData);
        httpMsg.setUrl(TemplateUtil.evaluate(url, velocityContext));
        httpMsg.setBody(TemplateUtil.evaluate(body, velocityContext));

        HashMap<String, Object> paramMap = Maps.newHashMap();
        for (HttpMsgForm.NameValueObject nameValueObject : paramList) {
            paramMap.put(nameValueObject.getName(), TemplateUtil.evaluate(nameValueObject.getValue(), velocityContext));
        }
        httpMsg.setParamMap(paramMap);

        HashMap<String, Object> headerMap = Maps.newHashMap();
        for (HttpMsgForm.NameValueObject nameValueObject : headerList) {
            headerMap.put(nameValueObject.getName(), TemplateUtil.evaluate(nameValueObject.getValue(), velocityContext));
        }
        httpMsg.setHeaderMap(headerMap);

        List<HttpCookie> cookies = Lists.newArrayList();
        for (HttpMsgForm.CookieObject cookieObject : cookieList) {
            HttpCookie httpCookie = new HttpCookie(cookieObject.getName(), TemplateUtil.evaluate(cookieObject.getValue(), velocityContext));
            httpCookie.setDomain(cookieObject.getDomain());
            httpCookie.setPath(cookieObject.getPath());
            try {
                httpCookie.setMaxAge(DateUtils.parseDate(cookieObject.getExpiry(), "yyyy-MM-dd HH:mm:ss").getTime());
            } catch (ParseException e) {
                log.error(e.toString());
            }
            cookies.add(httpCookie);
        }
        httpMsg.setCookies(cookies);

        return httpMsg;
    }
}
