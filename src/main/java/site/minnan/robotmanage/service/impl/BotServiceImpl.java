package site.minnan.robotmanage.service.impl;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.BotReply;
import site.minnan.robotmanage.service.BotService;
import site.minnan.robotmanage.strategy.MessageHandler;
import site.minnan.robotmanage.strategy.MessageHandlerSupportService;

import java.util.Optional;

/**
 * 机器人处理类
 *
 * @author Minnan on 2024/01/11
 */
@Service
@Slf4j
public class BotServiceImpl implements BotService {

    MessageHandlerSupportService messageHandlerSupportService;

    public BotServiceImpl(MessageHandlerSupportService messageHandlerSupportService) {
        this.messageHandlerSupportService = messageHandlerSupportService;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public BotReply handleMessage(MessageDTO dto) {
        MessageHandler messageHandler = messageHandlerSupportService.judgeMessageHandler(dto);
        Optional<String> resultOption = Optional.empty();
        try {
            resultOption = messageHandler.handleMessage(dto);
        } catch (Exception e) {
            log.info("处理消息异常", e);
        }
        String message = resultOption.orElse(fallBackMessage());
        return BotReply.of(0, message);
    }

    /**
     * 默认返回的消息
     *
     * @return
     */
    private String fallBackMessage() {
        return "[CQ:image,file=https://minnan.site:2005/rot/678f889fb8aace1c7e3f5e6cbce5b7b6.png,subType=0]";
    }
}
