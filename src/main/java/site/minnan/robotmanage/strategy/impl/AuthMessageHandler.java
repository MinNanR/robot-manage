package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Auth;
import site.minnan.robotmanage.entity.dao.AuthRepository;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;
import java.util.function.Function;

/**
 * 权限消息器
 *
 * @author Minnan on 2024/02/02
 */
@Component("auth")
@Slf4j
public class AuthMessageHandler implements MessageHandler {

    private final AuthRepository authRepository;

    public AuthMessageHandler(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage().replace("#", "");
        dto.setRawMessage(message);
        if (message.startsWith("修改权限")) {
            String reply = modifyAuth(dto);
            return Optional.of(reply);
        } else if (message.startsWith("添加权限")) {
            String reply = addAuth(dto);
            return Optional.of(reply);
        } else if (message.startsWith("查询权限")) {
            String reply = referAuth(dto);
            return Optional.of(reply);
        }
        return Optional.empty();
    }

    /**
     * 修改权限，直接赋权限码
     *
     * @param dto
     * @return
     */
    private String modifyAuth(MessageDTO dto) {
        String message = dto.getRawMessage().substring(4);
        String[] paramSplit = message.split("[:：]");
        String groupId = dto.getGroupId();
        String userId = paramSplit[0];
        String authString = paramSplit[1];
        int authNumber = Integer.parseInt(authString);
        Specification<Auth> specification = (root, query, builder) -> {
            Predicate groupPredicate = builder.equal(root.get("groupId"), groupId);
            Predicate userPredicate = builder.equal(root.get("userId"), userId);
            return query.where(groupPredicate, userPredicate).getRestriction();
        };
        Optional<Auth> authOpt = authRepository.findOne(specification);

        Auth auth = authOpt.orElseGet(() -> {
            Auth newAuth = new Auth();
            newAuth.setUserId(userId);
            newAuth.setGroupId(groupId);
            return newAuth;
        });
        auth.setAuthNumber(authNumber);
        authRepository.save(auth);
        return "更新权限成功";
    }

    /**
     * 修改权限，使用或操作修改某一个权限位
     *
     * @param dto
     * @return
     */
    private String addAuth(MessageDTO dto) {
        String message = dto.getRawMessage().substring(4);
        String[] paramSplit = message.split("[:：]");
        String groupId = dto.getGroupId();
        String userId = paramSplit[0];
        String authString = paramSplit[1];
        int modifyNumber = Integer.parseInt(authString);
        Specification<Auth> specification = (root, query, builder) -> {
            Predicate groupPredicate = builder.equal(root.get("groupId"), groupId);
            Predicate userPredicate = builder.equal(root.get("userId"), userId);
            return query.where(groupPredicate, userPredicate).getRestriction();
        };
        Optional<Auth> authOpt = authRepository.findOne(specification);

        Auth auth = authOpt.orElseGet(() -> {
            Auth newAuth = new Auth();
            newAuth.setUserId(userId);
            newAuth.setGroupId(groupId);
            newAuth.setAuthNumber(0);
            return newAuth;
        });

        Integer authNumber = auth.getAuthNumber();
        if (modifyNumber > 0) {
            //修改权限码为正数时，表示添加某个位上的权限，直接相或则位新权限码
            authNumber = authNumber | modifyNumber;
        } else {
            //修改权限码为负数时，表示移除某给位上的权限，取绝对值的反码，再与原本的权限码相与得到新权限码
            //这里转成字符串再做比特填充后取反码，用字符串可以自己指定比特位数，用Integer类转的话只能做32比特转换
            //后续增加权限类型只需要修改最后一个填充长度就可以
            String newAuthBin = StrUtil.fillBefore(Integer.toBinaryString(Math.abs(modifyNumber)), '0', 9);
            //取反操作
            String newAuthRevBin = newAuthBin
                    .replace("0", "2")
                    .replace("1", "0")
                    .replace("2", "1");
            int newAuthNumber = Integer.parseInt(newAuthRevBin, 2);
            authNumber = authNumber & newAuthNumber;
        }
        auth.setAuthNumber(authNumber);
        authRepository.save(auth);
        return "更新权限成功";
    }


    private String referAuth(MessageDTO dto) {
        String userId = dto.getRawMessage().substring(4);
        String groupId = dto.getGroupId();
        Specification<Auth> specification = (root, query, builder) -> {
            Predicate groupPredicate = builder.equal(root.get("groupId"), groupId);
            Predicate userPredicate = builder.equal(root.get("userId"), userId);
            return query.where(groupPredicate, userPredicate).getRestriction();
        };
        Optional<Auth> authOpt = authRepository.findOne(specification);
        log.info(JSONUtil.toJsonStr(authOpt.get()));

        Function<Boolean, String> f = b -> b ? "有" : "无";

        int authNumber = authOpt.map(Auth::getAuthNumber).orElse(0);

        String message = """
                用户权限:
                添加问题权限：%s
                问题查询权限：%s
                问题模糊查询权限：%s
                删除问题权限：%s
                删除答案权限：%s
                BOSS复制权限：%s
                使用权限：%s
                检测维护权限：%s
                """.formatted(f.apply((authNumber & 1) != 0),
                f.apply(((authNumber >> 1) & 1) != 0),
                f.apply(((authNumber >> 2) & 1) != 0),
                f.apply(((authNumber >> 3) & 1) != 0),
                f.apply(((authNumber >> 4) & 1) != 0),
                f.apply(((authNumber >> 5) & 1) != 0),
                f.apply((((authNumber >> 6) & 1) ^ 1) != 0),
                f.apply(((authNumber >> 7) & 1) != 0));

        return message;


    }

    public static void main(String[] args) {
        Function<Boolean, String> f = b -> b ? "有" : "无";
        int authNumber = 385;
        System.out.println(f.apply((authNumber & 1) != 0));
    }
}
