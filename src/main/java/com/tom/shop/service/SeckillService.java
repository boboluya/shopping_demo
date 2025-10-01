package com.tom.shop.service;

import com.tom.shop.dto.SeckillResult;
import com.tom.shop.mapper.SeckillProductMapper;
import com.tom.shop.model.SeckillProduct;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Log4j2
public class SeckillService {

    private static final String PRODUCT_CACHE_HASH = "seckill:product:data";
    private static final String STOCK_CACHE_HASH = "seckill:product:stock";
    private static final String ACTIVE_SET_KEY = "seckill:product:active";
    private static final String SOLD_OUT_SET_KEY = "seckill:product:soldout";

    @Resource
    private SeckillProductMapper seckillProductMapper;

    @Resource
    private RedisService redisService;

    @Value("${seckill.sync-batch-size:200}")
    private int syncBatchSize;

    private final ConcurrentHashMap<Long, Boolean> soldOutFlags = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Long> pendingSyncQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void warmUpCache() {
        refreshActiveProducts();
    }

    /**
     * 定期刷新活动商品缓存，确保活动、库存状态及时更新
     */
    @Scheduled(fixedDelayString = "${seckill.cache-refresh-ms:60000}")
    public void scheduledRefresh() {
        refreshActiveProducts();
    }

    /**
     * 定期将 Redis 库存同步回数据库，避免高并发下对数据库的直接压力
     */
    @Scheduled(fixedDelayString = "${seckill.sync-delay-ms}")
    public void syncStockToDatabase() {
        if (pendingSyncQueue.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new HashSet<>();
        Long productId;
        while ((productId = pendingSyncQueue.poll()) != null) {
            uniqueIds.add(productId);
            if (uniqueIds.size() >= syncBatchSize) {
                break;
            }
        }
        for (Long id : uniqueIds) {
            Integer stock = redisService.hGet(STOCK_CACHE_HASH, id.toString(), Integer.class);
            if (stock == null) {
                continue;
            }
            seckillProductMapper.syncStock(id, stock);
        }
    }

    /**
     * 获取当前所有正在进行的秒杀商品
     */
    public List<SeckillProduct> getActiveSeckillProducts() {
        List<SeckillProduct> cachedProducts = redisService.hVals(PRODUCT_CACHE_HASH, SeckillProduct.class);
        LocalDateTime now = LocalDateTime.now();
        if (!cachedProducts.isEmpty()) {
            List<SeckillProduct> activeList = cachedProducts.stream()
                    .filter(Objects::nonNull)
                    .filter(product -> isProductActive(product, now) && product.getStock() > 0)
                    .sorted(Comparator.comparing(SeckillProduct::getStartTime)
                            .thenComparing(SeckillProduct::getId))
                    .toList();
            if (!activeList.isEmpty()) {
                return activeList;
            }
        }
        List<SeckillProduct> activeFromDb = seckillProductMapper.selectActiveSeckillProducts(now);
        activeFromDb.forEach(product -> cacheProduct(product, now));
        return activeFromDb;
    }

    /**
     * 根据ID获取秒杀商品详情
     */
    public SeckillProduct getSeckillProductById(Long id) {
        SeckillProduct cached = fetchProductFromCache(id);
        if (cached != null) {
            return cached;
        }
        SeckillProduct product = seckillProductMapper.selectById(id);
        cacheProduct(product, LocalDateTime.now());
        return product;
    }

    /**
     * 执行秒杀购买逻辑（优先操作 Redis，异步回写数据库）
     */
    public SeckillResult performSeckill(Long productId) {
        if (soldOutFlags.getOrDefault(productId, false)) {
            return SeckillResult.error("商品已售罄");
        }
        LocalDateTime now = LocalDateTime.now();
        SeckillProduct product = fetchProductFromCache(productId);
        if (!isProductActive(product, now)) {
            product = seckillProductMapper.selectActiveSeckillProductById(productId, now);
            if (product == null) {
                markSoldOut(productId);
                return SeckillResult.error("秒杀活动未开始或已结束");
            }
            cacheProduct(product, now);
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            markSoldOut(productId);
            return SeckillResult.error("商品已售罄");
        }

        String productIdKey = productId.toString();
        if (!redisService.hasKeyField(STOCK_CACHE_HASH, productIdKey)) {
            cacheProduct(product, now);
        }

        Long newStock = redisService.hIncrement(STOCK_CACHE_HASH, productIdKey, -1);

        if (newStock == null) {
            return SeckillResult.error("系统繁忙，请稍后重试");
        }

        if (newStock < 0) {
            redisService.hIncrement(STOCK_CACHE_HASH, productIdKey, 1);
            markSoldOut(productId);
            return SeckillResult.error("商品已售罄");
        }

        int updatedStock = newStock.intValue();
        product.setStock(updatedStock);
        redisService.hSet(PRODUCT_CACHE_HASH, productIdKey, product);

        if (updatedStock <= 0) {
            markSoldOut(productId);
        } else {
            markAvailable(productId);
        }

        pendingSyncQueue.offer(productId);

        return SeckillResult.ok(product);
    }

    private void refreshActiveProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> activeProducts = seckillProductMapper.selectActiveSeckillProducts(now);
        Set<String> activeIds = new HashSet<>();
        for (SeckillProduct product : activeProducts) {
            cacheProduct(product, now);
            activeIds.add(product.getId().toString());
        }
        Set<String> cachedActiveIds = redisService.members(ACTIVE_SET_KEY);
        if (!cachedActiveIds.isEmpty()) {
            for (String cachedId : cachedActiveIds) {
                if (!activeIds.contains(cachedId)) {
                    redisService.removeFromSet(ACTIVE_SET_KEY, cachedId);
                    try {
                        soldOutFlags.remove(Long.valueOf(cachedId));
                    } catch (NumberFormatException ignored) {
                        log.warn("Invalid product ID in cache: {}", cachedId);
                    }
                }
            }
        }
    }

    private SeckillProduct fetchProductFromCache(Long productId) {
        if (productId == null) {
            return null;
        }
        return redisService.hGet(PRODUCT_CACHE_HASH, productId.toString(), SeckillProduct.class);
    }

    private void cacheProduct(SeckillProduct product, LocalDateTime now) {
        if (product == null || product.getId() == null) {
            return;
        }
        String idKey = product.getId().toString();
        redisService.hSet(PRODUCT_CACHE_HASH, idKey, product);
        redisService.hSet(STOCK_CACHE_HASH, idKey, product.getStock() == null ? 0 : product.getStock());
        if (isProductActive(product, now) && (product.getStock() != null && product.getStock() > 0)) {
            markAvailable(product.getId());
        } else if (product.getStock() != null && product.getStock() <= 0) {
            markSoldOut(product.getId());
        } else {
            redisService.removeFromSet(ACTIVE_SET_KEY, idKey);
            soldOutFlags.remove(product.getId());
        }
    }

    private boolean isProductActive(SeckillProduct product, LocalDateTime now) {
        if (product == null) {
            return false;
        }
        LocalDateTime start = product.getStartTime();
        LocalDateTime end = product.getEndTime();
        return (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
    }

    private void markSoldOut(Long productId) {
        if (productId == null) {
            return;
        }
        String idKey = productId.toString();
        redisService.addToSet(SOLD_OUT_SET_KEY, idKey);
        redisService.removeFromSet(ACTIVE_SET_KEY, idKey);
        soldOutFlags.put(productId, true);
    }

    private void markAvailable(Long productId) {
        if (productId == null) {
            return;
        }
        String idKey = productId.toString();
        redisService.removeFromSet(SOLD_OUT_SET_KEY, idKey);
        redisService.addToSet(ACTIVE_SET_KEY, idKey);
        soldOutFlags.remove(productId);
    }
}

