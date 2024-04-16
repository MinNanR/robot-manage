package site.minnan.robotmanage.entity.dao.jms;

import org.springframework.data.jpa.repository.Query;
import site.minnan.robotmanage.entity.aggregate.jms.JmsUsage;

import java.time.LocalDate;
import java.util.List;

public interface JmsUsageRepository extends BaseRepository<JmsUsage> {

//    @Query(value = "FROM JmsUsage t WHERE t.source = :source AND t.date between date(:dateStart) and date(:dateEnd)")
    List<JmsUsage> findBySourceAndDateBetween(Integer source, LocalDate dateStart, LocalDate dateEnd);
}
