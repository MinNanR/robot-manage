package site.minnan.robotmanage.entity.vo.bot;

import cn.hutool.core.util.ReUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BotReply {

    Integer statusCode;

    Map<String, String> data;

    public static BotReply of(Integer statusCode, String msg) {
        BotReply reply = new BotReply();
        reply.statusCode = statusCode;
        Map<String, String> payload = new HashMap<>();
        msg = ReUtil.replaceAll(msg, "minnan.site:\\d+/", "minnan.site/");
        msg = ReUtil.replaceAll(msg, ",subType=0", "");
        payload.put("msg", msg);
        reply.data = payload;
        return reply;
    }
}
