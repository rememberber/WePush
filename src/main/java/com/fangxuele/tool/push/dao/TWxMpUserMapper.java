package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TWxMpUser;

public interface TWxMpUserMapper {
    int deleteByPrimaryKey(String openId);

    int insert(TWxMpUser record);

    int insertSelective(TWxMpUser record);

    TWxMpUser selectByPrimaryKey(String openId);

    int updateByPrimaryKeySelective(TWxMpUser record);

    int updateByPrimaryKey(TWxMpUser record);

    int deleteAll();
}