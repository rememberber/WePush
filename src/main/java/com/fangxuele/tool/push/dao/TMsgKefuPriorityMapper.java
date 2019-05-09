package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgKefuPriority;

public interface TMsgKefuPriorityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgKefuPriority record);

    int insertSelective(TMsgKefuPriority record);

    TMsgKefuPriority selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgKefuPriority record);

    int updateByPrimaryKey(TMsgKefuPriority record);
}