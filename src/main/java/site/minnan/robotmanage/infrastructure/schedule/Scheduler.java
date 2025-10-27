package site.minnan.robotmanage.infrastructure.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.service.JmsService;
import site.minnan.robotmanage.service.MaintainService;
import site.minnan.robotmanage.service.ProxyService;
import site.minnan.robotmanage.strategy.impl.HolidayMessageHandler;

/**
 * 定时任务调度器
 *
 * @author Minnan on 2024/01/24
 */
@Component
@Slf4j
public class Scheduler {

    @Value("${schedulerEnable}")
    private boolean schedulerEnable;

    @PostConstruct
    public void log(){
        if (schedulerEnable) {
            log.info("定时任务已开启");
        } else {
            log.info("定时任务已禁用");
        }
    }


    private MaintainService maintainService;

    private HolidayMessageHandler holidayMessageHandler;

    private ProxyService proxyService;

    private final JmsService jmsService;

    @Autowired
    private CharacterSupportService characterSupportService;

    public Scheduler(MaintainService maintainService, HolidayMessageHandler holidayMessageHandler, ProxyService proxyService, JmsService jmsService) {
        this.maintainService = maintainService;
        this.holidayMessageHandler = holidayMessageHandler;
        this.proxyService = proxyService;
        this.jmsService = jmsService;
    }


    @Scheduled(cron = "0 05,20,35,50 * * * *")
    public void detectMaintain() {
        log.info("开始检测官网维护公告");
        maintainService.detectMaintainV2();
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

    @Scheduled(cron = "0 00,15,30,45 * * * *")
    public void holdProxy() {
        log.info("开始检查代理服务器状态");
        try {
            proxyService.updateProxy();
        } catch (InterruptedException e) {
            log.error("检查代理服务器状态异常", e);
        }
        log.info("结束检查代理服务器状态");
    }

//    @Scheduled(cron = "0 0 0/1 * * ? ")
//    @Scheduled(cron = "0/5 * * * * ? ")
    public void query() {
        jmsService.schedule();
    }

    @Scheduled(cron = "0 0 4,5,6 * * *")
    public void characterExp() {
        log.info("开始查询角色经验数据");
        characterSupportService.expDailyTask(0);
        log.info("结束查询角色经验数据");
    }
}
