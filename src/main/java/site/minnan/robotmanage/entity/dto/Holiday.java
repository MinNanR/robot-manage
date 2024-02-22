package site.minnan.robotmanage.entity.dto;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.util.Date;

public record Holiday(Date date, String name) {

    public String format(DateTime day) {
        long differ = DateUtil.betweenDay(day, date, true);
        return "距离%s还有%d天".formatted(name, differ);
    }
}
