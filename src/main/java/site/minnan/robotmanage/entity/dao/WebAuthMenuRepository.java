package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import site.minnan.robotmanage.entity.aggregate.WebAuthMenu;

import java.util.List;

public interface WebAuthMenuRepository extends JpaRepository<WebAuthMenu, Integer>, JpaSpecificationExecutor<WebAuthMenu> {

    List<WebAuthMenu> findByRole(Integer role);
}
