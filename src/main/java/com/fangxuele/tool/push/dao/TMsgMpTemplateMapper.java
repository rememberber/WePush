package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TMsgMpTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgMpTemplateMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgMpTemplate record);

    int insertSelective(TMsgMpTemplate record);

    TMsgMpTemplate selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgMpTemplate record);

    int updateByPrimaryKey(TMsgMpTemplate record);

    List<TMsgMpTemplate> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgMpTemplate tMsgMpTemplate);
}