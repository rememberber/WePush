package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgDing;

public interface TMsgDingMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgDing record);

    int insertSelective(TMsgDing record);

    TMsgDing selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgDing record);

    int updateByPrimaryKey(TMsgDing record);
}