package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.VersionSummary;
import com.fangxuele.tool.push.dao.TWxAccountMapper;
import com.fangxuele.tool.push.domain.TWxAccount;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.UpdateInfoDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.SettingForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

    public static void checkUpdate(boolean initCheck) {
        // 当前版本
        String currentVersion = UiConsts.APP_VERSION;

        // 从github获取最新版本相关信息
        String versionSummaryJsonContent = HttpUtil.get(UiConsts.CHECK_VERSION_URL);
        if (StringUtils.isEmpty(versionSummaryJsonContent) && !initCheck) {
            JOptionPane.showMessageDialog(MainWindow.getInstance().getSettingPanel(),
                    "检查超时，请关注GitHub Release！", "网络错误",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        versionSummaryJsonContent = versionSummaryJsonContent.replace("\n", "");

        VersionSummary versionSummary = JSON.parseObject(versionSummaryJsonContent, VersionSummary.class);
        // 最新版本
        String newVersion = versionSummary.getCurrentVersion();
        String versionIndexJsonContent = versionSummary.getVersionIndex();
        // 版本索引
        Map<String, String> versionIndexMap = JSON.parseObject(versionIndexJsonContent, Map.class);
        // 版本明细列表
        List<VersionSummary.Version> versionDetailList = versionSummary.getVersionDetailList();

        if (newVersion.compareTo(currentVersion) > 0) {
            // 当前版本索引
            int currentVersionIndex = Integer.parseInt(versionIndexMap.get(currentVersion));
            // 版本更新日志：
            StringBuilder versionLogBuilder = new StringBuilder("<h1>惊现新版本！立即下载？</h1>");
            VersionSummary.Version version;
            for (int i = currentVersionIndex + 1; i < versionDetailList.size(); i++) {
                version = versionDetailList.get(i);
                versionLogBuilder.append("<h2>").append(version.getVersion()).append("</h2>");
                versionLogBuilder.append("<b>").append(version.getTitle()).append("</b><br/>");
                versionLogBuilder.append("<p>").append(version.getLog().replaceAll("\\n", "</p><p>")).append("</p>");
            }
            String versionLog = versionLogBuilder.toString();

            UpdateInfoDialog updateInfoDialog = new UpdateInfoDialog();
            updateInfoDialog.setHtmlText(versionLog);
            updateInfoDialog.setNewVersion(newVersion);
            updateInfoDialog.pack();
            updateInfoDialog.setVisible(true);
        } else {
            if (!initCheck) {
                JOptionPane.showMessageDialog(MainWindow.getInstance().getSettingPanel(),
                        "当前已经是最新版本！", "恭喜",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * 平滑升级
     * 涉及的版本更新脚本和sql方法尽量幂等，以免升级过程中由于断电死机等异常中断造成重复执行升级操作
     */
    public static void smoothUpgrade() {
        // 取得当前版本
        String currentVersion = UiConsts.APP_VERSION;
        // 取得升级前版本
        String beforeVersion = App.config.getBeforeVersion();

        if (currentVersion.compareTo(beforeVersion) <= 0) {
            // 如果两者一致则不执行任何升级操作
            return;
        } else {
            log.info("平滑升级开始");
            // 否则先执行db_init.sql更新数据库新增表
            try {
                MybatisUtil.initDbFile();
            } catch (Exception e) {
                log.error("执行平滑升级时先执行db_init.sql操作失败", e);
                return;
            }

            // 然后取两个版本对应的索引
            String versionSummaryJsonContent = FileUtil.readString(UiConsts.class.getResource("/version_summary.json"), CharsetUtil.UTF_8);
            versionSummaryJsonContent = versionSummaryJsonContent.replace("\n", "");
            VersionSummary versionSummary = JSON.parseObject(versionSummaryJsonContent, VersionSummary.class);
            String versionIndex = versionSummary.getVersionIndex();
            Map<String, String> versionIndexMap = JSON.parseObject(versionIndex, Map.class);
            int currentVersionIndex = Integer.parseInt(versionIndexMap.get(currentVersion));
            int beforeVersionIndex = Integer.parseInt(versionIndexMap.get(beforeVersion));
            log.info("旧版本{}", beforeVersion);
            log.info("当前版本{}", currentVersion);
            // 遍历索引范围
            beforeVersionIndex++;
            for (int i = beforeVersionIndex; i <= currentVersionIndex; i++) {
                log.info("更新版本索引{}开始", i);
                // 执行每个版本索引的更新内容，按时间由远到近
                // 取得resources:upgrade下对应版本的sql，如存在，则先执行sql进行表结构或者数据更新等操作
                String sqlFile = "/upgrade/" + i + ".sql";
                URL sqlFileUrl = UiConsts.class.getResource(sqlFile);
                if (sqlFileUrl != null) {
                    String sql = FileUtil.readString(sqlFileUrl, CharsetUtil.UTF_8);
                    try {
                        MybatisUtil.executeSql(sql);
                        log.info("执行索引为{}的版本对应的sql完毕", i);
                    } catch (SQLException e) {
                        log.error("执行索引为{}的版本对应的sql时异常", i, e);
                        if (!e.getMessage().contains("duplicate column")) {
                            return;
                        }
                    }
                }
                upgrade(i);
                log.info("更新版本索引{}结束", i);
            }

            // 升级完毕且成功，则赋值升级前版本号为当前版本
            App.config.setBeforeVersion(currentVersion);
            App.config.save();
            log.info("平滑升级结束");
        }
    }

    /**
     * 执行升级脚本
     *
     * @param versionIndex 版本索引
     */
    private static void upgrade(int versionIndex) {
        log.info("执行升级脚本开始，版本索引：{}", versionIndex);
        switch (versionIndex) {
            case 21:
                String accountName = "默认账号";
                TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);
                if (StringUtils.isNotBlank(App.config.getWechatAppId())) {
                    boolean update = false;
                    List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(UiConsts.WX_ACCOUNT_TYPE_MP, accountName);
                    if (tWxAccountList.size() > 0) {
                        update = true;
                    }

                    TWxAccount tWxAccount = new TWxAccount();
                    String now = SqliteUtil.nowDateForSqlite();
                    tWxAccount.setAccountType(UiConsts.WX_ACCOUNT_TYPE_MP);
                    tWxAccount.setAccountName(accountName);
                    tWxAccount.setAppId(App.config.getWechatAppId());
                    tWxAccount.setAppSecret(App.config.getWechatAppSecret());
                    tWxAccount.setToken(App.config.getWechatToken());
                    tWxAccount.setAesKey(App.config.getWechatAesKey());
                    tWxAccount.setModifiedTime(now);
                    if (update) {
                        tWxAccount.setId(tWxAccountList.get(0).getId());
                        wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                    } else {
                        tWxAccount.setCreateTime(now);
                        wxAccountMapper.insert(tWxAccount);
                    }

                    SettingForm.initSwitchMultiAccount();
                }
                if (StringUtils.isNotBlank(App.config.getMiniAppAppId())) {
                    boolean update = false;
                    List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(UiConsts.WX_ACCOUNT_TYPE_MA, accountName);
                    if (tWxAccountList.size() > 0) {
                        update = true;
                    }

                    TWxAccount tWxAccount = new TWxAccount();
                    String now = SqliteUtil.nowDateForSqlite();
                    tWxAccount.setAccountType(UiConsts.WX_ACCOUNT_TYPE_MA);
                    tWxAccount.setAccountName(accountName);
                    tWxAccount.setAppId(App.config.getMiniAppAppId());
                    tWxAccount.setAppSecret(App.config.getMiniAppAppSecret());
                    tWxAccount.setToken(App.config.getMiniAppToken());
                    tWxAccount.setAesKey(App.config.getMiniAppAesKey());
                    tWxAccount.setModifiedTime(now);
                    if (update) {
                        tWxAccount.setId(tWxAccountList.get(0).getId());
                        wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                    } else {
                        tWxAccount.setCreateTime(now);
                        wxAccountMapper.insert(tWxAccount);
                    }
                    SettingForm.initSwitchMultiAccount();
                }
                break;
            case 26:
                if (StringUtils.isNotBlank(App.config.getMysqlDatabase())) {
                    App.config.setMysqlUrl(App.config.getMysqlUrl() + '/' + App.config.getMysqlDatabase());
                    App.config.save();
                }
                break;
            case 46:
            case 47:
            case 48:
                if(SystemUtil.isJBR()){
                    App.config.setProps(Init.FONT_SIZE_INIT_PROP,"");
                }
            default:
        }
        log.info("执行升级脚本结束，版本索引：{}", versionIndex);
    }
}
