package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;

import java.util.List;

@Repository
public interface StrategyRepository extends JpaRepository<HandlerStrategy, Integer>, JpaSpecificationExecutor<HandlerStrategy> {

    /**
     * 查询所有消息处理策略，根据排序字段排序
     *
     * @param enabled 是否启用
     * @return
     */
    List<HandlerStrategy> getAllByEnabledIsOrderByOrdinal(Integer enabled);

    /**
     *
     * @param existComponentList
     * @return
     */
    List<HandlerStrategy> getAllByComponentNameNotIn(List<String> existComponentList);

    /**
     * 查找后继处理策略
     *
     * @param ordinal
     * @return
     */
    HandlerStrategy findFirstByOrdinalGreaterThanOrderByOrdinal(Integer ordinal);

    /**
     * 查找前驱处理策略
     *
     * @param ordinal
     * @return
     */
    HandlerStrategy findFirstByOrdinalLessThanOrderByOrdinalDesc(Integer ordinal);


}
