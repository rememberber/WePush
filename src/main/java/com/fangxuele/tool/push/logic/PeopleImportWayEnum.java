package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 人群导入方式枚举
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/4/29.
 */
public enum PeopleImportWayEnum {
    /**
     * 导入方式
     */
    BY_FILE(1, "通过文件导入"),
    BY_SQL(2, "通过SQL导入"),
    BY_WX_MP(3, "通过微信公众平台导入"),
    BY_WX_CP(4, "通过微信企业通讯录导入"),
    BY_DING(5, "通过钉钉通讯录导入"),
    BY_NUM(6, "通过数量导入");

    private int code;

    private String name;

    public static final int BY_FILE_CODE = 1;
    public static final int BY_SQL_CODE = 2;
    public static final int BY_WX_MP_CODE = 3;
    public static final int BY_WX_CP_CODE = 4;
    public static final int BY_DING_CODE = 5;
    public static final int BY_NUM_CODE = 6;

    PeopleImportWayEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(int code) {
        String name = "";
        switch (code) {
            case 1:
                name = BY_FILE.name;
                break;
            case 2:
                name = BY_SQL.name;
                break;
            case 3:
                name = BY_WX_MP.name;
                break;
            case 4:
                name = BY_WX_CP.name;
                break;
            case 5:
                name = BY_DING.name;
                break;
            case 6:
                name = BY_NUM.name;
                break;
            default:
                name = "";
        }
        return name;
    }

    public static String getName(PeopleImportWayEnum wayEnum) {
        return wayEnum.name;
    }

}
