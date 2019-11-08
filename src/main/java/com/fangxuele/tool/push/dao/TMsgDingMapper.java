package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgDing;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgDingMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgDing record);

    int insertSelective(TMsgDing record);

    TMsgDing selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgDing record);

    int updateByPrimaryKey(TMsgDing record);

    List<TMsgDing> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgDing tMsgDing);

    List<TMsgDing> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);
}