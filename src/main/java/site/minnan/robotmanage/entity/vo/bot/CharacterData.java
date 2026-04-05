package site.minnan.robotmanage.entity.vo.bot;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 角色信息查询结果
 *
 * @author Minnan on 2024/01/15
 */
@Getter
@Setter
public class CharacterData {

    //角色图片
    private String characterImgUrl;

    //角色名称
    private String name;

    //服务器名称
    private String server;

    //职业名称
    private String job;

    //等级
    private String level;

    //经验百分比
    private String expPercent;

    //职业区排名
    private String serverClassRank;

    //等级区排名
    private String serverLevelRank;

    //职业全服排名
    private String globalClassRank;

    //等级全服排名
    private String globalLevelRank;

    //成就值
    private String achievementPoints;

    //成就值排名
    private String achievementRank;

    //联盟等级
    private String legionLevel;

    //联盟等级排名
    private String legionRank;

    //联盟战斗力
    private String legionPower;

    //每日联盟币
    private String legionCoinsPerDay;

    //经验数据
    private List<ExpData> expData;

    //排名附近的人
    private List<CharacterData> nearRank;

    private Integer worldId;

    private String source;

    private String updateTime;

    private String queryTime;

    private Long currentExp;

    private Long expNeed;

    /**
     * 无排名信息时，使用默认排名信息
     */
    public void setRankEmpty() {
        this.serverClassRank = "-";
        this.serverLevelRank = "-";
        this.globalClassRank = "-";
        this.globalLevelRank = "-";
    }

    public void setRank(CharacterData data) {
        this.serverClassRank = data.getServerClassRank();
        this.serverLevelRank = data.getServerLevelRank();
        this.globalClassRank = data.getGlobalClassRank();
        this.globalLevelRank = data.getGlobalLevelRank();
    }


    private String formatNumber(long number) {
        if (number < 1_000_000) {
            return Long.toString(number);
        } else if (number < 1_000_000_000) {
            return "%.2fM".formatted((float) number / 1_000_000);
        } else if (number < 1_000_000_000_000L) {
            return "%.2fB".formatted((float) number / 1_000_000_000);
        }
        return "%.2fT".formatted((float) number / 1_000_000_000_000L);
    }

    public String parseLegionPower() {
        long legionPower = Long.parseLong(this.legionPower);
        return formatNumber(legionPower);
    }

    public String getCurrentExpString() {
        return formatNumber(currentExp);
    }

    public String getExpNeedString() {
        return formatNumber(expNeed);
    }
}
