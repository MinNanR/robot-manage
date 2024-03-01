package site.minnan.robotmanage.entity.aggregate;

import cn.hutool.core.date.DateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略使用数量统计
 *
 * @author Minnan on 2024/02/29
 */
@Entity
@Table(name = "strategy_statistics")
@Data
@NoArgsConstructor
public class StrategyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "strategy_id")
    private Integer strategyId;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "note_date")
    private String noteDate;

    @Column(name = "refer_count")
    private Integer referCount;

    @Column(name = "update_time")
    private String updateTime;

    public void refer() {
        this.referCount = this.referCount + 1;
        this.updateTime = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
    }

    public StrategyStatistics(HandlerStrategy strategy) {
        this.strategyId = strategy.getId();
        this.strategyName = strategy.getStrategyName();
        DateTime now = DateTime.now();
        this.referCount = 0;
        this.noteDate = now.toString("yyyy-MM-dd");
        this.updateTime = now.toString();
    }

}
