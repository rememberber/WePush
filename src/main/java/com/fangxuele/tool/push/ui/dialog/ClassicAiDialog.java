package com.fangxuele.tool.push.ui.dialog;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ai.ClassicAiInstaller;
import com.fangxuele.tool.push.ai.ClassicAiIntegration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.concurrent.Callable;

/** Hand-written dialog to keep AI integration out of generated IntelliJ forms. */
public final class ClassicAiDialog extends JDialog {
    private final JTextArea status = new JTextArea(5, 48);
    private final JButton codex = new JButton("一键接入 Codex");
    private final JButton skill = new JButton("仅安装 Skill");
    private final JButton generic = new JButton("复制通用 MCP 配置");
    private final JButton toggle = new JButton();

    public ClassicAiDialog() {
        super(App.mainFrame, "AI 助手接入 · Classic", true);
        JPanel panel = new JPanel(new BorderLayout(12, 16));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.add(new JLabel("<html><b>让 Codex 等 AI 助手使用 WePush Classic</b><br><br>"
                + "查询任务与历史、查看发送预览、空跑校验，并按授权执行正式发送。<br>"
                + "支持已保存的手动固定线程任务；使用期间请保持 Classic 运行。<br>"
                + "安装名称为 wepush-classic，可与 Next 同时使用，无需额外安装 Node.js 或 Java。</html>"), BorderLayout.NORTH);
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setOpaque(false);
        panel.add(status, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        List.of(codex, skill, generic, toggle).forEach(buttons::add);
        panel.add(buttons, BorderLayout.SOUTH);
        setContentPane(panel);
        codex.addActionListener(event -> work(() -> {
            ClassicAiInstaller installer = new ClassicAiInstaller();
            installer.install(true);
            ClassicAiIntegration.enable();
            return "已安装 Codex MCP 和 Skill。请重新启动 Codex 或新建会话加载接入。\nSkill：" + installer.skillPath();
        }));
        skill.addActionListener(event -> work(() -> {
            ClassicAiInstaller installer = new ClassicAiInstaller();
            installer.install(false);
            ClassicAiIntegration.enable();
            return "已安装 Skill，请让助手加载该目录中的 SKILL.md。\nSkill：" + installer.skillPath();
        }));
        generic.addActionListener(event -> work(() -> {
            ClassicAiIntegration.enable();
            String config = new ClassicAiInstaller().genericConfig();
            SwingUtilities.invokeLater(() -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(config), null));
            return "通用 stdio MCP 配置已复制。将 mcpServers 中的 wepush-classic 条目合并到助手的 MCP 设置，再重新连接。";
        }));
        toggle.addActionListener(event -> work(() -> {
            if (ClassicAiIntegration.isRunning()) {
                ClassicAiIntegration.disable();
                return "本机接入已关闭。已启动的任务继续执行，安装文件保留。";
            }
            ClassicAiIntegration.enable();
            return "本机接入已开启；下次启动 Classic 时自动恢复。";
        }));
        refresh();
        status.setText(ClassicAiIntegration.isRunning() ? "本机接入已开启。可安装或更新助手配置。" : "本机接入尚未开启。选择安装方式即可开启并完成配置。");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(App.mainFrame);
    }

    private void refresh() { toggle.setText(ClassicAiIntegration.isRunning() ? "关闭本机接入" : "开启本机接入"); }

    private void work(Callable<String> action) {
        List.of(codex, skill, generic, toggle).forEach(button -> button.setEnabled(false));
        status.setText("正在处理……");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return action.call(); }
            @Override protected void done() {
                try { status.setText(get()); }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    status.setText("未完成：" + cause.getMessage());
                }
                List.of(codex, skill, generic, toggle).forEach(button -> button.setEnabled(true));
                refresh();
            }
        }.execute();
    }
}
