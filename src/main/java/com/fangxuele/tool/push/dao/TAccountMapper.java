package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsgKefu;

import java.util.List;

public interface TAccountMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TAccount record);

    int insertSelective(TAccount record);

    TAccount selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TAccount record);

    int updateByPrimaryKey(TAccount record);

    List<TAccount> selectByMsgType(int msgType);
}