package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "question")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @Column(name = "group_id")
    private String groupId;

    private Integer share;
}
