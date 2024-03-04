package site.minnan.robotmanage.service;

import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dto.StatisticsQueryCommand;
import site.minnan.robotmanage.entity.vo.bot.StrategyUsageCount;
import site.minnan.robotmanage.service.impl.StatisticsServiceImpl;

import java.util.List;

/**
 * 策略使用数量统计
 *
 * @author Minnan on 2024/02/29
 */
@Service
public interface StatisticsService {

    /**
     * 统计一个消息策略使用
     *
     * @param strategy
     */
    void refer(HandlerStrategy strategy);

    /**
     * 查询统计结果
     *
     * @param command 查询指令
     * @return
     */
    List<StrategyUsageCount> getStrategyUsageCount(StatisticsQueryCommand command);

    /**
     * 解析统计指令
     *
     * @param queryCommand
     * @return
     */
    StatisticsQueryCommand parseQueryCommand(String queryCommand);
}
