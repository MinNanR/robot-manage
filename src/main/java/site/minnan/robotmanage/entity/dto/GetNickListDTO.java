package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 查询昵称列表参数
 *
 * @author Minnan on 2024/01/30
 */
@Getter
@Setter
public class GetNickListDTO {

    private Integer pageIndex;

    private Integer pageSize;

    private String userId;

    private String keyword;
}
