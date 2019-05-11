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
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.domain.TPushHistory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
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

        switch (Init.config.getMsgType()) {
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
                String aliyunAccessKeyId = Init.config.getAliyunAccessKeyId();
                String aliyunAccessKeySecret = Init.config.getAliyunAccessKeySecret();

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
            case MessageTypeEnum.TX_YUN_CODE:
                String txyunAppId = Init.config.getTxyunAppId();
                String txyunAppKey = Init.config.getTxyunAppKey();

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
                            params, Init.config.getAliyunSign(), "", "");
                    if (result.result != 0) {
                        throw new Exception(result.toString());
                    }
                }
                break;
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                String aliServerUrl = Init.config.getAliServerUrl();
                String aliAppKey = Init.config.getAliAppKey();
                String aliAppSecret = Init.config.getAliAppSecret();

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
            case MessageTypeEnum.YUN_PIAN_CODE:
                String yunpianApiKey = Init.config.getYunpianApiKey();

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
        if (StringUtils.isEmpty(Init.config.getWechatAppId()) || StringUtils.isEmpty(Init.config.getWechatAppSecret())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存公众号相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        configStorage.setAppId(Init.config.getWechatAppId());
        configStorage.setSecret(Init.config.getWechatAppSecret());
        configStorage.setToken(Init.config.getWechatToken());
        configStorage.setAesKey(Init.config.getWechatAesKey());
        return configStorage;
    }

    /**
     * 微信小程序配置
     *
     * @return WxMaInMemoryConfig
     */
    private static WxMaInMemoryConfig wxMaConfigStorage() {
        WxMaInMemoryConfig configStorage = new WxMaInMemoryConfig();
        if (StringUtils.isEmpty(Init.config.getMiniAppAppId()) || StringUtils.isEmpty(Init.config.getMiniAppAppSecret())
                || StringUtils.isEmpty(Init.config.getMiniAppToken()) || StringUtils.isEmpty(Init.config.getMiniAppAesKey())) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存小程序相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            PushForm.pushForm.getScheduleRunButton().setEnabled(true);
            PushForm.pushForm.getPushStartButton().setEnabled(true);
            PushForm.pushForm.getPushStopButton().setEnabled(false);
            PushForm.pushForm.getPushTotalProgressBar().setIndeterminate(false);
            return null;
        }
        configStorage.setAppid(Init.config.getMiniAppAppId());
        configStorage.setSecret(Init.config.getMiniAppAppSecret());
        configStorage.setToken(Init.config.getMiniAppToken());
        configStorage.setAesKey(Init.config.getMiniAppAesKey());
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
        String nowTime = DateUtil.now().replace(":", "_").replace(" ", "_");
        CSVWriter writer;
        int msgType = Init.config.getMsgType();
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