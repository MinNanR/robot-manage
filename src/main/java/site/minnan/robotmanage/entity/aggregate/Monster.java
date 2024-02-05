package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 怪物数据
 *
 * @author Minnan on 2024/02/05
 */
@Entity
@Table(name = "monster")
@Data
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String lv;

    private String hp;

    private String exp;

    private String location;
}
