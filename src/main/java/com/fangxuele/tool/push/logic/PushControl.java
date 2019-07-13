package com.fangxuele.tool.push.logic;

import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.domain.TPushHistory;
import com.fangxuele.tool.push.logic.msgmaker.AliTemplateMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.AliyunMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.MailMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.TxYunMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxCpMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxKefuMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxMaTemplateMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import com.fangxuele.tool.push.logic.msgmaker.YunPianMsgMaker;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MailMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.listener.MemberListener;
import com.fangxuele.tool.push.util.ConsoleUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * <pre>
 * 推送控制
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
 */
public class PushControl {

    /**
     * 是否空跑
     */
    public static boolean dryRun;

    private static TPushHistoryMapper pushHistoryMapper = MybatisUtil.getSqlSession().getMapper(TPushHistoryMapper.class);

    /**
     * 模板变量前缀
     */
    public static final String TEMPLATE_VAR_PREFIX = "var";

    /**
     * 预览消息
     */
    public static List<SendResult> preview() {
        List<SendResult> sendResultList = new ArrayList<>();
        if (!pushCheck()) {
            return null;
        }
        List<String[]> msgDataList = new ArrayList<>();
        for (String data : MessageEditForm.messageEditForm.getPreviewUserField().getText().split(";")) {
            msgDataList.add(data.split(MemberListener.TXT_FILE_DATA_SEPERATOR_REGEX));
        }

        // 准备消息构造器
        prepareMsgMaker();
        IMsgSender msgSender = MsgSenderFactory.getMsgSender();

        dryRun = false;
        if (msgSender != null) {
            for (String[] msgData : msgDataList) {
                sendResultList.add(msgSender.send(msgData));
            }
        } else {
            return null;
        }

        return sendResultList;
    }

    /**
     * 推送前检查
     *
     * @return boolean
     */
    public static boolean pushCheck() {
        if (StringUtils.isEmpty(MessageEditForm.messageEditForm.getMsgNameField().getText())) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMainPanel(), "请先选择一条消息！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            MainWindow.mainWindow.getTabbedPane().setSelectedIndex(2);

            return false;
        }
        if (PushData.allUser == null || PushData.allUser.size() == 0) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMainPanel(), "请先准备目标用户！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }
        if (!PushData.boostMode) {
            if ("0".equals(PushForm.pushForm.getMaxThreadPoolTextField().getText()) || StringUtils.isEmpty(PushForm.pushForm.getMaxThreadPoolTextField().getText())) {
                JOptionPane.showMessageDialog(PushForm.pushForm.getPushPanel(), "请设置每页分配用户数！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);

                return false;
            }
            if ("0".equals(PushForm.pushForm.getThreadCountTextField().getText()) || StringUtils.isEmpty(PushForm.pushForm.getThreadCountTextField().getText())) {
                JOptionPane.showMessageDialog(PushForm.pushForm.getPushPanel(), "请设置每个线程分配的页数！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);

                return false;
            }
        }

        int msgType = App.config.getMsgType();
        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
            case MessageTypeEnum.KEFU_CODE:
            case MessageTypeEnum.KEFU_PRIORITY_CODE: {
                if (StringUtils.isEmpty(App.config.getWechatAppId()) || StringUtils.isEmpty(App.config.getWechatAppSecret())) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存公众号相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
                break;
            }
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                if (StringUtils.isEmpty(App.config.getMiniAppAppId()) || StringUtils.isEmpty(App.config.getMiniAppAppSecret())) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请先在设置中填写并保存小程序相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
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
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                String yunpianApiKey = App.config.getYunpianApiKey();
                if (StringUtils.isEmpty(yunpianApiKey)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存云片网短信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
                break;
            case MessageTypeEnum.EMAIL_CODE:
                String mailHost = App.config.getMailHost();
                String mailFrom = App.config.getMailFrom();
                if (StringUtils.isBlank(mailHost) || StringUtils.isBlank(mailFrom)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存E-Mail相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
                break;
            case MessageTypeEnum.WX_CP_CODE:
                String wxCpCorpId = App.config.getWxCpCorpId();
                if (StringUtils.isBlank(wxCpCorpId)) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                            "请先在设置中填写并保存微信企业号/企业微信相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
                break;
            default:
        }
        return true;
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

        List<File> fileList = new ArrayList<>();
        // 保存已发送
        if (PushData.sendSuccessList.size() > 0) {
            File sendSuccessFile = new File(SystemUtil.configHome + "data" +
                    File.separator + "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + msgName +
                    "-发送成功-" + nowTime + ".csv");
            FileUtil.touch(sendSuccessFile);
            writer = new CSVWriter(new FileWriter(sendSuccessFile));

            for (String[] str : PushData.sendSuccessList) {
                writer.writeNext(str);
            }
            writer.close();

            savePushResult(msgName, "发送成功", sendSuccessFile);
            fileList.add(sendSuccessFile);
            // 保存累计推送总数
            App.config.setPushTotal(App.config.getPushTotal() + PushData.sendSuccessList.size());
            App.config.save();
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
            FileUtil.touch(unSendFile);
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String[] str : PushData.toSendList) {
                writer.writeNext(str);
            }
            writer.close();

            savePushResult(msgName, "未发送", unSendFile);
            fileList.add(unSendFile);
        }

        // 保存发送失败
        if (PushData.sendFailList.size() > 0) {
            File failSendFile = new File(SystemUtil.configHome + "data" + File.separator +
                    "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + msgName + "-发送失败-" + nowTime + ".csv");
            FileUtil.touch(failSendFile);
            writer = new CSVWriter(new FileWriter(failSendFile));
            for (String[] str : PushData.sendFailList) {
                writer.writeNext(str);
            }
            writer.close();

            savePushResult(msgName, "发送失败", failSendFile);
            fileList.add(failSendFile);
        }

        PushHisForm.init();

        // 发送推送结果邮件
        if ((PushData.scheduling || PushData.fixRateScheduling)
                && ScheduleForm.scheduleForm.getSendPushResultCheckBox().isSelected()) {
            ConsoleUtil.consoleWithLog("发送推送结果邮件开始");
            String mailResultTo = ScheduleForm.scheduleForm.getMailResultToTextField().getText().replace("；", ";").replace(" ", "");
            String[] mailTos = mailResultTo.split(";");
            ArrayList<String> mailToList = new ArrayList<>(Arrays.asList(mailTos));

            MailMsgSender mailMsgSender = new MailMsgSender();
            String title = "WePush推送结果：【" + MessageEditForm.messageEditForm.getMsgNameField().getText()
                    + "】" + PushData.sendSuccessList.size() + "成功；" + PushData.sendFailList.size() + "失败；"
                    + PushData.toSendList.size() + "未发送";
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("<h2>WePush推送结果</h2>");
            contentBuilder.append("<p>消息类型：" + MessageTypeEnum.getName(App.config.getMsgType()) + "</p>");
            contentBuilder.append("<p>消息名称：" + MessageEditForm.messageEditForm.getMsgNameField().getText() + "</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p style='color:green'><strong>成功数：" + PushData.sendSuccessList.size() + "</strong></p>");
            contentBuilder.append("<p style='color:red'><strong>失败数：" + PushData.sendFailList.size() + "</strong></p>");
            contentBuilder.append("<p>未推送数：" + PushData.toSendList.size() + "</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p>开始时间：" + DateFormatUtils.format(new Date(PushData.startTime), "yyyy-MM-dd HH:mm:ss") + "</p>");
            contentBuilder.append("<p>完毕时间：" + DateFormatUtils.format(new Date(PushData.endTime), "yyyy-MM-dd HH:mm:ss") + "</p>");
            contentBuilder.append("<p>总耗时：" + DateUtil.formatBetween(PushData.endTime - PushData.startTime, BetweenFormater.Level.SECOND) + "</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p>详情请查看附件</p>");

            contentBuilder.append("<br/>");
            contentBuilder.append("<hr/>");
            contentBuilder.append("<p>来自WePush，一款专注于批量推送的小而美的工具</p>");
            contentBuilder.append("<img alt=\"WePush\" src=\"http://download.zhoubochina.com/file/wx-zanshang.jpg\">");

            File[] files = new File[fileList.size()];
            fileList.toArray(files);
            mailMsgSender.sendPushResultMail(mailToList, title, contentBuilder.toString(), files);
            ConsoleUtil.consoleWithLog("发送推送结果邮件结束");
        }
    }

    /**
     * 保存结果到DB
     *
     * @param msgName
     * @param resultInfo
     * @param file
     */
    private static void savePushResult(String msgName, String resultInfo, File file) {
        TPushHistory tPushHistory = new TPushHistory();
        String now = SqliteUtil.nowDateForSqlite();
        tPushHistory.setMsgType(App.config.getMsgType());
        tPushHistory.setMsgName(msgName);
        tPushHistory.setResult(resultInfo);
        tPushHistory.setCsvFile(file.getAbsolutePath());
        tPushHistory.setCreateTime(now);
        tPushHistory.setModifiedTime(now);

        pushHistoryMapper.insertSelective(tPushHistory);
    }

    /**
     * 准备消息构造器
     */
    static void prepareMsgMaker() {
        int msgType = App.config.getMsgType();
        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                WxMpTemplateMsgMaker.prepare();
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                WxMaTemplateMsgMaker.prepare();
                break;
            case MessageTypeEnum.KEFU_CODE:
                WxKefuMsgMaker.prepare();
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                WxKefuMsgMaker.prepare();
                WxMpTemplateMsgMaker.prepare();
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                AliyunMsgMaker.prepare();
                break;
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                AliTemplateMsgMaker.prepare();
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                TxYunMsgMaker.prepare();
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                YunPianMsgMaker.prepare();
                break;
            case MessageTypeEnum.EMAIL_CODE:
                MailMsgMaker.prepare();
                break;
            case MessageTypeEnum.WX_CP_CODE:
                WxCpMsgMaker.prepare();
                break;
            default:
        }
    }

    /**
     * 重新导入目标用户(定时任务)
     */
    public static void reimportMembers() {
        if (PushData.fixRateScheduling && ScheduleForm.scheduleForm.getReimportCheckBox().isSelected()) {
            switch ((String) ScheduleForm.scheduleForm.getReimportComboBox().getSelectedItem()) {
                case "通过SQL导入":
                    MemberListener.importFromSql();
                    break;
                case "通过文件导入":
                    MemberListener.importFromFile();
                    break;
                case "导入所有关注公众号的用户":
                    MemberListener.importWxAll();
                    break;
                case "导入企业通讯录中所有用户":
                    MemberListener.importWxCpAll();
                    break;
                default:
            }
        }
    }

}