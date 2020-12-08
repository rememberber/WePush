package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgKefuPriorityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgKefuPriority record);

    int insertSelective(TMsgKefuPriority record);

    TMsgKefuPriority selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgKefuPriority record);

    int updateByPrimaryKey(TMsgKefuPriority record);

    List<TMsgKefuPriority> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgKefuPriority tMsgKefuPriority);

    List<TMsgKefuPriority> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    List<TMsgKefuPriority> selectByMsgTypeAndWxAccountId(@Param("msgType") int msgType, @Param("wxAccountId") Integer wxAccountId);
}