package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeople;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TPeopleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeople record);

    int insertSelective(TPeople record);

    TPeople selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeople record);

    int updateByPrimaryKey(TPeople record);

    TPeople selectByMsgTypeAndAccountIdAndName(@Param("msgType") String msgType, @Param("accountId") Integer selectedAccountId, @Param("peopleName") String peopleName);

    List<TPeople> selectByMsgTypeAndAccountId(@Param("msgType") String msgType, @Param("accountId") Integer selectedAccountId);
}