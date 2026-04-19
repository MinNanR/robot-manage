package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.dto.CharacterQueryDTO;
import site.minnan.robotmanage.entity.enumeration.World;
import site.minnan.robotmanage.entity.response.ResponseEntity;

public interface CharacterRankService {

    void flushRank();

    void flushRank(World world);

    ResponseEntity<?> getCharacterList(CharacterQueryDTO dto);
}
