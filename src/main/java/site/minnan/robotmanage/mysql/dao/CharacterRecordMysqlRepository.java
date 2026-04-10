package site.minnan.robotmanage.mysql.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.minnan.robotmanage.mysql.entity.CharacterRecordMysql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CharacterRecordMysqlRepository extends JpaRepository<CharacterRecordMysql, Integer> {

    @Query(value = """
            select *
            from character_record
            where lower(character_name) = :characterName and region = :region
            limit 1
            """, nativeQuery = true)
    Optional<CharacterRecordMysql> findFirstByCharacterNameIgnoreCaseAndRegion(
            @Param("characterName") String characterName,
            @Param("region") String region
    );

    @Query(value = """
            select r_g.rank_global as rankGlobal,
                   r_w.rank_world as rankWorld,
                   r_j.rank_job as rankJob,
                   r_w_j.rank_world_job as rankWorldJob
            from
            (select count(*) + 1 as rank_global
             from character_record
             where
                 region = :region
                 and level > :L
                 or (level = :L and level_percent > :P)
                 or (level = :L and level_percent = :P and id < :cid)
            ) as r_g,
            (select count(*) + 1 as rank_world
             from character_record
             where world_id = :W
               and (
                     level > :L
                  or (level = :L and level_percent > :P)
                  or (level = :L and level_percent = :P and id < :cid)
               )
            ) as r_w,
            (select count(*) + 1 as rank_job
             from character_record
             where :region = :region
               and job_name = :J
               and (
                     level > :L
                  or (level = :L and level_percent > :P)
                  or (level = :L and level_percent = :P and id < :cid)
               )
            ) as r_j,
            (select count(*) + 1 as rank_world_job
             from character_record
             where world_id = :W
               and job_name = :J
               and (
                     level > :L
                  or (level = :L and level_percent > :P)
                  or (level = :L and level_percent = :P and id < :cid)
               )
            ) as r_w_j
            """, nativeQuery = true)
    RankProjection calcRank(
            @Param("L") Integer level,
            @Param("P") BigDecimal levelPercent,
            @Param("cid") Integer characterId,
            @Param("W") Integer worldId,
            @Param("J") String jobName,
            @Param("region") String region
    );

    @Query(value = """
            select id,
                   character_name as characterName,
                   level,
                   round(level_percent, 2) as levelPercent,
                   rn
            from (
                select id,
                       character_name,
                       level,
                       level_percent,
                       row_number() over (
                           partition by world_id, job_name
                           order by level desc, level_percent desc, id
                       ) as rn
                from character_record
                where world_id = :worldId and job_name = :jobName
            ) t
            where rn between :rn - 2 and :rn + 2
            order by rn
            """, nativeQuery = true)
    List<NearRankProjection> findNearRank(
            @Param("worldId") Integer worldId,
            @Param("jobName") String jobName,
            @Param("rn") Integer rn
    );

    @Modifying
    @Query(value = "update character_record set query_time = now() where id = :id", nativeQuery = true)
    int updateQueryTime(@Param("id") Integer id);

    interface RankProjection {
        Integer getRankGlobal();

        Integer getRankWorld();

        Integer getRankJob();

        Integer getRankWorldJob();
    }

    interface NearRankProjection {
        Integer getId();

        String getCharacterName();

        Integer getLevel();

        BigDecimal getLevelPercent();

        Integer getRn();
    }
}
