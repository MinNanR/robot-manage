package site.minnan.robotmanage.entity.aggregate.jms;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "jms_usage")
public class JmsUsage extends BaseEntity {
    @Convert(converter = LocalDateConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    private Integer hour;
    private Long usage;
    private Integer source;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        JmsUsage jmsUsage = (JmsUsage) o;
        return getId() != null && Objects.equals(getId(), jmsUsage.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    private static class LocalDateConverter implements AttributeConverter<LocalDate, Long> {

        @Override
        public Long convertToDatabaseColumn(LocalDate attribute) {
            return attribute.toEpochDay() * 24 * 60 * 60 * 1000;
        }

        @Override
        public LocalDate convertToEntityAttribute(Long dbData) {
            return Instant.ofEpochMilli(dbData)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
    }
}
