package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxMpAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.fangxuele.tool.push.util.WeWxMpServiceImpl;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

@Getter
@Slf4j
public class WxMpAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JTextField appIdTextField;
    private JTextField appSecretTextField;
    private JTextField tokenTextField;
    private JTextField aesKeyTextField;

    private static WxMpAccountForm wxMpAccountForm;

    public volatile static WxMpDefaultConfigImpl wxMpConfigStorage;
    public volatile static WxMpService wxMpService;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.WX_MP_CODE, accountName);

            WxMpAccountForm instance = getInstance();
            WxMpAccountConfig wxMpAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), WxMpAccountConfig.class);
            instance.getAppIdTextField().setText(wxMpAccountConfig.getAppId());
            instance.getAppSecretTextField().setText(wxMpAccountConfig.getAppSecret());
            instance.getTokenTextField().setText(wxMpAccountConfig.getToken());
            instance.getAesKeyTextField().setText(wxMpAccountConfig.getAesKey());
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {

            WxMpAccountForm instance = getInstance();
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.WX_MP_CODE, accountName);

            boolean existSameAccount = false;

            if (tAccount != null) {
                existSameAccount = true;
            }

            int isCover = JOptionPane.NO_OPTION;
            if (existSameAccount) {
                // 如果存在，是否覆盖
                isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的账号，\n是否覆盖？", "确认",
                        JOptionPane.YES_NO_OPTION);
            }

            if (!existSameAccount || isCover == JOptionPane.YES_OPTION) {

                String now = SqliteUtil.nowDateForSqlite();

                TAccount tAccount1 = new TAccount();
                tAccount1.setMsgType(String.valueOf(MessageTypeEnum.WX_MP_CODE));
                tAccount1.setAccountName(accountName);

                WxMpAccountConfig wxMpAccountConfig = new WxMpAccountConfig();
                wxMpAccountConfig.setAppId(instance.getAppIdTextField().getText());
                wxMpAccountConfig.setAppSecret(instance.getAppSecretTextField().getText());
                wxMpAccountConfig.setToken(instance.getTokenTextField().getText());
                wxMpAccountConfig.setAesKey(instance.getAesKeyTextField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(wxMpAccountConfig));

                tAccount1.setModifiedTime(now);

                if (existSameAccount) {
                    accountMapper.updateByMsgTypeAndAccountName(tAccount1);
                } else {
                    tAccount1.setCreateTime(now);
                    accountMapper.insertSelective(tAccount1);
                }

                JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }
    }

    @Override
    public void clear() {
        UIUtil.clearForm(getInstance());
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static WxMpAccountForm getInstance() {
        if (wxMpAccountForm == null) {
            wxMpAccountForm = new WxMpAccountForm();
        }
        UndoUtil.register(wxMpAccountForm);
        return wxMpAccountForm;
    }

    /**
     * 微信公众号配置
     *
     * @return WxMpConfigStorage
     */
    private static WxMpDefaultConfigImpl wxMpConfigStorage(String accountName) {
        TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.getMsgTypeForAccount(), accountName);
        if (tAccount == null) {
            log.error("未获取到对应的微信公众号账号配置:{}", accountName);
        }

        WxMpAccountConfig wxMpAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), WxMpAccountConfig.class);

        WxMpDefaultConfigImpl configStorage = new WxMpDefaultConfigImpl();
        configStorage.setAppId(wxMpAccountConfig.getAppId());
        configStorage.setSecret(wxMpAccountConfig.getAppSecret());
        configStorage.setToken(wxMpAccountConfig.getToken());
        configStorage.setAesKey(wxMpAccountConfig.getAesKey());
        if (App.config.isMpUseProxy()) {
            configStorage.setHttpProxyHost(App.config.getMpProxyHost());
            configStorage.setHttpProxyPort(Integer.parseInt(App.config.getMpProxyPort()));
            configStorage.setHttpProxyUsername(App.config.getMpProxyUserName());
            configStorage.setHttpProxyPassword(App.config.getMpProxyPassword());
        }
        DefaultApacheHttpClientBuilder clientBuilder = DefaultApacheHttpClientBuilder.get();
        //从连接池获取链接的超时时间(单位ms)
        clientBuilder.setConnectionRequestTimeout(10000);
        //建立链接的超时时间(单位ms)
        clientBuilder.setConnectionTimeout(5000);
        //连接池socket超时时间(单位ms)
        clientBuilder.setSoTimeout(5000);
        //空闲链接的超时时间(单位ms)
        clientBuilder.setIdleConnTimeout(60000);
        //空闲链接的检测周期(单位ms)
        clientBuilder.setCheckWaitTime(3000);
        //每路最大连接数
        clientBuilder.setMaxConnPerHost(App.config.getMaxThreads());
        //连接池最大连接数
        clientBuilder.setMaxTotalConn(App.config.getMaxThreads());
        //HttpClient请求时使用的User Agent
//        clientBuilder.setUserAgent(..)
        configStorage.setApacheHttpClientBuilder(clientBuilder);
        return configStorage;
    }

    /**
     * 获取微信公众号工具服务
     *
     * @return WxMpService
     */
    public static WxMpService getWxMpService(String accountName) {
        invalidAccount();

        if (wxMpConfigStorage == null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (wxMpConfigStorage == null) {
                    wxMpConfigStorage = wxMpConfigStorage(accountName);
                }
            }
        }
        if (wxMpService == null && wxMpConfigStorage != null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (wxMpService == null && wxMpConfigStorage != null) {
                    wxMpService = new WeWxMpServiceImpl();
                    wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
                }
            }
        }
        return wxMpService;
    }

    public static void invalidAccount() {
        wxMpConfigStorage = null;
        wxMpService = null;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 5, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("AppId");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appIdTextField = new JTextField();
        panel1.add(appIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Token");
        panel1.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tokenTextField = new JTextField();
        panel1.add(tokenTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("AppSecret");
        panel1.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appSecretTextField = new JTextField();
        panel1.add(appSecretTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("AES Key");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aesKeyTextField = new JTextField();
        panel1.add(aesKeyTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
