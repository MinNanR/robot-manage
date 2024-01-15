package site.minnan.robotmanage.infrastructure.cachekeygen;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;

import java.lang.reflect.Method;

@Component("DivinateKeyGenerator")
public class DivinateKeyGenerator implements KeyGenerator {


    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0) {
            return SimpleKey.EMPTY;
        }
        String today = DateTime.now().toString("yyyyMMdd");
        Object param = params[0];
        if (param instanceof MessageDTO dto) {
            String userId = dto.getSender().userId();
            return StrUtil.format("{}:{}", today, userId);
        }
        return "";
    }
}
