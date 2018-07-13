package com.fangxuele.tool.wechat.push.util;

import com.yunpian.sdk.YunpianClient;
import com.yunpian.sdk.model.Result;
import com.yunpian.sdk.model.SmsSingleSend;

import java.io.File;
import java.util.Map;

/**
 * 系统工具
 */
public class SystemUtil {
    public static String osName = System.getProperty("os.name");
    public static String configHome = System.getProperty("user.home") + File.separator + ".wepush"
            + File.separator;

    public static boolean isMacOs() {
        if (osName.contains("Mac")) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {

        //初始化clnt,使用单例方式
        YunpianClient clnt = new YunpianClient("6f6acda6a2c8e30b7100772a24620a3d").init();

        //发送短信API
        Map<String, String> param = clnt.newParam(2);
        param.put(YunpianClient.MOBILE, "17180103770");
        param.put(YunpianClient.TEXT, "【云片网】您的验证码是1234");
        Result<SmsSingleSend> r = clnt.sms().single_send(param);
        //获取返回结果，返回码:r.getCode(),返回码描述:r.getMsg(),API结果:r.getData(),其他说明:r.getDetail(),调用异常:r.getThrowable()

        //账户:clnt.user().* 签名:clnt.sign().* 模版:clnt.tpl().* 短信:clnt.sms().* 语音:clnt.voice().* 流量:clnt.flow().* 隐私通话:clnt.call().*
        System.err.println(r);
        System.err.println(clnt.tpl());
        //释放clnt
        clnt.close();
    }
}
