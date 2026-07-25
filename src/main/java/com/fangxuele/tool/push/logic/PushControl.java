package com.fangxuele.tool.push.logic;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.ui.dialog.importway.ImportByFile;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 推送控制
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
 */
public class PushControl {
    private static final Log logger = LogFactory.get();

    /**
     * 模板变量前缀
     */
    public static final String TEMPLATE_VAR_PREFIX = "var";

    /**
     * 发送预览消息。previewUserText 须由调用方在 EDT 上读取后传入，避免后台线程访问 Swing。
     */
    public static List<SendResult> preview(Integer tMsgId, String previewUserText) {
        List<SendResult> sendResultList = new ArrayList<>();
        List<String[]> msgDataList = new ArrayList<>();
        String users = previewUserText == null ? "" : previewUserText;
        for (String data : users.split(";")) {
            if (StringUtils.isEmpty(data)) {
                continue;
            }
            msgDataList.add(data.split(ImportByFile.TXT_FILE_DATA_SEPERATOR_REGEX));
        }

        // 准备消息构造器
        IMsgSender msgSender = MsgSenderFactory.getMsgSender(tMsgId, 0);

        if (msgSender != null) {
            for (String[] msgData : msgDataList) {
                sendResultList.add(msgSender.send(msgData));
            }
        } else {
            return null;
        }

        return sendResultList;
    }
}