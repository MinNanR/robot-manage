package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "question")
@Data
public class Question {

    @Id
    private Integer id;

    private String content;

    @Column(name = "group_id")
    private String groupId;

    private Integer share;
}
