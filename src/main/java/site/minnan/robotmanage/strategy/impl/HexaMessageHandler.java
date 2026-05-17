package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollUtil;
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
     * 起源2技能核心需求数量
     */
    private static final int[] USAGE_SKILL_ASSENT = new int[]{
            0, 100, 30, 35, 40, 45, 50, 55, 60, 65, 200,
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

    private static final String VERSION_CROWN = "crown";

    private enum Version {
        NEW_AGE("newAge", origin, M1, V1, V2, V3, V4),

        DREAMER("dreamer", origin, M1, M2, V1, V2, V3, V4, janus),

        NEXT("next", origin, M1, M2, M3, M4, V1, V2, V3, V4, janus),

        ASSEMBLE("assemble", origin, ascent, M1, M2, M3, M4, V1, V2, V3, V4, janus),

        CROWN("crown", origin, ascent, M1, M2, M3, M4, V1, V2, V3, V4, janus, hecate),
        ;
        private final String versionName;

        private final List<Skill> skillList;

        Version(String versionName, Skill... skills) {
            this.versionName = versionName;
            this.skillList = Arrays.asList(skills);
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

        private int count(SkillType skillType) {
            int count = 0;
            for (Skill skill : skillList) {
                if (skill.skillType == skillType) {
                    count++;
                }
            }
            return count;
        }

        private List<Skill> getSkill(SkillType skillType) {
            List<Skill> result = new ArrayList<>();
            for (Skill skill : skillList) {
                if (skill.skillType == skillType) {
                    result.add(skill);
                }
            }
            return result;
        }
    }

    private interface SkillTypeInterface {
        List<Integer> getTarget(UserHexa hexa);
    }

    private enum SkillType implements SkillTypeInterface {
        H() {
            @Override
            public List<Integer> getTarget(UserHexa hexa) {
                return hexa.h;
            }
        },
        M() {
            @Override
            public List<Integer> getTarget(UserHexa hexa) {
                return hexa.m;
            }
        },
        V() {
            @Override
            public List<Integer> getTarget(UserHexa hexa) {
                return hexa.v;
            }
        },
        C() {
            @Override
            public List<Integer> getTarget(UserHexa hexa) {
                return hexa.c;
            }
        }
    }


    private record Skill(String label, SkillType skillType, int[] usage) {
    }

    private static final Skill origin = new Skill("H1", SkillType.H, new int[]{
            0, 0, 30, 35, 40, 45, 50, 55, 60, 65, 200,
            80, 90, 100, 110, 120, 130, 140, 150, 160, 350,
            170, 180, 190, 200, 210, 220, 230, 240, 250, 500
    });

    private static final Skill ascent = new Skill("H2", SkillType.H, new int[]{
            0, 100, 30, 35, 40, 45, 50, 55, 60, 65, 200,
            80, 90, 100, 110, 120, 130, 140, 150, 160, 350,
            170, 180, 190, 200, 210, 220, 230, 240, 250, 500
    });

    private static final Skill M1 = new Skill("精通1", SkillType.M, new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    });

    private static final Skill M2 = new Skill("精通2", SkillType.M, new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    });

    private static final Skill M3 = new Skill("精通3", SkillType.M, new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    });

    private static final Skill M4 = new Skill("精通4", SkillType.M, new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    });

    private static final Skill V1 = new Skill("V1", SkillType.V, new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    });

    private static final Skill V2 = new Skill("V2", SkillType.V, new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    });

    private static final Skill V3 = new Skill("V3", SkillType.V, new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    });

    private static final Skill V4 = new Skill("V4", SkillType.V, new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    });

    private static final Skill janus = new Skill("通用1", SkillType.C, new int[]{
            0, 125, 38, 44, 50, 57, 63, 69, 75, 72, 300,
            110, 124, 138, 152, 165, 179, 193, 207, 220,
            525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750
    });

    private static final Skill hecate = new Skill("通用2", SkillType.C, new int[]{
            0, 125, 38, 44, 50, 57, 63, 69, 75, 72, 300,
            110, 124, 138, 152, 165, 179, 193, 207, 220,
            525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750
    });

    @Value("${game_version:assemble}")
    private String gameVersion;

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
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
            return Optional.of("请输入正确的计算指令" + "\n" + tips(Version.getByName(gameVersion)));
        }

    }

    private Optional<String> doCalculate(MessageDTO dto, UserHexa userTarget) {
        String message = dto.getRawMessage();
        String param = message.toLowerCase().replace("hexa", "").strip();
        Version version = Version.getByName(gameVersion);
        UserHexa userCurrent;
        try {
            userCurrent = parseUserHexa(param);
        } catch (Exception e) {
            return Optional.of("请输入正确的计算指令" + "\n" + tips(version));
        }

        SkillType[] types = SkillType.values();
        List<CalculatePart> sgementList = Arrays.stream(types)
                .flatMap(e -> {
                    List<Integer> current = e.getTarget(userCurrent);
                    List<Integer> target = e.getTarget(userTarget);
                    List<Skill> skillList = version.getSkill(e);
                    int length = Math.min(current.size(), target.size());
                    length = Math.min(length, skillList.size());
                    return Stream.iterate(0, i -> i + 1).limit(length)
                            .map(idx -> executeCalculatePart(current.get(idx), target.get(idx), skillList.get(idx)));
                })
                .toList();

        String content = sgementList.stream().map(e -> e.content).collect(Collectors.joining("\n"));
        int totalSpent = sgementList.stream().mapToInt(e -> e.spent).sum();
        int totalNeed = sgementList.stream().mapToInt(e -> e.need).sum();

        String process = NumberUtil.formatPercent((double) totalSpent / totalNeed, 2);

        String totalLine = "总进度:%d/%d，还需要%d个小核，进度%s".formatted(totalSpent, totalNeed, totalNeed - totalSpent, process);

        botSessionUtil.endSession(dto.getGroupId(), dto.getSender().userId());
        return Optional.of(content + "\n" + totalLine);
    }


    private UserHexa parseUserHexa(String userInput) throws Exception {
        List<String> originLevelString = ReUtil.findAllGroup1("h(\\s?(\\d+\\s*)*)", userInput);
        List<String> masteryLevelString = ReUtil.findAllGroup1("m(\\s?(\\d+\\s*)*)", userInput);
        List<String> enhanceLevelString = ReUtil.findAllGroup1("v(\\s?(\\d+\\s*)*)", userInput);
        List<String> commonLevelString = ReUtil.findAllGroup1("c(\\s?(\\d+\\s*)*)", userInput);

        Version version = Version.getByName(gameVersion);
//        SkillCount skillCount = version.skillList.size();

        List<Integer> h = new ArrayList<>();
        List<Integer> m = new ArrayList<>();
        List<Integer> v = new ArrayList<>();
        List<Integer> c = new ArrayList<>();

        if (CollUtil.isNotEmpty(originLevelString)) {
            String levelString = originLevelString.get(0);
            List<Integer> levels = splitLevel(levelString);
            int step = Math.min(levels.size(), version.count(SkillType.H));
            h = levels.subList(0, step);
        }

        if (CollUtil.isNotEmpty(masteryLevelString)) {
            String levelString = masteryLevelString.get(0);
            List<Integer> levels = splitLevel(levelString);
            int step = Math.min(levels.size(), version.count(SkillType.M));
            m = levels.subList(0, step);
        }

        if (CollUtil.isNotEmpty(enhanceLevelString)) {
            String levelString = enhanceLevelString.get(0);
            List<Integer> levels = splitLevel(levelString);
            int step = Math.min(levels.size(), version.count(SkillType.V));
            v = levels.subList(0, step);
        }

        if (CollUtil.isNotEmpty(commonLevelString)) {
            String levelString = commonLevelString.get(0);
            List<Integer> levels = splitLevel(levelString);
            int step = Math.min(levels.size(), version.count(SkillType.C));
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


    private UserHexa maxTarget() {
        Version version = Version.getByName(gameVersion);

        List<Integer> h = Collections.nCopies(version.count(SkillType.H), 30);
        List<Integer> m = Collections.nCopies(version.count(SkillType.M), 30);
        List<Integer> v = Collections.nCopies(version.count(SkillType.V), 30);
        List<Integer> c = Collections.nCopies(version.count(SkillType.C), 30);

        return new UserHexa(h, m, v, c);
    }

    private record CalculatePart(String content, Integer spent, Integer need) {
    }

    private CalculatePart executeCalculatePart(int current, int target, Skill skill) {
        int need = Arrays.stream(ArrayUtil.sub(skill.usage, 0, target + 1)).sum();
        int spent = Arrays.stream(ArrayUtil.sub(skill.usage, 0, current + 1)).sum();
        int needToMax = need - spent;
        String process = NumberUtil.formatPercent((double) spent / need, 2);
        String line = "%s: %d/%d，还需%d个小核，进度%s".formatted(skill.label, spent, need, needToMax, process);
        return new CalculatePart(line, spent, need);

    }

    public List<Integer> splitLevel(String s) {
        String[] split = s.trim().split("\\s+");
        return Arrays.stream(split)
                .map(Integer::parseInt)
                .toList();
    }

    public String tips() {
        return """
                hexa h 起源等级 m 精通1等级 精通2等级 v V1等级 V2等级 V3等级 V4等级 c 通用等级
                如果输入的技能数量少于游戏实际数量，则认为未输入的技能不参与计算
                """;
    }

    private static String tips(Version version) {
        SkillType[] types = SkillType.values();
        StringBuilder sb = new StringBuilder();
        sb.append("hexa ");
        for (SkillType type : types) {
            String typeCommand = type.name().toLowerCase();
            sb.append(typeCommand);
            List<Skill> skillList = version.getSkill(type);
            skillList.forEach(skill -> sb.append(" ").append(skill.label).append("等级"));
            sb.append(" ");
        }
        sb.append("\n").append("如果输入的技能数量少于游戏实际数量，则认为未输入的技能不参与计算");
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(tips(Version.CROWN));
    }
}
