package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.CharacterRecord;

@Repository
public interface CharacterRecordRepository extends JpaRepository<CharacterRecord, Integer>, JpaSpecificationExecutor<CharacterRecord> {

    CharacterRecord getByCharacterNameIgnoreCaseAndRegion(String characterName, String region);
}
