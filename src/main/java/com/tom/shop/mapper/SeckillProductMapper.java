package com.tom.shop.mapper;

import com.tom.shop.model.SeckillProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SeckillProductMapper {

    /**
     * 查询正在进行的秒杀商品（根据当前时间窗口）
     */
    @Select("SELECT * FROM seckill_product WHERE start_time <= #{now} AND end_time >= #{now} AND stock > 0")
    List<SeckillProduct> selectActiveSeckillProducts(@Param("now") LocalDateTime now);

    /**
     * 根据商品ID查询正在进行的秒杀商品
     */
    @Select("SELECT * FROM seckill_product WHERE id = #{id} AND start_time <= #{now} AND end_time >= #{now} AND stock > 0")
    SeckillProduct selectActiveSeckillProductById(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 根据ID查询商品详情
     */
    @Select("SELECT * FROM seckill_product WHERE id = #{id}")
    SeckillProduct selectById(@Param("id") Long id);

    /**
     * 查询商品当前库存（用于并发扣减判断）
     */
    @Select("SELECT stock FROM seckill_product WHERE id = #{id}")
    Integer selectStockById(@Param("id") Long id);

    /**
     * 在并发场景下扣减库存并更新版本号
     */
    @Update("UPDATE seckill_product SET stock = stock - 1, version = version + 1 WHERE id = #{id} AND stock = #{currentStock} AND stock > 0")
    int decrementStock(@Param("id") Long id, @Param("currentStock") Integer currentStock);

    /**
     * 将库存值同步写回数据库
     */
    @Update("UPDATE seckill_product SET stock = #{stock}, version = version + 1 WHERE id = #{id}")
    int syncStock(@Param("id") Long id, @Param("stock") Integer stock);
}
