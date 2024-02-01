package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新快捷查询参数
 *
 * @author  on 2024/02/01
 */
@Getter
@Setter
public class UpdateQueryMapDTO extends OperateDTO{

    private Integer id;

    private String queryContent;

    private String queryUrl;
}
