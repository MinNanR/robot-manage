package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 经验票计算器
 *
 * @author Minnan on 2024/09/09
 */
@Component("expTicket")
public class ExpTicketMessageHandler implements MessageHandler {

    private static final List<Integer> data = ListUtil.toList(
            299, 326, 355, 386, 422, 461, 503, 549, 600, 657,
            428, 465, 503, 546, 593, 753, 803, 856, 914, 975,
            1242, 1300, 1364, 1429, 1501, 1911, 2003, 2105, 2207, 2315,
            2836, 2865, 2896, 2933, 2965, 3784, 3826, 3879, 3924, 3971,
            5073, 5066, 5129, 5205, 5271, 6740, 6829, 6920, 7014, 7110,
            10088, 10229, 10374, 10523, 10675, 10831, 10990, 11154, 11302,
            11473, 22619, 22845, 23073, 23304, 23537, 30598, 30904, 31213,
            31525, 31840, 70685, 71392, 72106, 72827, 73555, 148581, 163439,
            179783, 197761, 217537, 439425, 483367, 531704, 584874, 643362,
            1299590, 1429549, 1572504, 1729754, 1902730, 3843513, 4227865,
            4650651, 5115716, 5627288, 11367121, 12503833, 13754216, 15129638, 22694456);

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        message = message.substring(3);
        message = message.replace("-", " ");
        String[] paramSplit = message.split("\\s+");
        int levelStart;
        int levelEnd;
        try {
            levelStart = Integer.parseInt(paramSplit[0].strip());
            if (paramSplit.length == 1) {
                levelEnd = levelStart;
            } else {
                levelEnd = Integer.parseInt(paramSplit[1].strip());
            }
        } catch (NumberFormatException e) {
            return Optional.of("请输入正确的参数");
        }

        if (levelStart < 200 || levelEnd < 200 || levelEnd > 300 || levelEnd < levelStart) {
            return Optional.of("请输入正确的参数");
        }

        int totalTicket = data.stream()
                .skip(levelStart - 200)
                .limit(Math.max(levelEnd - levelStart, 1))
                .mapToInt(e -> e)
                .sum();

        String reply;
        if (levelStart == levelEnd) {
            reply = StrUtil.format("{}级需要{}张经验票", levelStart, totalTicket);
        } else {
            reply = StrUtil.format("{}级到{}级总共需要{}张经验票", levelStart, levelEnd, totalTicket);
        }

        return Optional.of(reply);
    }
}
