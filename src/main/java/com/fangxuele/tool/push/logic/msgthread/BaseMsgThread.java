package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import me.chanjar.weixin.mp.api.WxMpService;

import javax.swing.*;
import java.util.List;

/**
 * <pre>
 * 消息发送服务线程父类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/3/29.
 */
public class BaseMsgThread extends Thread {

    public static final Log logger = LogFactory.get();

    /**
     * 起始索引
     */
    private int startIndex;

    /**
     * 截止索引
     */
    private int endIndex;

    /**
     * 页大小
     */
    private int pageSize;

    /**
     * 当前线程要发送的list
     */
    public List<String[]> list;

    /**
     * 微信工具服务
     */
    public WxMpService wxMpService;

    /**
     * 当前线程成功数
     */
    public final ThreadLocal<Integer> currentThreadSuccessCount = ThreadLocal.withInitial(() -> 0);

    /**
     * 当前线程失败数
     */
    public final ThreadLocal<Integer> currentThreadFailCount = ThreadLocal.withInitial(() -> 0);

    /**
     * 线程列表table
     */
    public JTable pushThreadTable;

    /**
     * 当前线程所在的线程列表行
     */
    public int tableRow;

    public static int msgType;

    /**
     * 构造函数
     *
     * @param start 起始页
     * @param end   截止页
     */
    public BaseMsgThread(int start, int end) {
        this.startIndex = start;
        this.endIndex = end;
    }

    @Override
    public void run() {

    }

    /**
     * 初始化当前线程
     */
    public void initCurrentThread() {
        ConsoleUtil.consoleWithLog("线程" + this.getName() + "负责处理第:" + startIndex + "-" +
                endIndex + "条数据");

        list = PushData.toSendList.subList(startIndex, endIndex);

        // 初始化线程列表行
        pushThreadTable = PushForm.getInstance().getPushThreadTable();
        currentThreadSuccessCount.set(0);
        currentThreadFailCount.set(0);
        pushThreadTable.setValueAt(currentThreadSuccessCount.get(), tableRow, 2);
        pushThreadTable.setValueAt(currentThreadFailCount.get(), tableRow, 3);
        pushThreadTable.setValueAt(list.size(), tableRow, 4);
        pushThreadTable.setValueAt(0, tableRow, 5);
    }

    /**
     * 当前线程结束
     */
    public void currentThreadFinish() {
        ConsoleUtil.consoleWithLog(this.getName() + "已处理完第" + startIndex + "-" +
                endIndex + "条的数据");

        PushData.increaseStoppedThread();
    }

    public WxMpService getWxMpService() {
        return wxMpService;
    }

    public void setWxMpService(WxMpService wxMpService) {
        this.wxMpService = wxMpService;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getTableRow() {
        return tableRow;
    }

    public void setTableRow(int tableRow) {
        this.tableRow = tableRow;
    }
}
