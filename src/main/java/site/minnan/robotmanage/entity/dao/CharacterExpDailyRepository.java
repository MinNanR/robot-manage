package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import site.minnan.robotmanage.entity.aggregate.CharacterExpDaily;

import java.util.List;

public interface CharacterExpDailyRepository extends JpaRepository<CharacterExpDaily,Integer>, JpaSpecificationExecutor<CharacterExpDaily> {

    List<CharacterExpDaily> findAllByCharacterIdAndRecordDateAfter(Integer characterId, String recordDate);
}
