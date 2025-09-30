package com.tom.shop.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tom.shop.dto.SeckillResult;
import com.tom.shop.mapper.SeckillProductMapper;
import com.tom.shop.model.SeckillProduct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SeckillService {

    @Resource
    private SeckillProductMapper seckillProductMapper;

    // 使用内存标记已售罄的商品，减少数据库压力
    private final ConcurrentHashMap<Long, Boolean> soldOutFlags = new ConcurrentHashMap<>();

    /**
     * 获取所有正在进行的秒杀商品
     */
    public List<SeckillProduct> getActiveSeckillProducts() {
        return seckillProductMapper.selectActiveSeckillProducts(LocalDateTime.now());
    }

    /**
     * 根据ID获取秒杀商品详情
     */
    public SeckillProduct getSeckillProductById(Long id) {
        return seckillProductMapper.selectById(id);
    }

    /**
     * 执行秒杀操作
     */
    @Transactional
    public SeckillResult performSeckill(Long productId) {
        if (soldOutFlags.getOrDefault(productId, false)) {
            return SeckillResult.error("商品已售罄");
        }
        Integer stock = seckillProductMapper.selectStockById(productId);
        if (stock == null || stock <= 0) {
            soldOutFlags.put(productId, true);
            return SeckillResult.error("商品已售罄");
        }
        SeckillProduct product = seckillProductMapper.selectActiveSeckillProductById(productId, LocalDateTime.now());
        if (product == null) {
            return SeckillResult.error("秒杀活动未开始或已结束");
        }
        try {
            UpdateWrapper<SeckillProduct> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", productId)
                    .eq("stock", stock) // 确保库存没有被其他线程修改
                    .set("stock", stock - 1);
            int rows = seckillProductMapper.update(null, updateWrapper);
            if (rows <= 0) {
                return SeckillResult.error("系统繁忙，请稍后重试");
            }
            SeckillProduct updatedProduct = seckillProductMapper.selectById(productId);
            if (updatedProduct.getStock() <= 0) {
                soldOutFlags.put(productId, true);
            }
            return SeckillResult.ok(updatedProduct);
        } catch (Exception e) {
            return SeckillResult.error("系统繁忙，请稍后重试");
        }
    }


}
