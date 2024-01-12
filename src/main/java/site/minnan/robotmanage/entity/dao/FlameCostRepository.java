package site.minnan.robotmanage.entity.dao;

import org.hibernate.annotations.SQLSelect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.FlameCost;

import java.util.List;

@Repository
public interface FlameCostRepository extends JpaRepository<FlameCost, Integer>, JpaSpecificationExecutor<FlameCost> {

    @Query(value = "select id, max(target) as target, min_level, max_level, avg_cost, median_cost from flame_cost where median_cost <= ?1 group by max_level order by target", nativeQuery = true)
    List<FlameCost> findExpect(Float expect);

    @Query(value = "select id,target, max_level, min_level, avg_cost, median_cost from flame_cost where target = ?1", nativeQuery = true)
    List<FlameCost> findCost(Integer target);
}
