package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterQueryDTO {

    private Integer pageIndex;

    private Integer pageSize;

    /** 查询关键字 */
    private String keyword;

    /** 排名类型，1-按角色等级，2-按联盟等级 */
    private Integer rankType;

    private String region;

    private Integer worldId;

    private String jobName;

    private Integer characterId;

}
