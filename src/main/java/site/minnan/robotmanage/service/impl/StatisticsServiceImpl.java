package site.minnan.robotmanage.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.aggregate.StrategyStatistics;
import site.minnan.robotmanage.entity.dao.StrategyRepository;
import site.minnan.robotmanage.entity.dao.StrategyStatisticsRepository;
import site.minnan.robotmanage.entity.dto.StatisticsQueryCommand;
import site.minnan.robotmanage.entity.vo.bot.StrategyUsageCount;
import site.minnan.robotmanage.service.StatisticsService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 消息策略统计服务
 *
 * @author Minnan on 2024/02/29
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {


    private StrategyStatisticsRepository strategyStatisticsRepository;

    private StrategyRepository strategyRepository;

    public StatisticsServiceImpl(StrategyStatisticsRepository strategyStatisticsRepository, StrategyRepository strategyRepository) {
        this.strategyStatisticsRepository = strategyStatisticsRepository;
        this.strategyRepository = strategyRepository;
    }

    /**
     * 统计一个消息策略使用
     *
     * @param strategy
     */
    @Override
    public void refer(HandlerStrategy strategy) {
        DateTime now = DateTime.now();
        Specification<StrategyStatistics> specification = ((root, query, criteriaBuilder) -> {
            Predicate noteDatePredicate = criteriaBuilder.equal(root.get("noteDate"), now.toString());
            Predicate idPredicate = criteriaBuilder.equal(root.get("strategyId"), strategy.getId());
            return query.where(noteDatePredicate, idPredicate).getRestriction();
        });
        Optional<StrategyStatistics> statisticsOpt = strategyStatisticsRepository.findOne(specification);
        StrategyStatistics statistics = statisticsOpt.orElseGet(() -> new StrategyStatistics(strategy));
        statistics.refer();
        strategyStatisticsRepository.save(statistics);
    }

    /**
     * 查询统计结果
     *
     * @param command 查询输入参数
     * @return
     */
    @Override
    public List<StrategyUsageCount> getStrategyUsageCount(StatisticsQueryCommand command) {
        Specification<StrategyStatistics> spec = command.spec();
        List<StrategyStatistics> list = strategyStatisticsRepository.findAll(spec);

        Specification<HandlerStrategy> handlerStrategySpecification = ((root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("enabled"), 1);
            return query.where(predicate).getRestriction();
        });

        List<HandlerStrategy> strategyList = strategyRepository.findAll(handlerStrategySpecification);

        Map<Integer, List<StrategyStatistics>> groupByStrategyId = list.stream()
                .collect(Collectors.groupingBy(StrategyStatistics::getStrategyId));

        return strategyList.stream()
                .sorted(Comparator.comparing(HandlerStrategy::getOrdinal))
                .map(e -> {
                    List<StrategyStatistics> l = groupByStrategyId.getOrDefault(e.getId(), Collections.emptyList());
                    int totalCount = l.stream().mapToInt(StrategyStatistics::getReferCount).sum();
                    return new StrategyUsageCount(e.getStrategyName(), totalCount);
                })
                .collect(Collectors.toList());


    }

    @Override
    public StatisticsQueryCommand parseQueryCommand(String queryCommand) {
        queryCommand = queryCommand.strip();
        if (queryCommand.isBlank()) {
            //空参默认查询昨天的统计
            Specification<StrategyStatistics> spec = ((root, query, criteriaBuilder) -> {
                Predicate predicate = criteriaBuilder.equal(root.get("noteDate"), DateUtil.yesterday().toDateStr());
                return query.where(predicate).getRestriction();
            });
            return new StatisticsQueryCommand(spec, DateUtil.yesterday(), DateUtil.yesterday());
        }
        String[] split = queryCommand.split(" ");
        String commandType = split[0].strip();
        if ("-p".equals(commandType) || "-past".equals(commandType)) {
            //-p -past 查询过去多少天
            DateTime yesterday = DateUtil.yesterday();
            String param = split[1];
            DateTime startTime;
            int dayDiffer;
            if ("-m".equals(param)) {
                //-m 过去一个月
                startTime = yesterday.offsetNew(DateField.MONTH, -1);
            } else if ("-w".equals(param)) {
                //-w 过去一个星期
                startTime = yesterday.offsetNew(DateField.WEEK_OF_YEAR, -1);
            } else {
                //默认多少天
                dayDiffer = Integer.parseInt(param);
                startTime = yesterday.offsetNew(DateField.DAY_OF_YEAR, (dayDiffer - 1) * -1);
            }
            Specification<StrategyStatistics> spec = ((root, query, criteriaBuilder) -> {
                Predicate predicate = criteriaBuilder.between(root.get("noteDate"), startTime.toDateStr(), yesterday.toDateStr());
                return query.where(predicate).getRestriction();
            });
            return new StatisticsQueryCommand(spec, startTime, yesterday);
        } else if ("-r".equals(commandType) || "-range".equals(commandType)) {
            //-r -range 范围查询
            String param1 = split[1];
            String param2 = split[2];
            Specification<StrategyStatistics> spec = ((root, query, criteriaBuilder) -> {
                Predicate predicate = criteriaBuilder.between(root.get("noteDate"), param1, param2);
                return query.where(predicate).getRestriction();
            });
            return new StatisticsQueryCommand(spec, DateTime.of(param1, "yyyy-MM-dd"), DateTime.of(param2, "yyyy-MM-dd"));
        } else if ("-m".equals(commandType) || "-month".equals(commandType)) {
            //按月份查询
            String param;
            if (split.length > 1) {
                //查询指定月份
                param = split[1];
            } else {
                //查询本月
                param = DateTime.now().toString("yyyy-MM");
            }
            Specification<StrategyStatistics> spec = ((root, query, criteriaBuilder) -> {
                Predicate predicate = criteriaBuilder.like(root.get("noteDate"), param + "%");
                return query.where(predicate).getRestriction();
            });
            DateTime monthTime = DateTime.of(param, "yyyy-MM");
            return new StatisticsQueryCommand(spec, DateUtil.beginOfMonth(monthTime), DateUtil.endOfMonth(monthTime));
        } else {
            //默认查询昨天
            Specification<StrategyStatistics> spec = ((root, query, criteriaBuilder) -> {
                Predicate predicate = criteriaBuilder.equal(root.get("noteDate"), DateUtil.yesterday().toDateStr());
                return query.where(predicate).getRestriction();
            });
            return new StatisticsQueryCommand(spec, DateUtil.yesterday(), DateUtil.yesterday());
        }
    }

    public static void main(String[] args) {
        String content = "{groupid=348273823, height=935, url=/download?appid=1407&fileid=CgozODg5MDAwODQ5EhRzBcZtmkwaGcvzqltKEwGvgZoIZhi-jQUg_woopbqYnarQhANQgL2jAQ&rkey=CAQSKAB6JWENi5LMt0gEBEwU2dALUqmOBNqP8UYR_nHpXKUy3YTVvMMVhk0, width=945}";

        String[] split = content.split(",");
        String url = "";
        for (String item : split) {
            String[] itemSplit = item.strip().split("=");
            String key = itemSplit[0];
            if ("url".equals(key)) {
                url = Stream.of(itemSplit).skip(1).collect(Collectors.joining("="));
                break;
            }
        }
        System.out.println(url);
    }
}
