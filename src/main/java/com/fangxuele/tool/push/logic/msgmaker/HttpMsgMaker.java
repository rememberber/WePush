package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.bean.HttpMsg;
import com.fangxuele.tool.push.ui.form.msg.HttpMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
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
public class HttpMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String method;
    public static String url;
    public static String body;
    public static String bodyType;
    public static List<HttpMsgForm.NameValueObject> paramList;
    public static List<HttpMsgForm.NameValueObject> headerList;
    public static List<HttpMsgForm.CookieObject> cookieList;

    @Override
    public void prepare() {
        method = (String) HttpMsgForm.getInstance().getMethodComboBox().getSelectedItem();
        url = HttpMsgForm.getInstance().getUrlTextField().getText().trim();
        body = HttpMsgForm.getInstance().getBodyTextArea().getText();
        bodyType = (String) HttpMsgForm.getInstance().getBodyTypeComboBox().getSelectedItem();

        // Params=========================
        if (HttpMsgForm.getInstance().getParamTable().getModel().getRowCount() == 0) {
            HttpMsgForm.initParamTable();
        }
        DefaultTableModel paramTableModel = (DefaultTableModel) HttpMsgForm.getInstance().getParamTable().getModel();
        int rowCount = paramTableModel.getRowCount();
        HttpMsgForm.NameValueObject nameValueObject;
        paramList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) paramTableModel.getValueAt(i, 0)).trim();
            String value = ((String) paramTableModel.getValueAt(i, 1)).trim();
            nameValueObject = new HttpMsgForm.NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            paramList.add(nameValueObject);
        }
        // Headers=========================
        if (HttpMsgForm.getInstance().getHeaderTable().getModel().getRowCount() == 0) {
            HttpMsgForm.initHeaderTable();
        }
        DefaultTableModel headerTableModel = (DefaultTableModel) HttpMsgForm.getInstance().getHeaderTable().getModel();
        rowCount = headerTableModel.getRowCount();
        headerList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) headerTableModel.getValueAt(i, 0)).trim();
            String value = ((String) headerTableModel.getValueAt(i, 1)).trim();
            nameValueObject = new HttpMsgForm.NameValueObject();
            nameValueObject.setName(name);
            nameValueObject.setValue(value);
            headerList.add(nameValueObject);
        }
        // Cookies=========================
        if (HttpMsgForm.getInstance().getCookieTable().getModel().getRowCount() == 0) {
            HttpMsgForm.initCookieTable();
        }
        DefaultTableModel cookieTableModel = (DefaultTableModel) HttpMsgForm.getInstance().getCookieTable().getModel();
        rowCount = cookieTableModel.getRowCount();
        cookieList = Lists.newArrayList();
        HttpMsgForm.CookieObject cookieObject;
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) cookieTableModel.getValueAt(i, 0)).trim();
            String value = ((String) cookieTableModel.getValueAt(i, 1)).trim();
            String domain = ((String) cookieTableModel.getValueAt(i, 2)).trim();
            String path = ((String) cookieTableModel.getValueAt(i, 3)).trim();
            String expiry = ((String) cookieTableModel.getValueAt(i, 4)).trim();
            cookieObject = new HttpMsgForm.CookieObject();
            cookieObject.setName(name);
            cookieObject.setValue(value);
            cookieObject.setDomain(domain);
            cookieObject.setPath(path);
            cookieObject.setExpiry(expiry);
            cookieList.add(cookieObject);
        }
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
