package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgHttp;

public interface TMsgHttpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgHttp record);

    int insertSelective(TMsgHttp record);

    TMsgHttp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgHttp record);

    int updateByPrimaryKey(TMsgHttp record);
}