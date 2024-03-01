package site.minnan.robotmanage.entity.aggregate;

import cn.hutool.core.util.NumberUtil;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "boss")
public class Boss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * boss昵称
     */
    @Column(name = "boss_nick_name")
    private String bossNickName;

    /**
     * 血量
     */
    private String hp;

    /**
     * 等级
     */
    private String level;

    /**
     * 物理防御力
     */
    @Column(name = "physical_defense")
    private Integer physicalDefense;

    /**
     * 魔法防御力
     */
    @Column(name = "magical_defense")
    private Integer magicalDefense;

    /**
     * 元素抗性，0-无，1-有
     */
    @Column(name = "element_reduction")
    private Integer elementReduction;

    /**
     * arc要求
     */
    private String arc;

    /**
     * aut要求
     */
    private String aut;

    /**
     * 重新进入间隔
     */
    @Column(name = "reenter_interval")
    private String reenterInterval;

    /**
     * 奖励次数
     */
    @Column(name = "claim_limit")
    private String claimLimit;

    /**
     * 蛋钱
     */
    private Long reward;

    /**
     * boss名称
     */
    @Column(name = "boss_name")
    private String bossName;

    public String toMsg() {
        StringBuilder sb = new StringBuilder();

        String base = """
                BOSS：%s
                血量：%s
                等级：%s
                物理防御力：%d
                魔法防御力：%d
                元素抗性：%s
                入场间隔：%s
                通关限制：%s
                """;
        String baseString = base.formatted(bossName, hp, level, physicalDefense, magicalDefense, elementReduction == 1 ? "有" : "无",
                reenterInterval == null ? "无" : reenterInterval, claimLimit);

        sb.append(baseString);
        if (arc != null) {
            sb.append("神秘要求：").append(arc).append("\n");
        }
        if (aut != null) {
            sb.append("真实之力要求：").append(aut).append("\n");
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
