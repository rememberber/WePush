package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeopleImportConfig;

public interface TPeopleImportConfigMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeopleImportConfig record);

    int insertSelective(TPeopleImportConfig record);

    TPeopleImportConfig selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeopleImportConfig record);

    int updateByPrimaryKey(TPeopleImportConfig record);
}