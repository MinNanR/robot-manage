package site.minnan.robotmanage.infrastructure.exception;

/**
 * 实体已存在异常（用于新增时检查唯一约束）
 * @author Minnan on 2020/12/17
 */
public class EntityAlreadyExistException extends RuntimeException{

    public EntityAlreadyExistException(){
        super();
    }

    public EntityAlreadyExistException(String message){
        super(message);
    }
}
