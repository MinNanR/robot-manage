package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private Integer id;

    private Integer questionId;

    private String content;
}
