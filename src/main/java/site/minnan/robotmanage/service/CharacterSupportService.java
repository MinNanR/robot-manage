package site.minnan.robotmanage.service;

import jakarta.persistence.EntityNotFoundException;
import site.minnan.robotmanage.entity.vo.CharacterData;

/**
 * 角色信息相关服务
 *
 * @author Minna on 2024/01/15
 */
public interface CharacterSupportService {

    /**
     * 查询角色信息
     *
     * @param queryName 查询角色名称
     * @return
     */
    CharacterData fetchCharacterInfo(String queryName);

    /**
     * 解析查询目标
     *
     * @param dto
     * @return
     */
    String parseQueryContent(String queryContent, String userId);

    /**
     * 排名查询角色
     *
     * @param queryMessage
     * @return
     */
    String rankQueryCharacter(String queryMessage) throws EntityNotFoundException;
}
