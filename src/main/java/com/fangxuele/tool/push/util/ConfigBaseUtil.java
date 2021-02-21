package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.setting.Setting;

import java.io.File;

/**
 * <pre>
 * 配置管理基类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/14.
 */
public class ConfigBaseUtil {
    /**
     * 设置文件路径
     */
    private String settingFilePath = SystemUtil.CONFIG_HOME + "config" + File.separator + "config.setting";

    Setting setting;

    ConfigBaseUtil() {
        setting = new Setting(FileUtil.touch(settingFilePath), CharsetUtil.CHARSET_UTF_8, false);
    }

    public void setProps(String key, String value) {
        setting.put(key, value);
    }

    public String getProps(String key) {
        return setting.get(key);
    }

    /**
     * 存盘
     */
    public void save() {
        setting.store(settingFilePath);
    }
}
