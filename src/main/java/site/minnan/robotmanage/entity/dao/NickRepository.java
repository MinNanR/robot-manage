package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.Nick;

import java.util.List;

public interface NickRepository extends JpaRepository<Nick, Integer>, JpaSpecificationExecutor<Nick> {

    Nick findByQqAndNick(String qq, String nick);

    List<Nick> findByQq(String qq);
}

