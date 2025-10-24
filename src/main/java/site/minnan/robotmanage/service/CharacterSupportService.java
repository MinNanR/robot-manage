package site.minnan.robotmanage.service;

import jakarta.persistence.EntityNotFoundException;
import site.minnan.robotmanage.entity.aggregate.CharacterRecord;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.aggregate.QueryMap;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.dto.UpdateQueryMapDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;

import java.util.Optional;

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
     * 查询角色信息
     * @param queryName 查询角色名称
     * @param server 服务器
     * @return
     */
    CharacterData fetchCharacterInfo(String queryName, String server);

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

    /**
     * 修改快捷查询
     *
     * @param dto
     */
    void updateQueryMap(UpdateQueryMapDTO dto);

    /**
     * 添加快捷查询
     *
     * @param dto
     */
    void addQueryMap(UpdateQueryMapDTO dto);

    /**
     * 查询经验每日任务
     */
    void expDailyTask();

    Optional<CharacterData> queryCharacterInfoLocal(String queryName, String region);

    /**
     * 初始化角色信息
     *
     * @param queryName
     * @param region
     */
    void initCharacter(String queryName, String region);
}
