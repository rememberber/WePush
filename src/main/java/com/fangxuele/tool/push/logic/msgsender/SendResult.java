package com.fangxuele.tool.push.logic.msgsender;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <pre>
 * 发送结果
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Getter
@Setter
@ToString
public class SendResult {
    private boolean success = false;

    private String info;
}
