package com.fangxuele.tool.push.bean;

import java.io.Serializable;
import java.util.List;

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

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getVersionIndex() {
        return versionIndex;
    }

    public void setVersionIndex(String versionIndex) {
        this.versionIndex = versionIndex;
    }

    public List<Version> getVersionDetailList() {
        return versionDetailList;
    }

    public void setVersionDetailList(List<Version> versionDetailList) {
        this.versionDetailList = versionDetailList;
    }

    /**
     * 版本实体
     */
    public static class Version implements Serializable {

        private static final long serialVersionUID = 4637273116136790268L;

        private String version;

        private String title;

        private String log;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLog() {
            return log;
        }

        public void setLog(String log) {
            this.log = log;
        }
    }

}
