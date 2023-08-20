package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import org.apache.ibatis.annotations.Param;

public interface TPeopleImportConfigMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeopleImportConfig record);

    int insertSelective(TPeopleImportConfig record);

    TPeopleImportConfig selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeopleImportConfig record);

    int updateByPrimaryKey(TPeopleImportConfig record);

    TPeopleImportConfig selectByPeopleId(@Param("peopleId") Integer selectedPeopleId);
}