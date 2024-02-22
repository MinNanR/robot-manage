package site.minnan.robotmanage.infrastructure.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.service.MaintainService;
import site.minnan.robotmanage.strategy.impl.HolidayMessageHandler;

/**
 * 定时任务调度器
 *
 * @author Minnan on 2024/01/24
 */
@Component
@Slf4j
public class Scheduler {

    private MaintainService maintainService;

    private HolidayMessageHandler holidayMessageHandler;

    public Scheduler(MaintainService maintainService, HolidayMessageHandler holidayMessageHandler) {
        this.maintainService = maintainService;
        this.holidayMessageHandler = holidayMessageHandler;
    }


    @Scheduled(cron = "0 05,20,35,50 * * * *")
    public void detectMaintain() {
        log.info("开始检测官网维护公告");
        maintainService.detectMaintain();;
        log.info("结束检测官网维护公告");
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshHoliday() {
        log.info("开始刷新假期数据");
        try {
            holidayMessageHandler.refreshHoliday();
        } catch (JsonProcessingException e) {
            log.error("刷新假期数据异常", e);
        }
        log.info("结束刷新假期数据");

    }

}
