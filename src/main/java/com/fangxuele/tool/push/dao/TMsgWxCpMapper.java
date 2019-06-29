package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgWxCp;

public interface TMsgWxCpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgWxCp record);

    int insertSelective(TMsgWxCp record);

    TMsgWxCp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgWxCp record);

    int updateByPrimaryKey(TMsgWxCp record);
}