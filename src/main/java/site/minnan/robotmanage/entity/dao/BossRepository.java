package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.Boss;

import java.util.List;

@Repository
public interface BossRepository extends JpaRepository<Boss, Integer>, JpaSpecificationExecutor<Boss> {

    /**
     * 根据boss昵称查找boss信息
     *
     * @param bossNickName
     * @return
     */
    Boss findBossByBossNickNameIs(String bossNickName);

    /**
     * 列表查询BOSS
     *
     * @param bossNickName
     * @return
     */
    List<Boss> findBossesByBossNickNameLike(String bossNickName);
}
