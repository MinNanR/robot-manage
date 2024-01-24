package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.MaintainRecord;

public interface MaintainRecordRepository extends JpaRepository<MaintainRecord, Integer>, JpaSpecificationExecutor<MaintainRecord> {

    /**
     * 按newsId查找公告
     *
     * @param newsId
     * @return
     */
    MaintainRecord findFirstByNewsId(Integer newsId);

    /**
     * 查询最近一次维护记录
     *
     * @param currentTime
     * @return
     */
    MaintainRecord findFirstByEndTimeGreaterThanEqualOrderByStartTimeDesc(String currentTime);
}
