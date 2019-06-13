package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import me.chanjar.weixin.mp.api.WxMpService;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * <pre>
 * 消息发送服务线程父类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/3/29.
 */
public class BaseMsgServiceThread extends Thread {

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
    public int currentThreadSuccessCount;

    /**
     * 当前线程失败数
     */
    public int currentThreadFailCount;

    /**
     * 线程列表tableModel
     */
    public DefaultTableModel tableModel;

    /**
     * 当前线程所在的线程列表行
     */
    public int tableRow;

    /**
     * 构造函数
     *
     * @param start 起始页
     * @param end   截止页
     */
    public BaseMsgServiceThread(int start, int end) {
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
                endIndex + "条的数据");

        list = PushData.toSendList.subList(startIndex, endIndex);

        // 初始化线程列表行
        tableModel = (DefaultTableModel) PushForm.pushForm.getPushThreadTable().getModel();
        currentThreadSuccessCount = 0;
        currentThreadFailCount = 0;
        tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);
        tableModel.setValueAt(currentThreadFailCount, tableRow, 3);
        tableModel.setValueAt(list.size(), tableRow, 4);
        tableModel.setValueAt(0, tableRow, 5);
    }

    /**
     * 当前线程结束
     */
    public void currentThreadFinish() {
        ConsoleUtil.consoleWithLog(this.getName() + "已处理完第" + startIndex + "-" +
                endIndex + "条的数据");

        PushData.increaseStopedThread();
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
