package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.FlameCost;
import site.minnan.robotmanage.entity.aggregate.QueryMap;

/**
 *
 *
 * @author Minnan on 2024/01/15
 */
@Repository
public interface QueryMapRepository extends JpaRepository<QueryMap, Integer>, JpaSpecificationExecutor<QueryMap> {


}
