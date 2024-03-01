package site.minnan.robotmanage.strategy.impl;

import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.StatisticsQueryCommand;
import site.minnan.robotmanage.entity.vo.bot.StrategyUsageCount;
import site.minnan.robotmanage.service.StatisticsService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 统计使用数量消息处理
 *
 * @author Minnan on 2024/02/29
 */
@Component("statistics")
public class StatisticsMessageHandler implements MessageHandler {

    private StatisticsService statisticsService;

    public StatisticsMessageHandler(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage().replace("统计", "");
        StatisticsQueryCommand command = statisticsService.parseQueryCommand(message);
        List<StrategyUsageCount> countList = statisticsService.getStrategyUsageCount(command);

        String content = countList.stream()
//                .sorted(Comparator.comparing(StrategyUsageCount::count))
//                .limit(10)
                .map(StrategyUsageCount::formatted).collect(Collectors.joining("\n"));

        int totalCount = countList.stream().mapToInt(StrategyUsageCount::count).sum();

        String reply = """
                统计日期 %s 至 %s
                %s,
                总计使用%d次
                """.formatted(command.startTime().toDateStr(), command.endTime().toDateStr(), content, totalCount);

        return Optional.of("\n" + reply);
    }
}
