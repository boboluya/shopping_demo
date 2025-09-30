# shopping_demo

shopping_demo

## 秒杀系统说明

这是一个基于Spring Boot的购物秒杀系统，专注于实现高并发场景下的商品秒杀功能。

### 功能特性

1. 秒杀商品管理
2. 高并发秒杀处理
3. 库存扣减与超卖防护
4. 活动时间控制

### API接口

- `GET /api/seckill/products` - 获取所有正在进行的秒杀商品
- `GET /api/seckill/product/{id}` - 获取指定秒杀商品详情
- `POST /api/seckill/purchase/{productId}` - 执行秒杀购买

### 技术栈

- Spring Boot 3.5.6
- Java 21
- PostgreSQL数据库
- Redis缓存
- MyBatis Plus
- Lombok

### 数据库初始化

系统包含两个SQL脚本文件：
- `schema.sql` - 创建表结构
- `data.sql` - 插入测试数据

您可以直接在PostgreSQL中执行这些脚本以初始化数据库。

### 并发处理

系统通过以下方式处理高并发场景：

1. 数据库乐观锁防止超卖
2. 内存标记已售罄商品减少数据库访问
3. 事务控制确保数据一致性

### MyBatis Plus特性

1. 使用乐观锁防止并发更新冲突
2. 自动分页功能
3. 简化的CRUD操作