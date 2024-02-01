package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.WebAuthUser;

import java.util.Optional;

/**
 *
 *
 * @author Minnan on 2024/01/31
 */
@Repository
public interface WebAuthUserRepository extends JpaRepository<WebAuthUser, Integer>, JpaSpecificationExecutor<WebAuthUser> {

    Optional<WebAuthUser> findFirstByUsername(String username);
}
