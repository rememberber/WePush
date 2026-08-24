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

    /** HTTP 状态码；非 HTTP 发送器可为空。 */
    private Integer httpStatus;

    /** 服务端 Retry-After 换算后的毫秒数；未返回时为空。 */
    private Long retryAfterMillis;
}
