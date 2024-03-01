package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.StrategyStatistics;

@Repository
public interface StrategyStatisticsRepository extends JpaRepository<StrategyStatistics, Integer>, JpaSpecificationExecutor<StrategyStatistics> {

}
