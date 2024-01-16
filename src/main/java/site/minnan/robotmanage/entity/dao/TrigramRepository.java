package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.Trigram;

/**
 * 卦象仓库
 *
 * @author Minnan on 2024/01/16
 */
@Repository
public interface TrigramRepository extends JpaRepository<Trigram, Integer> , JpaSpecificationExecutor<Trigram> {

    /**
     * 根据索引查找卦象
     *
     * @param index
     * @return
     */
    Trigram findByIndez(Integer index);

}
