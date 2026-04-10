package site.minnan.robotmanage.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<String> testCharacterPic(@PathVariable String region, @PathVariable String characterName, HttpServletResponse response) {
        log.info("region={},characterName={}", region, characterName);
        Optional<CharacterData> characterDataOpt = characterSupportService.queryCharacterInfoLocal(characterName, region);
        if (characterDataOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String html = characterSupportService.createCharacterHtml(characterDataOpt.get());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
