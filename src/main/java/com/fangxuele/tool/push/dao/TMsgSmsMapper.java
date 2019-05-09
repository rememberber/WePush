package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgSms;

public interface TMsgSmsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgSms record);

    int insertSelective(TMsgSms record);

    TMsgSms selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgSms record);

    int updateByPrimaryKey(TMsgSms record);
}