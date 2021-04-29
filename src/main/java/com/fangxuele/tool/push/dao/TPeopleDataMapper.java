package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeopleData;

import java.util.List;

public interface TPeopleDataMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeopleData record);

    int insertSelective(TPeopleData record);

    TPeopleData selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeopleData record);

    int updateByPrimaryKey(TPeopleData record);

    List<TPeopleData> selectByPeopleId(Integer peopleId);

    Long countByPeopleId(Integer peopleId);
}