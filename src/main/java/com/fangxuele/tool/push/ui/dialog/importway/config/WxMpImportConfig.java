package com.fangxuele.tool.push.ui.dialog.importway.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class WxMpImportConfig implements Serializable {

    private static final long serialVersionUID = 870694763462632807L;

    /**
     * 1:全部，2：标签取并集，3:标签取交集
     */
    private Integer userType;

    /**
     * 标签id
     */
    private Long tagId;
}
