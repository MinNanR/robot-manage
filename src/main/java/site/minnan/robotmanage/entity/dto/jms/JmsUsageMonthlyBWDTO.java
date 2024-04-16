package site.minnan.robotmanage.entity.dto.jms;

import cn.hutool.core.io.unit.DataSizeUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import site.minnan.robotmanage.entity.aggregate.jms.JmsUsage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Roger Liu
 * @date 2024/04/16
 */
@Data
@AllArgsConstructor
public class JmsUsageMonthlyBWDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    private Long usage;
    private String usageStr;
    private List<Detail> details;

    public JmsUsageMonthlyBWDTO(LocalDate date, Long usage, List<Detail> details) {
        this.date = date;
        this.usage = usage;
        this.details = details;
    }

    public String getUsageStr() {
        return DataSizeUtil.format(usage);
    }

    @Data
    public static class Detail {
        private Integer hour;
        private Long usage;
        private String usageStr;

        public Detail(Integer hour, Long usage) {
            this.hour = hour;
            this.usage = usage;
        }

        public String getUsageStr() {
            return DataSizeUtil.format(usage);
        }
    }

    public static List<JmsUsageMonthlyBWDTO> from(List<JmsUsage> jmsUsages) {
        Map<LocalDate, List<Detail>> detailsByDate = jmsUsages.stream()
                .collect(Collectors.groupingBy(
                        JmsUsage::getDate,
                        Collectors.mapping(
                                jmsUsage -> new Detail(jmsUsage.getHour(), jmsUsage.getUsage()),
                                Collectors.toList()
                        )
                ));

        return detailsByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Detail> details = entry.getValue().stream()
                            .sorted(Comparator.comparing(Detail::getHour))
                            .collect(Collectors.toList());
                    long totalUsage = details.stream()
                            .mapToLong(Detail::getUsage)
                            .max().orElse(0L);
                    return new JmsUsageMonthlyBWDTO(date, totalUsage, details);
                })
                .sorted(Comparator.comparing(JmsUsageMonthlyBWDTO::getDate))
                .toList();
    }
}
