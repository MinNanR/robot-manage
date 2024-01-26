package site.minnan.robotmanage.entity.vo.bot;

/**
 * 经验数据
 *
 * @author Minnan on 2024/01/15
 */
public record ExpData(String dateLabel, Long expDifference) {

    //格式话输出经验数值
    public String formatExpDifference() {
        return String.format("%.4fb", (float) expDifference / 1000000000);
    }
}
