package site.minnan.robotmanage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.service.MaintainService;

@SpringBootTest(classes = BotApplication.class)
public class MaintainTest {

    @Autowired
    private MaintainService maintainService;

    /**
     * 测试探测维护
     */
    @Test
    public void testDetect() {
        maintainService.detectMaintain();
    }

}
