package com.fangxuele.tool.push.ui.component;

import com.fangxuele.tool.push.util.UpdateDownloadManager;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 主界面底部的「新版本已就绪」安装提示。
 */
@Slf4j
public class UpdateInstallPromptPanel extends JPanel {

    private final JButton actionButton = new JButton();
    private final JLabel titleLabel = new JLabel();
    private final JLabel versionLabel = new JLabel();
    private boolean installing;
    private Popup notesPopup;
    private Timer hideNotesTimer;

    public UpdateInstallPromptPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setVisible(false);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        actionButton.setLayout(new BorderLayout(8, 0));
        actionButton.setFocusable(false);
        actionButton.setMargin(new Insets(8, 10, 8, 10));
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D()));
        versionLabel.setFont(versionLabel.getFont().deriveFont(Math.max(10f, versionLabel.getFont().getSize2D() - 1f)));
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(versionLabel);

        actionButton.add(textPanel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.CENTER);

        actionButton.addActionListener(e -> install());
        actionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showNotesPopup();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                scheduleHideNotesPopup();
            }
        });
        applyAccentStyle();
        applyTexts();

        UpdateDownloadManager.getInstance().addListener(this::refresh);
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName()) || String.valueOf(e.getPropertyName()).contains("accent")) {
                SwingUtilities.invokeLater(this::applyAccentStyle);
            }
        });
    }

    private void refresh(UpdateDownloadManager manager) {
        boolean ready = manager.getStatus() == UpdateDownloadManager.Status.READY
                && manager.getVersion() != null
                && manager.getDownloadedFile() != null
                && manager.getDownloadedFile().exists();
        setVisible(ready);
        if (!ready) {
            hideNotesPopupNow();
        }
        if (ready) {
            applyTexts();
            actionButton.setEnabled(!installing);
        }
        revalidate();
        repaint();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void applyTexts() {
        titleLabel.setText("安装新版本");
        String version = UpdateDownloadManager.getInstance().getVersion();
        if (version != null) {
            versionLabel.setText(version + " 已就绪");
        }
        actionButton.setToolTipText(null);
    }

    private void applyAccentStyle() {
        Color accent = UIManager.getColor("Component.accentColor");
        if (accent == null) {
            accent = UIManager.getColor("Button.default.background");
        }
        if (accent == null) {
            accent = new Color(0x3F6FAE);
        }
        Color contrastColor = UIManager.getColor("Component.accentContrastColor");
        if (contrastColor == null) {
            contrastColor = Color.WHITE;
        }
        final Color contrast = contrastColor;
        actionButton.setBackground(accent);
        actionButton.setForeground(contrast);
        titleLabel.setForeground(contrast);
        versionLabel.setForeground(new Color(contrast.getRed(), contrast.getGreen(), contrast.getBlue(), 200));
        FlatSVGIcon icon = new FlatSVGIcon("icon/down.svg");
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> contrast));
        actionButton.setIcon(icon);
        actionButton.setOpaque(true);
        actionButton.setBorderPainted(false);
        actionButton.setContentAreaFilled(true);
    }

    private void showNotesPopup() {
        cancelHideNotesPopup();
        String html = UpdateDownloadManager.getInstance().getReleaseNotesHtml();
        if (StringUtils.isBlank(html) || !isShowing()) {
            return;
        }
        if (notesPopup != null) {
            return;
        }

        String version = UpdateDownloadManager.getInstance().getVersion();
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null
                        ? UIManager.getColor("Component.borderColor")
                        : new Color(0xD0D0D4), 1),
                new EmptyBorder(0, 0, 0, 0)));
        card.setBackground(UIManager.getColor("Panel.background"));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(10, 12, 8, 12));
        header.setOpaque(true);
        Color headerBg = UIManager.getColor("Panel.background");
        if (UIManager.getColor("PopupMenu.background") != null) {
            headerBg = UIManager.getColor("PopupMenu.background");
        }
        header.setBackground(headerBg);
        JLabel heading = new JLabel("更新内容");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() + 0.5f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(heading);
        if (version != null) {
            header.add(Box.createVerticalStrut(2));
            JLabel ready = new JLabel(version + " 已就绪");
            ready.setForeground(UIManager.getColor("Label.disabledForeground") != null
                    ? UIManager.getColor("Label.disabledForeground")
                    : Color.GRAY);
            ready.setFont(ready.getFont().deriveFont(Math.max(10f, ready.getFont().getSize2D() - 1f)));
            ready.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(ready);
        }
        card.add(header, BorderLayout.NORTH);

        JEditorPane notesPane = new JEditorPane();
        notesPane.setEditable(false);
        notesPane.setOpaque(false);
        notesPane.setBorder(new EmptyBorder(8, 12, 12, 12));
        notesPane.setContentType("text/html; charset=utf-8");
        HTMLEditorKit kit = new HTMLEditorKit();
        notesPane.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        Font font = actionButton.getFont();
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) {
            fg = Color.DARK_GRAY;
        }
        Color muted = UIManager.getColor("Label.disabledForeground");
        if (muted == null) {
            muted = Color.GRAY;
        }
        Color accent = UIManager.getColor("Component.accentColor");
        if (accent == null) {
            accent = new Color(0xC48A3A);
        }
        styleSheet.addRule("body{font-family:" + font.getFamily() + ";font-size:" + Math.max(11, font.getSize() - 1)
                + "pt;margin:0;color:" + toCssColor(fg) + ";}");
        styleSheet.addRule("h2{color:" + toCssColor(accent) + ";font-size:12pt;margin:10px 0 4px 0;}");
        styleSheet.addRule("h2:first-child{margin-top:0;}");
        styleSheet.addRule("p{margin:4px 0;color:" + toCssColor(muted) + ";}");
        styleSheet.addRule("b{color:" + toCssColor(fg) + ";}");
        notesPane.setText(html);
        notesPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(notesPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(360, 280));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, BorderLayout.CENTER);

        MouseAdapter keepOpen = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelHideNotesPopup();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                scheduleHideNotesPopup();
            }
        };
        card.addMouseListener(keepOpen);
        notesPane.addMouseListener(keepOpen);
        scrollPane.addMouseListener(keepOpen);

        Point location = actionButton.getLocationOnScreen();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int popupWidth = 360;
        int popupHeight = 300;
        int x = location.x;
        if (x + popupWidth > screen.width - 16) {
            x = Math.max(16, screen.width - popupWidth - 16);
        }
        int y = location.y - popupHeight - 8;
        y = Math.max(16, Math.min(y, screen.height - popupHeight - 16));
        notesPopup = PopupFactory.getSharedInstance().getPopup(actionButton, card, x, y);
        notesPopup.show();
    }

    private static String toCssColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void scheduleHideNotesPopup() {
        cancelHideNotesPopup();
        hideNotesTimer = new Timer(140, e -> hideNotesPopupNow());
        hideNotesTimer.setRepeats(false);
        hideNotesTimer.start();
    }

    private void cancelHideNotesPopup() {
        if (hideNotesTimer != null) {
            hideNotesTimer.stop();
            hideNotesTimer = null;
        }
    }

    private void hideNotesPopupNow() {
        cancelHideNotesPopup();
        if (notesPopup != null) {
            notesPopup.hide();
            notesPopup = null;
        }
    }

    private void install() {
        if (installing) {
            return;
        }
        installing = true;
        actionButton.setEnabled(false);
        hideNotesPopupNow();
        try {
            UpdateDownloadManager.getInstance().installAndExit();
        } catch (Exception e) {
            log.error("打开更新安装包失败", e);
            installing = false;
            actionButton.setEnabled(true);
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "打开安装包失败：" + e.getMessage(),
                    "失败",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
