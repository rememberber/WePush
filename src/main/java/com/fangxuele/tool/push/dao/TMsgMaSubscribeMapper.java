package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMaSubscribe;

public interface TMsgMaSubscribeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMaSubscribe record);

    int insertSelective(TMsgMaSubscribe record);

    TMsgMaSubscribe selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMaSubscribe record);

    int updateByPrimaryKey(TMsgMaSubscribe record);
}