package site.minnan.robotmanage.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.MaintainRecord;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.service.MaintainService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;

/**
 * 触发检查维护公告
 *
 * @author Minnan on 2024/02/02
 */
@Component("executeDetect")
@Slf4j
public class ExecuteDetectMaintainMessageHandler implements MessageHandler {

    private final MaintainService maintainService;

    public ExecuteDetectMaintainMessageHandler(MaintainService maintainService) {
        this.maintainService = maintainService;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        try {
            Optional<MaintainRecord> maintainRecordOpt = maintainService.detectMaintainV2();
            String reply = maintainRecordOpt.map(e -> "检测到新维护公告，公告id %s，维护时间为%s-%s".formatted(e.getNewsId(), e.getStartTime(), e.getEndTime()))
                    .orElse("没有检测到新的维护公告");
            return Optional.of(reply);
        } catch (Exception e) {
            log.error("检测维护公告异常", e);
            return Optional.of("没有检测到新的维护公告");
        }
    }
}
