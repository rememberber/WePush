package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMpTemplate;

public interface TMsgMpTemplateMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMpTemplate record);

    int insertSelective(TMsgMpTemplate record);

    TMsgMpTemplate selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMpTemplate record);

    int updateByPrimaryKey(TMsgMpTemplate record);
}