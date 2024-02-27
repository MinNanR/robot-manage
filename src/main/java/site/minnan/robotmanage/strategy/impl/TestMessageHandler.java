package site.minnan.robotmanage.strategy.impl;

import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.SendMessageDTO;
import site.minnan.robotmanage.service.BotService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;

/**
 * 测试消息数据
 */
@Component("test")
public class TestMessageHandler implements MessageHandler {

    private BotService botService;

    public TestMessageHandler(BotService botService) {
        this.botService = botService;
    }


    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        new Thread(() -> {
            SendMessageDTO sendMessageDTO = new SendMessageDTO(dto, "异步回复消息");
            botService.sendAsyncMessage(sendMessageDTO);
        }).start();
        return Optional.of("");
    }
}
