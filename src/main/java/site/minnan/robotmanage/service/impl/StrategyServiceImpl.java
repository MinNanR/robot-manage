package site.minnan.robotmanage.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dao.StrategyRepository;
import site.minnan.robotmanage.entity.dto.GetStrategyListDTO;
import site.minnan.robotmanage.entity.dto.ModifyStrategyOrdinalDTO;
import site.minnan.robotmanage.entity.dto.UpdateStrategyDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.service.StrategyService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 消息处理策略服务
 *
 * @author Minnan on 2024/01/29
 */
@Service
public class StrategyServiceImpl implements StrategyService, InitializingBean {

    public StrategyServiceImpl(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    @Autowired
    @Qualifier("strategyComponent")
    private List<String> strategyComponentList;

    private StrategyRepository strategyRepository;


    /**
     * 启动时，将没有注册组件的消息处理策略禁用
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Console.log(strategyComponentList);
        List<HandlerStrategy> handlerStrategyList = strategyRepository.getAllByComponentNameNotIn(strategyComponentList);
        handlerStrategyList.forEach(e -> e.setEnabled(0));
        strategyRepository.saveAll(handlerStrategyList);
    }

    /**
     * 获取处理消息组件下拉框
     *
     * @return
     */
    @Override
    public List<String> getComponentDropDown() {
        return strategyComponentList;
    }

    /**
     * 查询消息处理策略列表
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<HandlerStrategy> getStrategyList(GetStrategyListDTO dto) {
        Specification<HandlerStrategy> specification = ((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotBlank(dto.getKeyword())) {
                predicates.add(builder.like(root.get("strategyName"), "%%%s%%".formatted(dto.getKeyword())));
            }

            return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
        });
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize(), Sort.by("ordinal"));
        Page<HandlerStrategy> queryResult = strategyRepository.findAll(specification, page);
        return new ListQueryVO<>(queryResult.toList(), queryResult.getTotalElements(), queryResult.getTotalPages());
    }

    /**
     * 修改策略配置
     *
     * @param dto
     */
    @Override
    public void modifyStrategy(UpdateStrategyDTO dto) {
        Integer id = dto.getId();
        Optional<HandlerStrategy> strategyOpt = strategyRepository.findById(id);
        HandlerStrategy strategy = strategyOpt.orElseThrow(() -> new EntityNotExistException("处理策略不存在"));
        strategy.modify(dto);
        strategyRepository.save(strategy);
    }

    /**
     * 设置策略启用或停用
     *
     * @param dto
     */
    @Override
    public void updateStrategyEnable(UpdateStrategyDTO dto) {
        Integer id = dto.getId();
        Optional<HandlerStrategy> strategyOpt = strategyRepository.findById(id);
        HandlerStrategy strategy = strategyOpt.orElseThrow(() -> new EntityNotExistException("策略不存在"));
        strategy.setEnabled(dto.getEnabled());
        strategyRepository.save(strategy);
    }

    /**
     * 修改策略排序
     *
     * @param dto
     */
    @Override
    public void modifyStrategyOrdinal(ModifyStrategyOrdinalDTO dto) {
        Integer id = dto.getId();
        Optional<HandlerStrategy> strategyOpt = strategyRepository.findById(id);
        HandlerStrategy strategy = strategyOpt.orElseThrow(() -> new EntityNotExistException("处理策略不存在"));

        Integer modifyType = dto.getModifyType();
        Integer ordinal = strategy.getOrdinal();

        HandlerStrategy relatedStrategy = modifyType == 1 ? strategyRepository.findFirstByOrdinalLessThanOrderByOrdinalDesc(ordinal)
                : strategyRepository.findFirstByOrdinalGreaterThanOrderByOrdinal(ordinal);

        Integer relatedOrdinal = relatedStrategy.getOrdinal();

        int temp = ordinal;
        strategy.setOrdinal(relatedOrdinal);
        relatedStrategy.setOrdinal(temp);

        strategyRepository.saveAll(ListUtil.toList(strategy, relatedStrategy));
    }

    /**
     * 添加消息处理策略
     *
     * @param strategy
     */
    @Override
    public void addStrategy(HandlerStrategy strategy) {
        PageRequest page = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "ordinal"));
        Page<HandlerStrategy> list = strategyRepository.findAll(page);
        HandlerStrategy first = list.iterator().next();

        int newOrdinal = first.getOrdinal() + 1;
        strategy.setOrdinal(newOrdinal);
        strategy.setEnabled(1);

        strategyRepository.save(strategy);
    }
}
