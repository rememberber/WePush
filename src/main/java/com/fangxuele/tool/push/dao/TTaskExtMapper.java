package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTask;

import java.util.List;

public interface TTaskExtMapper {

    TTask selectByTitle(String title);

    List<TTask> selectAll();
}