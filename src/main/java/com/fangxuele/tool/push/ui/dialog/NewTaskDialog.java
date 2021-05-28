package com.fangxuele.tool.push.ui.dialog;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.domain.*;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class NewTaskDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField titleTextField;
    private JComboBox msgTypeComboBox;
    private JComboBox accountComboBox;
    private JComboBox msgComboBox;
    private JComboBox peopleComboBox;
    private JRadioButton manualTaskRadioButton;
    private JRadioButton scheduleRadioButton;
    private JRadioButton triggerRadioButton;
    private JPanel schedulePanel;
    private JRadioButton runAtThisTimeRadioButton;
    private JTextField startAtThisTimeTextField;
    private JRadioButton runPerDayRadioButton;
    private JTextField startPerDayTextField;
    private JRadioButton runPerWeekRadioButton;
    private JComboBox schedulePerWeekComboBox;
    private JTextField startPerWeekTextField;
    private JCheckBox reimportCheckBox;
    private JCheckBox sendPushResultCheckBox;
    private JTextField mailResultToTextField;
    private JRadioButton cronRadioButton;
    private JTextField cronTextField;
    private JLabel cronOnlineLabel;
    private JLabel cronHelpLabel;
    private JScrollPane scrollPane;

    private static TTaskMapper taskMapper = MybatisUtil.getSqlSession().getMapper(TTaskMapper.class);
    private static TTaskExtMapper taskExtMapper = MybatisUtil.getSqlSession().getMapper(TTaskExtMapper.class);
    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgWxUniformMapper wxUniformMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxUniformMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMaSubscribeMapper msgMaSubscribeMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaSubscribeMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgMpSubscribeMapper msgMpSubscribeMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpSubscribeMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);
    private static TMsgMailMapper msgMailMapper = MybatisUtil.getSqlSession().getMapper(TMsgMailMapper.class);
    private static TMsgHttpMapper msgHttpMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);
    private static TMsgWxCpMapper msgWxCpMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxCpMapper.class);
    private static TMsgDingMapper msgDingMapper = MybatisUtil.getSqlSession().getMapper(TMsgDingMapper.class);
    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    private static Map<String, Integer> msgTypeMap = Maps.newHashMap();
    private static Map<String, Integer> accountMap = Maps.newHashMap();

    public NewTaskDialog() {

        super(App.mainFrame, "新建任务");
        setContentPane(contentPane);
        setModal(true);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.4, 0.8);

        getRootPane().setDefaultButton(buttonOK);

        init();

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        cronHelpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么是Cron表达式？</h1>");
                tipsBuilder.append("<a href='https://baike.baidu.com/item/cron#3'>百度百科</a>");
                tipsBuilder.append("<p>举几个例子:</p>\n");
                tipsBuilder.append("<p>\"0 0 2 1 * ? *\" 表示在每月的1日的凌晨2点调度任务</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * MON-FRI\" 表示周一到周五每天上午10：15执行作业</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L 2002-2006\" 表示2002-2006年的每个月的最后一个星期五上午10:15执行作</p>\n");
                tipsBuilder.append("<p>\"0 0 10,14,16 * * ?\" 每天上午10点，下午2点，4点</p>\n");
                tipsBuilder.append("<p>\"0 0/30 9-17 * * ?\" 朝九晚五工作时间内每半小时</p>\n");
                tipsBuilder.append("<p>\"0 0 12 ? * WED\" 表示每个星期三中午12点</p>\n");
                tipsBuilder.append("<p>\"0 0 12 * * ?\" 每天中午12点触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * *\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ?\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ? *\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ? 2005\" 2005年的每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 * 14 * * ?\" 在每天下午2点到下午2:59期间的每1分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0/5 14 * * ?\" 在每天下午2点到下午2:55期间的每5分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0/5 14,18 * * ?\" 在每天下午2点到2:55期间和下午6点到6:55期间的每5分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0-5 14 * * ?\" 在每天下午2点到下午2:05期间的每1分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 10,44 14 ? 3 WED\" 每年三月的星期三的下午2:10和2:44触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * MON-FRI\" 周一至周五的上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 15 * ?\" 每月15日上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 L * ?\" 每月最后一日的上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L\" 每月的最后一个星期五上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L 2002-2005\" 2002年至2005年的每月的最后一个星期五上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6#3\" 每月的第三个星期五上午10:15触发");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.getTextPane1().setCaretPosition(0);
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });

        cronOnlineLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("http://cron.qqe2.com/"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });
    }

    private void init() {
        initUI();
        initData();
    }

    private void initUI() {
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        scrollPane.getVerticalScrollBar().setDoubleBuffered(true);
    }

    private void initData() {
        // 消息类型
        initMsgTypeComboBoxData();
        // 账号
        initAccountComboBoxData();
    }

    /**
     * 初始化消息类型下拉框数据
     */
    private void initMsgTypeComboBoxData() {
        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.HTTP), MessageTypeEnum.HTTP_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.HTTP));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.MP_TEMPLATE), MessageTypeEnum.MP_TEMPLATE_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.MP_TEMPLATE));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.MP_SUBSCRIBE), MessageTypeEnum.MP_SUBSCRIBE_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.MP_SUBSCRIBE));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.MA_SUBSCRIBE), MessageTypeEnum.MA_SUBSCRIBE_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.MA_SUBSCRIBE));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.KEFU), MessageTypeEnum.KEFU_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.KEFU));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.KEFU_PRIORITY), MessageTypeEnum.KEFU_PRIORITY_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.KEFU_PRIORITY));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.WX_UNIFORM_MESSAGE), MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.WX_UNIFORM_MESSAGE));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.WX_CP), MessageTypeEnum.WX_CP_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.WX_CP));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.DING), MessageTypeEnum.DING_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.DING));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.ALI_YUN), MessageTypeEnum.ALI_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.ALI_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.TX_YUN), MessageTypeEnum.TX_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.TX_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.HW_YUN), MessageTypeEnum.HW_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.HW_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.BD_YUN), MessageTypeEnum.BD_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.BD_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.YUN_PIAN), MessageTypeEnum.YUN_PIAN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.YUN_PIAN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.UP_YUN), MessageTypeEnum.UP_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.UP_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.QI_NIU_YUN), MessageTypeEnum.QI_NIU_YUN_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.QI_NIU_YUN));

        msgTypeMap.put(MessageTypeEnum.getName(MessageTypeEnum.EMAIL), MessageTypeEnum.EMAIL_CODE);
        this.msgTypeComboBox.addItem(MessageTypeEnum.getName(MessageTypeEnum.EMAIL));
    }

    /**
     * 初始化账号下拉框数据
     */
    private void initAccountComboBoxData() {
        String selectedMsgTypeStr = (String) msgTypeComboBox.getSelectedItem();
        Integer selectedMsgType = msgTypeMap.get(selectedMsgTypeStr);
        List<TAccount> tAccounts = accountMapper.selectByMsgType(selectedMsgType);

        for (TAccount tAccount : tAccounts) {
            accountMap.put(tAccount.getAccountName(), tAccount.getId());
            accountComboBox.addItem(tAccount.getAccountName());
        }
    }

    /**
     * 初始化消息下拉框数据
     */
    private void initMessageComboBoxData() {
        String selectedMsgTypeStr = (String) msgTypeComboBox.getSelectedItem();
        Integer msgType = msgTypeMap.get(selectedMsgTypeStr);

        String selectedAccountStr = (String) accountComboBox.getSelectedItem();
        Integer selectedAccount = msgTypeMap.get(selectedAccountStr);

//        switch (msgType) {
//            case MessageTypeEnum.KEFU_CODE:
//                List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgKefu tMsgKefu : tMsgKefuList) {
//                    data = new Object[1];
//                    data[0] = tMsgKefu.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.KEFU_PRIORITY_CODE:
//                List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgKefuPriority tMsgKefuPriority : tMsgKefuPriorityList) {
//                    data = new Object[1];
//                    data[0] = tMsgKefuPriority.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
//                List<TMsgWxUniform> tMsgWxUniformList = wxUniformMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgWxUniform tMsgWxUniform : tMsgWxUniformList) {
//                    data = new Object[1];
//                    data[0] = tMsgWxUniform.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.MA_TEMPLATE_CODE:
//                List<TMsgMaTemplate> tMsgMaTemplateList = msgMaTemplateMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgMaTemplate tMsgMaTemplate : tMsgMaTemplateList) {
//                    data = new Object[1];
//                    data[0] = tMsgMaTemplate.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
//                List<TMsgMaSubscribe> tMsgMaSubscribeList = msgMaSubscribeMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgMaSubscribe tMsgMaSubscribe : tMsgMaSubscribeList) {
//                    data = new Object[1];
//                    data[0] = tMsgMaSubscribe.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.MP_TEMPLATE_CODE:
//                List<TMsgMpTemplate> tMsgMpTemplateList = msgMpTemplateMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgMpTemplate tMsgMpTemplate : tMsgMpTemplateList) {
//                    data = new Object[1];
//                    data[0] = tMsgMpTemplate.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
//                List<TMsgMpSubscribe> tMsgMpSubscribeList = msgMpSubscribeMapper.selectByMsgTypeAndWxAccountId(msgType, wxAccountId);
//                for (TMsgMpSubscribe tMsgMpSubscribe : tMsgMpSubscribeList) {
//                    data = new Object[1];
//                    data[0] = tMsgMpSubscribe.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.EMAIL_CODE:
//                List<TMsgMail> tMsgMailList = msgMailMapper.selectByMsgType(msgType);
//                for (TMsgMail tMsgMail : tMsgMailList) {
//                    data = new Object[1];
//                    data[0] = tMsgMail.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.WX_CP_CODE:
//                List<TMsgWxCp> tMsgWxCpList = msgWxCpMapper.selectByMsgType(msgType);
//                for (TMsgWxCp tMsgWxCp : tMsgWxCpList) {
//                    data = new Object[1];
//                    data[0] = tMsgWxCp.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.HTTP_CODE:
//                List<TMsgHttp> tMsgHttpList = msgHttpMapper.selectByMsgType(msgType);
//                for (TMsgHttp tMsgHttp : tMsgHttpList) {
//                    data = new Object[1];
//                    data[0] = tMsgHttp.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            case MessageTypeEnum.DING_CODE:
//                List<TMsgDing> tMsgDingList = msgDingMapper.selectByMsgType(msgType);
//                for (TMsgDing tMsgDing : tMsgDingList) {
//                    data = new Object[1];
//                    data[0] = tMsgDing.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//            default:
//                List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgType(msgType);
//                for (TMsgSms tMsgSms : tMsgSmsList) {
//                    data = new Object[1];
//                    data[0] = tMsgSms.getMsgName();
//                    model.addRow(data);
//                }
//                break;
//        }
    }

    private void onOK() {
        String title = this.titleTextField.getText().trim();
        TTask tTask = taskExtMapper.selectByTitle(title);
        if (tTask != null) {
            JOptionPane.showMessageDialog(this, "存在同名任务！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        } else {
            String now = SqliteUtil.nowDateForSqlite();
            TTask task = new TTask();
            task.setTitle(title);
            task.setMsgType("");
            task.setAccountId(0);
            task.setMessageId(0);
            task.setPeopleId(0);
            task.setTaskType(0);
            task.setCron("");
            task.setReimportPeople(0);
            task.setResultAlert(0);
            task.setAlertEmails(this.mailResultToTextField.getText().trim());
            task.setCreateTime(now);
            task.setModifiedTime(now);

            taskMapper.insert(task);
        }
        dispose();
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
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("确定");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        contentPane.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(panel3);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(null, "基本信息", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("任务名称");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        panel4.add(titleTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("消息类型");
        panel4.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("账号");
        panel4.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("消息");
        panel4.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("人群");
        panel4.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTypeComboBox = new JComboBox();
        panel4.add(msgTypeComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        accountComboBox = new JComboBox();
        panel4.add(accountComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgComboBox = new JComboBox();
        panel4.add(msgComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleComboBox = new JComboBox();
        panel4.add(peopleComboBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(null, "运行规则", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        schedulePanel = new JPanel();
        schedulePanel.setLayout(new GridLayoutManager(5, 7, new Insets(5, 5, 0, 0), -1, -1));
        panel5.add(schedulePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        runAtThisTimeRadioButton = new JRadioButton();
        runAtThisTimeRadioButton.setText("在此时间开始推送：");
        schedulePanel.add(runAtThisTimeRadioButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startAtThisTimeTextField = new JTextField();
        startAtThisTimeTextField.setText("");
        schedulePanel.add(startAtThisTimeTextField, new GridConstraints(0, 2, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        runPerDayRadioButton = new JRadioButton();
        runPerDayRadioButton.setText("每天固定时间开始推送：");
        schedulePanel.add(runPerDayRadioButton, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startPerDayTextField = new JTextField();
        schedulePanel.add(startPerDayTextField, new GridConstraints(1, 2, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        runPerWeekRadioButton = new JRadioButton();
        runPerWeekRadioButton.setText("每周固定时间开始推送：");
        schedulePanel.add(runPerWeekRadioButton, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("yyyy-MM-dd HH:mm:ss");
        schedulePanel.add(label6, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("HH:mm:ss");
        schedulePanel.add(label7, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("HH:mm:ss");
        schedulePanel.add(label8, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("每周");
        schedulePanel.add(label9, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("的");
        schedulePanel.add(label10, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        schedulePerWeekComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("一");
        defaultComboBoxModel1.addElement("二");
        defaultComboBoxModel1.addElement("三");
        defaultComboBoxModel1.addElement("四");
        defaultComboBoxModel1.addElement("五");
        defaultComboBoxModel1.addElement("六");
        defaultComboBoxModel1.addElement("日");
        schedulePerWeekComboBox.setModel(defaultComboBoxModel1);
        schedulePanel.add(schedulePerWeekComboBox, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startPerWeekTextField = new JTextField();
        schedulePanel.add(startPerWeekTextField, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        reimportCheckBox = new JCheckBox();
        reimportCheckBox.setText("开始执行时更新人群");
        schedulePanel.add(reimportCheckBox, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronRadioButton = new JRadioButton();
        cronRadioButton.setText("按Cron表达式触发推送：");
        schedulePanel.add(cronRadioButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronTextField = new JTextField();
        schedulePanel.add(cronTextField, new GridConstraints(3, 2, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cronOnlineLabel = new JLabel();
        cronOnlineLabel.setText("<html><a href='http://cron.qqe2.com/'>在线Cron表达式生成器</a></html>");
        schedulePanel.add(cronOnlineLabel, new GridConstraints(3, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronHelpLabel = new JLabel();
        cronHelpLabel.setIcon(new ImageIcon(getClass().getResource("/icon/helpButton.png")));
        cronHelpLabel.setText("");
        schedulePanel.add(cronHelpLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("任务类型");
        panel6.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        manualTaskRadioButton = new JRadioButton();
        manualTaskRadioButton.setText("手动任务");
        panel6.add(manualTaskRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scheduleRadioButton = new JRadioButton();
        scheduleRadioButton.setText("定时任务");
        panel6.add(scheduleRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        triggerRadioButton = new JRadioButton();
        triggerRadioButton.setEnabled(false);
        triggerRadioButton.setText("触发任务");
        panel6.add(triggerRadioButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel7.setBorder(BorderFactory.createTitledBorder(null, "告警和通知", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        sendPushResultCheckBox = new JCheckBox();
        sendPushResultCheckBox.setText("将推送结果发送邮件给（多个以分号分隔）：");
        panel7.add(sendPushResultCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        mailResultToTextField = new JTextField();
        panel7.add(mailResultToTextField, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
