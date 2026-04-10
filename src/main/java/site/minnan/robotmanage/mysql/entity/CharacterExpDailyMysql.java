package site.minnan.robotmanage.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "character_exp_daily")
public class CharacterExpDailyMysql {

    @EmbeddedId
    private CharacterExpDailyId id;

    @Column(name = "current_exp")
    private Long currentExp;

    private Integer level;

    @Column(name = "level_percent")
    private BigDecimal levelPercent;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
