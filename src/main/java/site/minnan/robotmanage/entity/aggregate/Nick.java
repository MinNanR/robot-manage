package site.minnan.robotmanage.entity.aggregate;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 查询昵称
 *
 * @author Minnan on 2024/01/16
 */
@Data
@Entity
@Table(name = "nick")
public class Nick {

    @Id
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


}
