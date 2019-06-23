package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMail;

public interface TMsgMailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMail record);

    int insertSelective(TMsgMail record);

    TMsgMail selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMail record);

    int updateByPrimaryKey(TMsgMail record);
}