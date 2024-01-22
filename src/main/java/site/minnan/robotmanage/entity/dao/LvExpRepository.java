package site.minnan.robotmanage.entity.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.LvExp;


import java.util.List;

public interface LvExpRepository extends JpaRepository<LvExp, Integer>, JpaSpecificationExecutor<LvExp> {

    LvExp findByLv(Integer lv);

    List<LvExp> findByLvGreaterThanEqual(Integer lv, Pageable page);
}
