package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新消息处理策略参数
 *
 * @author Minnan on 2024/01/29
 */
@Getter
@Setter
public class UpdateStrategyDTO extends OperateDTO {

    //id
    private Integer id;

    //策略名称
    private String strategyName;

    //表达式匹配类型，1-全匹配，2-正则匹配
    private Integer expressionType;

    //匹配表达式
    private String expression;

    //使用组件名称
    private String componentName;

    //权限掩码
    private Integer authMask;

    //是否启用
    private Integer enabled;
}
