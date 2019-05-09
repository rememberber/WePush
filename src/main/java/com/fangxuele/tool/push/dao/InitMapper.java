package com.fangxuele.tool.push.dao;

/**
 * <pre>
 * 初始化数据库
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/9.
 */
public interface InitMapper {
    /**
     * 初始化创建所有表
     *
     * @return
     */
    int createAllTables();
}
