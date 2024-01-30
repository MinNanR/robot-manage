package site.minnan.robotmanage.entity.aggregate;


import jakarta.persistence.*;
import lombok.Data;
import site.minnan.robotmanage.entity.dto.UpdateStrategyDTO;

@Data
@Entity
@Table(name = "strategy")
public class HandlerStrategy {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 判定表达式
     */
    private String expression;

    /**
     * 表达式判定类型，1-相等，2-正则
     */
    @Column(name = "expression_type")
    private Integer expressionType;

    /**
     * 使用的组件名称
     */
    @Column(name = "component_name")
    private String componentName;

    /**
     * 权限掩码
     */
    @Column(name = "auth_mask")
    private Integer authMask;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 排序
     */
    private Integer ordinal;

    /**
     * 策略名称
     */
    @Column(name = "strategy_name")
    private String strategyName;

    public void modify(UpdateStrategyDTO dto) {
        this.strategyName = dto.getStrategyName();
        this.expressionType = dto.getExpressionType();
        this.expression = dto.getExpression();
        this.componentName = dto.getComponentName();
        this.authMask = dto.getAuthMask();
    }
}
