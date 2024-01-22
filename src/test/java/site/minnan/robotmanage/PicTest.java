package site.minnan.robotmanage;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.glassfish.jaxb.runtime.v2.runtime.BinderImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.config.ProxyConfig;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.net.Proxy;

@SpringBootTest(classes = BotApplication.class)
public class PicTest {

    @Autowired
    @Qualifier("query")
    MessageHandler messageHandler;

    private MessageDTO initParam(String message) {
        String jsonString = "{'raw_message': '#" + message + "', 'group_id': '667082876', 'sender': {'user_id': '978312456'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        return MessageDTO.fromJson(jsonObject);
    }

    @Test
    public void testPic() {
//        MessageDTO param = initParam("查询CoderMinnan");
//        messageHandler.handleMessage(param);
//        String characterImgUrl = "https://i.mapleranks.com/u/HFCNGJLMBEGIMKBLCIEIPKBJKLMEBMPHLJDDMGJDBBEAAKKALKPOKOKLDEBPPIDLEPBDIFJJACEAIJHEFHHADFOFDMNOHKIBFMAECGFBIEMPKCNGCOHDLJMGEABCKEBBPCGIEJMHJKCBEFLBHHNFHOOJBOFKICECNKNNHOAEHEELKBBPCDCJKPIBPOIAJKPOANCNEGDBOEBFPLPCOBKJOKBMKJECJDBPGCCMEPBGJFMPKBOIPKBGALECCCKPLNGP.png";
//        HttpUtil.createGet(characterImgUrl)
//                .setProxy(proxy);
    }

    public static void main(String[] args) {
        String characterImgUrl = "https://i.mapleranks.com/u/HFCNGJLMBEGIMKBLCIEIPKBJKLMEBMPHLJDDMGJDBBEAAKKALKPOKOKLDEBPPIDLEPBDIFJJACEAIJHEFHHADFOFDMNOHKIBFMAECGFBIEMPKCNGCOHDLJMGEABCKEBBPCGIEJMHJKCBEFLBHHNFHOOJBOFKICECNKNNHOAEHEELKBBPCDCJKPIBPOIAJKPOANCNEGDBOEBFPLPCOBKJOKBMKJECJDBPGCCMEPBGJFMPKBOIPKBGALECCCKPLNGP.png";
        Proxy proxy = new ProxyConfig().proxy();
        HttpResponse response = HttpUtil.createGet(characterImgUrl)
                .setProxy(proxy)
                .execute();

        String encode = Base64.encode(response.bodyStream());
        System.out.println(encode);
    }
}
