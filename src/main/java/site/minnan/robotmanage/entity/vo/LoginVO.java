package site.minnan.robotmanage.entity.vo;

import lombok.*;

/**
 * 登陆返回参数
 *
 * @author Minnan on 2024/01/31
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginVO {

    private Integer id;

    private String nickName;

    private String token;



}
