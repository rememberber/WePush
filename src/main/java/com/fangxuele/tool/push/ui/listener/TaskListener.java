package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.task.Task;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TTaskHisMapper;
import com.fangxuele.tool.push.dao.TTaskMapper;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.domain.TTaskHis;
import com.fangxuele.tool.push.logic.InfinityTaskRunThread;
import com.fangxuele.tool.push.logic.TaskModeEnum;
import com.fangxuele.tool.push.logic.TaskRunThread;
import com.fangxuele.tool.push.logic.TaskTypeEnum;
import com.fangxuele.tool.push.ui.dialog.InfinityTaskHisDetailDialog;
import com.fangxuele.tool.push.ui.dialog.NewTaskDialog;
import com.fangxuele.tool.push.ui.dialog.TaskHisDetailDialog;
import com.fangxuele.tool.push.ui.form.TaskForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 推送任务相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/5/21.
 */
public class TaskListener {

    private static TTaskMapper taskMapper = MybatisUtil.getSqlSession().getMapper(TTaskMapper.class);
    private static TTaskHisMapper taskHisMapper = MybatisUtil.getSqlSession().getMapper(TTaskHisMapper.class);

    public static Map<Integer, Scheduler> scheduledTaskMap = new HashMap<>(16);

    public static void addListeners() {
        TaskForm taskForm = TaskForm.getInstance();

        taskForm.getNewTaskButton().addActionListener(e -> {
            NewTaskDialog dialog = new NewTaskDialog();
            dialog.pack();
            dialog.setVisible(true);
        });

        taskForm.getModifyButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要修改的任务！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
            TTask tTask = taskMapper.selectByPrimaryKey(taskId);

            NewTaskDialog dialog = new NewTaskDialog(tTask);
            dialog.pack();
            dialog.setVisible(true);
        });

        taskForm.getTaskHisDetailButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskHisListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要查看的任务记录！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskHisId = (Integer) taskForm.getTaskHisListTable().getValueAt(selectedRow, 0);
            TTaskHis tTaskHis = taskHisMapper.selectByPrimaryKey(taskHisId);
            if (TaskModeEnum.FIX_THREAD_TASK_CODE == tTaskHis.getTaskMode()) {
                TaskRunThread taskRunThread = TaskRunThread.taskRunThreadMap.get(taskHisId);

                TaskHisDetailDialog dialog = new TaskHisDetailDialog(taskRunThread, taskHisId);
                dialog.pack();
                dialog.setVisible(true);
            } else if (TaskModeEnum.INFINITY_TASK_CODE == tTaskHis.getTaskMode()) {
                InfinityTaskRunThread infinityTaskRunThread = InfinityTaskRunThread.infinityTaskRunThreadMap.get(taskHisId);

                InfinityTaskHisDetailDialog dialog = new InfinityTaskHisDetailDialog(infinityTaskRunThread, taskHisId);
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        taskForm.getStartButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要执行的任务！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
            TTask tTask = taskMapper.selectByPrimaryKey(taskId);

            int isPush = JOptionPane.showConfirmDialog(taskForm.getMainPanel(),
                    "确定开始推送吗？\n\n任务：" +
                            tTask.getTitle() + "\n",
                    "确认推送？",
                    JOptionPane.YES_NO_OPTION);
            if (isPush == JOptionPane.YES_OPTION) {
                if (TaskModeEnum.FIX_THREAD_TASK_CODE == tTask.getTaskMode()) {
                    ThreadUtil.execute(new TaskRunThread(taskId, 0));
                } else if (TaskModeEnum.INFINITY_TASK_CODE == tTask.getTaskMode()) {
                    ThreadUtil.execute(new InfinityTaskRunThread(taskId, 0));
                }
            }
        });

        taskForm.getStartDryRunButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要执行的任务！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
            TTask tTask = taskMapper.selectByPrimaryKey(taskId);

            int isPush = JOptionPane.showConfirmDialog(taskForm.getMainPanel(),
                    "确定开始推送吗？\n\n任务：" +
                            tTask.getTitle() + "\n",
                    "确认推送？",
                    JOptionPane.YES_NO_OPTION);
            if (isPush == JOptionPane.YES_OPTION) {
                if (TaskModeEnum.FIX_THREAD_TASK_CODE == tTask.getTaskMode()) {
                    ThreadUtil.execute(new TaskRunThread(taskId, 1));
                } else if (TaskModeEnum.INFINITY_TASK_CODE == tTask.getTaskMode()) {
                    ThreadUtil.execute(new InfinityTaskRunThread(taskId, 1));
                }
            }
        });

        // 点击左侧表格事件
        taskForm.getTaskListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selectedRow = taskForm.getTaskListTable().getSelectedRow();
                Integer selectedTaskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);

                TaskForm.initTaskHisListTable(selectedTaskId);
                super.mousePressed(e);
            }
        });

        taskForm.getStopButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskHisListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要停止的任务记录！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskHisId = (Integer) taskForm.getTaskHisListTable().getValueAt(selectedRow, 0);

            TTaskHis tTaskHis = taskHisMapper.selectByPrimaryKey(taskHisId);

            if (tTaskHis.getTaskMode() == TaskModeEnum.FIX_THREAD_TASK_CODE) {
                TaskRunThread taskRunThread = TaskRunThread.taskRunThreadMap.get(taskHisId);
                if (taskRunThread != null && taskRunThread.isRunning()) {
                    int isStop = JOptionPane.showConfirmDialog(App.mainFrame,
                            "确定停止当前的推送吗？", "确认停止？",
                            JOptionPane.YES_NO_OPTION);
                    if (isStop == JOptionPane.YES_OPTION) {
                        taskRunThread.running = false;
                    }
                }
            } else if (tTaskHis.getTaskMode() == TaskModeEnum.INFINITY_TASK_CODE) {
                InfinityTaskRunThread infinityTaskRunThread = InfinityTaskRunThread.infinityTaskRunThreadMap.get(taskHisId);
                if (infinityTaskRunThread != null && infinityTaskRunThread.isRunning()) {
                    int isStop = JOptionPane.showConfirmDialog(App.mainFrame,
                            "确定停止当前的推送吗？", "确认停止？",
                            JOptionPane.YES_NO_OPTION);
                    if (isStop == JOptionPane.YES_OPTION) {
                        infinityTaskRunThread.running = false;
                    }
                }
            }
        });

        taskForm.getHisDeleteButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskHisListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要删除的任务记录！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskHisId = (Integer) taskForm.getTaskHisListTable().getValueAt(selectedRow, 0);

            int isDelete = JOptionPane.showConfirmDialog(App.mainFrame,
                    "确定删除当前的任务记录吗？", "确认删除？",
                    JOptionPane.YES_NO_OPTION);
            if (isDelete == JOptionPane.YES_OPTION) {

                TTaskHis tTaskHis = taskHisMapper.selectByPrimaryKey(taskHisId);

                // 删除文件
                String successFilePath = tTaskHis.getSuccessFilePath();
                if (StringUtils.isNotBlank(successFilePath)) {
                    File successFile = new File(successFilePath);
                    if (successFile.exists()) {
                        successFile.delete();
                    }
                }
                String failFilePath = tTaskHis.getFailFilePath();
                if (StringUtils.isNotBlank(failFilePath)) {
                    File failFile = new File(failFilePath);
                    if (failFile.exists()) {
                        failFile.delete();
                    }
                }
                String noSendFilePath = tTaskHis.getNoSendFilePath();
                if (StringUtils.isNotBlank(noSendFilePath)) {
                    File noSendFile = new File(noSendFilePath);
                    if (noSendFile.exists()) {
                        noSendFile.delete();
                    }
                }
                String logFilePath = tTaskHis.getLogFilePath();
                if (StringUtils.isNotBlank(logFilePath)) {
                    File logFile = new File(logFilePath);
                    if (logFile.exists()) {
                        logFile.delete();
                    }
                }

                taskHisMapper.deleteByPrimaryKey(taskHisId);

                int selectedTaskRow = taskForm.getTaskListTable().getSelectedRow();
                Integer selectedTaskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedTaskRow, 0);
                TaskForm.initTaskHisListTable(selectedTaskId);
            }
        });

        taskForm.getDeleteButton().addActionListener(e -> {
            int selectedRow = taskForm.getTaskListTable().getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(taskForm.getMainPanel(), "请先选择要删除的任务！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer taskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);

            int isDelete = JOptionPane.showConfirmDialog(App.mainFrame,
                    "确定删除当前的任务吗？", "确认删除？",
                    JOptionPane.YES_NO_OPTION);
            if (isDelete == JOptionPane.YES_OPTION) {
                // 删除任务历史记录
                taskHisMapper.selectByTaskId(taskId).forEach(tTaskHis -> {
                    // 删除文件
                    String successFilePath = tTaskHis.getSuccessFilePath();
                    if (StringUtils.isNotBlank(successFilePath)) {
                        File successFile = new File(successFilePath);
                        if (successFile.exists()) {
                            successFile.delete();
                        }
                    }
                    String failFilePath = tTaskHis.getFailFilePath();
                    if (StringUtils.isNotBlank(failFilePath)) {
                        File failFile = new File(failFilePath);
                        if (failFile.exists()) {
                            failFile.delete();
                        }
                    }
                    String noSendFilePath = tTaskHis.getNoSendFilePath();
                    if (StringUtils.isNotBlank(noSendFilePath)) {
                        File noSendFile = new File(noSendFilePath);
                        if (noSendFile.exists()) {
                            noSendFile.delete();
                        }
                    }
                    String logFilePath = tTaskHis.getLogFilePath();
                    if (StringUtils.isNotBlank(logFilePath)) {
                        File logFile = new File(logFilePath);
                        if (logFile.exists()) {
                            logFile.delete();
                        }
                    }
                    taskHisMapper.deleteByPrimaryKey(tTaskHis.getId());
                });

                // 删除定时任务
                Scheduler scheduler = scheduledTaskMap.get(taskId);
                if (scheduler != null) {
                    scheduler.stop();
                    scheduledTaskMap.remove(taskId);
                }

                TaskRunThread taskRunThread = TaskRunThread.taskRunThreadMap.get(taskId);
                if (taskRunThread != null) {
                    taskRunThread.running = false;
                    taskRunThread.interrupt();
                }
                TaskRunThread.taskRunThreadMap.remove(taskId);

                InfinityTaskRunThread infinityTaskRunThread = InfinityTaskRunThread.infinityTaskRunThreadMap.get(taskId);
                if (infinityTaskRunThread != null) {
                    infinityTaskRunThread.running = false;
                    infinityTaskRunThread.interrupt();
                }
                InfinityTaskRunThread.infinityTaskRunThreadMap.remove(taskId);

                taskMapper.deleteByPrimaryKey(taskId);
                TaskForm.initTaskListTable();
            }
        });
    }

    /**
     * 重启应用时把所有定时任务重新加入到任务队列
     */
    public static void addAllScheduledTask() {
        List<TTask> tTaskList = taskMapper.selectAll();
        for (TTask tTask : tTaskList) {
            if (tTask.getTaskPeriod() == TaskTypeEnum.SCHEDULE_TASK_CODE && StringUtils.isNotBlank(tTask.getCron())) {
                Scheduler scheduler = new Scheduler();
                scheduler.setMatchSecond(true);
                String schedulerId = scheduler.schedule(tTask.getCron(), (Task) () -> {
                    if (tTask.getTaskMode() == TaskModeEnum.FIX_THREAD_TASK_CODE) {
                        TaskRunThread taskRunThread = new TaskRunThread(tTask.getId(), 0);
                        taskRunThread.setFixRateScheduling(true);
                        taskRunThread.start();
                    } else if (tTask.getTaskMode() == TaskModeEnum.INFINITY_TASK_CODE) {
                        InfinityTaskRunThread infinityTaskRunThread = new InfinityTaskRunThread(tTask.getId(), 0);
                        infinityTaskRunThread.setFixRateScheduling(true);
                        infinityTaskRunThread.start();
                    }
                });
                scheduler.start();
                TaskListener.scheduledTaskMap.put(tTask.getId(), scheduler);
            }
        }
    }
}
