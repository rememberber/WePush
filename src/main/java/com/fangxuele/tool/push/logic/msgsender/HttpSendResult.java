package com.fangxuele.tool.push.logic.msgsender;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <pre>
 * HttpSendResult
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/19.
 */
@Getter
@Setter
@ToString
public class HttpSendResult extends SendResult {
    private String headers;

    private String body;

    private String cookies;
}
