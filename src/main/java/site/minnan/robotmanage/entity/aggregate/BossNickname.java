package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Boss 昵称
 */
@Getter
@Setter
@Entity
@Table(name = "boss_nickname", indexes = {
        @Index(name = "idx_boss_nick_name", columnList = "boss_nick_name", unique = true)
})
public class BossNickname {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "boss_nick_name", nullable = false, unique = true)
    private String bossNickName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_id", nullable = false)
    private Boss boss;
}
