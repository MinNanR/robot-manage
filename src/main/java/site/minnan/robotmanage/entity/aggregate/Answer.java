package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 词条答案
 *
 * @author Minnan on 2023/06/09
 */
@Data
@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer questionId;

    private String content;
}
