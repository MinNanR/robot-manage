package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_map")
public class JobMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "job_detail")
    private Integer jobDetail;

    private String jobName;
}
