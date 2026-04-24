package site.minnan.robotmanage.infrastructure.utils;

import ch.obermuhlner.math.big.BigDecimalMath;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Flame calculator.
 *
 * @author Minnan on 2026/04/23
 */
public class FlameCalculator {

    private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;

    public record ScoreTransfer(double att, double all) {
    }

    /**
     * 火花属性值计算接口
     */
    private interface StatBase {
        /**
         * 获得每1T级火花的数值
         * @param level 装备等级
         * @return
         */
        default int base(int level) {
            return 1;
        }
    }

    /**
     * 火花属性枚举/词条种类枚举
     */
    private enum FlameStat implements StatBase {
        // single stat
        SINGLE_STAT() {
            @Override
            public int base(int level) {
                return switch (level) {
                    case 250 -> 13;
                    case 200 -> 11;
                    case 160 -> 9;
                    case 150, 140 -> 8;
                    default -> 0;
                };
            }
        },
        // double stat
        DOUBLE_STAT_1() {
            @Override
            public int base(int level) {
                return switch (level) {
                    case 250 -> 7;
                    case 200 -> 6;
                    case 160 -> 5;
                    case 150, 140 -> 4;
                    default -> 0;
                };
            }
        },
        DOUBLE_STAT_2() {
            @Override
            public int base(int level) {
                return DOUBLE_STAT_1.base(level);
            }
        },
        DOUBLE_STAT_3() {
            @Override
            public int base(int level) {
                return DOUBLE_STAT_1.base(level);
            }
        },
        // all stat
        ALL(),
        // attack
        ATT()
    }

    /**
     * 记录一条火花
     * @param stat 词条种类
     * @param tier T级
     * @param tierIndex T级索引
     */
    private record FlameLine(FlameStat stat, int tier, int tierIndex) {
        /**
         * 计算这一条火花的得分
         * @param itemLevel 装备等级
         * @param transfer  att和all的换算
         * @return 得分
         */
        private int score(int itemLevel, ScoreTransfer transfer) {
            int baseScore = stat.base(itemLevel) * tier;
            return switch (stat) {
                case SINGLE_STAT, DOUBLE_STAT_1, DOUBLE_STAT_2, DOUBLE_STAT_3 -> baseScore;
                case ATT -> (int) Math.ceil(baseScore * transfer.att);
                case ALL -> (int) Math.ceil(baseScore * transfer.all);
            };
        }
    }

    /**
     * Flame property definition.
     *
     * @param tiers available tiers
     * @param rates probability for each tier
     */
    public record FlameProp(int[] tiers, BigDecimal[] rates, String label) {
    }

    /** 紫火：abyss flame*/
    public static final FlameProp ABYSS_FLAME = new FlameProp(
            new int[]{4, 5, 6},
            new BigDecimal[]{new BigDecimal("0.63"), new BigDecimal("0.34"), new BigDecimal("0.3")},
            "紫火"
    );

    /** 彩火/黑火：eternal flame */
    public static final FlameProp ETERNAL_FLAME = new FlameProp(
            new int[]{4, 5, 6, 7},
            new BigDecimal[]{new BigDecimal("0.29"), new BigDecimal("0.45"), new BigDecimal("0.25"), new BigDecimal("0.01")},
            "彩火/黑火"
    );

    /** 红火：powerful flame */
    public static final FlameProp POWERFUL_FLAME = new FlameProp(
            new int[]{3, 4, 5, 6},
            new BigDecimal[]{new BigDecimal("0.2"), new BigDecimal("0.3"), new BigDecimal("0.36"), new BigDecimal("0.14")},
            "红火"
    );



    public record Result(int avg, int median, int P75, int P85, int P95) {
    }

    public static final Result INFEASIBLE = new Result(Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);


    /** selected flame property */
    private final FlameProp flameProp;

    /** item level */
    private final int itemLevel;

    /** score transfer */
    private final ScoreTransfer scoreTransfer;

    /** score table for each stat/tier pair */
    private final int[][] statScores;

    /** upper bound for each stat, used by pruning */
    private final int[] maxScoreByStat;

    /** best possible remaining score for each selected-state mask */
    private final int[] maxRemainingScoreByMask;

    /** memoized search cache */
    private final Map<StateKey, BigDecimal> memo = new HashMap<>();

    public FlameCalculator(FlameProp flameProp, int itemLevel, ScoreTransfer scoreTransfer) {
        this.flameProp = flameProp;
        this.itemLevel = itemLevel;
        this.scoreTransfer = scoreTransfer;
        this.statScores = buildStatScores();
        this.maxScoreByStat = buildMaxScoreByStat();
        this.maxRemainingScoreByMask = buildMaxRemainingScoreByMask();
    }

    /**
     * Calculate the average attempts required for the target score.
     *
     * @param score target score
     * @return average attempts
     */
    public Result process(int score) {
        //检查目标是否大于该道具的最大值，maxRemainingScoreByMask[0]为该道具的理论最大值，如果大于该值则不用计算
        if (score > maxRemainingScoreByMask[0]) {
            return INFEASIBLE;
        }

        BigDecimal rate = BigDecimal.ZERO;
        memo.clear();

        for (FlameStat flameStat : FlameStat.values()) {
            int statIndex = flameStat.ordinal();
            for (int tierIndex = 0; tierIndex < flameProp.tiers.length; tierIndex++) {
                int currentScore = statScores[statIndex][tierIndex];
                BigDecimal baseRate = BigDecimal.ONE.divide(BigDecimal.valueOf(19), CALCULATION_CONTEXT)
                        .multiply(flameProp.rates[tierIndex], CALCULATION_CONTEXT);
                if (currentScore >= score) {
                    rate = rate.add(baseRate);
                    continue;
                }
                int selectedMask = 1 << statIndex;
                BigDecimal suffixRate = doCalculate(selectedMask, currentScore, score);
                if (suffixRate.signum() > 0) {
                    rate = rate.add(baseRate.multiply(suffixRate, CALCULATION_CONTEXT));
                }
            }
        }

        int avg = BigDecimal.ONE.divide(rate, CALCULATION_CONTEXT).intValue();
        BigDecimal base = BigDecimalMath.log(BigDecimal.ONE.subtract(rate), CALCULATION_CONTEXT);
        int median = BigDecimalMath.log(BigDecimal.valueOf(0.5), CALCULATION_CONTEXT)
                .divide(base, CALCULATION_CONTEXT).setScale(0, RoundingMode.CEILING)
                .intValueExact();
        int p75 = BigDecimalMath.log(BigDecimal.valueOf(1 - 0.75), CALCULATION_CONTEXT)
                .divide(base, CALCULATION_CONTEXT).setScale(0, RoundingMode.CEILING)
                .intValueExact();
        int p85 = BigDecimalMath.log(BigDecimal.valueOf(1 - 0.85), CALCULATION_CONTEXT)
                .divide(base, CALCULATION_CONTEXT).setScale(0, RoundingMode.CEILING)
                .intValueExact();
        int p95 = BigDecimalMath.log(BigDecimal.valueOf(1 - 0.95), CALCULATION_CONTEXT)
                .divide(base, CALCULATION_CONTEXT).setScale(0, RoundingMode.CEILING)
                .intValueExact();
        return new Result(avg, median, p75, p85, p95);
    }

    private BigDecimal doCalculate(int selectedMask, int currentScore, int targetScore) {
        int currentLineCount = Integer.bitCount(selectedMask);
        if (currentScore >= targetScore) {
            return BigDecimal.ONE;
        }
        if (currentLineCount == 4) {
            return BigDecimal.ZERO;
        }
        if (currentScore + maxRemainingScoreByMask[selectedMask] < targetScore) {
            return BigDecimal.ZERO;
        }

        StateKey key = new StateKey(selectedMask, currentScore);
        BigDecimal cached = memo.get(key);
        if (cached != null) {
            return cached;
        }

        BigDecimal result = BigDecimal.ZERO;
        BigDecimal lineRate = BigDecimal.ONE.divide(BigDecimal.valueOf(19 - currentLineCount), CALCULATION_CONTEXT);
        for (FlameStat flameStat : FlameStat.values()) {
            int statIndex = flameStat.ordinal();
            if ((selectedMask & (1 << statIndex)) != 0) {
                continue;
            }
            int nextMask = selectedMask | (1 << statIndex);
            for (int tierIndex = 0; tierIndex < flameProp.tiers.length; tierIndex++) {
                int nextScore = currentScore + statScores[statIndex][tierIndex];
                if (nextScore < targetScore && nextScore + maxRemainingScoreByMask[nextMask] < targetScore) {
                    continue;
                }
                BigDecimal nextRate = lineRate.multiply(flameProp.rates[tierIndex], CALCULATION_CONTEXT);
                if (nextScore >= targetScore) {
                    result = result.add(nextRate);
                    continue;
                }
                BigDecimal suffixRate = doCalculate(nextMask, nextScore, targetScore);
                if (suffixRate.signum() > 0) {
                    result = result.add(nextRate.multiply(suffixRate, CALCULATION_CONTEXT));
                }
            }
        }

        memo.put(key, result);
        return result;
    }

    private int[][] buildStatScores() {
        FlameStat[] flameStats = FlameStat.values();
        int[][] scores = new int[flameStats.length][flameProp.tiers.length];
        for (FlameStat flameStat : flameStats) {
            int statIndex = flameStat.ordinal();
            for (int tierIndex = 0; tierIndex < flameProp.tiers.length; tierIndex++) {
                FlameLine line = new FlameLine(flameStat, flameProp.tiers[tierIndex], tierIndex);
                scores[statIndex][tierIndex] = line.score(itemLevel, scoreTransfer);
            }
        }
        return scores;
    }

    private int[] buildMaxScoreByStat() {
        int[] scores = new int[FlameStat.values().length];
        for (FlameStat flameStat : FlameStat.values()) {
            int statIndex = flameStat.ordinal();
            for (int tierIndex = 0; tierIndex < flameProp.tiers.length; tierIndex++) {
                scores[statIndex] = Math.max(scores[statIndex], statScores[statIndex][tierIndex]);
            }
        }
        return scores;
    }

    private int[] buildMaxRemainingScoreByMask() {
        int totalMasks = 1 << FlameStat.values().length;
        int[] scores = new int[totalMasks];
        for (int mask = 0; mask < totalMasks; mask++) {
            scores[mask] = calculateMaxRemainingScore(mask);
        }
        return scores;
    }

    private int calculateMaxRemainingScore(int selectedMask) {
        int remainingSlots = 4 - Integer.bitCount(selectedMask);
        if (remainingSlots <= 0) {
            return 0;
        }

        int[] candidates = new int[maxScoreByStat.length];
        int size = 0;
        for (int statIndex = 0; statIndex < maxScoreByStat.length; statIndex++) {
            if ((selectedMask & (1 << statIndex)) == 0) {
                candidates[size++] = maxScoreByStat[statIndex];
            }
        }

        Arrays.sort(candidates, 0, size);
        int maxRemainingScore = 0;
        for (int i = size - 1; i >= 0 && remainingSlots > 0; i--, remainingSlots--) {
            maxRemainingScore += candidates[i];
        }
        return maxRemainingScore;
    }

    private record StateKey(int selectedMask, int currentScore) {
    }

    public static void main(String[] args) {
        ScoreTransfer transfer = new ScoreTransfer(3, 12);
        FlameCalculator f = new FlameCalculator(ETERNAL_FLAME, 250, transfer);
        f.process((int) Math.ceil(130 + 5 * transfer.all()));
    }
}
