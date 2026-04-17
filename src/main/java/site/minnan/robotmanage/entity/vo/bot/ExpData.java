package site.minnan.robotmanage.entity.vo.bot;

/**
 * 经验数据
 *
 * @author Minnan on 2024/01/15
 */
public record ExpData(String dateLabel, Long expDifference, Double expProcess) {

    //格式化输出经验数值
    public String formatExpDifference() {
        return String.format("%.4fb", (float) expDifference / 1000000000);
    }

    public String formatExpDifferenceShort() {
        if (expDifference < 1_000_000) {
            return Long.toString(expDifference);
        } else if (expDifference < 1_000_000_000) {
            return "%.2fM".formatted((float) expDifference / 1_000_000);
        } else if (expDifference < 1_000_000_000_000L) {
            return "%.2fB".formatted((float) expDifference / 1_000_000_000);
        }
        return "%.2fT".formatted((float) expDifference / 1_000_000_000_000L);
    }
}
