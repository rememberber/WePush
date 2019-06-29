package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgMailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMail record);

    int insertSelective(TMsgMail record);

    TMsgMail selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMail record);

    int updateByPrimaryKey(TMsgMail record);

    List<TMsgMail> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgMail tMsgMail);

    List<TMsgMail> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);
}