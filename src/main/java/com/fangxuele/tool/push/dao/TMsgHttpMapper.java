package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgHttp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgHttpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgHttp record);

    int insertSelective(TMsgHttp record);

    TMsgHttp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgHttp record);

    int updateByPrimaryKey(TMsgHttp record);

    TMsgHttp selectByUnique(@Param("accountId") Integer selectedAccountId, @Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgHttp tMsgHttp);

    List<TMsgHttp> selectByMsgType(int msgType);

    int deleteByMsgTypeAndName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    List<TMsgHttp> selectByMsgTypeAndAccountId(@Param("msgType") int msgType, @Param("accountId") Integer selectedAccountId);
}