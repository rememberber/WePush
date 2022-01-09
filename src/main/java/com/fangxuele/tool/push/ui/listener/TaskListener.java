package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.ui.dialog.NewTaskDialog;
import com.fangxuele.tool.push.ui.form.TaskForm;

/**
 * <pre>
 * 推送任务相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/5/21.
 */
public class TaskListener {

    public static void addListeners() {
        TaskForm taskForm = TaskForm.getInstance();

        taskForm.getNewTaskButton().addActionListener(e -> {
            NewTaskDialog dialog = new NewTaskDialog();
            dialog.pack();
            dialog.setVisible(true);
        });
    }
}
