package com.fangxuele.tool.push.bean;

import lombok.Data;

import java.util.Map;

/**
 * 极光推送消息（经模板引擎求值后）
 */
@Data
public class JiguangPushMsg {
    private String title;

    private String content;

    private Map<String, String> extras;
}
