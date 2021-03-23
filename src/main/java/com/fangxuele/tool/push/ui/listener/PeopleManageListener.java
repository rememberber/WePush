package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.dialog.NewPeopleDialog;
import com.fangxuele.tool.push.ui.form.PeopleManageForm;

/**
 * <pre>
 * 人群管理相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/3/23.
 */
public class PeopleManageListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        PeopleManageForm peopleManageForm = PeopleManageForm.getInstance();

        // 新建人群
        peopleManageForm.getCreatePeopleButton().addActionListener(e -> {
            NewPeopleDialog dialog = new NewPeopleDialog();
            dialog.pack();
            dialog.setVisible(true);
        });
    }
}
