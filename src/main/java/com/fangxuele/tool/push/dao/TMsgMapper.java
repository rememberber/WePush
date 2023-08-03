package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsg;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsg record);

    int insertSelective(TMsg record);

    TMsg selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsg record);

    int updateByPrimaryKey(TMsg record);

    TMsg selectByUnique(@Param("msgType") int msgType, @Param("accountId") Integer accountId, @Param("msgName") String msgName);

    List<TMsg> selectByMsgTypeAndAccountId(@Param("msgType") int msgType, @Param("accountId") Integer accountId);
}