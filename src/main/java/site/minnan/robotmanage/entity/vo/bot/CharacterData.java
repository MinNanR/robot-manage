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

    /**
     * 无排名信息时，使用默认排名信息
     */
    public void setRankEmpty() {
        this.serverClassRank = "-";
        this.serverLevelRank = "-";
        this.globalClassRank = "-";
        this.globalLevelRank = "-";
    }

    public String parseLegionPower() {
        return "%.2fM".formatted((float) Integer.parseInt(legionPower) / 1000000);
    }
}
