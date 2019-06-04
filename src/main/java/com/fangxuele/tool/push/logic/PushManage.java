package com.fangxuele.tool.push.logic;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.binarywang.wx.miniapp.config.WxMaInMemoryConfig;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.profile.DefaultProfile;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.domain.TPushHistory;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.form.TxYunMsgForm;
import com.fangxuele.tool.push.ui.listener.MemberListener;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.opencsv.CSVWriter;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;
import com.yunpian.sdk.YunpianClient;
import com.yunpian.sdk.model.Result;
import com.yunpian.sdk.model.SmsSingleSend;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 推送管理
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
 */
public class PushManage {

    private static final Log logger = LogFactory.get();

    private static TPushHistoryMapper pushHistoryMapper = MybatisUtil.getSqlSession().getMapper(TPushHistoryMapper.class);

    /**
     * 模板变量前缀
     */
    public static final String TEMPLATE_VAR_PREFIX = "var";

    public volatile static WxMpService wxMpService;

    /**
     * 阿里云短信client
     */
    public volatile static IAcsClient iAcsClient;

    /**
     * 腾讯云短信sender
     */
    public volatile static SmsSingleSender smsSingleSender;

    /**
     * 阿里大于短信client
     */
    public volatile static TaobaoClient taobaoClient;

    /**
     * 云片网短信client
     */
    public volatile static YunpianClient yunpianClient;

    public volatile static WxMpInMemoryConfigStorage wxMpConfigStorage;

    public volatile static WxMaService wxMaService;

    public volatile static WxMaInMemoryConfig wxMaConfigStorage;

    /**
     * 预览消息
     *
     * @throws Exception 异常
     */
    public static boolean preview() throws Exception {
        List<String[]> msgDataList = new ArrayList<>();

        for (String data : MessageEditForm.messageEditForm.getPreviewUserField().getText().split(";")) {
            msgDataList.add(data.split(MemberListener.TXT_FILE_DATA_SEPERATOR_REGEX));
        }

        switch (App.config.getMsgType()) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                WxMpTemplateMessage wxMessageTemplate;
                WxMpService wxMpService = getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return false;
                }

                for (String[] msgData : msgDataList) {
                    wxMessageTemplate = MessageMaker.makeMpTemplateMessage(msgData);
                    wxMessageTemplate.setToUser(msgData[0].trim());
                    // ！！！发送模板消息！！！
                    wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                }
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                WxMaTemplateMessage wxMaMessageTemplate;
                WxMaService wxMaService = getWxMaService();
                if (wxMaService.getWxMaConfig() == null) {
                    return false;
                }

                for (String[] msgData : msgDataList) {
                    wxMaMessageTemplate = MessageMaker.makeMaTemplateMessage(msgData);
                    wxMaMessageTemplate.setToUser(msgData[0].trim());
                    wxMaMessageTemplate.setFormId(msgData[1].trim());
                    // ！！！发送小程序模板消息！！！
                    wxMaService.getMsgService().sendTemplateMsg(wxMaMessageTemplate);
                }
                break;
            case MessageTypeEnum.KEFU_CODE:
                wxMpService = getWxMpService();
                WxMpKefuMessage wxMpKefuMessage;
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return false;
                }

                for (String[] msgData : msgDataList) {
                    wxMpKefuMessage = MessageMaker.makeKefuMessage(msgData);
                    wxMpKefuMessage.setToUser(msgData[0]);
                    // ！！！发送客服消息！！！
                    wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                }
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                wxMpService = getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return false;
                }

                for (String[] msgData : msgDataList) {
                    try {
                        wxMpKefuMessage = MessageMaker.makeKefuMessage(msgData);
                        wxMpKefuMessage.setToUser(msgData[0]);
                        // ！！！发送客服消息！！！
                        wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    } catch (Exception e) {
                        wxMessageTemplate = MessageMaker.makeMpTemplateMessage(msgData);
                        wxMessageTemplate.setToUser(msgData[0].trim());
                        // ！！！发送模板消息！！！
                        wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                    }
                }
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                String aliyunAccessKeyId = App.config.getAliyunAccessKeyId();
                String aliyunAccessKeySecret = App.config.getAliyunAccessKeySecret();

                if (StringUtils.isEmpty(aliyunAccessKeyId) || StringUtils.isEmpty(aliyunAccessKeySecret)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存阿里云短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                IAcsClient acsClient = getAliyunIAcsClient();
                for (String[] msgData : msgDataList) {
                    SendSmsRequest request = MessageMaker.makeAliyunMessage(msgData);
                    request.setPhoneNumbers(msgData[0]);
                    SendSmsResponse response = acsClient.getAcsResponse(request);

                    if (response.getCode() == null || !"OK".equals(response.getCode())) {
                        throw new Exception(response.getMessage() + ";\n\nErrorCode:" +
                                response.getCode() + ";\n\ntelNum:" + msgData[0]);
                    }
                }
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                String txyunAppId = App.config.getTxyunAppId();
                String txyunAppKey = App.config.getTxyunAppKey();

                if (StringUtils.isEmpty(txyunAppId) || StringUtils.isEmpty(txyunAppKey)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存腾讯云短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                SmsSingleSender smsSingleSender = getTxYunSender();

                for (String[] msgData : msgDataList) {
                    String[] params = MessageMaker.makeTxyunMessage(msgData);
                    SmsSingleSenderResult result = smsSingleSender.sendWithParam("86", msgData[0],
                            Integer.valueOf(TxYunMsgForm.txYunMsgForm.getMsgTemplateIdTextField().getText()),
                            params, App.config.getAliyunSign(), "", "");
                    if (result.result != 0) {
                        throw new Exception(result.toString());
                    }
                }
                break;
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                String aliServerUrl = App.config.getAliServerUrl();
                String aliAppKey = App.config.getAliAppKey();
                String aliAppSecret = App.config.getAliAppSecret();

                if (StringUtils.isEmpty(aliServerUrl) || StringUtils.isEmpty(aliAppKey)
                        || StringUtils.isEmpty(aliAppSecret)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存阿里大于相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                TaobaoClient client = getTaobaoClient();
                for (String[] msgData : msgDataList) {
                    AlibabaAliqinFcSmsNumSendRequest request = MessageMaker.makeAliTemplateMessage(msgData);
                    request.setRecNum(msgData[0]);
                    AlibabaAliqinFcSmsNumSendResponse response = client.execute(request);
                    if (response.getResult() == null || !response.getResult().getSuccess()) {
                        throw new Exception(response.getBody() + ";\n\nErrorCode:" +
                                response.getErrorCode() + ";\n\ntelNum:" + msgData[0]);
                    }
                }
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                String yunpianApiKey = App.config.getYunpianApiKey();

                if (StringUtils.isEmpty(yunpianApiKey)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存云片网短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                YunpianClient yunpianClient = getYunpianClient();

                for (String[] msgData : msgDataList) {
                    Map<String, String> params = MessageMaker.makeYunpianMessage(msgData);
                    params.put(YunpianClient.MOBILE, msgData[0]);
                    Result<SmsSingleSend> result = yunpianClient.sms().single_send(params);
                    if (result.getCode() != 0) {
                        throw new Exception(result.toString());
                    }
                }
                yunpianClient.close();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 微信公众号配置
     *
     * @return WxMpConfigStorage
     */
    private static WxMpInMemoryConfigStorage wxMpConfigStorage() {
        if (StringUtils.isEmpty(App.config.getWechatAppId()) || StringUtils.isEmpty(App.config.getWechatAppSecret())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存公众号相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        WxMpInMemoryConfigStorage configStorage = new WxMpInMemoryConfigStorage();
        configStorage.setAppId(App.config.getWechatAppId());
        configStorage.setSecret(App.config.getWechatAppSecret());
        configStorage.setToken(App.config.getWechatToken());
        configStorage.setAesKey(App.config.getWechatAesKey());
        return configStorage;
    }

    /**
     * 微信小程序配置
     *
     * @return WxMaInMemoryConfig
     */
    private static WxMaInMemoryConfig wxMaConfigStorage() {
        WxMaInMemoryConfig configStorage = new WxMaInMemoryConfig();
        if (StringUtils.isEmpty(App.config.getMiniAppAppId()) || StringUtils.isEmpty(App.config.getMiniAppAppSecret())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存小程序相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        configStorage.setAppid(App.config.getMiniAppAppId());
        configStorage.setSecret(App.config.getMiniAppAppSecret());
        configStorage.setToken(App.config.getMiniAppToken());
        configStorage.setAesKey(App.config.getMiniAppAesKey());
        configStorage.setMsgDataFormat("JSON");
        return configStorage;
    }

    /**
     * 获取微信公众号工具服务
     *
     * @return WxMpService
     */
    public static WxMpService getWxMpService() {
        if (wxMpConfigStorage == null) {
            synchronized (PushManage.class) {
                if (wxMpConfigStorage == null) {
                    wxMpConfigStorage = wxMpConfigStorage();
                }
            }
        }
        if (wxMpService == null && wxMpConfigStorage != null) {
            synchronized (PushManage.class) {
                if (wxMpService == null && wxMpConfigStorage != null) {
                    wxMpService = new WxMpServiceImpl();
                    wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
                }
            }
        }
        return wxMpService;
    }

    /**
     * 获取微信小程序工具服务
     *
     * @return WxMaService
     */
    static WxMaService getWxMaService() {
        if (wxMaService == null) {
            synchronized (PushManage.class) {
                if (wxMaService == null) {
                    wxMaService = new WxMaServiceImpl();
                }
            }
        }
        if (wxMaConfigStorage == null) {
            synchronized (PushManage.class) {
                if (wxMaConfigStorage == null) {
                    wxMaConfigStorage = wxMaConfigStorage();
                    if (wxMaConfigStorage != null) {
                        wxMaService.setWxMaConfig(wxMaConfigStorage);
                    }
                }
            }
        }
        return wxMaService;
    }

    /**
     * 获取阿里云短信发送客户端
     *
     * @return IAcsClient
     */
    public static IAcsClient getAliyunIAcsClient() {
        if (iAcsClient == null) {
            synchronized (PushManage.class) {
                if (iAcsClient == null) {
                    String aliyunAccessKeyId = App.config.getAliyunAccessKeyId();
                    String aliyunAccessKeySecret = App.config.getAliyunAccessKeySecret();

                    // 创建DefaultAcsClient实例并初始化
                    DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);

                    // 多个SDK client共享一个连接池，此处设置该连接池的参数，
                    // 比如每个host的最大连接数，超时时间等
                    HttpClientConfig clientConfig = HttpClientConfig.getDefault();
                    clientConfig.setMaxRequestsPerHost(App.config.getMaxThreadPool());
                    clientConfig.setConnectionTimeoutMillis(10000L);

                    profile.setHttpClientConfig(clientConfig);
                    iAcsClient = new DefaultAcsClient(profile);
                }
            }
        }
        return iAcsClient;
    }

    /**
     * 获取腾讯云短信发送客户端
     *
     * @return SmsSingleSender
     */
    public static SmsSingleSender getTxYunSender() {
        if (smsSingleSender == null) {
            synchronized (PushManage.class) {
                if (smsSingleSender == null) {
                    String txyunAppId = App.config.getTxyunAppId();
                    String txyunAppKey = App.config.getTxyunAppKey();

                    smsSingleSender = new SmsSingleSender(Integer.valueOf(txyunAppId), txyunAppKey);
                }
            }
        }
        return smsSingleSender;
    }

    /**
     * 获取阿里大于短信发送客户端
     *
     * @return TaobaoClient
     */
    public static TaobaoClient getTaobaoClient() {
        if (taobaoClient == null) {
            synchronized (PushManage.class) {
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

    /**
     * 获取云片网短信发送客户端
     *
     * @return YunpianClient
     */
    public static YunpianClient getYunpianClient() {
        if (yunpianClient == null) {
            synchronized (PushManage.class) {
                if (yunpianClient == null) {
                    String yunpianApiKey = App.config.getYunpianApiKey();

                    yunpianClient = new YunpianClient(yunpianApiKey).init();
                }
            }
        }
        return yunpianClient;
    }

    /**
     * 推送停止或结束后保存数据
     */
    static void savePushData() throws IOException {
        File pushHisDir = new File(SystemUtil.configHome + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        String msgName = MessageEditForm.messageEditForm.getMsgNameField().getText();
        String nowTime = DateUtil.now().replace(":", "_").replace(" ", "_");
        CSVWriter writer;
        int msgType = App.config.getMsgType();
        String now = SqliteUtil.nowDateForSqlite();

        // 保存已发送
        if (PushData.sendSuccessList.size() > 0) {
            File sendSuccessFile = new File(SystemUtil.configHome + "data" +
                    File.separator + "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + msgName +
                    "-发送成功-" + nowTime + ".csv");
            if (!sendSuccessFile.exists()) {
                sendSuccessFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(sendSuccessFile));

            for (String[] str : PushData.sendSuccessList) {
                writer.writeNext(str);
            }
            writer.close();

            TPushHistory tPushHistory = new TPushHistory();
//          TODO  tPushHistory.setMsgId(0);
            tPushHistory.setMsgType(msgType);
            tPushHistory.setMsgName(msgName);
            tPushHistory.setResult("发送成功");
            tPushHistory.setCsvFile(sendSuccessFile.getAbsolutePath());
            tPushHistory.setCreateTime(now);
            tPushHistory.setModifiedTime(now);

            pushHistoryMapper.insertSelective(tPushHistory);
        }

        // 保存未发送
        for (String[] str : PushData.sendSuccessList) {
            PushData.toSendList.remove(str);
        }
        for (String[] str : PushData.sendFailList) {
            PushData.toSendList.remove(str);
        }
        if (PushData.toSendList.size() > 0) {
            File unSendFile = new File(SystemUtil.configHome + "data" + File.separator +
                    "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + msgName + "-未发送-" + nowTime +
                    ".csv");
            if (!unSendFile.exists()) {
                unSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String[] str : PushData.toSendList) {
                writer.writeNext(str);
            }
            writer.close();

            TPushHistory tPushHistory = new TPushHistory();
//          TODO  tPushHistory.setMsgId(0);
            tPushHistory.setMsgType(msgType);
            tPushHistory.setMsgName(msgName);
            tPushHistory.setResult("未发送");
            tPushHistory.setCsvFile(unSendFile.getAbsolutePath());
            tPushHistory.setCreateTime(now);
            tPushHistory.setModifiedTime(now);

            pushHistoryMapper.insertSelective(tPushHistory);
        }

        // 保存发送失败
        if (PushData.sendFailList.size() > 0) {
            File failSendFile = new File(SystemUtil.configHome + "data" + File.separator +
                    "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + msgName + "-发送失败-" + nowTime + ".csv");
            if (!failSendFile.exists()) {
                failSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(failSendFile));
            for (String[] str : PushData.sendFailList) {
                writer.writeNext(str);
            }
            writer.close();

            TPushHistory tPushHistory = new TPushHistory();
//          TODO  tPushHistory.setMsgId(0);
            tPushHistory.setMsgType(msgType);
            tPushHistory.setMsgName(msgName);
            tPushHistory.setResult("发送失败");
            tPushHistory.setCsvFile(failSendFile.getAbsolutePath());
            tPushHistory.setCreateTime(now);
            tPushHistory.setModifiedTime(now);

            pushHistoryMapper.insertSelective(tPushHistory);
        }

        PushHisForm.init();
    }

    /**
     * 输出到控制台和log
     *
     * @param log
     */
    public static void console(String log) {
        PushForm.pushForm.getPushConsoleTextArea().append(log + "\n");
        PushForm.pushForm.getPushConsoleTextArea().setCaretPosition(PushForm.pushForm.getPushConsoleTextArea().getText().length());
        logger.warn(log);
    }
}