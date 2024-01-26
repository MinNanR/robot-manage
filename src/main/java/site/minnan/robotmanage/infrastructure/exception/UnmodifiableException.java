package site.minnan.robotmanage.infrastructure.exception;

/**
 * 实体当前状态不可修改异常
 *
 * @author Minnan on 2021/1/8
 */
public class UnmodifiableException extends RuntimeException {

    public UnmodifiableException() {
        super();
    }

    public UnmodifiableException(String message) {
        super(message);
    }
}
