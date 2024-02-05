package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.Monster;

public interface MonsterRepository extends JpaRepository<Monster, Integer>, JpaSpecificationExecutor<Monster> {
}
