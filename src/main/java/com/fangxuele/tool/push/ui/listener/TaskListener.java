package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import com.fangxuele.tool.push.dao.TTaskMapper;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.logic.TaskRunThread;
import com.fangxuele.tool.push.ui.dialog.NewTaskDialog;
import com.fangxuele.tool.push.ui.dialog.TaskHisDetailDialog;
import com.fangxuele.tool.push.ui.form.TaskForm;
import com.fangxuele.tool.push.util.MybatisUtil;

import javax.swing.*;

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

    public static void addListeners() {
        TaskForm taskForm = TaskForm.getInstance();

        taskForm.getNewTaskButton().addActionListener(e -> {
            NewTaskDialog dialog = new NewTaskDialog();
            dialog.pack();
            dialog.setVisible(true);
        });

        taskForm.getTaskHisDetailButton().addActionListener(e -> {
            TaskHisDetailDialog dialog = new TaskHisDetailDialog();
            dialog.pack();
            dialog.setVisible(true);
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
                ThreadUtil.execute(new TaskRunThread(taskId, 0));
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
                ThreadUtil.execute(new TaskRunThread(taskId, 1));
            }
        });
    }
}
