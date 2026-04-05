package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.Boss;

import java.util.List;
import java.util.Optional;

@Repository
public interface BossRepository extends JpaRepository<Boss, Integer>, JpaSpecificationExecutor<Boss> {

    @EntityGraph(attributePaths = "nicknames")
    @Query("select b from Boss b join b.nicknames n where upper(n.bossNickName) = upper(:nick)")
    Optional<Boss> findBossByNickName(@Param("nick") String nickName);

    @EntityGraph(attributePaths = "nicknames")
    @Query("select distinct b from Boss b join b.nicknames n where upper(n.bossNickName) like concat('%', upper(:nick), '%')")
    List<Boss> findBossesByNickNameLike(@Param("nick") String nickName);

    @EntityGraph(attributePaths = "nicknames")
    Optional<Boss> findByBossNameIgnoreCase(String bossName);
}
