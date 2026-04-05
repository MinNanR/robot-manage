package site.minnan.robotmanage.entity.aggregate;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "boss_info", indexes = {
        @Index(name = "uk_boss_name", columnList = "boss_name", unique = true)
})
public class Boss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 血量 */
    private String hp;

    /** 等级 */
    private String level;

    /** 物理防御力 */
    @Column(name = "physical_defense")
    private Integer physicalDefense;

    /** 魔法防御力 */
    @Column(name = "magical_defense")
    private Integer magicalDefense;

    /** 元素减伤，0-无，1-有 */
    @Column(name = "element_reduction")
    private Integer elementReduction;

    /** arc要求 */
    private String arc;

    /** aut要求 */
    private String aut;

    /** 重新进入间隔 */
    @Column(name = "reenter_interval")
    private String reenterInterval;

    /** 领取限制 */
    @Column(name = "claim_limit")
    private String claimLimit;

    /** 奖励 */
    private Long reward;

    /** boss名称（唯一） */
    @Column(name = "boss_name", nullable = false, unique = true)
    private String bossName;

    @OneToMany(mappedBy = "boss", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BossNickname> nicknames = new ArrayList<>();

    public void setNicknames(List<BossNickname> nicknames) {
        this.nicknames.clear();
        if (nicknames != null) {
            nicknames.forEach(this::addNickname);
        }
    }

    public void addNickname(BossNickname nickname) {
        if (nickname != null) {
            nickname.setBoss(this);
            this.nicknames.add(nickname);
        }
    }

    public String toMsg() {
        StringBuilder sb = new StringBuilder();

        int pd = physicalDefense == null ? 0 : physicalDefense;
        int md = magicalDefense == null ? 0 : magicalDefense;
        String base = """
                BOSS：%s
                血量：%s
                等级：%s
                物理防御力：%d
                魔法防御力：%d
                元素抗性：%s
                通关限制：%s
                """;
        String baseString = base.formatted(bossName, hp, level, pd, md,
                elementReduction != null && elementReduction == 1 ? "有" : "无",
                claimLimit == null ? "无" : claimLimit);

        sb.append(baseString);
        if (StrUtil.isNotBlank(arc)) {
            sb.append("ARC要求：").append(arc).append("\n");
        }
        if (StrUtil.isNotBlank(aut)) {
            sb.append("AUT要求：").append(aut).append("\n");
        }
        sb.append("蛋钱：");
        if (reward == null) {
            sb.append("无");
        } else {
            sb.append(NumberUtil.decimalFormat(",###", reward));
        }
        return sb.toString();
    }
}
