package com.fangxuele.tool.push.ui.form;

import cn.hutool.core.io.FileUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.domain.TTaskHis;
import com.fangxuele.tool.push.logic.*;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.util.JTableUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Date;
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
    private JButton modifyButton;
    private JButton startDryRunButton;
    private JButton startButton;
    private JTable taskHisListTable;
    private JButton stopButton;
    private JButton hisDeleteButton;
    private JButton taskHisDetailButton;
    private JLabel jvmMemoryLabel;
    private JLabel availableProcessorLabel;
    private JLabel scheduleDetailLabel;
    private JPanel pushUpPanel;
    private JLabel taskTitle;
    private JSplitPane mainSplitPane;
    private JLabel msgTypeLabel;
    private JLabel msgNameLabel;
    private JLabel peopleNameLabel;
    private JLabel schedulePlanDetailLabel;
    private JLabel plan1Label;
    private JLabel plan2Label;
    private JLabel plan3Label;
    private JLabel plan4Label;
    private JLabel plan5Label;
    private JLabel planToContinueLabel;
    private JLabel modeLabel;
    private JLabel threadCntLabel;

    private static TaskForm taskForm;

    private static TTaskMapper taskMapper = MybatisUtil.getSqlSession().getMapper(TTaskMapper.class);
    private static TTaskHisMapper taskHisMapper = MybatisUtil.getSqlSession().getMapper(TTaskHisMapper.class);
    private static TTaskExtMapper taskExtMapper = MybatisUtil.getSqlSession().getMapper(TTaskExtMapper.class);
    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

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

        hidePlanLabel();

        taskForm.getDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        taskForm.getHisDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));
        taskForm.getStartButton().setIcon(new FlatSVGIcon("icon/send.svg"));
        taskForm.getNewTaskButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        taskForm.getModifyButton().setIcon(new FlatSVGIcon("icon/modify.svg"));
        taskForm.getStartDryRunButton().setIcon(new FlatSVGIcon("icon/debug.svg"));
        taskForm.getTaskHisDetailButton().setIcon(new FlatSVGIcon("icon/detail.svg"));
        taskForm.getStopButton().setIcon(new FlatSVGIcon("icon/stop.svg"));

        taskForm.getTaskListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        taskForm.getTaskHisListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);
        JTableUtil.setTableHeaderLeftAlignment(taskForm.getTaskListTable());
        JTableUtil.setTableHeaderLeftAlignment(taskForm.getTaskHisListTable());
        taskForm.getTaskListTable().setShowHorizontalLines(true);
        taskForm.getTaskHisListTable().setShowHorizontalLines(true);
        taskForm.getTaskListTable().setShowVerticalLines(true);
        taskForm.getTaskHisListTable().setShowVerticalLines(true);
        taskForm.getMainSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 2));

        initTaskListTable();

        // 每秒钟刷新一次
        new Timer(1000, e -> {
            // 可用处理器核心
            taskForm.getAvailableProcessorLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
            // JVM内存占用
            taskForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));
        }).start();
    }

    public static void initTaskListTable() {
        JTable taskListTable = taskForm.getTaskListTable();

        // 任务数据列表
        String[] headerNames = {"id", "任务名称", "消息类型", "周期", "消息名称", "人群"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        taskListTable.setModel(model);

        taskListTable.getTableHeader().setReorderingAllowed(false);

        Object[] data;

        List<TTask> taskList = taskExtMapper.selectAll();
        for (TTask task : taskList) {
            data = new Object[6];
            data[0] = task.getId();
            data[1] = task.getTitle();
            data[2] = MessageTypeEnum.getName(task.getMsgType());
            data[3] = getTaskType(task);
            data[4] = getMsgName(task.getMessageId());
            data[5] = peopleMapper.selectByPrimaryKey(task.getPeopleId()).getPeopleName();
            model.addRow(data);
        }
        // 隐藏id列
        JTableUtil.hideColumn(taskListTable, 0);

        // 设置列宽
//        TableColumnModel tableColumnModel = taskListTable.getColumnModel();
//        tableColumnModel.getColumn(3).setMaxWidth(60);

        // 如果有数据，则默认选中第一行
        if (taskListTable.getRowCount() > 0) {
            taskListTable.setRowSelectionInterval(0, 0);
            initTaskHisListTable((Integer) taskListTable.getValueAt(0, 0));

            TTask tTask = taskList.get(0);

            fillSchedulePlan(tTask);
        } else {
            JTable taskHisListTable = taskForm.getTaskHisListTable();
            // 清空任务历史列表
            String[] headerNames2 = {"id", "是否空跑", "开始时间", "结束时间", "总量", "成功", "失败", "状态"};
            DefaultTableModel model2 = new DefaultTableModel(null, headerNames2);
            taskHisListTable.setModel(model2);
        }
    }

    public static void fillSchedulePlan(TTask tTask) {
        taskForm.getTaskTitle().setText(tTask.getTitle());
        taskForm.getMsgNameLabel().setText("消息名称：" + getMsgName(tTask.getMessageId()));
        taskForm.getMsgTypeLabel().setText("消息类型：" + MessageTypeEnum.getName(tTask.getMsgType()));
        taskForm.getPeopleNameLabel().setText("人群：" + peopleMapper.selectByPrimaryKey(tTask.getPeopleId()).getPeopleName());
        taskForm.getModeLabel().setText("模式：" + TaskModeEnum.getDescByCode(tTask.getTaskMode()));
        taskForm.getThreadCntLabel().setText("线程数：" + tTask.getThreadCnt());

        if (tTask.getTaskPeriod() == TaskTypeEnum.SCHEDULE_TASK_CODE && StringUtils.isNotBlank(tTask.getCron())) {
            List<String> latest5RunTimeList = Lists.newArrayList();
            Date now = new Date();
            for (int i = 0; i < 5; i++) {
                if (PeriodTypeEnum.RUN_AT_THIS_TIME_TASK_CODE == tTask.getPeriodType()) {
                    latest5RunTimeList.add(tTask.getPeriodTime());
                    break;
                }
                if (PeriodTypeEnum.RUN_PER_DAY_TASK_CODE == tTask.getPeriodType()) {
                    Date date = CronPatternUtil.nextDateAfter(new CronPattern(tTask.getCron()), DateUtils.addDays(now, i), true);
                    latest5RunTimeList.add(DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
                    continue;
                }
                if (PeriodTypeEnum.RUN_PER_WEEK_TASK_CODE == tTask.getPeriodType()) {
                    Date date = CronPatternUtil.nextDateAfter(new CronPattern(tTask.getCron()), DateUtils.addDays(now, i * 7), true);
                    latest5RunTimeList.add(DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
                    continue;
                }
                if (PeriodTypeEnum.CRON_TASK_CODE == tTask.getPeriodType()) {
                    Date date = CronPatternUtil.nextDateAfter(new CronPattern(tTask.getCron()), DateUtils.addDays(now, i), true);
                    latest5RunTimeList.add(DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
                    continue;
                }
            }
            taskForm.getSchedulePlanDetailLabel().setText("执行计划：");
            if (latest5RunTimeList.size() > 0) {
                taskForm.getPlan1Label().setText(latest5RunTimeList.get(0) == null ? "" : latest5RunTimeList.get(0));
            }
            if (latest5RunTimeList.size() > 1) {
                taskForm.getPlan2Label().setText(latest5RunTimeList.get(1) == null ? "" : latest5RunTimeList.get(1));
            }
            if (latest5RunTimeList.size() > 2) {
                taskForm.getPlan3Label().setText(latest5RunTimeList.get(2) == null ? "" : latest5RunTimeList.get(2));
            }
            if (latest5RunTimeList.size() > 3) {
                taskForm.getPlan4Label().setText(latest5RunTimeList.get(3) == null ? "" : latest5RunTimeList.get(3));
            }
            if (latest5RunTimeList.size() > 4) {
                taskForm.getPlan5Label().setText(latest5RunTimeList.get(4) == null ? "" : latest5RunTimeList.get(4));
            }
            taskForm.getPlanToContinueLabel().setText("……");
        } else {
            taskForm.getSchedulePlanDetailLabel().setText("执行计划：无");
            hidePlanLabel();
        }
    }

    private static void hidePlanLabel() {
        taskForm.getPlan1Label().setText("");
        taskForm.getPlan2Label().setText("");
        taskForm.getPlan3Label().setText("");
        taskForm.getPlan4Label().setText("");
        taskForm.getPlan5Label().setText("");
        taskForm.getPlanToContinueLabel().setText("");
    }

    private static String getTaskType(TTask task) {
        if (TaskTypeEnum.SCHEDULE_TASK.getCode() == task.getTaskPeriod()) {
            return task.getCron();
        } else {
            return TaskTypeEnum.getDescByCode(task.getTaskPeriod());
        }
    }

    private static String getMsgName(Integer messageId) {
        String msgName = msgMapper.selectByPrimaryKey(messageId).getMsgName();
        return msgName;
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
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        mainSplitPane = new JSplitPane();
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setDividerLocation(516);
        mainSplitPane.setDoubleBuffered(true);
        mainPanel.add(mainSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainSplitPane.setLeftComponent(panel1);
        taskListScrollPane = new JScrollPane();
        panel1.add(taskListScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        taskListTable = new JTable();
        taskListTable.setDragEnabled(false);
        taskListScrollPane.setViewportView(taskListTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        newTaskButton = new JButton();
        newTaskButton.setText("新建");
        panel2.add(newTaskButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("删除");
        panel2.add(deleteButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modifyButton = new JButton();
        modifyButton.setText("修改");
        panel2.add(modifyButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startDryRunButton = new JButton();
        startDryRunButton.setText("空跑测试");
        panel2.add(startDryRunButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("立即执行");
        panel2.add(startButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainSplitPane.setRightComponent(panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pushUpPanel = new JPanel();
        pushUpPanel.setLayout(new GridLayoutManager(1, 5, new Insets(5, 5, 5, 5), -1, -1));
        panel4.add(pushUpPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        separator1.setOrientation(1);
        pushUpPanel.add(separator1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        pushUpPanel.add(panel5, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jvmMemoryLabel = new JLabel();
        jvmMemoryLabel.setText("JVM内存占用：--");
        panel5.add(jvmMemoryLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        availableProcessorLabel = new JLabel();
        availableProcessorLabel.setText("可用处理器核心：--");
        panel5.add(availableProcessorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scheduleDetailLabel = new JLabel();
        scheduleDetailLabel.setForeground(new Color(-276358));
        scheduleDetailLabel.setText("");
        panel5.add(scheduleDetailLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        pushUpPanel.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        taskTitle = new JLabel();
        Font taskTitleFont = this.$$$getFont$$$(null, -1, 24, taskTitle.getFont());
        if (taskTitleFont != null) taskTitle.setFont(taskTitleFont);
        taskTitle.setForeground(new Color(-276358));
        taskTitle.setText("任务标题");
        panel6.add(taskTitle, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeLabel = new JLabel();
        msgTypeLabel.setText("消息类型：");
        panel6.add(msgTypeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgNameLabel = new JLabel();
        msgNameLabel.setText("消息名称：");
        panel6.add(msgNameLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleNameLabel = new JLabel();
        peopleNameLabel.setText("人群：");
        panel6.add(peopleNameLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeLabel = new JLabel();
        modeLabel.setText("模式：");
        panel6.add(modeLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadCntLabel = new JLabel();
        threadCntLabel.setText("线程数：");
        panel6.add(threadCntLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        pushUpPanel.add(panel7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        schedulePlanDetailLabel = new JLabel();
        schedulePlanDetailLabel.setText("执行计划：");
        panel7.add(schedulePlanDetailLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        plan1Label = new JLabel();
        plan1Label.setText("");
        panel7.add(plan1Label, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        plan2Label = new JLabel();
        plan2Label.setText("");
        panel7.add(plan2Label, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        plan3Label = new JLabel();
        plan3Label.setText("");
        panel7.add(plan3Label, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        plan4Label = new JLabel();
        plan4Label.setText("");
        panel7.add(plan4Label, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        plan5Label = new JLabel();
        plan5Label.setText("");
        panel7.add(plan5Label, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        planToContinueLabel = new JLabel();
        planToContinueLabel.setText("……");
        panel7.add(planToContinueLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        separator2.setOrientation(1);
        pushUpPanel.add(separator2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel8.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        taskHisListTable = new JTable();
        scrollPane1.setViewportView(taskHisListTable);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        stopButton = new JButton();
        stopButton.setText("停止");
        panel9.add(stopButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hisDeleteButton = new JButton();
        hisDeleteButton.setText("删除");
        panel9.add(hisDeleteButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel9.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        taskHisDetailButton = new JButton();
        taskHisDetailButton.setText("详情");
        panel9.add(taskHisDetailButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    public static void initTaskHisListTable(Integer selectedTaskId) {
        JTable taskHisListTable = taskForm.getTaskHisListTable();

        // 任务数据列表
        String[] headerNames = {"id", "是否空跑", "开始时间", "结束时间", "总量", "成功", "失败", "状态"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        taskHisListTable.setModel(model);

        taskHisListTable.getTableHeader().setReorderingAllowed(false);

        Object[] data;

        List<TTaskHis> taskHisList = taskHisMapper.selectByTaskId(selectedTaskId);
        for (TTaskHis taskHis : taskHisList) {
            data = new Object[8];
            data[0] = taskHis.getId();
            data[1] = taskHis.getDryRun() == 1 ? "空跑" : "否";
            data[2] = taskHis.getStartTime();
            data[3] = taskHis.getEndTime();
            data[4] = taskHis.getTotalCnt();
            data[5] = taskHis.getSuccessCnt();
            data[6] = taskHis.getFailCnt();
            data[7] = TaskStatusEnum.getDescByCode(taskHis.getStatus());
            model.addRow(data);
        }
        // 隐藏id列
        JTableUtil.hideColumn(taskHisListTable, 0);
        // 设置列宽
//        TableColumnModel tableColumnModel = taskHisListTable.getColumnModel();
//        tableColumnModel.getColumn(1).setMaxWidth(50);
//        tableColumnModel.getColumn(6).setMaxWidth(50);

        // 如果有数据，则默认选中第一行
        if (taskHisListTable.getRowCount() > 0) {
            taskHisListTable.setRowSelectionInterval(0, 0);
        }
    }
}
