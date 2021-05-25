package com.fangxuele.tool.push.dao;

import com.fangxuele.tool.push.domain.TTask;

public interface TTaskExtMapper {

    TTask selectByTitle(String title);
}