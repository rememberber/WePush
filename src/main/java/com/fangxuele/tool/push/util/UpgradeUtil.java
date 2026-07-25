package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.VersionSummary;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.UpdateInfoDialog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <pre>
 * 更新升级工具类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/5/24.
 */
@Slf4j
public class UpgradeUtil {
    private static final int CHECK_TIMEOUT_MS = 10_000;
    private static final AtomicBoolean MANUAL_CHECKING = new AtomicBoolean(false);

    private static int parseVersionIndex(Map<String, String> versionIndexMap, String version) {
        String index = versionIndexMap.get(version);
        if (StringUtils.isBlank(index)) {
            throw new IllegalStateException("版本索引缺失：" + version);
        }
        return Integer.parseInt(index);
    }

    static List<VersionSummary.Version> versionChangesAfter(VersionSummary versionSummary, String currentVersion) {
        Map<String, String> versionIndexMap = JSON.parseObject(versionSummary.getVersionIndex(), Map.class);
        int currentVersionIndex = parseVersionIndex(versionIndexMap, currentVersion);
        int latestVersionIndex = parseVersionIndex(versionIndexMap, versionSummary.getCurrentVersion());
        if (latestVersionIndex <= currentVersionIndex) {
            return List.of();
        }

        List<VersionSummary.Version> versionDetailList = versionSummary.getVersionDetailList();
        if (versionDetailList == null) {
            throw new IllegalStateException("版本明细列表缺失");
        }
        List<VersionSummary.Version> changes = new ArrayList<>();
        for (VersionSummary.Version version : versionDetailList) {
            int versionIndex = parseVersionIndex(versionIndexMap, version.getVersion());
            if (versionIndex > currentVersionIndex && versionIndex <= latestVersionIndex) {
                changes.add(version);
            }
        }
        if (changes.size() != latestVersionIndex - currentVersionIndex) {
            throw new IllegalStateException("版本更新说明不完整：" + currentVersion + " -> " + versionSummary.getCurrentVersion());
        }
        changes.sort(Comparator.comparingInt(version -> parseVersionIndex(versionIndexMap, version.getVersion())));
        return changes;
    }

    /**
     * 检查更新。网络请求在后台线程执行，提示对话框在 EDT 弹出。
     *
     * @param initCheck true 表示启动后自动检查（已是最新时不打扰）；false 表示用户手动检查（始终给出结果反馈）
     */
    public static void checkUpdate(boolean initCheck) {
        if (!initCheck && !MANUAL_CHECKING.compareAndSet(false, true)) {
            return;
        }
        ThreadUtil.execute(() -> {
            try {
                doCheckUpdate(initCheck);
            } catch (Exception e) {
                log.error("检查更新失败", e);
                if (!initCheck) {
                    showMessage("检查超时，请关注 GitHub Release！", "网络错误");
                }
            } finally {
                if (!initCheck) {
                    MANUAL_CHECKING.set(false);
                }
            }
        });
    }

    private static void doCheckUpdate(boolean initCheck) {
        String currentVersion = UiConsts.APP_VERSION;

        String versionSummaryJsonContent;
        try {
            versionSummaryJsonContent = HttpUtil.get(UiConsts.CHECK_VERSION_URL, CHECK_TIMEOUT_MS);
        } catch (Exception e) {
            log.error("检查更新网络请求失败", e);
            if (!initCheck) {
                showMessage("检查超时，请关注 GitHub Release！", "网络错误");
            }
            return;
        }
        if (StringUtils.isEmpty(versionSummaryJsonContent) || versionSummaryJsonContent.contains("404: Not Found")) {
            if (!initCheck) {
                showMessage("检查超时，请关注 GitHub Release！", "网络错误");
            }
            return;
        }
        versionSummaryJsonContent = versionSummaryJsonContent.replace("\n", "");

        VersionSummary versionSummary = JSON.parseObject(versionSummaryJsonContent, VersionSummary.class);
        String newVersion = versionSummary.getCurrentVersion();
        List<VersionSummary.Version> versionChanges;
        try {
            versionChanges = versionChangesAfter(versionSummary, currentVersion);
        } catch (IllegalStateException | NumberFormatException e) {
            log.error("检查更新时版本配置异常", e);
            if (!initCheck) {
                showMessage("检查更新失败，版本配置异常。", "失败");
            }
            return;
        }

        if (!versionChanges.isEmpty()) {
            // 启动时自动检查且开启静默下载：后台下载安装包，就绪后在主界面底部提示安装
            if (initCheck && App.config.isAutoDownloadUpdate()) {
                UpdateDownloadManager.getInstance().startSilentDownload(
                        newVersion, buildVersionChangesHtml(versionChanges, null));
                return;
            }

            String html = buildVersionChangesHtml(versionChanges, "惊现新版本！立即下载？");
            SwingUtilities.invokeLater(() -> {
                UpdateInfoDialog updateInfoDialog = new UpdateInfoDialog();
                updateInfoDialog.setHtmlText(html);
                updateInfoDialog.setNewVersion(newVersion);
                updateInfoDialog.pack();
                updateInfoDialog.setVisible(true);
            });
        } else if (!initCheck) {
            showMessage("当前已经是最新版本！", "恭喜");
        }
    }

    private static void showMessage(String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(App.mainFrame, message, title, JOptionPane.INFORMATION_MESSAGE));
    }

    static String buildVersionChangesHtml(List<VersionSummary.Version> versionChanges, String title) {
        StringBuilder versionLogBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(title)) {
            versionLogBuilder.append("<h1>").append(title).append("</h1>");
        }
        for (VersionSummary.Version version : versionChanges) {
            versionLogBuilder.append("<h2>").append(version.getVersion()).append("</h2>");
            versionLogBuilder.append("<b>").append(version.getTitle()).append("</b><br/>");
            versionLogBuilder.append("<p>").append(version.getLog().replaceAll("\\n", "</p><p>")).append("</p>");
        }
        return versionLogBuilder.toString();
    }

    /**
     * 平滑升级
     * 涉及的版本更新脚本和sql方法尽量幂等，以免升级过程中由于断电死机等异常中断造成重复执行升级操作
     */
    public static void smoothUpgrade() {
        String currentVersion = UiConsts.APP_VERSION;
        String beforeVersion = App.config.getBeforeVersion();

        String versionSummaryJsonContent = FileUtil.readString(UiConsts.class.getResource("/version_summary.json"), CharsetUtil.UTF_8);
        versionSummaryJsonContent = versionSummaryJsonContent.replace("\n", "");
        VersionSummary versionSummary = JSON.parseObject(versionSummaryJsonContent, VersionSummary.class);
        String versionIndex = versionSummary.getVersionIndex();
        Map<String, String> versionIndexMap = JSON.parseObject(versionIndex, Map.class);
        int currentVersionIndex;
        int beforeVersionIndex;
        try {
            currentVersionIndex = parseVersionIndex(versionIndexMap, currentVersion);
            beforeVersionIndex = parseVersionIndex(versionIndexMap, beforeVersion);
        } catch (IllegalStateException | NumberFormatException e) {
            log.error("平滑升级时版本索引配置异常", e);
            return;
        }

        if (currentVersionIndex <= beforeVersionIndex) {
            return;
        }

        log.info("平滑升级开始");
        try {
            MybatisUtil.initDbFile();
        } catch (Exception e) {
            log.error("执行平滑升级时先执行db_init.sql操作失败", e);
            return;
        }

        log.info("旧版本{}", beforeVersion);
        log.info("当前版本{}", currentVersion);
        beforeVersionIndex++;
        for (int i = beforeVersionIndex; i <= currentVersionIndex; i++) {
            log.info("更新版本索引{}开始", i);
            String sqlFile = "/upgrade/" + i + ".sql";
            URL sqlFileUrl = UiConsts.class.getResource(sqlFile);
            if (sqlFileUrl != null) {
                String sql = FileUtil.readString(sqlFileUrl, CharsetUtil.UTF_8);
                try {
                    MybatisUtil.executeSql(sql);
                    log.info("执行索引为{}的版本对应的sql完毕", i);
                } catch (SQLException e) {
                    log.error("执行索引为{}的版本对应的sql时异常", i, e);
                    if (!e.getMessage().contains("duplicate column") && !e.getMessage().contains("constraint")) {
                        return;
                    }
                }
            }
            upgrade(i);
            log.info("更新版本索引{}结束", i);
        }

        App.config.setBeforeVersion(currentVersion);
        App.config.save();
        log.info("平滑升级结束");
    }

    /**
     * 执行升级脚本
     *
     * @param versionIndex 版本索引
     */
    private static void upgrade(int versionIndex) {
        log.info("执行升级脚本开始，版本索引：{}", versionIndex);
        switch (versionIndex) {
            default:
        }
        log.info("执行升级脚本结束，版本索引：{}", versionIndex);
    }
}
