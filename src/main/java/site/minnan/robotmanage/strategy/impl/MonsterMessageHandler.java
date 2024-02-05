package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Monster;
import site.minnan.robotmanage.entity.dao.MonsterRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 查询怪物信息
 *
 * @author Minnan on 2024/02/05
 */
@Component("monster")
public class MonsterMessageHandler implements MessageHandler {

    private MonsterRepository monsterRepository;

    public MonsterMessageHandler(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        String param = message.replaceAll("怪物", "");
        Specification<Monster> specification = ((root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("lv"), param);
            return query.where(predicate).getRestriction();
        });

        List<Monster> monsterList = monsterRepository.findAll(specification);
        if (monsterList.isEmpty()) {
            return Optional.of("无匹配等级怪物");
        }

        List<String> lines = new ArrayList<>();
        for (Monster monster : monsterList) {
            String hp = NumberUtil.decimalFormat(",###", Long.parseLong(monster.getHp()));
            String exp = NumberUtil.decimalFormat(",###", Long.parseLong(monster.getExp()));
            String monsterString = """
                    怪物名称：%s
                    等级：%s
                    血量：%s
                    经验：%s
                    出没地图：%s
                    """.formatted(monster.getName(), monster.getLv(), hp, exp, monster.getLocation());
            lines.add(monsterString);
        }

        String reply = "\n" + String.join("-----------\n", lines);

        return Optional.of(reply);
    }
}
