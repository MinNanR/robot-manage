package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.Boss;
import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetBossListDTO;
import site.minnan.robotmanage.entity.dto.SaveBossDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.boss.BossInfoVO;

public interface BossService {

    ListQueryVO<BossInfoVO> getBossList(GetBossListDTO dto);

    BossInfoVO getBossInfo(DetailsQueryDTO dto);

    void addBoss(SaveBossDTO dto);

    void updateBoss(SaveBossDTO dto);

    void deleteBoss(DetailsQueryDTO dto);
}
