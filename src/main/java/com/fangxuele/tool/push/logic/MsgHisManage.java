package com.fangxuele.tool.push.logic;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史消息管理（单例）
 * Created by rememberber(https://github.com/rememberber) on 2017/6/17.
 */
public class MsgHisManage {
    private static final Log logger = LogFactory.get();

    private static MsgHisManage ourInstance = new MsgHisManage();

    private File msgHisFile;

    /**
     * 历史消息保存的csv的列数
     */
    public static final int ARRAY_LENGTH = 15;

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error(e);
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

    /**
     * 写入(保存)消息历史
     *
     * @param map key:消息名称 value:消息详情
     * @throws IOException
     */
    public void writeMsgHis(Map<String, String[]> map) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(msgHisFile));
        String[] entries = new String[]{"消息名称", "消息类型", "模板ID", "跳转URL", "客服消息类型", "客服消息标题/内容", "客服消息图片URL", "客服消息描述", "客服消息跳转URL"};
        writer.writeNext(entries);
        writer.writeAll(map.values());
        writer.close();
    }

    /**
     * 保持模板数据
     *
     * @param msgName 消息名称
     * @throws IOException
     */
    public void writeTemplateData(String msgName) throws IOException {
        File dir = new File(SystemUtil.configHome + "data" + File.separator + "template_data" + File.separator);
        File file = new File(SystemUtil.configHome + "data" + File.separator + "template_data" + File.separator
                + msgName + ".csv");
        if (!file.exists()) {
            dir.mkdirs();
            file.createNewFile();
        }

        CSVWriter writer = new CSVWriter(new FileWriter(file));

        List<String[]> records = new ArrayList<String[]>();

        // 如果table为空，则初始化
        if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        // 逐行读取
        DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable()
                .getModel();
        int rowCount = tableModel.getRowCount();
        String[] arryData;
        for (int i = 0; i < rowCount; i++) {
            arryData = new String[3];
            arryData[0] = (String) tableModel.getValueAt(i, 0);
            arryData[1] = (String) tableModel.getValueAt(i, 1);
            arryData[2] = ((String) tableModel.getValueAt(i, 2)).trim();
            records.add(arryData);
        }

        // 写入文件
        writer.writeAll(records);
        writer.close();
    }

}
