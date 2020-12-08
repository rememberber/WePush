package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgKefu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgKefuMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgKefu record);

    int insertSelective(TMsgKefu record);

    TMsgKefu selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgKefu record);

    int updateByPrimaryKey(TMsgKefu record);

    List<TMsgKefu> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgKefu tMsgKefu);

    List<TMsgKefu> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    List<TMsgKefu> selectByMsgTypeAndWxAccountId(@Param("msgType") int msgType, @Param("wxAccountId") Integer wxAccountId);
}