package site.minnan.robotmanage.mysql.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import site.minnan.robotmanage.mysql.entity.CharacterTag;

import java.util.List;

public interface CharacterTagRepository extends JpaRepository<CharacterTag,Integer> {

    List<CharacterTag> findByUserIdOrderByCreateTime(String userId);

}
