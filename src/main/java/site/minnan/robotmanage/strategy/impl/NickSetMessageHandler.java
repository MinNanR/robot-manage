package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ReUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.dao.NickRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 查询设置/删除消息处理
 *
 * @author Minnan on 2024/01/16
 */
@Component("nickSet")
public class NickSetMessageHandler implements MessageHandler {

    private NickRepository nickRepository;

    public NickSetMessageHandler(NickRepository nickRepository) {
        this.nickRepository = nickRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String rawMessage = dto.getRawMessage();
        if (ReUtil.isMatch("^查询$", rawMessage)) {
            return listNick(dto);
        } else if (ReUtil.isMatch("^(查询绑定|查询设置|绑定).*", rawMessage)) {
            return nickBind(dto);
        } else if (ReUtil.isMatch("^查询删除.*", rawMessage)) {
            return nickDelete(dto);
        }
        return Optional.empty();
    }

    /**
     * 检查查询关键字合法性
     *
     * @param nickName 查询关键字
     * @return 合法返回True，不合法返回False
     */
    private boolean validateQueryNick(String nickName) {
        return !nickName.startsWith("问题") && !(nickName.startsWith("设置") | nickName.startsWith("经验"))
                && !nickName.contains("第");
    }

    /**
     * 查询关键字绑定
     *
     * @param dto
     * @return
     */
    private Optional<String> nickBind(MessageDTO dto) {
        String message = dto.getRawMessage();
        message = ReUtil.delPre("(查询设置|查询绑定|绑定)", message);
        String[] messageSplit = message.split("[:：]");
        String nickName, characterName;
        if (messageSplit.length == 1) {
            nickName = "我";
            characterName = messageSplit[0];
        } else {
            nickName = messageSplit[0];
            characterName = messageSplit[1];
        }

        if (nickName.isBlank()) {
            return Optional.of("查询关键字不能为空");
        }
        if (!validateQueryNick(nickName)) {
            return Optional.of("查询关键字包含保留用字");
        }

        String userId = dto.getSender().userId();
        Nick nick = nickRepository.findByQqAndNick(userId, nickName);
        if (nick == null) {
            nick = new Nick(userId, nickName, characterName);
        } else {
            nick.setCharacter(characterName);
        }
        nickRepository.save(nick);
        return Optional.of("设置成功");
    }

    /**
     * 查询关键字删除
     *
     * @param dto
     * @return
     */
    private Optional<String> nickDelete(MessageDTO dto) {
        String message = dto.getRawMessage();
        String content = message.replace("查询删除", "");
        String userId = dto.getSender().userId();
        Nick nick = nickRepository.findByQqAndNick(userId, content);
        if (nick == null) {
            return Optional.of("无匹配关键字");
        } else {
            nickRepository.delete(nick);
            return Optional.of("删除成功");
        }
    }

    /**
     * 关键查询
     *
     * @param dto
     * @return
     */
    private Optional<String> listNick(MessageDTO dto) {
        String userId = dto.getSender().userId();
        List<Nick> nickList = nickRepository.findByQq(userId);
        if (nickList.isEmpty()) {
            return Optional.of("暂无绑定的查询关键字");
        }
        String nickListStr = nickList.stream()
                .map(e -> "%s:%s".formatted(e.getNick(), e.getCharacter()))
                .collect(Collectors.joining("\n"));
        String result = "你已绑定以下查询关键字\n" + nickListStr;
        return Optional.of(result);
    }
}
