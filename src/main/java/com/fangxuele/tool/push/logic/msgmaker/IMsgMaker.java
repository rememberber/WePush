package com.fangxuele.tool.push.logic.msgmaker;

public interface IMsgMaker {
    /**
     * 消息加工器接口
     *
     * @param msgData 消息数据
     * @return Object
     */
    Object makeMsg(String[] msgData);
}
