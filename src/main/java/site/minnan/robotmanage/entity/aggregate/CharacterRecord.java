package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "character_record")
public class CharacterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "character_name")
    private String characterName;

    //服务区，na：美区，eu：欧区
    private String region;

    @Column(name = "world_id")
    private Integer worldId;

    private Integer level;

    @Column(name = "level_percent")
    private String levelPercent;

    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "job_detail")
    private Integer jobDetail;

    @Column(name = "character_img_url")
    private String characterImgUrl;

    @Column(name = "update_time")
    private String updateTime;

    @Column(name = "query_time")
    private String queryTime;

    @Column(name = "job_name")
    private String jobName;
}
