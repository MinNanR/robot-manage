package site.minnan.robotmanage.entity.dto.jms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import site.minnan.robotmanage.entity.aggregate.jms.JmsUsage;

import java.time.LocalDateTime;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@Data
public class JmsQueryRespDTO {

    @JsonProperty("monthly_bw_limit_b")
    private Long monthlyBwLimitB;
    @JsonProperty("bw_counter_b")
    private Long bwCounterB;
    @JsonProperty("bw_reset_day_of_month")
    private Long bwResetDayOfMonth;

    public JmsUsage toUsage(Integer confId) {
        final LocalDateTime now = LocalDateTime.now();
        final JmsUsage usage = new JmsUsage();
        usage.setSource(confId);
        usage.setDate(now.toLocalDate());
        usage.setHour(now.getHour());
        usage.setUsage(bwCounterB);
        return usage;
    }
}
