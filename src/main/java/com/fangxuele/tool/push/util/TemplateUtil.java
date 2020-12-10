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

/**
 * <pre>
 * 模板工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/1/5.
 */
public class TemplateUtil {

    private static VelocityEngine velocityEngine;

    static {
        velocityEngine = new VelocityEngine();
        velocityEngine.init();
    }

    public static String evaluate(String content, VelocityContext velocityContext) {

        if (content.contains("NICK_NAME")) {
            WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
            String nickName = "";
            try {
                nickName = wxMpService.getUserService().userInfo(velocityContext.get(PushControl.TEMPLATE_VAR_PREFIX + "0").toString()).getNickname();
            } catch (WxErrorException e) {
                e.printStackTrace();
            }
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
}
