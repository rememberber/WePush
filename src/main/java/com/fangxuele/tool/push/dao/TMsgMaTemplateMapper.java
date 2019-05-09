package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMaTemplate;

public interface TMsgMaTemplateMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMaTemplate record);

    int insertSelective(TMsgMaTemplate record);

    TMsgMaTemplate selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMaTemplate record);

    int updateByPrimaryKey(TMsgMaTemplate record);
}