package site.minnan.robotmanage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.aggregate.CharacterRecord;
import site.minnan.robotmanage.service.CharacterSupportService;

@SpringBootTest(classes = BotApplication.class, properties = "spring.profiles.active=dev")
public class RecordTest {

    @Autowired
    private CharacterSupportService characterSupportService;

    @Test
    public void fetchCharacterExp(){
        characterSupportService.expDailyTask();
    }
}
