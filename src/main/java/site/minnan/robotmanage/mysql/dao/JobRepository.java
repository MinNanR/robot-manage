package site.minnan.robotmanage.mysql.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import site.minnan.robotmanage.mysql.entity.Job;

public interface JobRepository extends JpaRepository<Job,Integer> {
}
