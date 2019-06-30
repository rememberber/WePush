package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgWxCp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgWxCpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgWxCp record);

    int insertSelective(TMsgWxCp record);

    TMsgWxCp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgWxCp record);

    int updateByPrimaryKey(TMsgWxCp record);

    List<TMsgWxCp> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgWxCp tMsgWxCp);

    List<TMsgWxCp> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);
}