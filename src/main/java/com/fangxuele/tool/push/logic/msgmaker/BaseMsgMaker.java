package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.logic.PushControl;
import org.apache.velocity.VelocityContext;

/**
 * <pre>
 * 消息加工器基类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class BaseMsgMaker {
    /**
     * 获取模板引擎上下文
     *
     * @param msgData 消息数据
     * @return VelocityContext 模板引擎上下文
     */
    VelocityContext getVelocityContext(String[] msgData) {
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        return velocityContext;
    }
}
