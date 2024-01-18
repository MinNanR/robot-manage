package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.Auth;

public interface AuthRepository extends JpaRepository<Auth, Integer>, JpaSpecificationExecutor<Auth> {

    Auth findByUserIdAndGroupId(String userId, String groupId);
}
