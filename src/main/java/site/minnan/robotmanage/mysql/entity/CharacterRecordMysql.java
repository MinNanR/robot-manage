package site.minnan.robotmanage.mysql.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "character_record")
public class CharacterRecordMysql {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "character_name")
    private String characterName;

    private String region;

    @Column(name = "world_id")
    private Integer worldId;

    private Integer level;

    @Column(name = "level_percent")
    private BigDecimal levelPercent;

    @Column(name = "character_img_url")
    private String characterImgUrl;

    @Column(name = "job_name")
    private String jobName;

    private Integer legion;

    @Column(name = "legion_raid_power")
    private Long legionRaidPower;

    @Column(name = "legion_rank")
    private Integer legionRank;

    @Column(name = "update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    @Column(name = "query_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date queryTime;
}
