package com.fangxuele.tool.push.ui.dialog.importway.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxCpImportConfig implements Serializable {

    private static final long serialVersionUID = 870694763462632807L;

    /**
     * 1:全部，2:按标签，3:按部门
     */
    private Integer userType;

    /**
     * 标签id
     */
    private String tagId;

    /**
     * 部门id
     */
    private Long depId;
}
