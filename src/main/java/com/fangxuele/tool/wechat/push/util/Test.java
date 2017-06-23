package com.fangxuele.tool.wechat.push.util;

/**
 * Created by zhouy on 2017/6/14.
 */
public class Test {
    public static void main(String[] args) {
        Config configer = Config.getInstance();
        System.out.println(configer.getMsgName());
        configer.setMsgName("摇一摇广告3");
        System.out.println(configer.getMsgName());
        System.out.println(configer.getMemberFilePath());
        configer.setMemberFilePath("");
        configer.save();
    }
}
