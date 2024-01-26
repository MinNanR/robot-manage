package site.minnan.robotmanage.infrastructure.validate;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 参数校验失败消息
 *
 * @author Minnan on 2021/3/31
 */
@Data
@AllArgsConstructor
public class ValidateMessage {

    private String field;

    private String message;
}
