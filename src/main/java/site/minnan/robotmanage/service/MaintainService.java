package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.MaintainRecord;

import java.util.Optional;

/**
 * 维护公告服务
 *
 * @author Minnan on 2024/01/24
 */
public interface MaintainService {

    /**
     * 探测维护公告
     */
    Optional<MaintainRecord> detectMaintain();

    /**
     * 查询最近一次维护时间
     *
     * @return
     */
    Optional<MaintainRecord> getMaintain();
}
