package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.date.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.mysql.dao.CharacterExpDailyMysqlRepository;
import site.minnan.robotmanage.mysql.dao.CharacterRecordMysqlRepository;
import site.minnan.robotmanage.mysql.dao.CharacterTagRepository;
import site.minnan.robotmanage.mysql.entity.CharacterRecordMysql;
import site.minnan.robotmanage.mysql.entity.CharacterTag;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component("tag")
public class TagMessageHandler implements MessageHandler {

    @Autowired
    private CharacterTagRepository characterTagRepository;

    @Autowired
    private CharacterRecordMysqlRepository characterRecordMysqlRepository;


    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String userId = dto.getSender().userId();

        String rawMessage = dto.getRawMessage();
        String[] paramSplit = rawMessage.split("[:：]");
        String command =  paramSplit[0];
        String targetName = paramSplit[1].strip();
        String server = (String) dto.getPayload().getOrDefault("server", "u");
        String region = "u".equals(server) ? "na" : "eu";
        targetName = targetName.toLowerCase();
        Optional<CharacterRecordMysql> tagTargetOpt = characterRecordMysqlRepository.findFirstByCharacterNameIgnoreCaseAndRegion(targetName, region);
        if (tagTargetOpt.isEmpty()) {
            return Optional.of("角色不存在");
        }
        CharacterRecordMysql tagTarget = tagTargetOpt.get();

        List<CharacterTag> tagList = characterTagRepository.findByUserIdOrderByCreateTime(userId);

        Integer tagTargetId = tagTarget.getId();
        Optional<CharacterTag> existOpt = tagList.stream().filter(e -> Objects.equals(e.getCharacterId(), tagTargetId)).findFirst();
        if (existOpt.isPresent()){
            if (command.equals("tag")) {
                CharacterTag currentTag = existOpt.get();
                currentTag.setCreateTime(DateTime.now().toLocalDateTime());
                characterTagRepository.save(currentTag);
                return Optional.of("角色%s已添加到关注列表".formatted(tagTarget.getCharacterName()));
            } else if (command.equals("untag")) {
                characterTagRepository.delete(existOpt.get());
                return Optional.of("已将角色%s移出关注列表".formatted(tagTarget.getCharacterName()));
            }
        }

        if (tagList.size() >= 5) {
            CharacterTag oldestTag = tagList.get(0);
            characterTagRepository.delete(oldestTag);
        }
        CharacterTag tag = new CharacterTag();
        tag.setUserId(userId);
        tag.setCharacterId(tagTargetId);
        tag.setCreateTime(DateTime.now().toLocalDateTime());
        characterTagRepository.save(tag);
        return Optional.of("角色%s已添加到关注列表".formatted(tagTarget.getCharacterName()));
    }
}
