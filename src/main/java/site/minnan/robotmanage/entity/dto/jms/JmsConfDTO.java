package site.minnan.robotmanage.entity.dto.jms;

import lombok.Data;
import site.minnan.robotmanage.entity.aggregate.jms.JmsConf;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@Data
public class JmsConfDTO {

    private Integer id;
    private String alias;

    public static JmsConfDTO from(JmsConf conf) {
        final JmsConfDTO dto = new JmsConfDTO();
        dto.id = conf.getId();
        dto.alias = conf.getAlias();
        return dto;
    }
}
