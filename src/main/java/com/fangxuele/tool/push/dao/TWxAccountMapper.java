package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TWxAccount;

public interface TWxAccountMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TWxAccount record);

    int insertSelective(TWxAccount record);

    TWxAccount selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TWxAccount record);

    int updateByPrimaryKey(TWxAccount record);
}