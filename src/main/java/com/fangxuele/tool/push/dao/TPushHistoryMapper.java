package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPushHistory;

public interface TPushHistoryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPushHistory record);

    int insertSelective(TPushHistory record);

    TPushHistory selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPushHistory record);

    int updateByPrimaryKey(TPushHistory record);
}