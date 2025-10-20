package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.CharacterExpDaily;

public interface CharacterExpDailyRepository extends JpaRepository<CharacterExpDaily,Integer>, JpaSpecificationExecutor<CharacterExpDaily> {
}
