package site.minnan.robotmanage.entity.response;

/**
 * 返回状态码
 * @author Minnan on 2020/12/16
 */
public enum ResponseCode {
    SUCCESS("000", "操作成功"),

    INVALID_USER("002","非法用户"),

    FAIL("001","操作失败"),

    INVALID_PARAM("005", "参数非法"),

    USERNAME_EXIST("010", "用户名已存在"),

    UNKNOWN_ERROR("500", "未知错误");

    private final String code;

    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}
