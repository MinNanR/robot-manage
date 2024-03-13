package site.minnan.robotmanage.service.impl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.service.ProxyService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 维护代理
 *
 * @author Minnan on 2024/03/13
 */
@Service
@Slf4j
public class ProxyServiceImpl implements ProxyService {

    @Value("${proxyUrlPrefix}")
    private String URL_PREFIX;

    @Getter
    @Setter
    private class ProxyResponse {
        String code;
        JSONObject data;
        String message;
    }

    @Override
    public void updateProxy() throws InterruptedException {
        Map<String, Object> param = MapBuilder.create(new HashMap<String, Object>())
                .put("username", "minnan")
                .put("password", "min2023*").build();
        //1.登陆 /api/login
        HttpRequest loginRequest = HttpUtil.createPost(URL_PREFIX + "/api/login")
                .body(JSONUtil.toJsonStr(param), "application/json");
        HttpResponse loginRes = loginRequest.execute();
        ProxyResponse loginResponse = JSONUtil.toBean(loginRes.body(), ProxyResponse.class);
        String token = loginResponse.data.getStr("token");
        // 查询服务状态 /api/touch
        HttpRequest touchRequest = HttpUtil.createGet(URL_PREFIX + "/api/touch")
                .auth(token);
        HttpResponse touchRes = touchRequest.execute();
        ProxyResponse touchResponse = JSONUtil.toBean(touchRes.body(), ProxyResponse.class);
        Boolean isRunning = touchResponse.data.getBool("running");
        Console.log(JSONUtil.toJsonPrettyStr(touchResponse.data.getObj("touch")));
        if (isRunning) {
            JSONArray connectedServer = touchResponse.data.getByPath("touch.connectedServer", JSONArray.class);
            Map<Integer, Integer> pingResult = ping(connectedServer, token);
            boolean isDisconnected = false;
            for (Object c : connectedServer) {
                JSONObject server = (JSONObject) c;
                Integer id = server.getInt("id");
                Integer ms = pingResult.getOrDefault(id, -1);
                if (ms < 0) {
                    isDisconnected = true;
                    break;
                }
            }
            if (!isDisconnected) {
                log.info("代理正常运行中");
                return;
            }
            log.info("代理连接超时，即将关闭代理");
            //关闭代理
            HttpRequest shutdownRequest = HttpUtil.createRequest(Method.DELETE, URL_PREFIX + "/api/v2ray")
                            .auth(token);
            shutdownRequest.execute();
            Thread.sleep(5000);
        }
        //3.订阅更新 /api/subscription
        JSONObject subscription = touchResponse.data.getByPath("touch.subscriptions[0]", JSONObject.class);
        Map<String, Object> subscribeParam = MapBuilder.create(new HashMap<String, Object>())
                .put("id", subscription.getInt("id"))
                .put("_type", subscription.getStr("_type"))
                .build();
        log.info("开始更新代理订阅,参数:{}", JSONUtil.toJsonStr(subscribeParam));
        HttpRequest subscriptionRequest = HttpUtil.createRequest(Method.PUT, URL_PREFIX + "/api/subscription")
                .auth(token)
                .body(JSONUtil.toJsonStr(subscribeParam), "application/json");
        HttpResponse subscriptionRes = subscriptionRequest.execute();
        ProxyResponse subscriptionResponse = JSONUtil.toBean(subscriptionRes.body(), ProxyResponse.class);
        JSONArray servers = subscriptionResponse.data.getByPath("touch.subscriptions[0].servers", JSONArray.class);
        if (servers == null || servers.size() == 0) {
            log.info("订阅服务器数量为0，结束代理维护");
            return;
        }
        log.info("已更新代理服务器配置，新配置为：{}", servers.toJSONString(0));

        //4.测速  /api/pingLatency
        Map<Integer, Integer> pingResult = ping(servers, token);
        Optional<JSONObject> serverToActiveOpt = servers.stream()
                .map(e -> (JSONObject) e)
                .filter(e -> pingResult.getOrDefault(e.getInt("id"), -1) > 100)
                .min(Comparator.comparing(e -> pingResult.get(e.getInt("id"))));
        if (serverToActiveOpt.isEmpty()) {
            log.info("所有服务器已超时或不可用，结束代理维护");
            return;
        }

        //5.选择 /api/connection  /api/v2ray
        JSONObject serverToActive = serverToActiveOpt.get();
        log.info("即将启动服务器，{}", serverToActive.toJSONString(0));
        HttpRequest connectionRequest = HttpUtil.createPost(URL_PREFIX + "/api/connection")
                .auth(token)
                .body(serverToActive.toJSONString(0), "application/json");
        connectionRequest.execute();
        HttpRequest activeRequest = HttpUtil.createPost(URL_PREFIX + "/api/v2ray")
                .auth(token);
        HttpResponse activeRes = activeRequest.execute();
        if (activeRes.isOk()) {
            log.info("已启动代理服务器, 服务器信息：{}", serverToActive.toJSONString(0));
        } else {
            log.info("启动代理服务器失败，服务器信息：{}，启动响应信息：{}", serverToActive.toJSONString(0), activeRes.body());
        }
        log.info("结束代理服务器维护");

    }

    private Map<Integer, Integer> ping(JSONArray param, String token) {
        String paramJsonStr = JSONUtil.toJsonStr(param);
        String paramString = URLEncodeUtil.encode(paramJsonStr);
        HttpRequest pingRequest = HttpUtil.createGet(URL_PREFIX + "/api/pingLatency?whiches=" + paramString)
                .auth(token);
        HttpResponse pingRes = pingRequest.execute();
        ProxyResponse pingResponse = JSONUtil.toBean(pingRes.body(), ProxyResponse.class);
        JSONArray pingArray = pingResponse.data.getJSONArray("whiches");
        return pingArray.stream()
                .map(e -> (JSONObject) e)
                .collect(Collectors.toMap(e -> e.getInt("id"), e -> {
                    String latency = e.getStr("pingLatency");
                    return "TIMEOUT".equalsIgnoreCase(latency) ? -1 : Integer.parseInt(latency.replace("ms", ""));
                }));
    }

}
