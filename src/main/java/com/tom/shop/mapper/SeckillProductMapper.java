package com.tom.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.shop.model.SeckillProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 查询正在进行的秒杀商品
     */
    @Select("SELECT * FROM seckill_product WHERE start_time <= #{now} AND end_time >= #{now} AND stock > 0")
    List<SeckillProduct> selectActiveSeckillProducts(@Param("now") LocalDateTime now);

    /**
     * 根据ID查询正在进行的秒杀商品
     */
    @Select("SELECT * FROM seckill_product WHERE id = #{id} AND start_time <= #{now} AND end_time >= #{now} AND stock > 0")
    SeckillProduct selectActiveSeckillProductById(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 查询商品库存（用于更新操作）
     */
    @Select("SELECT stock FROM seckill_product WHERE id = #{id}")
    Integer selectStockById(@Param("id") Long id);
}
