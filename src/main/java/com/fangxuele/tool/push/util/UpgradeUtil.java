package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.VersionSummary;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
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
    /**
     * 平滑升级
     * 涉及的版本更新脚本和sql方法尽量幂等，以免升级过程中由于断电死机等异常中断造成重复执行升级操作
     */
    public static void smoothUpgrade() {
        // 取得当前版本
        String currentVersion = UiConsts.APP_VERSION;
        // 取得升级前版本
        String beforeVersion = Init.config.getBeforeVersion();
        if (currentVersion.equals(beforeVersion)) {
            // 如果两者一致则不执行任何升级操作
            return;
        } else {
            // 否则先执行db_init.sql更新数据库新增表
            try {
                MybatisUtil.initDbFile();
            } catch (Exception e) {
                log.error("执行平滑升级时先执行db_init.sql操作失败", e);
            }
            // 然后取两个版本对应的索引
            String versionSummaryJsonContent = FileUtil.readString(UiConsts.class.getResource("version_summary.json"), CharsetUtil.UTF_8);
            versionSummaryJsonContent = versionSummaryJsonContent.replace("\n", "");
            VersionSummary versionSummary = JSON.parseObject(versionSummaryJsonContent, VersionSummary.class);
            String versionIndex = versionSummary.getVersionIndex();
            Map<String, String> versionIndexMap = JSON.parseObject(versionIndex, Map.class);
            int currentVersionIndex = Integer.parseInt(versionIndexMap.get(currentVersion));
            int beforeVersionIndex = Integer.parseInt(versionIndexMap.get(beforeVersion));
            // 遍历索引范围
            beforeVersionIndex++;
            for (int i = beforeVersionIndex; i <= currentVersionIndex; i++) {
                // 执行每个版本索引的更新内容，按时间由远到近
                // 取得resources:upgrade下对应版本的sql，如存在，则先执行sql进行表结构或者数据更新等操作
                String sqlFile = "upgrade" + File.separator + i + ".sql";
                URL sqlFileUrl = UiConsts.class.getResource(sqlFile);
                if (sqlFileUrl != null) {
                    String sql = FileUtil.readString(sqlFileUrl, CharsetUtil.UTF_8);
                    try {
                        MybatisUtil.executeSql(sql);
                    } catch (SQLException e) {
                        log.error("执行索引为{}的版本对应的sql时异常", i, e);
                        return;
                    }
                }
                upgrade(i);
            }
            // 升级完毕且成功，则赋值升级前版本号为当前版本
            Init.config.setBeforeVersion(currentVersion);
        }
    }

    /**
     * 执行升级脚本
     *
     * @param versionIndex 版本索引
     */
    private static void upgrade(int versionIndex) {
        switch (versionIndex) {
            case 21:
                break;
            case 22:
                break;
            default:
        }
    }
}
