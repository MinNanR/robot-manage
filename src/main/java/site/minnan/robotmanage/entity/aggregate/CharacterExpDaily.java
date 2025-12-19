package site.minnan.robotmanage.entity.aggregate;

import cn.hutool.json.JSONObject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "character_exp_daily")
public class CharacterExpDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "record_date")
    private String recordDate;

    @Column(name = "current_exp")
    private String currentExp;

    @Column(name = "exp_differ")
    private String expDiffer;

    private String level;

    @Column(name = "level_percent")
    private String levelPercent;

    @Column(name = "create_time")
    private String createTime;

    public CharacterExpDaily() {}

    public CharacterExpDaily(CharacterRecord character,String recordDate, JSONObject expData) {
        characterId = character.getId();
        this.recordDate = recordDate;
        currentExp = expData.getStr("exp");
        level = expData.getStr("level");
    }
}
