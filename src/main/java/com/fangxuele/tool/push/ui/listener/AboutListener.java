package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.UpgradeUtil;
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
        AboutForm aboutForm = AboutForm.getInstance();
        aboutForm.getCompanyLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
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
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });

        // 检查更新
        aboutForm.getCheckUpdateLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                ThreadUtil.execute(() -> UpgradeUtil.checkUpdate(false));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        // 帮助文档
        aboutForm.getHelpDocLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
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
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }

    /**
     * 初始化二维码
     */
    public static void initQrCode() {
        String qrCodeContent = HttpUtil.get(UiConsts.QR_CODE_URL);
        if (StringUtils.isNotEmpty(qrCodeContent)) {
            Map<String, String> urlMap = JSONUtil.toBean(qrCodeContent, Map.class);
            JLabel qrCodeLabel = AboutForm.getInstance().getQrCodeLabel();

            try {
                URL url = new URL(urlMap.get("url"));
                BufferedImage image = ImageIO.read(url);
                qrCodeLabel.setIcon(new ImageIcon(image));
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e);
            }

            MainWindow.getInstance().getAboutPanel().updateUI();
        }
    }
}
