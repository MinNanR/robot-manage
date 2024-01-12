package site.minnan.robotmanage.entity.dto;

import lombok.Data;

@Data
public class GetQuestionListDTO {

    private Integer pageIndex;

    private Integer pageSize;

    private String groupId;

    private String content;
}
