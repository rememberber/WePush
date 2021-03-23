package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMpSubscribe;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgMpSubscribeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMpSubscribe record);

    int insertSelective(TMsgMpSubscribe record);

    TMsgMpSubscribe selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMpSubscribe record);

    int updateByPrimaryKey(TMsgMpSubscribe record);

    List<TMsgMpSubscribe> selectByMsgTypeAndWxAccountId(@Param("msgType") int msgType, @Param("wxAccountId") Integer wxAccountId);

    int deleteByMsgTypeAndName(int msgType, String msgName);

    List<TMsgMpSubscribe> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgMpSubscribe tMsgMpSubscribe);
}