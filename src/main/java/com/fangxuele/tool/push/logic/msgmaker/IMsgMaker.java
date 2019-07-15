package com.fangxuele.tool.push.logic.msgmaker;

public interface IMsgMaker {
    /**
     * 准备(界面字段等)
     */
    void prepare();

    /**
     * 消息加工器接口
     *
     * @param msgData 消息数据
     * @return Object
     */
    Object makeMsg(String[] msgData);
}
