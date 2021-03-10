package com.fangxuele.tool.push.ui.form.account;

/**
 * <pre>
 * 账号编辑form界面接口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/3/10.
 */
public interface IAccountForm {
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
}
