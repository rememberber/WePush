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
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
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
import me.chanjar.weixin.mp.api.WxMpConfigStorage;
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
import java.util.Objects;

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

    /**
     * 模板变量前缀
     */
    public static final String TEMPLATE_VAR_PREFIX = "var";

    /**
     * 预览消息
     *
     * @throws Exception 异常
     */
    public static boolean preview() throws Exception {
        List<String[]> msgDataList = new ArrayList<>();

        for (String data : MessageEditForm.messageEditForm.getPreviewUserField().getText().split(";")) {
            msgDataList.add(data.split(","));
        }

        switch (Objects.requireNonNull(MessageEditForm.messageEditForm.getMsgTypeComboBox().getSelectedItem()).toString()) {
            case MessageTypeConsts.MP_TEMPLATE:
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
            case MessageTypeConsts.MA_TEMPLATE:
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
            case MessageTypeConsts.KEFU:
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
            case MessageTypeConsts.KEFU_PRIORITY:
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
            case MessageTypeConsts.ALI_YUN:
                String aliyunAccessKeyId = Init.configer.getAliyunAccessKeyId();
                String aliyunAccessKeySecret = Init.configer.getAliyunAccessKeySecret();

                if (StringUtils.isEmpty(aliyunAccessKeyId) || StringUtils.isEmpty(aliyunAccessKeySecret)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存阿里云短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                //初始化acsClient,暂不支持region化
                IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);
                DefaultProfile.addEndpoint("cn-hangzhou", "Dysmsapi", "cn-hangzhou");

                IAcsClient acsClient = new DefaultAcsClient(profile);
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
            case MessageTypeConsts.TX_YUN:
                String txyunAppId = Init.configer.getTxyunAppId();
                String txyunAppKey = Init.configer.getTxyunAppKey();

                if (StringUtils.isEmpty(txyunAppId) || StringUtils.isEmpty(txyunAppKey)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存腾讯云短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                SmsSingleSender ssender = new SmsSingleSender(Integer.valueOf(txyunAppId), txyunAppKey);

                for (String[] msgData : msgDataList) {
                    String[] params = MessageMaker.makeTxyunMessage(msgData);
                    SmsSingleSenderResult result = ssender.sendWithParam("86", msgData[0],
                            Integer.valueOf(MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText()),
                            params, Init.configer.getAliyunSign(), "", "");
                    if (result.result != 0) {
                        throw new Exception(result.toString());
                    }
                }
                break;
            case MessageTypeConsts.ALI_TEMPLATE:
                String aliServerUrl = Init.configer.getAliServerUrl();
                String aliAppKey = Init.configer.getAliAppKey();
                String aliAppSecret = Init.configer.getAliAppSecret();

                if (StringUtils.isEmpty(aliServerUrl) || StringUtils.isEmpty(aliAppKey)
                        || StringUtils.isEmpty(aliAppSecret)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存阿里大于相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                TaobaoClient client = new DefaultTaobaoClient(aliServerUrl, aliAppKey, aliAppSecret);
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
            case MessageTypeConsts.YUN_PIAN:
                String yunpianApiKey = Init.configer.getYunpianApiKey();

                if (StringUtils.isEmpty(yunpianApiKey)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存云片网短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                YunpianClient clnt = new YunpianClient(yunpianApiKey).init();

                for (String[] msgData : msgDataList) {
                    Map<String, String> params = MessageMaker.makeYunpianMessage(msgData);
                    params.put(YunpianClient.MOBILE, msgData[0]);
                    Result<SmsSingleSend> result = clnt.sms().single_send(params);
                    if (result.getCode() != 0) {
                        throw new Exception(result.toString());
                    }
                }
                clnt.close();
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
    private static WxMpConfigStorage wxMpConfigStorage() {
        WxMpInMemoryConfigStorage configStorage = new WxMpInMemoryConfigStorage();
        if (StringUtils.isEmpty(Init.configer.getWechatAppId()) || StringUtils.isEmpty(Init.configer.getWechatAppSecret())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存公众号相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        configStorage.setAppId(Init.configer.getWechatAppId());
        configStorage.setSecret(Init.configer.getWechatAppSecret());
        configStorage.setToken(Init.configer.getWechatToken());
        configStorage.setAesKey(Init.configer.getWechatAesKey());
        return configStorage;
    }

    /**
     * 微信小程序配置
     *
     * @return WxMaInMemoryConfig
     */
    private static WxMaInMemoryConfig wxMaConfigStorage() {
        WxMaInMemoryConfig configStorage = new WxMaInMemoryConfig();
        if (StringUtils.isEmpty(Init.configer.getMiniAppAppId()) || StringUtils.isEmpty(Init.configer.getMiniAppAppSecret())
                || StringUtils.isEmpty(Init.configer.getMiniAppToken()) || StringUtils.isEmpty(Init.configer.getMiniAppAesKey())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存小程序相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        configStorage.setAppid(Init.configer.getMiniAppAppId());
        configStorage.setSecret(Init.configer.getMiniAppAppSecret());
        configStorage.setToken(Init.configer.getMiniAppToken());
        configStorage.setAesKey(Init.configer.getMiniAppAesKey());
        configStorage.setMsgDataFormat("JSON");
        return configStorage;
    }

    /**
     * 获取微信公众号工具服务
     *
     * @return WxMpService
     */
    public static WxMpService getWxMpService() {
        WxMpService wxMpService = new WxMpServiceImpl();
        WxMpConfigStorage wxMpConfigStorage = wxMpConfigStorage();
        if (wxMpConfigStorage != null) {
            wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
        }
        return wxMpService;
    }

    /**
     * 获取微信小程序工具服务
     *
     * @return WxMaService
     */
    static WxMaService getWxMaService() {
        WxMaService wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaConfigStorage());
        return wxMaService;
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
        String nowTime = DateUtil.now().replaceAll(":", "_");

        String[] strArray;
        CSVWriter writer;

        // 保存已发送
        if (PushData.sendSuccessList.size() > 0) {
            File toSendFile = new File(SystemUtil.configHome + "data" +
                    File.separator + "push_his" + File.separator + msgName +
                    "-发送成功-" + nowTime + ".csv");
            if (!toSendFile.exists()) {
                toSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(toSendFile));

            for (String[] str : PushData.sendSuccessList) {
                writer.writeNext(str);
            }
            writer.close();
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
                    "push_his" + File.separator + msgName + "-未发送-" + nowTime +
                    ".csv");
            if (!unSendFile.exists()) {
                unSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String[] str : PushData.toSendList) {
                writer.writeNext(str);
            }
            writer.close();
        }

        // 保存发送失败
        if (PushData.sendFailList.size() > 0) {
            File failSendFile = new File(SystemUtil.configHome + "data" + File.separator +
                    "push_his" + File.separator + msgName + "-发送失败-" + nowTime + ".csv");
            if (!failSendFile.exists()) {
                failSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(failSendFile));
            for (String[] str : PushData.sendFailList) {
                writer.writeNext(str);
            }
            writer.close();
        }

        Init.initMemberTab();
        Init.initSettingTab();
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