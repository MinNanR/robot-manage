package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Boss;
import site.minnan.robotmanage.entity.dao.BossRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/***
 * BOSS信息处理
 *
 * @author minnan on 2024/01/12
 */
@Component("boss")
public class BossMessageHandler implements MessageHandler {

    private BossRepository bossRepository;

    public BossMessageHandler(BossRepository bossRepository) {
        this.bossRepository = bossRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String bossName = dto.getRawMessage().substring(4).strip().toUpperCase();

        Boss boss = bossRepository.findBossByBossNickNameIs(bossName);
        if (boss != null) {
            String msg = boss.toMsg();
            return Optional.of("\n" + msg);
        } else {
            List<Boss> bossList = bossRepository.findBossesByBossNickNameLike(bossName);
            if (bossList.isEmpty()) {
                return Optional.of("无此BOSS");
            } else {
                String listBoss = bossList.stream()
                        .map(e -> StrUtil.format("{}：{}", e.getBossNickName(), e.getBossName()))
                        .collect(Collectors.joining("\n"));
                return Optional.of(listBoss);
            }
        }
    }
}
