package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TPeopleData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TPeopleDataMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPeopleData record);

    int insertSelective(TPeopleData record);

    TPeopleData selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPeopleData record);

    int updateByPrimaryKey(TPeopleData record);

    List<TPeopleData> selectByPeopleIdLimit20(Integer peopleId);

    Long countByPeopleId(Integer peopleId);

    int deleteByPeopleId(Integer peopleId);

    List<TPeopleData> selectByPeopleId(Integer peopleId);

    List<TPeopleData> selectByPeopleIdAndKeyword(@Param("peopleId") Integer peopleId, @Param("keyWord") String keyWord);
}