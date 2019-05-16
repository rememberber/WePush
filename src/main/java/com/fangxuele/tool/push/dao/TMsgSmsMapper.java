package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgSms;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgSmsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgSms record);

    int insertSelective(TMsgSms record);

    TMsgSms selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgSms record);

    int updateByPrimaryKey(TMsgSms record);

    List<TMsgSms> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgSms tMsgSms);

    List<TMsgSms> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);
}