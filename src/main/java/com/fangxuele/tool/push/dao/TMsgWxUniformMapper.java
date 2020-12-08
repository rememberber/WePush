package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgWxUniform;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgWxUniformMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgWxUniform record);

    int insertSelective(TMsgWxUniform record);

    TMsgWxUniform selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgWxUniform record);

    int updateByPrimaryKey(TMsgWxUniform record);

    List<TMsgWxUniform> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    List<TMsgWxUniform> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgWxUniform tMsgWxUniform);

    List<TMsgWxUniform> selectByMsgTypeAndWxAccountId(@Param("msgType") int msgType, @Param("wxAccountId") Integer wxAccountId);
}