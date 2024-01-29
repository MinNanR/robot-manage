package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 吃点什么消息处理
 *
 * @author Minnan on 2024/01/17
 */
@Component("menu")
public class MenuMessageHandler implements MessageHandler {


    public MenuMessageHandler(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    private RedisUtil redisUtil;

    public static final String LAUNCHER_KEY = "launcher";

    private static final String[] defaultMenu;

    static {
        defaultMenu = new String[]{"盖浇饭", "砂锅", "大排档", "米线", "满汉全席", "西餐", "麻辣烫", "自助餐", "炒面", "快餐", "水果",
                "西北风", "馄饨", "火锅", "烧烤", "泡面", "速冻水饺", "日本料理", "涮羊肉", "味千拉面", "肯德基",
                "面包", "扬州炒饭", "自助餐", "茶餐厅", "海底捞", "比萨", "麦当劳", "兰州拉面", "沙县小吃",
                "烤鱼", "海鲜", "铁板烧", "韩国料理", "粥", "快餐", "东南亚菜", "甜点", "农家菜", "川菜",
                "粤菜", "湘菜", "本帮菜", "竹笋烤肉"};
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
        message = message.replace("吃点什么", "").replace("今晚吃什么", "").strip();
        String[] menu = message.split(" ");
        menu = Arrays.stream(menu).filter(e -> !e.isBlank()).toArray(String[]::new);
        if (menu.length == 0) {
            menu = defaultMenu;
        }

        JSONObject launcher = new JSONObject();
        if (redisUtil.hasKey(LAUNCHER_KEY)) {
            String launcherStr = (String) redisUtil.getValue(LAUNCHER_KEY);
            launcher = JSONUtil.parseObj(launcherStr);
            int shitCountDown = launcher.getInt("shit_count_down");
            int windCountDown = launcher.getInt("wind_count_down");
            if (shitCountDown == 0) {
                menu = new String[]{"屎"};
                launcher.set("shit_count_down", RandomUtil.randomInt(20, 30));
            } else if ((windCountDown == 0)) {
                menu = new String[]{"西北风"};
                launcher.set("wind_count_down", RandomUtil.randomInt(10, 20));
            }
            launcher.set("shit_count_down", launcher.getInt("shit_count_down") - 1);
            launcher.set("wind_count_down", launcher.getInt("wind_count_down") - 1);
        } else {
            launcher.set("shit_count_down", RandomUtil.randomInt(20, 30));
            launcher.set("wind_count_down", RandomUtil.randomInt(10, 20));
        }

        redisUtil.valueSet(LAUNCHER_KEY, JSONUtil.toJsonStr(launcher), Duration.ofMinutes(10));

        String item = RandomUtil.randomEle(menu);
        String reply = "今晚吃%s！".formatted(item);
        return Optional.of(reply);
    }

}
