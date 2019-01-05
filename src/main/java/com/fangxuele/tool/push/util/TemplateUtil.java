package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.logic.PushManage;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;

/**
 * 模板工具
 * Created by rememberber(https://github.com/rememberber) on 2019/1/5.
 */
public class TemplateUtil {

    private static VelocityEngine velocityEngine;

    static {
        velocityEngine = new VelocityEngine();
        velocityEngine.init();
    }

    public static String evaluate(String content, VelocityContext velocityContext) {

        if (content.contains("NICK_NAME")) {
            WxMpService wxMpService = PushManage.getWxMpService();
            String nickName = "";
            try {
                nickName = wxMpService.getUserService().userInfo(velocityContext.get(PushManage.TEMPLATE_VAR_PREFIX + "0").toString()).getNickname();
            } catch (WxErrorException e) {
                e.printStackTrace();
            }
            velocityContext.put("NICK_NAME", nickName);
        }

        velocityContext.put("ENTER", "\n");

        StringWriter writer = new StringWriter();
        velocityEngine.evaluate(velocityContext, writer, "", content);

        return writer.toString();
    }
}
