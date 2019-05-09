package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgKefu;

public interface TMsgKefuMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgKefu record);

    int insertSelective(TMsgKefu record);

    TMsgKefu selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgKefu record);

    int updateByPrimaryKey(TMsgKefu record);
}