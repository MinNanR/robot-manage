package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveBossDTO extends OperateDTO {

    private Integer id;

    private String bossName;

    private String hp;

    private String level;

    private Integer physicalDefense;

    private Integer magicalDefense;

    private Boolean elementReduction;

    private String arc;

    private String aut;

    private String reenterInterval;

    private String claimLimit;

    private Long reward;

    private List<String> nicknames;
}
