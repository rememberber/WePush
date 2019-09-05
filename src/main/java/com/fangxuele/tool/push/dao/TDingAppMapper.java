package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TDingApp;

import java.util.List;

public interface TDingAppMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TDingApp record);

    int insertSelective(TDingApp record);

    TDingApp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDingApp record);

    int updateByPrimaryKey(TDingApp record);

    List<TDingApp> selectByAppName(String appName);

    List<TDingApp> selectAll();

    TDingApp selectByAgentId(String agentId);
}