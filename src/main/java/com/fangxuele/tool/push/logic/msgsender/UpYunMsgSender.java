package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.UpYunMsgMaker;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 又拍云模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class UpYunMsgSender implements IMsgSender {
    /**
     * 又拍云短信sender
     */
    public volatile static OkHttpClient okHttpClint;

    private UpYunMsgMaker upYunMsgMaker;

    private static final String URL = "https://sms-api.upyun.com/api/messages";

    public UpYunMsgSender() {
        upYunMsgMaker = new UpYunMsgMaker();
        okHttpClint = HttpMsgSender.getOkHttpClient();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = UpYunMsgMaker.templateId;
            String[] params = upYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];

            Request.Builder requestBuilder = new Request.Builder();
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            formBodyBuilder.add("mobile", telNum);
            formBodyBuilder.add("template_id", templateId);
            formBodyBuilder.add("vars", String.join("|", params));
            RequestBody requestBody = formBodyBuilder.build();
            requestBuilder.url(URL).post(requestBody);
            requestBuilder.addHeader("Authorization", App.config.getUpAuthorizationToken());
            Request request = requestBuilder.build();
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Response response = okHttpClint.newCall(request).execute();
                if (response.isSuccessful()) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.toString());
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

}
