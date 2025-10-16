package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 六转核心数量计算
 *
 * @author Minnan on 2024/03/12
 */
@Component("hexa")
public class HexaMessageHandler implements MessageHandler {

    private BotSessionUtil botSessionUtil;

    @Autowired
    public void setBotSessionUtil(BotSessionUtil botSessionUtil) {
        this.botSessionUtil = botSessionUtil;
    }

    /**
     * 起源技能核心需求数量
     */
    private static final int[] USAGE_SKILL = new int[]{
            0, 0, 30, 35, 40, 45, 50, 55, 60, 65, 200,
            80, 90, 100, 110, 120, 130, 140, 150, 160, 350,
            170, 180, 190, 200, 210, 220, 230, 240, 250, 500
    };

    /**
     * 精通技能核心需求数量
     */
    private static final int[] USAGE_MASTERY = new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    };

    /**
     * 强化技能核心需求数量
     */
    private static final int[] USAGE_ENHANCE = new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    };

    /**
     * 通用技能核心需求数量
     */
    private static final int[] USAGE_COMMON = new int[]{
            0, 125, 38, 44, 50, 57, 63, 69, 75, 72, 300,
            110, 124, 138, 152, 165, 179, 193, 207, 220,
            525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750
    };

    /**
     * 各版本技能数量
     *
     * @param h
     * @param m
     * @param v
     * @param c
     */
    private record SkillCount(int h, int m, int v, int c) {
    }

    /**
     * 储存用户的六转等级
     *
     * @param h 起源等级
     * @param m 精通等级
     * @param v V技能等级
     * @param c 通用等级
     */
    private record UserHexa(List<Integer> h, List<Integer> m, List<Integer> v, List<Integer> c) {
    }

//    private static final Map<String, SkillCount> skillCountMap;
//
//    private static final Map<String, String[][]> labelVersion;

    private static final String VERSION_NEW_AGE = "newAge";

    private static final String VERSION_DREAMER = "dreamer";

    private static final String VERSION_NEXT = "next";

    private static final String VERSION_ASSEMBLE = "assemble";

    private enum Version {

        NEW_AGE("newAge", new SkillCount(1, 1, 4, 0), new String[][]{{"起源"}, {"精通"}, {"V1", "V2", "V3", "V4"}, {}}),

        DREAMER("dreamer", new SkillCount(1, 2, 4, 1), new String[][]{{"起源"}, {"精通1", "精通2"}, {"V1", "V2", "V3", "V4"}, {"通用"}}),

        NEXT("next", new SkillCount(1, 4, 4, 1), new String[][]{{"起源"}, {"精通1", "精通2", "精通3", "精通4"}, {"V1", "V2", "V3", "V4"}, {"通用"}}),

        ASSEMBLE("assemble", new SkillCount(2, 4, 4, 1), new String[][]{{"起源1", "起源2"}, {"精通1", "精通2", "精通3", "精通4"}, {"V1", "V2", "V3", "V4"}, {"通用"}});

        final String versionName;

        final SkillCount skillCount;

        final String[][] labels;

        Version(String name, SkillCount skillCount, String[][] labels) {
            this.versionName = name;
            this.skillCount = skillCount;
            this.labels = labels;
        }

        static Version getByName(String name) {
            Version[] values = values();
            for (Version value : values) {
                if (value.versionName.equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return values[0];
        }
    }

//    static {
//        skillCountMap = Map.of(
//                VERSION_NEW_AGE, new SkillCount(1, 1, 4, 0),
//                VERSION_DREAMER, new SkillCount(1, 2, 4, 1),
//                VERSION_NEXT, new SkillCount(1, 4, 4, 1),
//                VERSION_ASSEMBLE, new SkillCount(2, 4, 4, 1)
//        );
//
//        labelVersion = Map.of(
//                VERSION_NEW_AGE, new String[][]{{"起源"}, {"精通"}, {"V1", "V2", "V3", "V4"}, {}},
//                VERSION_DREAMER, new String[][]{{"起源"}, {"精通1", "精通2"}, {"V1", "V2", "V3", "V4"}, {"通用"}},
//                VERSION_NEXT, new String[][]{{"起源"}, {"精通1", "精通2", "精通3", "精通4"}, {"V1", "V2", "V3", "V4"}, {"通用"}}
//        );
//
//
//    }


        @Value("${game_version:dreamer}")
        private String gameVersion;

        /**
         * 处理消息
         *
         * @param dto
         * @return
         */
        @Override
        public Optional<String> handleMessage (MessageDTO dto){
            String message = dto.getRawMessage();
            if ("hexa".equals(message.strip())) {
                return Optional.of(tips());
            }
            String param = message.toLowerCase().replace("hexa", "").strip();
            try {
                if (param.startsWith("set")) {
                    UserHexa target = parseUserHexa(param);
                    botSessionUtil.startSession(dto.getGroupId(), dto.getSender().userId(), d -> doCalculate(d, target));
                    String reply = """
                            已设置进度目标：
                            起源：%s
                            精通：%s
                            V强化：%s
                            通用：%s,
                            请依次按格式输入当前等级
                            """.formatted(
                            target.h != null ? target.h.stream().map(String::valueOf).collect(Collectors.joining(",")) : "-",
                            target.m != null ? target.m.stream().map(String::valueOf).collect(Collectors.joining(",")) : "-",
                            target.v != null ? target.v.stream().map(String::valueOf).collect(Collectors.joining(",")) : "-",
                            target.c != null ? target.c.stream().map(String::valueOf).collect(Collectors.joining(",")) : "-"
                    );
                    return Optional.of(reply);
                } else {
                    return doCalculate(dto, maxTarget());
                }
            } catch (Exception e) {
                return Optional.of("请输入正确的计算指令" + "\n" + tips());
            }

        }

        private Optional<String> doCalculate (MessageDTO dto, UserHexa userTarget){
            String message = dto.getRawMessage();
            String param = message.toLowerCase().replace("hexa", "").strip();
            UserHexa userCurrent;
            try {
                userCurrent = parseUserHexa(param);
            } catch (Exception e) {
                return Optional.of("请输入正确的计算指令" + "\n" + tips());
            }

            Version version = Version.getByName(gameVersion);
            String[][] labels = version.labels;
            CalculatePart sgementh = executeCalculatePart(userCurrent, userTarget, UserHexa::h, USAGE_SKILL, labels[0]);
            CalculatePart sgementm = executeCalculatePart(userCurrent, userTarget, UserHexa::m, USAGE_MASTERY, labels[1]);
            CalculatePart sgementv = executeCalculatePart(userCurrent, userTarget, UserHexa::v, USAGE_ENHANCE, labels[2]);
            CalculatePart sgementc = executeCalculatePart(userCurrent, userTarget, UserHexa::c, USAGE_COMMON, labels[3]);

            String content = Stream.of(sgementh, sgementm, sgementv, sgementc)
                    .flatMap(e -> e.content.stream())
                    .collect(Collectors.joining("\n"));

            int totalSpent = Stream.of(sgementh, sgementm, sgementv, sgementc)
                    .mapToInt(e -> e.spent)
                    .sum();

            int totalNeed = Stream.of(sgementh, sgementm, sgementv, sgementc)
                    .mapToInt(e -> e.need)
                    .sum();

            String process = NumberUtil.formatPercent((double) totalSpent / totalNeed, 2);

            String totalLine = "总进度:%d/%d，还需要%d个小核，进度%s".formatted(totalSpent, totalNeed, totalNeed - totalSpent, process);

            botSessionUtil.endSession(dto.getGroupId(), dto.getSender().userId());
            return Optional.of(content + "\n" + totalLine);
        }


        private UserHexa parseUserHexa (String userInput) throws Exception {
            List<String> originLevelString = ReUtil.findAllGroup1("h(\\s?(\\d+\\s*)*)", userInput);
            List<String> masteryLevelString = ReUtil.findAllGroup1("m(\\s?(\\d+\\s*)*)", userInput);
            List<String> enhanceLevelString = ReUtil.findAllGroup1("v(\\s?(\\d+\\s*)*)", userInput);
            List<String> commonLevelString = ReUtil.findAllGroup1("c(\\s?(\\d+\\s*)*)", userInput);

            Version version = Version.getByName(gameVersion);
            SkillCount skillCount = version.skillCount;

            List<Integer> h = null;
            List<Integer> m = null;
            List<Integer> v = null;
            List<Integer> c = null;

            if (CollUtil.isNotEmpty(originLevelString)) {
                String levelString = originLevelString.get(0);
                List<Integer> levels = splitLevel(levelString);
                int step = Math.min(levels.size(), skillCount.h);
                h = levels.subList(0, step);
            }

            if (CollUtil.isNotEmpty(masteryLevelString)) {
                String levelString = masteryLevelString.get(0);
                List<Integer> levels = splitLevel(levelString);
                int step = Math.min(levels.size(), skillCount.m);
                m = levels.subList(0, step);
            }

            if (CollUtil.isNotEmpty(enhanceLevelString)) {
                String levelString = enhanceLevelString.get(0);
                List<Integer> levels = splitLevel(levelString);
                int step = Math.min(levels.size(), skillCount.v);
                v = levels.subList(0, step);
            }

            if (CollUtil.isNotEmpty(commonLevelString)) {
                String levelString = commonLevelString.get(0);
                List<Integer> levels = splitLevel(levelString);
                int step = Math.min(levels.size(), skillCount.c);
                c = levels.subList(0, step);
            }

            boolean invalidate = Stream.of(h, m, v, c)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .anyMatch(e -> e < 0 || e > 30);
            if (invalidate) {
                throw new Exception("参数输入错误");
            }
            return new UserHexa(h, m, v, c);
        }


        private UserHexa maxTarget () {
            Version version = Version.getByName(gameVersion);
            SkillCount skillCount = version.skillCount;

            List<Integer> h = Collections.nCopies(skillCount.h, 30);
            List<Integer> m = Collections.nCopies(skillCount.m, 30);
            List<Integer> v = Collections.nCopies(skillCount.v, 30);
            List<Integer> c = Collections.nCopies(skillCount.c, 30);

            return new UserHexa(h, m, v, c);
        }

        private record CalculatePart(List<String> content, Integer spent, Integer need) {
        }

        private CalculatePart executeCalculatePart (UserHexa currentInfo, UserHexa
        targetInfo, Function < UserHexa, List < Integer >> getProcess,int[] usage, String[] labels){
            List<Integer> current = getProcess.apply(currentInfo);
            List<Integer> target = getProcess.apply(targetInfo);
            if (current == null || target == null) {
                return new CalculatePart(List.of(), 0, 0);
            }
            Assert.isTrue(current.size() <= labels.length && target.size() <= labels.length);

            Iterator<Integer> currentItr = current.iterator();
            Iterator<Integer> targetItr = target.iterator();

            int idx = 0;
            List<String> result = new ArrayList<>();
            int totalNeed = 0;
            int totalSpent = 0;
            while (currentItr.hasNext() && targetItr.hasNext()) {
                Integer currentItem = currentItr.next();
                Integer targetItem = targetItr.next();
                currentItem = Math.min(currentItem, targetItem);
                String label = labels[idx];
                int need = Arrays.stream(ArrayUtil.sub(usage, 0, targetItem + 1)).sum();
                int spent = Arrays.stream(ArrayUtil.sub(usage, 0, currentItem + 1)).sum();
                totalNeed += need;
                totalSpent += spent;
                int needToMax = need - spent;
                String process = NumberUtil.formatPercent((double) spent / need, 2);
                String line = "%s: %d/%d，还需%d个小核，进度%s".formatted(label, spent, need, needToMax, process);
                result.add(line);
                idx++;
            }

            return new CalculatePart(result, totalSpent, totalNeed);
        }

        public List<Integer> splitLevel (String s){
            String[] split = s.trim().split("\\s+");
            return Arrays.stream(split)
                    .map(Integer::parseInt)
                    .toList();
        }

        public String tips () {
            return """
                    hexa h 起源等级 m 精通1等级 精通2等级 v V1等级 V2等级 V3等级 V4等级 c 通用等级
                    如果输入的技能数量少于游戏实际数量，则认为未输入的技能不参与计算
                    """;
        }
    }
