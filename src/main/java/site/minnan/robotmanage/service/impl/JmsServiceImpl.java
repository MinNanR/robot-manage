package site.minnan.robotmanage.service.impl;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.jms.JmsConf;
import site.minnan.robotmanage.entity.aggregate.jms.JmsUsage;
import site.minnan.robotmanage.entity.dao.jms.JmsConfRepository;
import site.minnan.robotmanage.entity.dao.jms.JmsUsageRepository;
import site.minnan.robotmanage.entity.dto.jms.JmsConfDTO;
import site.minnan.robotmanage.entity.dto.jms.JmsQueryRespDTO;
import site.minnan.robotmanage.entity.dto.jms.JmsUsageMonthlyBWDTO;
import site.minnan.robotmanage.service.JmsService;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@Component
@Slf4j
public class JmsServiceImpl implements JmsService {

    private static final String REQ_URL = "https://justmysocks6.net/members/getbwcounter.php?service=%d&id=%s";

    private final ObjectMapper objectMapper;
    private final JmsConfRepository jmsConfRepository;
    private final JmsUsageRepository jmsUsageRepository;

    public JmsServiceImpl(ObjectMapper objectMapper, JmsConfRepository jmsConfRepository, JmsUsageRepository jmsUsageRepository) {
        this.objectMapper = objectMapper;
        this.jmsConfRepository = jmsConfRepository;
        this.jmsUsageRepository = jmsUsageRepository;
    }

    @Override
    public JmsQueryRespDTO query(JmsConf jmsConf) {
        final String resp = HttpUtil.get(REQ_URL.formatted(jmsConf.getServiceId(), jmsConf.getUuid()));
        log.info("定时拉取JMS【{}】服务流量数据，响应结果：{}", jmsConf.getAlias(), resp);
        try {
            return objectMapper.readValue(resp, JmsQueryRespDTO.class);
        } catch (JsonProcessingException e) {
            log.error("JMS响应转换异常");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void schedule() {
        final List<JmsConf> jmsConfList = jmsConfRepository.findAll();
        jmsConfList.forEach(conf -> {
            try {
                final JmsQueryRespDTO queryRespDTO = this.query(conf);
                jmsUsageRepository.save(queryRespDTO.toUsage(conf.getId()));
            } catch (Exception e) {
                log.error("jms服务【{}】拉取流量数据失败", conf.getAlias());
            }
        });
    }

    @Override
    public List<JmsConfDTO> findAllConfs() {
        return jmsConfRepository.findAll()
                .stream().map(JmsConfDTO::from)
                .toList();
    }

    @Override
    public List<JmsUsageMonthlyBWDTO> monthBw(Integer source, Integer year, Integer month) {
        final LocalDate start = LocalDate.of(year, month, 1);
        final List<JmsUsage> jmsUsages = jmsUsageRepository.findBySourceAndDateBetween(source, start, start.withDayOfMonth(start.lengthOfMonth()));
        return JmsUsageMonthlyBWDTO.from(jmsUsages);
    }
}
