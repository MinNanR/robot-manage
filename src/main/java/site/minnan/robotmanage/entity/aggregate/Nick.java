package site.minnan.robotmanage.entity.aggregate;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.IdGeneratorType;

/**
 * 查询昵称
 *
 * @author Minnan on 2024/01/16
 */
@Data
@Entity
@Table(name = "nick")
@NoArgsConstructor
public class Nick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 用户qq
     */
    private String qq;

    /**
     * 用户设置的昵称
     */
    private String nick;

    /**
     * 查询目标
     */
    private String character;

    public Nick(String qq, String nick, String character) {
        this.qq = qq;
        this.nick = nick;
        this.character = character;
    }

}
