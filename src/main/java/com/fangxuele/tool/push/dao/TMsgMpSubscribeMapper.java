package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMpSubscribe;

public interface TMsgMpSubscribeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMpSubscribe record);

    int insertSelective(TMsgMpSubscribe record);

    TMsgMpSubscribe selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMpSubscribe record);

    int updateByPrimaryKey(TMsgMpSubscribe record);
}