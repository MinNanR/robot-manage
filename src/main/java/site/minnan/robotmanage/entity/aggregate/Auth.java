package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 权限控制
 *
 * @author Minnan on 2024/01/18
 */
@Data
@Entity
@Table(name = "auth")
public class Auth {

    @Id
    private Integer id;

    //群号
    @Column(name = "group_id")
    private String groupId;

    //用户id（QQ号）
    @Column(name = "user_id")
    private String userId;

    //权限码
    //第一位：添加问题权限1
    //第二位：问题查询权限
    //第三位：问题模糊查询权限
    //第四位：删除问题权限
    //第五位：删除答案权限
    //第六位：BOSS复制权限
    //第七位：使用权限，此位用异或判断
    //第八位：触发维护检测
    //第九位：权限操作
    @Column(name = "auth_number")
    private Integer authNumber;

}
