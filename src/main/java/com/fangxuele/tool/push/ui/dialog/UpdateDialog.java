package com.fangxuele.tool.push.ui.dialog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * <pre>
 * 更新下载dialog
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/7.
 */
public class UpdateDialog extends JDialog {
    private static final long serialVersionUID = -5858063892133811698L;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBarDownload;
    private JButton buttonDownloadFromWeb;
    private JLabel statusLabel;
    private File downLoadFile;

    public UpdateDialog() {
        super(App.mainFrame, "下载新版");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            this.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            this.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            this.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            this.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            GridLayoutManager gridLayoutManager = (GridLayoutManager) contentPane.getLayout();
            gridLayoutManager.setMargin(new Insets(28, 0, 0, 0));
        }

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 600, 200);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        buttonDownloadFromWeb.addActionListener(e -> {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI("https://gitee.com/zhoubochina/WePush/releases"));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void downLoad(String newVersion) {
        buttonOK.setEnabled(false);
        ThreadUtil.execute(
                () -> {
                    String fileUrl = "";
                    // 从github获取最新版本相关信息
                    String downloadLinkInfo = HttpUtil.get(UiConsts.DOWNLOAD_LINK_INFO_URL);
                    if (StringUtils.isEmpty(downloadLinkInfo) || downloadLinkInfo.contains("404: Not Found")) {
                        JOptionPane.showMessageDialog(App.mainFrame,
                                "获取下载链接失败，请关注Gitee Release！", "网络错误",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    } else {
                        DocumentContext parse = JsonPath.parse(downloadLinkInfo);
                        if (SystemUtil.isWindowsOs()) {
                            fileUrl = parse.read("$.windows");
                        } else if (SystemUtil.isMacOs()) {
                            fileUrl = parse.read("$.mac");
                        } else if (SystemUtil.isLinuxOs()) {
                            fileUrl = parse.read("$.linux");
                        }
                    }

                    String fileName = FileUtil.getName(fileUrl);
                    URL url;
                    try {
                        url = new URL(fileUrl);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        //获取相应的文件长度
                        int fileLength = urlConnection.getContentLength();
                        progressBarDownload.setMaximum(fileLength);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File tempDir = new File(FileUtil.getTmpDirPath() + "WePush");
                    if (!tempDir.exists()) {
                        tempDir.mkdirs();
                    }
                    FileUtil.clean(tempDir);
                    downLoadFile = FileUtil.file(tempDir + File.separator + fileName);
                    HttpUtil.downloadFile(fileUrl, FileUtil.touch(downLoadFile), new StreamProgress() {

                        @Override
                        public void start() {
                            statusLabel.setText("开始下载。。。。");
                        }

                        @Override
                        public void progress(long progressSize) {
                            progressBarDownload.setValue((int) progressSize);
                            statusLabel.setText("已下载：" + FileUtil.readableFileSize(progressSize));
                        }

                        @Override
                        public void finish() {
                            statusLabel.setText("下载完成！");
                            buttonOK.setEnabled(true);
                        }
                    });
                }
        );
    }

    private void onOK() {
        try {
            Desktop.getDesktop().open(downLoadFile);
            dispose();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("立即安装");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel2.add(buttonCancel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDownloadFromWeb = new JButton();
        buttonDownloadFromWeb.setText("打开下载页面");
        panel2.add(buttonDownloadFromWeb, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        progressBarDownload = new JProgressBar();
        panel3.add(progressBarDownload, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusLabel = new JLabel();
        statusLabel.setText("就绪");
        panel3.add(statusLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
