package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.BossNickname;

import java.util.Optional;

@Repository
public interface BossNicknameRepository extends JpaRepository<BossNickname, Integer> {

    Optional<BossNickname> findByBossNickNameIgnoreCase(String bossNickName);
}
