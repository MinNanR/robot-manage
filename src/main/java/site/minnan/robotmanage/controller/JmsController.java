package site.minnan.robotmanage.controller;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.dto.jms.JmsConfDTO;
import site.minnan.robotmanage.entity.dto.jms.JmsUsageMonthlyBWDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.service.JmsService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@RestController
@RequestMapping("/jms/api/")
public class JmsController {

    private final JmsService jmsService;

    public JmsController(JmsService jmsService) {
        this.jmsService = jmsService;
    }

    @GetMapping("jmsConfs")
    public ResponseEntity<List<JmsConfDTO>> getJmsConfs() {
        return ResponseEntity.success(jmsService.findAllConfs());
    }

    @GetMapping("bw/{source}/usage/{year}-{month}")
    public ResponseEntity<List<JmsUsageMonthlyBWDTO>> bw(@PathVariable Integer source, @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.success(jmsService.monthBw(source, year, month));
    }

}
