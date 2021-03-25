package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeople;

public interface TPeopleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeople record);

    int insertSelective(TPeople record);

    TPeople selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeople record);

    int updateByPrimaryKey(TPeople record);
}