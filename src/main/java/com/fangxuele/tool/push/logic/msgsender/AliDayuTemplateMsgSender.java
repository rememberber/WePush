package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.AliTemplateMsgMaker;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 * 阿里大于模板短信消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class AliDayuTemplateMsgSender implements IMsgSender {
    /**
     * 阿里大于短信client
     */
    public volatile static TaobaoClient taobaoClient;

    private AliTemplateMsgMaker aliTemplateMsgMaker;

    public AliDayuTemplateMsgSender() {
        aliTemplateMsgMaker = new AliTemplateMsgMaker();
        taobaoClient = getTaobaoClient();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult result = new SendResult();

        try {
            AlibabaAliqinFcSmsNumSendRequest alibabaAliqinFcSmsNumSendRequest = aliTemplateMsgMaker.makeMsg(msgData);
            if (PushControl.dryRun) {
                result.setSuccess(true);
                return result;
            } else {
                AlibabaAliqinFcSmsNumSendResponse response = taobaoClient.execute(alibabaAliqinFcSmsNumSendRequest);
                if (response.getResult() != null && response.getResult().getSuccess()) {
                    result.setSuccess(true);
                } else {
                    result.setSuccess(false);
                    result.setInfo(response.getBody() + ";ErrorCode:" + response.getErrorCode());
                }
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setInfo(e.getMessage());
            log.error(e.toString());
        }

        return result;
    }

    /**
     * 获取阿里大于短信发送客户端
     *
     * @return TaobaoClient
     */
    private static TaobaoClient getTaobaoClient() {
        if (taobaoClient == null) {
            synchronized (AliDayuTemplateMsgSender.class) {
                if (taobaoClient == null) {
                    String aliServerUrl = App.config.getAliServerUrl();
                    String aliAppKey = App.config.getAliAppKey();
                    String aliAppSecret = App.config.getAliAppSecret();

                    taobaoClient = new DefaultTaobaoClient(aliServerUrl, aliAppKey, aliAppSecret);
                }
            }
        }
        return taobaoClient;
    }
}
