package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import site.minnan.robotmanage.entity.aggregate.JobMap;

public interface JobMapRepository extends JpaRepository<JobMap, Integer>, JpaSpecificationExecutor<JobMap> {

    @Query(value = "select * from job_map where (job_id = ?1 and job_detail is null) or (job_id = ?1 and job_detail = ?2)", nativeQuery = true)
    JobMap getJob(Integer jobId, Integer jobDetail);

}
