package site.minnan.robotmanage.infrastructure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * redis工具类
 * @author Minnan on 2020/12/17
 */
@Component
public class RedisUtil {
    @Autowired
    RedisTemplate redisTemplate;


    /**
     * 模糊查询redis数据库
     * @param pattern 扫描匹配字符串
     * @return 符合条件的所有键
     */
    public List<String> scan(String pattern){
        List<String> keys = new ArrayList<>();
        Consumer<byte[]> consumer = item -> keys.add(new String(item, StandardCharsets.UTF_8));
        redisTemplate.execute((RedisConnection connection) -> {
            try(Cursor<byte[]> cursor =
                        connection.scan(ScanOptions.scanOptions().count(Long.MAX_VALUE).match(pattern).build())){
                cursor.forEachRemaining(consumer);
                return null;
            }
        });
        return keys;
    }

    /**
     * 模糊查询一个键
     * @param pattern 匹配字符串
     * @return 符合条件的第一个键，空值时表示没有匹配的键
     */
    public String scanOne(String pattern){
        List<String> scanResult = scan(pattern);
        return scanResult.isEmpty() ? null : scanResult.get(0);
    }

    /**
     * redis中是否存在key
     * @param key 要判断的键
     * @return 是否存在key
     */
    public boolean hasKey(String key){
        return redisTemplate.hasKey(key);
    }

    /**
     * 存入hash键
     * @param key 键名
     * @param hashKey hash键
     * @param value 存入的值
     */
    public void hashPut(String key, String hashKey, Object value){
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void hashPutAll(String key, Map<String, Object> map){
        redisTemplate.opsForHash().putAll(key, map);
    }


    /**
     * 将一个bean按hash键方式存入redis
     * @param key 键名
     * @param bean 存入的bean
     */
    public void hashPutBean(String key, Object bean){
        BeanMap beanMap = BeanMap.create(bean);
        redisTemplate.opsForHash().putAll(key, beanMap);
    }

    public void putBeanAsJsonString(String key, Object bean, ObjectMapper objectMapper) throws JsonProcessingException {
        String value = objectMapper.writeValueAsString(bean);
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 将一个Object按value方式存入数据库
     * @param key 键名
     * @param value 值
     */
    public void valueSet(String key, Object value){
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 将一个Object按value方式存入数据库，同时设置过期时间
     * @param key 键名
     * @param value 值
     * @param duration  过期时间
     */
    public void valueSet(String key, Object value, Duration duration){
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 设置过期时间
     * @param key 键名
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     */
    public void setExpire(String key, Long timeout, TimeUnit timeUnit){
        redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 获取hash类型上的某个hash键值
     * @param key 键名
     * @param hashKey  hash键
     * @return 值
     */
    public Object getHashValue(String key, String hashKey){
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获得value
     * @param key 键名
     * @return 值
     */
    public Object getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取一个hash类型上的所有hash键及其值
     * @param key 键名
     * @return key-value形式返回hash类型上的所有值
     */
    public <T> Map<String, T> getHash(String key){
        return (Map<String, T>) redisTemplate.opsForHash().entries(key);
    }

    /**
     * 将一个hash类型转化成bean
     * @param key 键名
     * @param clazz  要转化的类型
     * @return  bean，返回空为空时表示转化失败
     */
    public <T> T getBeanFromHash(String key, Class<T> clazz){
        try {
            T bean = clazz.newInstance();
            Map<String, Object> objectMap = getHash(key);
            BeanMap.create(bean).putAll(objectMap);
            return bean;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getBeanFromJsonString(String key, ObjectMapper objectMapper, Class<T> clazz) throws JsonProcessingException {
        String value = (String) redisTemplate.opsForValue().get(key);
        return objectMapper.readValue(value, clazz);
    }

    /**
     * 将一个value类型转化成bean
     * @param key 键名
     * @return bean
     */
    public Object getBeanFromValue(String key){
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定key
     * @param key 键名
     * @return 删除是否成功
     */
    public boolean delete(String key){
        return redisTemplate.delete(key);
    }

    /**
     * 删除符合条件的所有key
     * @param pattern 匹配字符串
     */
    public void deleteFuzzy(String pattern){
        List<String> keyList = scan(pattern);
        keyList.forEach(key -> delete(key));
    }

    /**
     * 获取key的过期时间
     * @param key 键名
     * @param timeUnit 时间单位
     * @return
     */
    public long getTTL(String key, TimeUnit timeUnit){
        return redisTemplate.getExpire(key, timeUnit);
    }

    public boolean setnx(String key, Object value){
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public boolean setnx(String key, Object value, Duration timeout){
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }
}
