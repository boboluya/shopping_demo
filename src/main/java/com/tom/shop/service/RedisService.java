package com.tom.shop.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 获取 Hash 操作对象
    private HashOperations<String, String, Object> hashOps() {
        return redisTemplate.opsForHash();
    }

    /**
     * 向 Hash 中存入一个字段
     */
    public void hSet(String key, String field, Object value) {
        hashOps().put(key, field, value);
    }

    /**
     * 向 Hash 中存入一个字段，并设置过期时间（秒）
     */
    public void hSetWithExpire(String key, String field, Object value, long timeout, TimeUnit unit) {
        hSet(key, field, value);
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 批量存入 Hash 字段
     */
    public void hSetAll(String key, Map<String, Object> map) {
        hashOps().putAll(key, map);
    }

    /**
     * 获取 Hash 中指定字段的值
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field, Class<T> clazz) {
        Object value = hashOps().get(key, field);
        return (T) value; // 由于使用了 JSON 序列化，实际类型已保留
    }

    /**
     * 获取 Hash 中所有字段和值
     */
    public Object hGet(String key, String field) {
        return hashOps().get(key, field);
    }

    public Map<String, Object> hGetAll(String key) {
        return hashOps().entries(key);
    }

    public boolean hasKeyField(String key, String field) {
        return hashOps().hasKey(key, field);
    }

    public Long hIncrement(String key, String field, long delta) {
        return hashOps().increment(key, field, delta);
    }

    public Set<String> members(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) (Set<?>) members;
        return result;
    }

    /**
     * 判断 Hash 中是否存在指定字段
     */
    public boolean hExists(String key, String field) {
        return hashOps().hasKey(key, field);
    }

    /**
     * 删除 Hash 中的一个或多个字段
     */
    public Long hDel(String key, String... fields) {
        return hashOps().delete(key, (Object) fields);
    }

    /**
     * 获取 Hash 中所有字段名
     */
    @SuppressWarnings("unchecked")
    public Set<String> hKeys(String key) {
        Set<Object> keys = Collections.singleton(hashOps().keys(key));
        return (Set<String>) (Set<?>) keys;
    }

    /**
     * 获取 Hash 中所有值
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hVals(String key, Class<T> clazz) {
        List<Object> values = hashOps().values(key);
        return (List<T>) values;
    }

    /**
     * 设置整个 Hash 的过期时间
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 删除整个 key（包括 Hash）
     */
    public boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 从 Set 中移除成员。
     */
    public void removeFromSet(String setKey, String... members) {
        if (members == null || members.length == 0) {
            return;
        }
        redisTemplate.opsForSet().remove(setKey, (Object[]) members);
    }

    /**
     * 向 Set 中加入成员。
     */
    public void addToSet(String setKey, String... members) {
        if (members == null || members.length == 0) {
            return;
        }
        redisTemplate.opsForSet().add(setKey, members);
    }
}
