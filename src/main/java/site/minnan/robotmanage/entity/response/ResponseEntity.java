package site.minnan.robotmanage.entity.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 响应实体类
 * @author Minnan on 2020/12/16
 *
 * @param <T> 返回数据类型
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseEntity<T> implements Serializable {

    /**
     * 响应状态码
     */
    private String code;

    /**
     * 相应消息
     */
    private String message;

    /**
     * 相应内容
     */
    private T data;

    private ResponseEntity(ResponseCode responseCode) {
        this.code = responseCode.code();
        this.message = responseCode.message();
    }

    /**
     * 操作成功，无响应内容，通常用于添加或更新操作
     *
     * @return
     */
    public static ResponseEntity<?> success() {
        return new ResponseEntity<>(ResponseCode.SUCCESS);
    }

    /**
     * 成功并返回响应数据，通常用于查询
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> success(T data) {
        ResponseEntity<T> responseEntity = new ResponseEntity<>(ResponseCode.SUCCESS);
        responseEntity.setData(data);
        return responseEntity;
    }

    public static <T> ResponseEntity<T> success(T data, String message){
        ResponseEntity<T> responseEntity = new ResponseEntity<>(ResponseCode.SUCCESS);
        responseEntity.setData(data);
        responseEntity.setMessage(message);
        return responseEntity;
    }

    /**
     * 成功并返回定制消息
     *
     * @param message
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> message(String message) {
        ResponseEntity<T> responseEntity = new ResponseEntity<>(ResponseCode.SUCCESS);
        responseEntity.message = message;
        return responseEntity;
    }

    /**
     * 操作失败
     *
     * @param responseCode 失败的具体原因
     * @return
     * @see ResponseCode
     */
    public static ResponseEntity<?> fail(ResponseCode responseCode) {
        return new ResponseEntity<>(responseCode);
    }

    /**
     * 操作失败，并返回响应数据
     *
     * @param responseCode 响应的具体原因
     * @param data         响应数据
     * @param <T>          响应数据类型
     * @return
     * @see ResponseCode
     */
    public static <T> ResponseEntity<T> fail(ResponseCode responseCode, T data) {
        ResponseEntity<T> responseEntity = new ResponseEntity<>(responseCode);
        responseEntity.setData(data);
        return responseEntity;
    }

    public static <T> ResponseEntity<T> invalid(String message){
        ResponseEntity<T> responseEntity = new ResponseEntity<T>(ResponseCode.INVALID_USER);
        responseEntity.setMessage(message);
        return responseEntity;
    }

    /**
     * 失败并返回定制消息
     *
     * @param message 响应消息
     * @param <T>     响应数据类型
     * @return
     */
    public static <T> ResponseEntity<T> fail(String message) {
        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        ResponseCode responseCode = ResponseCode.FAIL;
        responseEntity.code = responseCode.code();
        responseEntity.message = message;
        return responseEntity;
    }
}
