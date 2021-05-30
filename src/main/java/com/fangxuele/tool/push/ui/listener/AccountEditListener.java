package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.ui.form.account.AccountFormFactory;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * <pre>
 * 编辑账号tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/3/19.
 */
public class AccountEditListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        AccountEditForm accountEditForm = AccountEditForm.getInstance();

        // 保存按钮事件
        accountEditForm.getAccountSaveButton().addActionListener(e -> {
            String accountName = accountEditForm.getAccountNameField().getText();
            if (StringUtils.isBlank(accountName)) {
                JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "请填写账号名称！\n\n", "失败",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                AccountFormFactory.getAccountForm().save(accountName);
                AccountManageForm.init();
                MessageManageForm.init();
                PeopleManageForm.init();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

    }
}