package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TTaskHis implements Serializable {
    private Integer id;

    private Integer taskId;

    private String startTime;

    private String endTime;

    private Integer totalCnt;

    private Integer successCnt;

    private Integer failCnt;

    private Integer status;

    private Integer dryRun;

    private String successFilePath;

    private String failFilePath;

    private String noSendFilePath;

    private String logFilePath;

    private String remark;

    private String createTime;

    private String modifiedTime;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime == null ? null : startTime.trim();
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime == null ? null : endTime.trim();
    }

    public Integer getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(Integer totalCnt) {
        this.totalCnt = totalCnt;
    }

    public Integer getSuccessCnt() {
        return successCnt;
    }

    public void setSuccessCnt(Integer successCnt) {
        this.successCnt = successCnt;
    }

    public Integer getFailCnt() {
        return failCnt;
    }

    public void setFailCnt(Integer failCnt) {
        this.failCnt = failCnt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDryRun() {
        return dryRun;
    }

    public void setDryRun(Integer dryRun) {
        this.dryRun = dryRun;
    }

    public String getSuccessFilePath() {
        return successFilePath;
    }

    public void setSuccessFilePath(String successFilePath) {
        this.successFilePath = successFilePath == null ? null : successFilePath.trim();
    }

    public String getFailFilePath() {
        return failFilePath;
    }

    public void setFailFilePath(String failFilePath) {
        this.failFilePath = failFilePath == null ? null : failFilePath.trim();
    }

    public String getNoSendFilePath() {
        return noSendFilePath;
    }

    public void setNoSendFilePath(String noSendFilePath) {
        this.noSendFilePath = noSendFilePath == null ? null : noSendFilePath.trim();
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath == null ? null : logFilePath.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime == null ? null : createTime.trim();
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime == null ? null : modifiedTime.trim();
    }
}