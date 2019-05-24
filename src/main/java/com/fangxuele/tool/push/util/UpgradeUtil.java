package com.fangxuele.tool.push.util;

/**
 * <pre>
 * 更新升级工具类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/5/24.
 */
public class UpgradeUtil {

    /**
     * 平滑升级
     */
    public static void smoothUpgrade() {
        // 取得当前版本
        // 取得升级前版本
        // 如果两者一致则不执行任何升级操作
        // 否则先执行db_init.sql更新数据库新增表
        // 然后取两个版本对应的索引
        // 遍历索引范围
        // 执行每个版本索引的更新内容，按时间由远到近
        // 取得resources:upgrade下对应版本的sql，如存在，则执行sql进行表结构或者数据更新等操作
        // 升级完毕且成功，则赋值升级前版本号为当前版本
    }
}
