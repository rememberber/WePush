package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TWxCpApp;

import java.util.List;

public interface TWxCpAppMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TWxCpApp record);

    int insertSelective(TWxCpApp record);

    TWxCpApp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TWxCpApp record);

    int updateByPrimaryKey(TWxCpApp record);

    List<TWxCpApp> selectByAgentId(String agentId);

    List<TWxCpApp> selectAll();

    List<TWxCpApp> selectByAppName(String appName);
}