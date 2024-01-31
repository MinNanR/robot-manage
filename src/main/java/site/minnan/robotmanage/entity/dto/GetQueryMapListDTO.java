package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 查询快捷查询链接参数
 *
 * @author Minnan on 2024/01/31
 */
@Getter
@Setter
public class GetQueryMapListDTO {

    private Integer pageIndex;

    private Integer pageSize;

    private String keyword;
}
