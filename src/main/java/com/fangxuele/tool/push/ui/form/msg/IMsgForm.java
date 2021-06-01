package com.fangxuele.tool.push.ui.form.msg;

/**
 * <pre>
 * 消息编辑form界面接口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/15.
 */
public interface IMsgForm {
    /**
     * 初始化界面
     *
     * @param msgId
     */
    void init(Integer msgId);

    /**
     * 保存消息
     *
     * @param msgName
     */
    void save(Integer accountId, String msgName);

    void clearAllField();
}
