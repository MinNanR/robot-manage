package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 系统用户
 *
 * @author Minnan on 2024/01/31
 */
@Entity
@Table(name = "web_auth_user")
@Data
public class WebAuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    private String password;

    private Integer role;

    @Column(name = "create_time")
    private String createTime;

    @Column(name ="nick_name")
    private String nickName;

    @Column(name = "password_stamp")
    private String passwordStamp;

}
