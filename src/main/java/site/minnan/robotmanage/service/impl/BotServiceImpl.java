package site.minnan.robotmanage.service.impl;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.SendMessageDTO;
import site.minnan.robotmanage.entity.vo.bot.BotReply;
import site.minnan.robotmanage.service.BotService;
import site.minnan.robotmanage.strategy.MessageHandler;
import site.minnan.robotmanage.strategy.MessageHandlerSupportService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 机器人处理类
 *
 * @author Minnan on 2024/01/11
 */
@Service
@Slf4j
public class BotServiceImpl implements BotService {

    private MessageHandlerSupportService messageHandlerSupportService;

    @Value("${sendMessageUrl}")
    private String sendMessageUrl;

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
        String message = resultOption.orElseGet(this::fallBackMessage);
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

    /**
     * 发送异步回复消息
     *
     * @param dto
     */
    @Override
    public void sendAsyncMessage(SendMessageDTO dto) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String sendContent = objectMapper.writeValueAsString(dto);
            log.info("发送异步消息，{}", sendContent);

            HttpRequest request = HttpUtil.createPost(sendMessageUrl)
                    .body(sendContent)
                    .header(Header.CONTENT_ENCODING, "utf-8")
                    .header(Header.CONTENT_TYPE, "application/json");
            HttpResponse response = request.execute();
            String responseBody = response.body();
            log.info("发送异步消息响应，{}", responseBody);
        } catch (JsonProcessingException e) {
            log.error("发送异步回复消息时序列化异常", e);
        } catch (Exception e) {
            log.error("调用发送异步消息接口异常", e);
        }
    }

    public static void main(String[] args) {
//        String messageJsonString = "{\"raw_message\":\"# 1\",\"group_id\":\"348273823\",\"sender\":{\"user_id\":\"978312456\",\"open_id\":\"314A0F32D962FF4255D2C4244ABAAC67\"},\"message_id\":\"246043\"}";
//        JSONObject messageJson = JSONUtil.parseObj(messageJsonString);
//        MessageDTO messageDTO = MessageDTO.fromJson(messageJson);
//        SendMessageDTO dto = new SendMessageDTO(messageDTO, "发送异步回复消息");
//        BotServiceImpl botService = new BotServiceImpl(null);
//        botService.sendMessageUrl = "http://lynn.badeling.site/msg/sendLynnMsg";
//        botService.sendAsyncMessage(dto);
        System.out.println(Charset.defaultCharset());
    }
}
