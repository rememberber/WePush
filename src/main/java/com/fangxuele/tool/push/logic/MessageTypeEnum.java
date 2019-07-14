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
    ALI_TEMPLATE(6, "阿里大于模板短信"),
    TX_YUN(7, "腾讯云短信"),
    YUN_PIAN(8, "云片网短信"),
    UP_YUN(9, "又拍云短信"),
    HW_YUN(10, "华为云短信"),
    EMAIL(11, "E-Mail"),
    WX_CP(12, "微信企业号/企业微信"),
    HTTP(13, "HTTP请求");

    private int code;

    private String name;

    public static final int MP_TEMPLATE_CODE = 1;
    public static final int MA_TEMPLATE_CODE = 2;
    public static final int KEFU_CODE = 3;
    public static final int KEFU_PRIORITY_CODE = 4;
    public static final int ALI_YUN_CODE = 5;
    public static final int ALI_TEMPLATE_CODE = 6;
    public static final int TX_YUN_CODE = 7;
    public static final int YUN_PIAN_CODE = 8;
    public static final int UP_YUN_CODE = 9;
    public static final int HW_YUN_CODE = 10;
    public static final int EMAIL_CODE = 11;
    public static final int WX_CP_CODE = 12;
    public static final int HTTP_CODE = 13;

    MessageTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
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
            case 6:
                name = ALI_TEMPLATE.name;
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
            default:
        }
        return name;
    }
}
