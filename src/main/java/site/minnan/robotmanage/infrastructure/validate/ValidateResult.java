package site.minnan.robotmanage.infrastructure.validate;

import java.util.ArrayList;

/**
 * 参数校验结果
 * @author Minnan on 2021/03/31
 */
public class ValidateResult extends ArrayList<ValidateMessage> {

    public void add(String field, String message) {
        super.add(new ValidateMessage(field, message));
    }
}
