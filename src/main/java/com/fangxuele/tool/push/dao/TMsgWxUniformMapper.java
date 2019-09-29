package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgWxUniform;

public interface TMsgWxUniformMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgWxUniform record);

    int insertSelective(TMsgWxUniform record);

    TMsgWxUniform selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgWxUniform record);

    int updateByPrimaryKey(TMsgWxUniform record);
}