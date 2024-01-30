package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息处理策略查询参数
 */
@Getter
@Setter
public class GetStrategyListDTO {

    private Integer pageIndex;

    private Integer pageSize;

    //关键字查询（策略名称）
    private String keyword;

}
