package site.minnan.robotmanage.entity.aggregate;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "flame_cost")
public class FlameCost {

    @Id
    private Integer id;

    /**
     * 装备等级最大值
     */
    @Column(name = "max_level")
    private Integer maxLevel;

    /**
     * 装备等级最小值
     */
    @Column(name = "min_level")
    private Integer minLevel;

    /**
     * 目标
     */
    private Integer target;

    /**
     * 花费平均数
     */
    @Column(name = "avg_cost")
    private Long avgCost;

    /**
     * 花费中位数
     */
    @Column(name = "median_cost")
    private Long medianCost;

    /**
     * 获取展示格式
     *
     * @return
     */
    private String formatStageName() {
        if (minLevel == 150) {
            return "超贝";
        } else if (minLevel == 161) {
            return "普通甜水";
        } else {
            return minLevel + "-" + maxLevel + "级装备";
        }
    }

    /**
     * 期望查询格式化
     *
     * @return
     */
    public String expectationFormat() {
        String name = formatStageName();
        return StrUtil.format("{}，期望目标：{}", name, target);
    }

    /***
     * 花费查询格式化
     *
     * @return
     */
    public String costFormat() {
        String name = formatStageName();
        return StrUtil.format("{}，花费平均数：{}，花费中位数：{}", name, numberFormat(avgCost), numberFormat(medianCost));
    }

    private String numberFormat(Long cost) {
        if (cost > 1000 * 1000) {
            return NumberUtil.decimalFormat("#.00T", cost / (1000 * 1000));
        } else if (cost > 1000) {
            return NumberUtil.decimalFormat("#.00B", cost / 1000);
        } else {
            return NumberUtil.decimalFormat("#.00M", cost);
        }
    }
}
