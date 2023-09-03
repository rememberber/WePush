package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 消息类型常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/3/26.
 */
public enum MessageTypeEnum {
    /**
     * 消息类型
     */
    MP_TEMPLATE(1, "公众号-模板消息"),
    MA_TEMPLATE(2, "小程序-模板消息"),
    KEFU(3, "公众号-客服消息"),
    KEFU_PRIORITY(4, "公众号-客服消息优先"),
    ALI_YUN(5, "阿里云短信"),
    TX_YUN(7, "腾讯云短信"),
    YUN_PIAN(8, "云片网短信"),
    UP_YUN(9, "又拍云短信"),
    HW_YUN(10, "华为云短信"),
    EMAIL(11, "E-Mail"),
    WX_CP(12, "微信企业号/企业微信"),
    HTTP(13, "HTTP请求"),
    DING(14, "钉钉"),
    BD_YUN(15, "百度云短信"),
    QI_NIU_YUN(16, "七牛云短信"),
    WX_UNIFORM_MESSAGE(17, "小程序-统一服务消息"),
    MA_SUBSCRIBE(18, "小程序-订阅消息"),
    MP_SUBSCRIBE(19, "公众号-订阅通知"),
    TX_YUN_3(20, "腾讯云短信3.0");

    private int code;

    private String name;

    public static final int MP_TEMPLATE_CODE = 1;
    public static final int MA_TEMPLATE_CODE = 2;
    public static final int KEFU_CODE = 3;
    public static final int KEFU_PRIORITY_CODE = 4;
    public static final int ALI_YUN_CODE = 5;
    public static final int TX_YUN_CODE = 7;
    public static final int YUN_PIAN_CODE = 8;
    public static final int UP_YUN_CODE = 9;
    public static final int HW_YUN_CODE = 10;
    public static final int EMAIL_CODE = 11;
    public static final int WX_CP_CODE = 12;
    public static final int HTTP_CODE = 13;
    public static final int DING_CODE = 14;
    public static final int BD_YUN_CODE = 15;
    public static final int QI_NIU_YUN_CODE = 16;
    public static final int WX_UNIFORM_MESSAGE_CODE = 17;
    public static final int MA_SUBSCRIBE_CODE = 18;
    public static final int MP_SUBSCRIBE_CODE = 19;
    public static final int TX_YUN_3_CODE = 20;

    MessageTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(MessageTypeEnum messageTypeEnum) {
        return messageTypeEnum.name;
    }

    public static String getName(int code) {
        String name = "";
        switch (code) {
            case 1:
                name = MP_TEMPLATE.name;
                break;
            case 2:
                name = MA_TEMPLATE.name;
                break;
            case 3:
                name = KEFU.name;
                break;
            case 4:
                name = KEFU_PRIORITY.name;
                break;
            case 5:
                name = ALI_YUN.name;
                break;
            case 7:
                name = TX_YUN.name;
                break;
            case 8:
                name = YUN_PIAN.name;
                break;
            case 9:
                name = UP_YUN.name;
                break;
            case 10:
                name = HW_YUN.name;
                break;
            case 11:
                name = EMAIL.name;
                break;
            case 12:
                name = WX_CP.name;
                break;
            case 13:
                name = HTTP.name;
                break;
            case 14:
                name = DING.name;
                break;
            case 15:
                name = BD_YUN.name;
                break;
            case 16:
                name = QI_NIU_YUN.name;
                break;
            case 17:
                name = WX_UNIFORM_MESSAGE.name;
                break;
            case 18:
                name = MA_SUBSCRIBE.name;
                break;
            case 19:
                name = MP_SUBSCRIBE.name;
                break;
            case 20:
                name = TX_YUN_3.name;
                break;
            default:
                name = "";
        }
        return name;
    }

    public static boolean isWxMpType(int msgType) {
        return msgType == MessageTypeEnum.KEFU_CODE
                || msgType == MessageTypeEnum.KEFU_PRIORITY_CODE
                || msgType == MessageTypeEnum.MP_TEMPLATE_CODE
                || msgType == MessageTypeEnum.MP_SUBSCRIBE_CODE
                ;
    }

    public static boolean isWxMaType(int msgType) {
        return msgType == MessageTypeEnum.MA_TEMPLATE_CODE
                || msgType == MessageTypeEnum.MA_SUBSCRIBE_CODE
                || msgType == MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE
                ;
    }

    public static boolean isWxMaOrMpType(int msgType) {
        return isWxMaType(msgType)
                || isWxMpType(msgType);
    }

}
