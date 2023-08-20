package com.fangxuele.tool.push.ui.form.account;

import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.util.MybatisUtil;

import javax.swing.*;

/**
 * <pre>
 * 账号编辑form界面接口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/3/10.
 */
public interface IAccountForm {

    TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    /**
     * 初始化界面
     *
     * @param accountName
     */
    void init(String accountName);

    /**
     * 保存
     *
     * @param accountName
     */
    void save(String accountName);

    /**
     * 清空表单
     */
    void clear();

    /**
     * 获取主面板
     *
     * @return
     */
    JPanel getMainPanel();

}
