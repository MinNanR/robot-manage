package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dto.GetStrategyListDTO;
import site.minnan.robotmanage.entity.dto.ModifyStrategyOrdinalDTO;
import site.minnan.robotmanage.entity.dto.UpdateStrategyDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;

import java.util.List;

/**
 * 消息处理策略服务
 *
 * @author Minnan on 2024/01/29
 */
public interface StrategyService {

    /**
     * 获取处理消息组件下拉框
     *
     * @return
     */
    List<String> getComponentDropDown();

    /**
     * 查询消息处理策略列表
     *
     * @return
     */
    ListQueryVO<HandlerStrategy> getStrategyList(GetStrategyListDTO dto);

    /**
     * 修改策略配置
     *
     * @param dto
     */
    void modifyStrategy(UpdateStrategyDTO dto);

    /**
     * 设置策略启用或停用
     *
     * @param dto
     */
    void updateStrategyEnable(UpdateStrategyDTO dto);

    /**
     * 修改策略排序
     *
     * @param dto
     */
    void modifyStrategyOrdinal(ModifyStrategyOrdinalDTO dto);

    /**
     * 添加消息处理策略
     *
     * @param strategy
     */
    void addStrategy(HandlerStrategy strategy);
}
