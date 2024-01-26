package site.minnan.robotmanage.infrastructure.exception;

/**
 * 实体不存在异常（更新，查询指定id或其他属性对象时不存在）
 * @author Minnan on 2020/12/17
 */
public class EntityNotExistException extends RuntimeException{

    public EntityNotExistException(){
        super();
    }

    public EntityNotExistException(String message){
        super(message);
    }
}
