package com.fangxuele.tool.push.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <pre>
 * 版本概要
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/4/20.
 */
@Data
public class VersionSummary implements Serializable {

    private static final long serialVersionUID = 4637273116136790267L;

    /**
     * 当前版本
     */
    private String currentVersion;

    /**
     * 版本索引
     */
    private String versionIndex;

    /**
     * 历史版本列表
     */
    private List<Version> versionDetailList;

    /**
     * <pre>
     * 版本类
     * </pre>
     *
     * @author <a href="https://github.com/rememberber">RememBerBer</a>
     * @since 2019/4/20.
     */
    @Data
    public static class Version implements Serializable {

        private static final long serialVersionUID = 4637273116136790268L;

        private String version;

        private String title;

        private String log;

    }

}
