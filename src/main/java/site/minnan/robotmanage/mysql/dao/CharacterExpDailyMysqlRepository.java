package site.minnan.robotmanage.mysql.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.minnan.robotmanage.mysql.entity.CharacterExpDailyId;
import site.minnan.robotmanage.mysql.entity.CharacterExpDailyMysql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CharacterExpDailyMysqlRepository extends JpaRepository<CharacterExpDailyMysql, CharacterExpDailyId> {

    @Query(value = """
            select *
            from character_exp_daily
            where character_id = :characterId
            order by record_date desc
            limit 20
            """, nativeQuery = true)
    List<CharacterExpDailyMysql> findRecentByCharacterId(@Param("characterId") Integer characterId);

    List<CharacterExpDailyMysql> findByIdCharacterIdInAndIdRecordDateBetween(
            List<Integer> characterIds,
            LocalDate startDate,
            LocalDate endDate
    );
}
