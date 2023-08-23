package com.fangxuele.tool.push.ui.dialog.importway;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.ui.listener.PeopleManageListener;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ImportByNum extends JDialog {
    private JPanel contentPane;
    private JLabel 数量Label;
    private JTextField importNumTextField;
    private JButton importFromNumButton;

    private static final Log logger = LogFactory.get();

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);

    private Integer peopleId;

    public ImportByNum(Integer peopleId) {
        super(App.mainFrame, "没有变量，直接按数量发送");
        setContentPane(contentPane);
        setModal(true);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.2, 0.2);
        getRootPane().setDefaultButton(importFromNumButton);

        this.peopleId = peopleId;

        importFromNumButton.addActionListener(e -> onOK());

        importFromNumButton.setIcon(new FlatSVGIcon("icon/import.svg"));

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        if (StringUtils.isBlank(importNumTextField.getText())) {
            JOptionPane.showMessageDialog(App.mainFrame, "请填写数量！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer num = Integer.valueOf(importNumTextField.getText());

        if (num <= 0) {
            JOptionPane.showMessageDialog(App.mainFrame, "数量必须大于0！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        importFromNum(num, false, false);
    }

    private void importFromNum(Integer num, Boolean clear, Boolean silence) {
        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
        JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
        JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

        int currentImported = 0;
        String now = SqliteUtil.nowDateForSqlite();
        String dataVersion = UUID.fastUUID().toString(true);

        // 保存导入配置
        TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(PeopleManageListener.selectedPeopleId);

        TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
        tPeopleImportConfig.setPeopleId(PeopleManageListener.selectedPeopleId);
        tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_NUM_CODE));
        tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
        tPeopleImportConfig.setLastDataVersion(dataVersion);
        tPeopleImportConfig.setModifiedTime(now);

        if (beforePeopleImportConfig != null) {
            tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
            peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
        } else {
            tPeopleImportConfig.setCreateTime(now);
            peopleImportConfigMapper.insert(tPeopleImportConfig);
        }

        try {
            int importNum = num;
            if (!silence) {
                progressBar.setVisible(true);
                progressBar.setMaximum(importNum);
            }


            for (int i = 0; i < importNum; i++) {
                String[] array = new String[1];
                array[0] = String.valueOf(i);

                TPeopleData tPeopleData = new TPeopleData();
                tPeopleData.setPeopleId(PeopleManageListener.selectedPeopleId);
                tPeopleData.setPin(array[0]);
                tPeopleData.setVarData(JSONUtil.toJsonStr(array));
                tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                tPeopleData.setDataVersion(dataVersion);
                tPeopleData.setCreateTime(now);
                tPeopleData.setModifiedTime(now);

                peopleDataMapper.insert(tPeopleData);

                currentImported++;
                if (!silence) {
                    memberCountLabel.setText(String.valueOf(currentImported));
                }
            }
            if (!silence) {
                PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);

                JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (Exception e1) {
            if (!silence) {
                JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
            }
            logger.error(e1);
            e1.printStackTrace();
        } finally {
            if (!silence) {
                progressBar.setMaximum(100);
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
            }
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importFromNumButton = new JButton();
        importFromNumButton.setText("导入");
        panel2.add(importFromNumButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        数量Label = new JLabel();
        数量Label.setHorizontalAlignment(11);
        数量Label.setHorizontalTextPosition(4);
        数量Label.setText("数量");
        panel3.add(数量Label, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        importNumTextField = new JTextField();
        panel3.add(importNumTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    public void reImport() {
        TPeopleImportConfig tPeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
        importFromNum(0, true, true);
        dispose();

    }
}
