package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetBossListDTO {

    private Integer pageIndex;

    private Integer pageSize;

    /** 关键字（boss名称或昵称） */
    private String keyword;
}
