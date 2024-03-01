package site.minnan.robotmanage.entity.dto;

import cn.hutool.core.date.DateTime;
import org.springframework.data.jpa.domain.Specification;
import site.minnan.robotmanage.entity.aggregate.StrategyStatistics;

/**
 * 统计查询指令
 *
 * @author Minnan on 2024/02/29
 */
public record StatisticsQueryCommand(Specification<StrategyStatistics> spec, DateTime startTime, DateTime endTime) {
}
