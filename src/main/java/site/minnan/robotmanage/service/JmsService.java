package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.jms.JmsConf;
import site.minnan.robotmanage.entity.dto.jms.JmsConfDTO;
import site.minnan.robotmanage.entity.dto.jms.JmsQueryRespDTO;
import site.minnan.robotmanage.entity.dto.jms.JmsUsageMonthlyBWDTO;

import java.util.List;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
public interface JmsService {

    JmsQueryRespDTO query(JmsConf jmsConf);

    void schedule();

    List<JmsConfDTO> findAllConfs();

    List<JmsUsageMonthlyBWDTO> monthBw(Integer source, Integer year, Integer month);
}
