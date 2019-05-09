package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTemplateData;

public interface TTemplateDataMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTemplateData record);

    int insertSelective(TTemplateData record);

    TTemplateData selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTemplateData record);

    int updateByPrimaryKey(TTemplateData record);
}