package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.VersionSummary;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.UpdateDialog;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * AboutPanel Listener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/23.
 */
public class AboutListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        AboutForm.aboutForm.getCompanyLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/zhoubochina/WePush"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                AboutForm.aboutForm.getCompanyLabel().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });

        // 检查更新
        AboutForm.aboutForm.getCheckUpdateLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ThreadUtil.execute(() -> checkUpdate(false));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                AboutForm.aboutForm.getCheckUpdateLabel().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        // 帮助文档
        AboutForm.aboutForm.getHelpDocLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/WePush/wiki"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                AboutForm.aboutForm.getHelpDocLabel().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }

    public static void checkUpdate(boolean initCheck) {
        // 当前版本
        String currentVersion = UiConsts.APP_VERSION;

        // 从github获取最新版本相关信息
        String content = HttpUtil.get(UiConsts.CHECK_VERSION_URL);
        if (StringUtils.isEmpty(content) && !initCheck) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(),
                    "检查超时，请关注GitHub Release！", "网络错误",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        content = content.replace("\n", "");

        VersionSummary versionSummary = JSON.parseObject(content, VersionSummary.class);
        // 最新版本
        String newVersion = versionSummary.getCurrentVersion();
        String versionIndex = versionSummary.getVersionIndex();
        // 版本索引
        Map<String, String> versionIndexMap = JSON.parseObject(versionIndex, Map.class);
        // 版本明细列表
        List<VersionSummary.Version> versionDetailList = versionSummary.getVersionDetailList();

        if (newVersion.compareTo(currentVersion) > 0) {
            // 当前版本索引
            int currentVersionIndex = Integer.parseInt(versionIndexMap.get(currentVersion));
            // 版本更新日志：
            StringBuilder versionLogBuilder = new StringBuilder("惊现新版本！立即下载？\n\n");
            VersionSummary.Version version;
            for (int i = currentVersionIndex + 1; i < versionDetailList.size(); i++) {
                version = versionDetailList.get(i);
                versionLogBuilder.append(version.getVersion()).append("\n");
                versionLogBuilder.append(version.getTitle()).append("\n");
                versionLogBuilder.append(version.getLog()).append("\n");
            }
            String versionLog = versionLogBuilder.toString();

            int downLoadNow = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                    versionLog, "惊现新版本！立即下载？",
                    JOptionPane.YES_NO_OPTION);

            if (downLoadNow == JOptionPane.YES_OPTION) {
                if (SystemUtil.isMacOs()) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI("https://github.com/rememberber/WePush/releases"));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    UpdateDialog dialog = new UpdateDialog();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    dialog.setBounds(screenSize.width / 2 - 300, screenSize.height / 2 - 50,
                            600, 100);

                    Dimension preferSize = new Dimension(600, 100);
                    dialog.setMaximumSize(preferSize);
                    dialog.pack();
                    dialog.downLoad(newVersion);
                    dialog.setVisible(true);
                }
            }
        } else {
            if (!initCheck) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(),
                        "当前已经是最新版本！", "恭喜",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * 初始化二维码
     */
    public static void initQrCode() {
        String qrCodeContent = HttpUtil.get(UiConsts.QR_CODE_URL);
        if (StringUtils.isNotEmpty(qrCodeContent)) {
            Map<String, String> urlMap = JSONUtil.toBean(qrCodeContent, Map.class);
            JLabel qrCodeLabel = AboutForm.aboutForm.getQrCodeLabel();

            try {
                URL url = new URL(urlMap.get("url"));
                BufferedImage image = ImageIO.read(url);
                qrCodeLabel.setIcon(new ImageIcon(image));
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e);
            }

            MainWindow.mainWindow.getAboutPanel().updateUI();
        }
    }
}
