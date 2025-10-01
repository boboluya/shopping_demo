package com.tom.shop.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 一组通用的 Redis 操作封装，基于 Hash 与 Set 提供常见读写能力。
 * <p>
 * 统一封装的好处：
 * <ul>
 *     <li>调用端无需直接操作 {@link RedisTemplate} / {@link StringRedisTemplate}，降低重复代码。</li>
 *     <li>可以集中处理空值、类型转换等细节，调用更加安全。</li>
 *     <li>未来若切换 Redis 库或序列化方式，只需调整此处实现。</li>
 * </ul>
 * </p>
 */
public class RedisCacheHelper {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisCacheHelper(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 读取指定 Hash 下的所有值，并转换为目标类型集合。
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getHashValues(RedisTemplate<String, T> template, String hashKey) {
        List<Object> rawValues = template.opsForHash().values(hashKey);
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyList();
        }
        return rawValues.stream()
                .filter(Objects::nonNull)
                .map(value -> (T) value)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定 Hash 字段的对象值。
     */
    @SuppressWarnings("unchecked")
    public <T> T getHashValue(RedisTemplate<String, T> template, String hashKey, String field) {
        Object raw = template.opsForHash().get(hashKey, field);
        return (T) raw;
    }

    /**
     * 写入 Hash 字段的对象值。
     */
    public <T> void putHashValue(RedisTemplate<String, T> template, String hashKey, String field, T value) {
        template.opsForHash().put(hashKey, field, value);
    }

    /**
     * 写入 Hash 字段的字符串值，常用于数值型字段的存储。
     */
    public void putHashValue(String hashKey, String field, Object value) {
        stringRedisTemplate.opsForHash().put(hashKey, field, String.valueOf(value));
    }

    /**
     * 判断 Hash 中是否存在指定字段。
     */
    public boolean hasHashField(String hashKey, String field) {
        return stringRedisTemplate.opsForHash().hasKey(hashKey, field);
    }

    /**
     * 以原子方式对 Hash 字段执行自增/自减操作。
     */
    public Long incrementHash(String hashKey, String field, long delta) {
        return stringRedisTemplate.opsForHash().increment(hashKey, field, delta);
    }

    /**
     * 以 Integer 形式读取 Hash 字段的值，转换失败时返回 null。
     */
    public Integer getHashFieldAsInt(String hashKey, String field) {
        String value = stringRedisTemplate.<String, String>opsForHash().get(hashKey, field);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 获取 Set 中的全部成员。
     */
    public Set<String> getSetMembers(String setKey) {
        Set<String> members = stringRedisTemplate.opsForSet().members(setKey);
        return members == null ? Collections.emptySet() : members;
    }

    /**
     * 向 Set 中加入成员。
     */
    public void addToSet(String setKey, String... members) {
        if (members == null || members.length == 0) {
            return;
        }
        stringRedisTemplate.opsForSet().add(setKey, members);
    }

    /**
     * 从 Set 中移除成员。
     */
    public void removeFromSet(String setKey, String... members) {
        if (members == null || members.length == 0) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(setKey, (Object[]) members);
    }

    @SuppressWarnings("unchecked")
    public <T> T cast(Object value) {
        return (T) value;
    }
}
