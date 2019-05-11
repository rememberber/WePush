package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTemplateData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TTemplateDataMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTemplateData record);

    int insertSelective(TTemplateData record);

    TTemplateData selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTemplateData record);

    int updateByPrimaryKey(TTemplateData record);

    List<TTemplateData> selectByMsgTypeAndMsgId(@Param("msgType") int msgType, @Param("msgId") int msgId);

    int deleteByMsgTypeAndMsgId(@Param("msgType") int msgType, @Param("msgId") int msgId);
}