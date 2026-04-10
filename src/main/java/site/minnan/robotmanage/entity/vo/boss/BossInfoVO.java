package site.minnan.robotmanage.entity.vo.boss;

import lombok.Getter;
import lombok.Setter;
import site.minnan.robotmanage.entity.aggregate.Boss;
import site.minnan.robotmanage.entity.aggregate.BossNickname;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BossInfoVO {

    private Integer id;

    private String bossName;

    private String hp;

    private String level;

    private Integer physicalDefense;

    private Integer magicalDefense;

    private Integer elementReduction;

    private String arc;

    private String aut;

    private String reenterInterval;

    private String claimLimit;

    private Long reward;

    private List<String> nicknames;

    public static BossInfoVO assemble(Boss boss) {
        BossInfoVO vo = new BossInfoVO();
        vo.setId(boss.getId());
        vo.setBossName(boss.getBossName());
        vo.setHp(boss.getHp());
        vo.setLevel(boss.getLevel());
        vo.setPhysicalDefense(boss.getPhysicalDefense());
        vo.setMagicalDefense(boss.getMagicalDefense());
        vo.setElementReduction(boss.getElementReduction());
        vo.setArc(boss.getArc());
        vo.setAut(boss.getAut());
        vo.setReenterInterval(boss.getReenterInterval());
        vo.setClaimLimit(boss.getClaimLimit());
        vo.setReward(boss.getReward());
        if (boss.getNicknames() != null) {
            vo.setNicknames(boss.getNicknames().stream().map(BossNickname::getBossNickName).collect(Collectors.toList()));
        }
        return vo;
    }
}
