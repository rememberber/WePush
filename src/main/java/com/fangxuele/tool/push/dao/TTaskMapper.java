package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTask;

public interface TTaskMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTask record);

    int insertSelective(TTask record);

    TTask selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTask record);

    int updateByPrimaryKey(TTask record);
}