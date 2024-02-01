package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改处理策略排序参数
 *
 * @author Minnan on 2024/01/30
 */
@Getter
@Setter
public class ModifyStrategyOrdinalDTO extends OperateDTO{

    //修改的策略id
    private Integer id;

    //修改类型，1-上移，2-下移
    private Integer modifyType;
}
