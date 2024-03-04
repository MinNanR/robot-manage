package site.minnan.robotmanage.entity.vo.bot;

public record StrategyUsageCount(String strategyName, Integer count) {

    public String formatted(){
        return strategyName + ":" + count + "次";
    }
}
