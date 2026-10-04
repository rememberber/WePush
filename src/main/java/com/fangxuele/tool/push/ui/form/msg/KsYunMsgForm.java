package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.logic.MessageTypeEnum;

/** 金山云与阿里云均采用命名模板参数，复用模板编辑界面。 */
public final class KsYunMsgForm extends AliYunMsgForm {
    private static KsYunMsgForm instance;
    private KsYunMsgForm() { super(MessageTypeEnum.KS_YUN_CODE); }
    public static KsYunMsgForm getInstance() {
        if (instance == null) instance = new KsYunMsgForm();
        return instance;
    }
}
