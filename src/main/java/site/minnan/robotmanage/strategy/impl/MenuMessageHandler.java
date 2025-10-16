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
import java.util.concurrent.locks.ReentrantLock;

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

    private static final String[] drinkingMenu;

    static {
        defaultMenu = new String[]{"盖浇饭", "砂锅", "大排档", "米线", "牛排", "麻辣烫", "自助餐", "炒面", "隆江猪脚饭",
                "西北风", "馄饨", "火锅", "烧烤", "泡面", "速冻水饺", "寿司", "涮羊肉", "兰州拉面", "肯德基",
                "面包", "扬州炒饭", "自助餐", "茶餐厅", "海底捞", "比萨", "麦当劳", "兰州拉面", "沙县小吃",
                "烤鱼", "海鲜", "铁板烧", "韩国料理", "粥", "快餐", "东南亚菜", "甜点", "农家菜", "川菜",
                "粤菜", "湘菜", "本帮菜", "竹笋烤肉",  "华莱士", "咖喱鸡", "冒菜"};
        drinkingMenu = new String[]{"可乐", "雪碧", "芬达", "茶π", "阿萨姆", "维他柠檬茶", "喜茶-水牛乳双拼抹茶", "喜茶-水牛乳双拼波波",
                "喜茶-烤黑糖波波真乳茶", "喜茶-黑糖波波牛乳", "喜茶-烤黑糖波波真牛乳", "喜茶-小奶茉", "喜茶-芝芝多肉葡萄", "喜茶-芝芝芒芒",
                "霸王茶姬-白雾红尘", "霸王茶姬-桂馥兰香", "霸王茶姬-伯牙绝弦", "霸王茶姬-万里木兰", "霸王茶姬-山野栀子", "霸王茶姬-花田乌龙",
                "霸王茶姬-木兰辞", "霸王茶姬-折桂令", "霸王茶姬-云中绿", "霸王茶姬-醉红袍", "奈雪-金色山脉珍珠奶茶", "奈雪-森林观音奶茶",
                "奈雪-芋泥芋圆奶茶", "奈雪-黑糖珍珠鲜奶茶", "奈雪-霸气芝士草莓", "奈雪-霸气葡萄", "奈雪-霸气杨枝甘露", "古茗-桂花酒酿小丸紫",
                "古茗-云岭茉莉白", "古茗-百香芒芒冰柚茶", "古茗-生椰茶麻薯", "古茗-布蕾脆脆奶芙", "古茗-超A芝士葡萄", "古茗-茉莉奶绿",
                "茶百道-盐盐冰淇淋乌龙", "茶百道-双拼冰淇淋红茶", "茶百道-抹茶奶布丁", "茶百道-招牌芋圆奶茶", "茶百道-豆乳玉麒麟",
                "茶百道-黑糖珍珠奶茶", "茶百道-奥利奥奶茶", "茶百道-轻轻红茶", "蜜雪冰城-冰鲜柠檬水", "蜜雪冰城-珍珠奶茶", "蜜雪冰城-棒打鲜橙",
                "蜜雪冰城-满杯百香果", "蜜雪冰城-芋圆葡萄", "蜜雪冰城-草莓啵啵", "蜜雪冰城-桑葚梅梅", "蜜雪冰城-布丁奶茶", "coco-珍珠奶茶",
                "coco-茉香奶绿", "coco-奶茶吨吨桶", "coco-奶茶三兄弟", "coco-星空葡萄", "coco-鲜芋牛奶",
                "KOI-黄金珍奶", "KOI-饼干奶茶", "KOI-布蕾咸芝士奶绿", "KOI-比利时酥酪奶茶", "KOI-水蜜桃乳酪奶绿", "KOI-水蜜桃果茶"};
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

        if ("吃喝".equals(message)) {
            String food = RandomUtil.randomEle(defaultMenu);
            String drinking = RandomUtil.randomEle(drinkingMenu);
            String reply = "今晚吃%s,喝%s".formatted(food, drinking);
            return Optional.of(reply);
        }

        message = message
                .replace("吃点什么", "")
                .replace("今晚吃什么", "")
                .replace("喝点什么", "")
                .replace("今晚喝什么", "")
                .strip();
        String[] menu = message.split(" ");
        menu = Arrays.stream(menu).filter(e -> !e.isBlank()).toArray(String[]::new);
        String verb = "吃";
        if (menu.length == 0) {
            if (dto.getRawMessage().contains("喝")) {
                menu = drinkingMenu;
                verb = "喝";
            } else {
                menu = defaultMenu;
            }
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

        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.tryLock();
        redisUtil.valueSet(LAUNCHER_KEY, JSONUtil.toJsonStr(launcher), Duration.ofMinutes(10));

        String item = RandomUtil.randomEle(menu);
        String reply = "今晚%s%s！".formatted(verb, item);
        return Optional.of(reply);
    }

}
