package com.fangxuele.tool.push.bean;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <pre>
 * 模板数据
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/13.
 */
@Getter
@Setter
public class TemplateData implements Serializable {

    private String name;

    private String value;

    private String color;
}
