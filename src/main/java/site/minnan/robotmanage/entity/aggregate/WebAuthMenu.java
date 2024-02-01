package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 菜单
 *
 * @author Minnan on 2024/02/01
 */
@Entity
@Table(name = "web_auth_menu")
@Data
public class WebAuthMenu {

    @Id
    private Integer id;

    private String url;

    private String icon;

    private Integer role;

    private String name;
}
