package site.minnan.robotmanage.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.service.CharacterSupportService;

import java.util.Optional;

@Controller
@RequestMapping("/robot")
@Slf4j
public class ViewController {

    @Autowired
    CharacterSupportService characterSupportService;

    @RequestMapping("/view/character/{region}/{characterName}")
    public ResponseEntity<String> testCharacterPic(@PathVariable String region, @PathVariable String characterName, @Param("template") String template) {
        log.info("region={},characterName={}", region, characterName);
        Optional<CharacterData> characterDataOpt = characterSupportService.queryCharacterInfoLocal(characterName, region, "978312456");
        if (characterDataOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        template = template == null ? "query" : template;
        String html = characterSupportService.createCharacterHtml(characterDataOpt.get(), "picTemplate/" + template);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @RequestMapping("/query/character/{region}/{characterName}")
    @ResponseBody
    public site.minnan.robotmanage.entity.response.ResponseEntity<CharacterData> getCharacterInfoLocal(@PathVariable String region,@PathVariable String characterName) {
        Optional<CharacterData> characterData = characterSupportService.queryCharacterInfoLocal(characterName, region, "");
        return characterData.map(site.minnan.robotmanage.entity.response.ResponseEntity::success)
                .orElseGet(() -> site.minnan.robotmanage.entity.response.ResponseEntity.fail("查询失败"));

    }
}
