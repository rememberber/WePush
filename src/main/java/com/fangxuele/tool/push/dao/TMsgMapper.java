package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsg;

public interface TMsgMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsg record);

    int insertSelective(TMsg record);

    TMsg selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsg record);

    int updateByPrimaryKey(TMsg record);
}