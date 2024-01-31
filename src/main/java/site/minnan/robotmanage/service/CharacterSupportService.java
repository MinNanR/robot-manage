package site.minnan.robotmanage.service;

import jakarta.persistence.EntityNotFoundException;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.aggregate.QueryMap;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;

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

    /**
     * 查询用户今日查询某个角色的次数
     *
     * @param target 查询母包
     * @param userId 用户id
     * @return
     */
    int getQueryCount(String target, String userId);

    /**
     * 查询昵称列表
     *
     * @param dto
     * @return
     */
    ListQueryVO<Nick> getNickList(GetNickListDTO dto);

    /**
     * 查询快捷查询链接参数
     *
     * @param dto
     * @return
     */
    ListQueryVO<QueryMap> getQueryMapList(GetQueryMapListDTO dto);
}
