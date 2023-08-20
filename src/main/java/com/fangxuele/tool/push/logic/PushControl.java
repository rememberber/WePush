package com.fangxuele.tool.push.logic;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.ui.dialog.importway.ImportByFile;
import com.fangxuele.tool.push.ui.form.MessageEditForm;

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

    public static List<SendResult> preview(Integer tMsgId) {
        List<SendResult> sendResultList = new ArrayList<>();
        List<String[]> msgDataList = new ArrayList<>();
        for (String data : MessageEditForm.getInstance().getPreviewUserField().getText().split(";")) {
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