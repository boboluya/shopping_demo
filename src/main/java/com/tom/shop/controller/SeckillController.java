package com.tom.shop.controller;

import com.tom.shop.dto.SeckillResult;
import com.tom.shop.model.SeckillProduct;
import com.tom.shop.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop/item")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 获取所有正在进行的秒杀商品
     */
    @GetMapping("/products")
    public ResponseEntity<List<SeckillProduct>> getActiveSeckillProducts() {
        var products = seckillService.getActiveSeckillProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 根据ID获取秒杀商品详情
     */
    @GetMapping("/product/{id}")
    public ResponseEntity<SeckillProduct> getSeckillProduct(@PathVariable Long id) {
        SeckillProduct product = seckillService.getSeckillProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 执行秒杀操作
     */
    @PostMapping("/purchase/{productId}")
    public ResponseEntity<SeckillResult> performSeckill(@PathVariable Long productId) {
        SeckillResult result = seckillService.performSeckill(productId);
        return ResponseEntity.ok(result);
    }
}
