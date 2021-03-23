package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TAccountMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TAccount record);

    int insertSelective(TAccount record);

    TAccount selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TAccount record);

    int updateByPrimaryKey(TAccount record);

    List<TAccount> selectByMsgType(int msgType);

    int deleteByMsgTypeAndAccountName(@Param("msgType") int msgType, @Param("accountName") String accountName);

    TAccount selectByMsgTypeAndAccountName(@Param("msgType") int msgType, @Param("accountName") String accountName);

    int updateByMsgTypeAndAccountName(TAccount tAccount1);
}