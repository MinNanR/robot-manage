package site.minnan.robotmanage.strategy.impl;

import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.MaintainRecord;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.service.MaintainService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;

/**
 * 维护消息处理
 *
 * @author Minnan on 2024/01/18
 */
@Service("maintain")
public class MaintainMessageHandler implements MessageHandler {

    private MaintainService maintainService;

    public MaintainMessageHandler(MaintainService maintainService) {
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
        Optional<MaintainRecord> maintain = maintainService.getMaintain();
        String result = maintain.map(e -> "维护时间：%s到%s".formatted(e.getStartTime(), e.getEndTime()))
                .orElse("暂无维护公告");
        return Optional.of(result);
    }
}
