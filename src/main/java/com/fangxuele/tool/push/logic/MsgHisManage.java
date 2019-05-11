package com.fangxuele.tool.push.logic;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.util.SystemUtil;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 历史消息管理（单例）
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/17.
 */
public class MsgHisManage {
    private static final Log logger = LogFactory.get();

    private static MsgHisManage ourInstance = new MsgHisManage();

    private File msgHisFile;

    private TPushHistoryMapper pushHistoryMapper = App.sqlSession.getMapper(TPushHistoryMapper.class);

    public static MsgHisManage getInstance() {
        return ourInstance;
    }

    private MsgHisManage() {
        try {
            msgHisFile = new File(SystemUtil.configHome + "data" + File.separator + "msg_his.csv");
            File msgHisDir = new File(SystemUtil.configHome + "data" + File.separator);
            if (!msgHisFile.exists()) {
                msgHisDir.mkdirs();
                msgHisFile.createNewFile();
                CSVWriter writer = new CSVWriter(new FileWriter(msgHisFile));
                String[] entries = new String[]{"消息名称", "消息类型", "模板ID", "跳转URL", "客服消息类型", "客服消息标题/内容", "客服消息图片URL", "客服消息描述", "客服消息跳转URL"};
                writer.writeNext(entries);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }

    /**
     * 读取历史消息
     *
     * @return key:消息名称 value:消息详情
     */
    public Map<String, String[]> readMsgHis() {
        Map<String, String[]> map = new LinkedHashMap<>();
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(msgHisFile));

            String[] nextLine;
            reader.readNext();// 第一行header
            while ((nextLine = reader.readNext()) != null) {
                map.put(nextLine[0], nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e);
                }
            }
        }
        return map;
    }

    /**
     * （根据消息名称）读取模板数据
     *
     * @param msgName 消息名称
     * @return {name,value,color}
     */
    public List<String[]> readTemplateData(String msgName) {
        CSVReader reader = null;
        List<String[]> list = new ArrayList<>();
        File dir = new File(SystemUtil.configHome + "data" + File.separator + "template_data" + File.separator);
        File file = new File(SystemUtil.configHome + "data" + File.separator + "template_data" + File.separator + msgName + ".csv");
        try {
            if (!file.exists()) {
                dir.mkdirs();
                file.createNewFile();
            }
            reader = new CSVReader(new FileReader(file));
            list = reader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e);
                }
            }
        }
        return list;
    }

}
