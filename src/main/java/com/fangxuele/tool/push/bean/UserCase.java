package com.fangxuele.tool.push.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * <pre>
 * 用户案例
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/4/20.
 */
@Data
public class UserCase implements Serializable {

    private static final long serialVersionUID = 2829237163275443844L;

    private String qrCodeUrl;

    private String title;

    private String desc;

}
