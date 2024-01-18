package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 经验阶段
 *
 * @author Minnan on 2024/01/18
 */
@Entity
@Table(name = "lv_exp")
@Data
public class LvExp {

    @Id
    private Integer id;

    private Integer lv;

    @Column(name = "total_exp_earned")
    private String totalExpEarned;

    @Column(name = "exp_to_next_level")
    private String expToNextLevel;

    @Column(name = "next_stage")
    private Integer nextStage;

    @Column(name = "next_stage_exp")
    private String nextStageExp;

}
