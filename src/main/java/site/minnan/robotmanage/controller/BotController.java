package site.minnan.robotmanage.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.bot.BotReply;
import site.minnan.robotmanage.service.BotService;

import java.io.IOException;

/**
 * 群聊机器人接收类
 *
 * @author Minnan on 2024/01/11
 */
@RestController
@RequestMapping("/badeling_bot")
@Slf4j
public class BotController {

    public BotController(BotService botService) {
        this.botService = botService;
    }

    private final BotService botService;

    @PostMapping("onMessage")
    public BotReply onMessage(HttpServletRequest request) {
        try (ServletInputStream inputStream = request.getInputStream()) {
            byte[] bodyBytes = IoUtil.readBytes(inputStream);
            String jsonString = new String(bodyBytes);
            log.info(jsonString);
            JSONObject jsonObject = JSONUtil.parseObj(jsonString);
            MessageDTO messageDTO = MessageDTO.fromJson(jsonObject);
            log.info("收到群号 [{}] 发送者[{}]的消息,消息id [{}], 消息内容 [{}]", messageDTO.getGroupId(),
                    messageDTO.getSender().userId(), messageDTO.getMessageId(), messageDTO.getRawMessage());
            BotReply reply = botService.handleMessage(messageDTO);
            String replyMsg = reply.getData().get("msg");
            log.info("处理消息[{}]结束，返回消息：{}", messageDTO.getMessageId(), replyMsg);
            return reply;
        } catch (IOException e) {
            log.error("读取request输入流异常", e);
            return BotReply.of(0, "民难科技内部错误");
        }
    }

}
