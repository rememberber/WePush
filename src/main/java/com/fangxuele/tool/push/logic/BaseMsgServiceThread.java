package com.fangxuele.tool.push.logic;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.MainWindow;
import me.chanjar.weixin.mp.api.WxMpService;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * 消息发送服务线程父类
 * Created by rememberber(https://github.com/rememberber) on 2017/3/29.
 */
public class BaseMsgServiceThread extends Thread {

    public static final Log logger = LogFactory.get();

    /**
     * 起始页号
     */
    private int pageFrom;

    /**
     * 截至页号
     */
    private int pageTo;

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
     * @param pageFrom 起始页
     * @param pageTo   截止页
     * @param pageSize 页大小
     */
    public BaseMsgServiceThread(int pageFrom, int pageTo, int pageSize) {
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.pageSize = pageSize;
    }

    @Override
    public void run() {

    }

    /**
     * 初始化当前线程
     */
    public void initCurrentThread() {
        PushManage.console(new StringBuffer().append("线程").append(this.getName()).append("负责处理:").append(pageFrom).append("-")
                .append(pageTo).append("页的数据").toString());

        int end = pageTo * pageSize + pageSize;
        if (PushData.totalRecords < end) {
            end = (int) PushData.totalRecords;
        }

        int start = pageFrom * pageSize;

        list = PushData.toSendList.subList(start, end);

        // 初始化线程列表行
        tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushThreadTable().getModel();
        tableRow = Integer.parseInt(Thread.currentThread().getName().substring(2));
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
        PushManage.console(new StringBuffer().append(this.getName()).append("已处理完第").append(pageFrom).append("-")
                .append(pageTo).append("页的数据").toString());

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

    public int getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(int pageFrom) {
        this.pageFrom = pageFrom;
    }

    public int getPageTo() {
        return pageTo;
    }

    public void setPageTo(int pageTo) {
        this.pageTo = pageTo;
    }
}
