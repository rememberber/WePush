package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.dao.TTaskExtMapper;
import com.fangxuele.tool.push.dao.TTaskMapper;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.component.TableInCellTaskDetailButtonColumn;
import com.fangxuele.tool.push.ui.component.TableInCellTaskExecuteButtonColumn;
import com.fangxuele.tool.push.ui.component.TableInCellTaskModifyButtonColumn;
import com.fangxuele.tool.push.util.JTableUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.List;
import java.util.Locale;

/**
 * <pre>
 * 推送任务
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/5/14.
 */
@Getter
public class TaskForm {
    private JPanel mainPanel;
    private JTable taskListTable;
    private JButton newTaskButton;
    private JScrollPane taskListScrollPane;
    private JButton deleteButton;
    private JPanel pushControlPanel;
    private JTextField sliderValueTextField;
    private JCheckBox dryRunCheckBox;
    private JLabel threadTipsLabel;
    private JLabel dryRunHelpLabel;
    private JCheckBox saveResponseBodyCheckBox;
    private JSlider threadCountSlider;
    private JLabel JVM内存占用Label;
    private JLabel 核心线程数0Label;
    private JLabel 活跃线程数0Label;
    private JLabel 最大线程数0Label;
    private JLabel 可用处理器核心Label;

    private static TaskForm taskForm;

    private static TTaskMapper taskMapper = MybatisUtil.getSqlSession().getMapper(TTaskMapper.class);
    private static TTaskExtMapper taskExtMapper = MybatisUtil.getSqlSession().getMapper(TTaskExtMapper.class);

    private TaskForm() {
        UndoUtil.register(this);
    }

    public static TaskForm getInstance() {
        if (taskForm == null) {
            taskForm = new TaskForm();
        }
        return taskForm;
    }

    public static void init() {
        taskForm = getInstance();
        taskForm.getTaskListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        taskForm.getTaskListTable().setShowHorizontalLines(true);
        initTaskListTable();
    }

    public static void initTaskListTable() {
        JTable taskListTable = taskForm.getTaskListTable();

        // 任务数据列表
        String[] headerNames = {"id", "任务名称", "状态", "上次开始", "上次结束", "运行周期", "消息类型", "消息名称", "人群", "详情", "修改", "执行"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        taskListTable.setModel(model);

        taskListTable.getTableHeader().setReorderingAllowed(false);

        // 执行按钮
        TableColumnModel tableColumnModel = taskListTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellTaskExecuteButtonColumn(taskListTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellTaskExecuteButtonColumn(taskListTable, headerNames.length - 1));

        // 修改按钮
        tableColumnModel.getColumn(headerNames.length - 2).
                setCellRenderer(new TableInCellTaskModifyButtonColumn(taskListTable, headerNames.length - 2));
        tableColumnModel.getColumn(headerNames.length - 2).
                setCellEditor(new TableInCellTaskModifyButtonColumn(taskListTable, headerNames.length - 2));

        // 详情按钮
        tableColumnModel.getColumn(headerNames.length - 3).
                setCellRenderer(new TableInCellTaskDetailButtonColumn(taskListTable, headerNames.length - 3));
        tableColumnModel.getColumn(headerNames.length - 3).
                setCellEditor(new TableInCellTaskDetailButtonColumn(taskListTable, headerNames.length - 3));

        Object[] data;

        List<TTask> taskList = taskExtMapper.selectAll();
        for (TTask task : taskList) {
            data = new Object[9];
            data[0] = task.getId();
            data[1] = task.getTitle();
            data[2] = "";
            data[3] = "";
            data[4] = "";
            data[5] = "";
            data[6] = task.getMsgType();
            data[7] = task.getMessageId();
            data[8] = task.getPeopleId();
            model.addRow(data);
        }
        // 隐藏id列
        JTableUtil.hideColumn(taskListTable, 0);
        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(taskForm.getNewTaskButton().getWidth());
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(taskForm.getNewTaskButton().getWidth());
        tableColumnModel.getColumn(headerNames.length - 2).setPreferredWidth(taskForm.getNewTaskButton().getWidth());
        tableColumnModel.getColumn(headerNames.length - 2).setMaxWidth(taskForm.getNewTaskButton().getWidth());
        tableColumnModel.getColumn(headerNames.length - 3).setPreferredWidth(taskForm.getNewTaskButton().getWidth());
        tableColumnModel.getColumn(headerNames.length - 3).setMaxWidth(taskForm.getNewTaskButton().getWidth());
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
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        taskListScrollPane = new JScrollPane();
        mainPanel.add(taskListScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        taskListTable = new JTable();
        taskListTable.setDragEnabled(false);
        taskListScrollPane.setViewportView(taskListTable);
        pushControlPanel = new JPanel();
        pushControlPanel.setLayout(new GridLayoutManager(1, 8, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(pushControlPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sliderValueTextField = new JTextField();
        sliderValueTextField.setEditable(false);
        sliderValueTextField.setHorizontalAlignment(10);
        sliderValueTextField.setMargin(new Insets(2, 6, 2, 6));
        sliderValueTextField.setRequestFocusEnabled(true);
        sliderValueTextField.setToolTipText("输入结束后请按回车键确认");
        pushControlPanel.add(sliderValueTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(60, -1), null, 0, false));
        dryRunCheckBox = new JCheckBox();
        dryRunCheckBox.setText("空跑");
        dryRunCheckBox.setToolTipText("空跑勾选时不会真实发送消息");
        pushControlPanel.add(dryRunCheckBox, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadTipsLabel = new JLabel();
        threadTipsLabel.setIcon(new ImageIcon(getClass().getResource("/icon/helpButton.png")));
        threadTipsLabel.setText("");
        pushControlPanel.add(threadTipsLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dryRunHelpLabel = new JLabel();
        dryRunHelpLabel.setIcon(new ImageIcon(getClass().getResource("/icon/helpButton.png")));
        dryRunHelpLabel.setText("");
        pushControlPanel.add(dryRunHelpLabel, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveResponseBodyCheckBox = new JCheckBox();
        saveResponseBodyCheckBox.setText("保存请求返回的Body");
        pushControlPanel.add(saveResponseBodyCheckBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadCountSlider = new JSlider();
        threadCountSlider.setDoubleBuffered(true);
        threadCountSlider.setExtent(0);
        threadCountSlider.setFocusCycleRoot(false);
        threadCountSlider.setFocusTraversalPolicyProvider(false);
        threadCountSlider.setFocusable(false);
        threadCountSlider.setInverted(false);
        threadCountSlider.setMajorTickSpacing(10);
        threadCountSlider.setMinimum(0);
        threadCountSlider.setMinorTickSpacing(5);
        threadCountSlider.setOpaque(false);
        threadCountSlider.setOrientation(0);
        threadCountSlider.setPaintLabels(true);
        threadCountSlider.setPaintTicks(true);
        threadCountSlider.setPaintTrack(true);
        threadCountSlider.setRequestFocusEnabled(false);
        threadCountSlider.setSnapToTicks(false);
        threadCountSlider.setToolTipText("线程数");
        threadCountSlider.setValue(0);
        threadCountSlider.setValueIsAdjusting(false);
        pushControlPanel.add(threadCountSlider, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newTaskButton = new JButton();
        newTaskButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        newTaskButton.setText("新建");
        pushControlPanel.add(newTaskButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("删除");
        pushControlPanel.add(deleteButton, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("计划中任务：2");
        panel2.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("TPS");
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("当前任务");
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        活跃线程数0Label = new JLabel();
        Font 活跃线程数0LabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, 活跃线程数0Label.getFont());
        if (活跃线程数0LabelFont != null) 活跃线程数0Label.setFont(活跃线程数0LabelFont);
        活跃线程数0Label.setText("活跃线程数：0");
        panel3.add(活跃线程数0Label, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        核心线程数0Label = new JLabel();
        核心线程数0Label.setText("核心线程数：0");
        panel3.add(核心线程数0Label, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        最大线程数0Label = new JLabel();
        最大线程数0Label.setText("最大线程数：0");
        panel3.add(最大线程数0Label, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        可用处理器核心Label = new JLabel();
        可用处理器核心Label.setText("可用处理器核心：--");
        panel3.add(可用处理器核心Label, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JVM内存占用Label = new JLabel();
        JVM内存占用Label.setText("JVM内存占用：--");
        panel3.add(JVM内存占用Label, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        separator1.setOrientation(1);
        panel1.add(separator1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}