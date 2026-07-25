package com.fangxuele.tool.push.util;

import cn.hutool.core.date.DateUtil;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 模板工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/1/5.
 */
public class TemplateUtil {

    private static final VelocityEngine velocityEngine;

    /**
     * openId → nickname，避免推送热路径每条同步打微信 API。
     */
    private static final Map<String, String> NICK_NAME_CACHE = new ConcurrentHashMap<>();

    static {
        velocityEngine = new VelocityEngine();
        velocityEngine.init();
    }

    public static String evaluate(String content, VelocityContext velocityContext) {

        if (content.contains("NICK_NAME")) {
            String nickName = resolveNickName(velocityContext);
            velocityContext.put("NICK_NAME", nickName);
        }

        velocityContext.put("ENTER", "\n");
        Date now = new Date();
        velocityContext.put("DATE", DateUtil.today());
        velocityContext.put("TIME", DateUtil.formatTime(now));
        velocityContext.put("DATE_TIME", DateUtil.formatDateTime(now));

        StringWriter writer = new StringWriter();
        velocityEngine.evaluate(velocityContext, writer, "", content);

        return writer.toString();
    }

    private static String resolveNickName(VelocityContext velocityContext) {
        Object openIdObj = velocityContext.get(PushControl.TEMPLATE_VAR_PREFIX + "0");
        if (openIdObj == null) {
            return "";
        }
        String openId = openIdObj.toString();
        if (openId.isEmpty()) {
            return "";
        }
        return NICK_NAME_CACHE.computeIfAbsent(openId, id -> {
            try {
                WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
                return wxMpService.getUserService().userInfo(id).getNickname();
            } catch (WxErrorException e) {
                return "";
            } catch (Exception e) {
                return "";
            }
        });
    }

    /**
     * 账号切换等场景可清空缓存。
     */
    public static void clearNickNameCache() {
        NICK_NAME_CACHE.clear();
    }
}
