package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.TxYun3AccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.TxYun3MsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 腾讯云3.0模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2023/9/4.
 */
@Slf4j
public class TxYun3MsgSender implements IMsgSender {
    /**
     * 腾讯云短信sender
     */
    private SmsClient smsClient;

    private TxYun3MsgMaker txYun3MsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, SmsClient> smsClientMap = new HashMap<>();

    private TxYun3AccountConfig txYun3AccountConfig;


    public TxYun3MsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        txYun3MsgMaker = new TxYun3MsgMaker(tMsg);
        smsClient = getTxYunSender(tMsg.getAccountId());
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        txYun3AccountConfig = JSON.parseObject(accountConfig, TxYun3AccountConfig.class);
    }

    public static void removeAccount(Integer account1Id) {
        smsClientMap.remove(account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            String templateId = txYun3MsgMaker.getTemplateId();
            String smsSign = txYun3AccountConfig.getSign();
            String[] params = txYun3MsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                SendSmsRequest req = new SendSmsRequest();

                /* 填充请求参数,这里request对象的成员变量即对应接口的入参
                 * 你可以通过官网接口文档或跳转到request对象的定义处查看请求参数的定义
                 * 基本类型的设置:
                 * 帮助链接：
                 * 短信控制台: https://console.cloud.tencent.com/smsv2
                 * 腾讯云短信小助手: https://cloud.tencent.com/document/product/382/3773#.E6.8A.80.E6.9C.AF.E4.BA.A4.E6.B5.81 */

                /* 短信应用ID: 短信SdkAppId在 [短信控制台] 添加应用后生成的实际SdkAppId，示例如1400006666 */
                // 应用 ID 可前往 [短信控制台](https://console.cloud.tencent.com/smsv2/app-manage) 查看
                String sdkAppId = txYun3AccountConfig.getSdkAppId();
                req.setSmsSdkAppId(sdkAppId);

                /* 短信签名内容: 使用 UTF-8 编码，必须填写已审核通过的签名 */
                // 签名信息可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-sign) 或 [国际/港澳台短信](https://console.cloud.tencent.com/smsv2/isms-sign) 的签名管理查看
                req.setSignName(smsSign);

                /* 模板 ID: 必须填写已审核通过的模板 ID */
                // 模板 ID 可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-template) 或 [国际/港澳台短信](https://console.cloud.tencent.com/smsv2/isms-template) 的正文模板管理查看
                req.setTemplateId(templateId);

                /* 模板参数: 模板参数的个数需要与 TemplateId 对应模板的变量个数保持一致，若无模板参数，则设置为空 */
                String[] templateParamSet = params;
                req.setTemplateParamSet(templateParamSet);

                /* 下发手机号码，采用 E.164 标准，+[国家或地区码][手机号]
                 * 示例如：+8613711112222， 其中前面有一个+号 ，86为国家码，13711112222为手机号，最多不要超过200个手机号 */
                String[] phoneNumberSet = {telNum};
                req.setPhoneNumberSet(phoneNumberSet);

                /* 用户的 session 内容（无需要可忽略）: 可以携带用户侧 ID 等上下文信息，server 会原样返回 */
//                String sessionContext = "";
//                req.setSessionContext(sessionContext);

                /* 短信码号扩展号（无需要可忽略）: 默认未开通，如需开通请联系 [腾讯云短信小助手] */
//                String extendCode = "";
//                req.setExtendCode(extendCode);

                /* 国内短信无需填写该项；国际/港澳台短信已申请独立 SenderId 需要填写该字段，默认使用公共 SenderId，无需填写该字段。注：月度使用量达到指定量级可申请独立 SenderId 使用，详情请联系 [腾讯云短信小助手](https://cloud.tencent.com/document/product/382/3773#.E6.8A.80.E6.9C.AF.E4.BA.A4.E6.B5.81)。*/
//                String senderid = "";
//                req.setSenderId(senderid);

                /* 通过 client 对象调用 SendSms 方法发起请求。注意请求方法名与请求对象是对应的
                 * 返回的 res 是一个 SendSmsResponse 类的实例，与请求对象对应 */
                SendSmsResponse res = smsClient.SendSms(req);

                if (res.getSendStatusSet()[0].getCode().equals("Ok")) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(JSONUtil.toJsonStr(res));
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

    public SmsClient getTxYunSender(Integer accountId) {
        if (smsClientMap.containsKey(accountId)) {
            return smsClientMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            TxYun3AccountConfig txYun3AccountConfig = JSON.parseObject(accountConfig, TxYun3AccountConfig.class);

            /* 必要步骤：
             * 实例化一个认证对象，入参需要传入腾讯云账户密钥对secretId，secretKey。
             * 这里采用的是从环境变量读取的方式，需要在环境变量中先设置这两个值。
             * 你也可以直接在代码中写死密钥对，但是小心不要将代码复制、上传或者分享给他人，
             * 以免泄露密钥对危及你的财产安全。
             * SecretId、SecretKey 查询: https://console.cloud.tencent.com/cam/capi */
            Credential cred = new Credential(txYun3AccountConfig.getSecretId(), txYun3AccountConfig.getSecretKey());


            // 实例化一个http选项，可选，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            // 设置代理（无需要直接忽略）
            // httpProfile.setProxyHost("真实代理ip");
            // httpProfile.setProxyPort(真实代理端口);
            /* SDK默认使用POST方法。
             * 如果你一定要使用GET方法，可以在这里设置。GET方法无法处理一些较大的请求 */
            httpProfile.setReqMethod("POST");
            /* SDK有默认的超时时间，非必要请不要进行调整
             * 如有需要请在代码中查阅以获取最新的默认值 */
//            httpProfile.setConnTimeout(60);
            /* 指定接入地域域名，默认就近地域接入域名为 sms.tencentcloudapi.com ，也支持指定地域域名访问，例如广州地域的域名为 sms.ap-guangzhou.tencentcloudapi.com */
            httpProfile.setEndpoint(txYun3AccountConfig.getEndPoint());


            /* 非必要步骤:
             * 实例化一个客户端配置对象，可以指定超时时间等配置 */
            ClientProfile clientProfile = new ClientProfile();
            /* SDK默认用TC3-HMAC-SHA256进行签名
             * 非必要请不要修改这个字段 */
//            clientProfile.setSignMethod("HmacSHA256");
            clientProfile.setHttpProfile(httpProfile);
            /* 实例化要请求产品(以sms为例)的client对象
             * 第二个参数是地域信息，可以直接填写字符串ap-guangzhou，支持的地域列表参考 https://cloud.tencent.com/document/api/382/52071#.E5.9C.B0.E5.9F.9F.E5.88.97.E8.A1.A8 */
            SmsClient client = new SmsClient(cred, txYun3AccountConfig.getRegion());

            smsClientMap.put(accountId, client);
            return client;
        }
    }
}
