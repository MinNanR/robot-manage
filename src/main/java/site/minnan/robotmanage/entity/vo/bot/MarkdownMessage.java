package site.minnan.robotmanage.entity.vo.bot;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MarkdownMessage {

    Map<String, String> markdown;

    public static MarkdownMessage ofContent(String content) {
        MarkdownMessage mm = new MarkdownMessage();
        mm.markdown = new HashMap<>();
        mm.markdown.put("content", content);
        return mm;
    }
}
