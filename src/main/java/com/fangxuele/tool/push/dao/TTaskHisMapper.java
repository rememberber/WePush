package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTaskHis;

public interface TTaskHisMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTaskHis record);

    int insertSelective(TTaskHis record);

    TTaskHis selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTaskHis record);

    int updateByPrimaryKey(TTaskHis record);
}