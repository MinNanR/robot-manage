package site.minnan.robotmanage.infrastructure.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.service.MaintainService;

/**
 * 定时任务调度器
 *
 * @author Minnan on 2024/01/24
 */
@Component
@Slf4j
public class Scheduler {

    private MaintainService maintainService;

    public Scheduler(MaintainService maintainService) {
        this.maintainService = maintainService;
    }


    @Scheduled(cron = "0 05,20,35,50 * * * *")
    public void detectMaintain() {
        log.info("开始检测官网维护公告");
        maintainService.detectMaintain();;
        log.info("结束检测官网维护公告");
    }


}
