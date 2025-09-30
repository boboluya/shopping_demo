INSERT INTO seckill_product (id, name, description, price, stock, start_time, end_time, created_at) 
SELECT 1, 'iPhone 15', 'Apple iPhone 15 最新款手机', 5999.00, 100, NOW() - INTERVAL '1 HOUR', NOW() + INTERVAL '2 HOUR', NOW()
WHERE NOT EXISTS (SELECT 1 FROM seckill_product WHERE id = 1);

INSERT INTO seckill_product (id, name, description, price, stock, start_time, end_time, created_at) 
SELECT 2, 'MacBook Pro', 'Apple MacBook Pro 14英寸笔记本电脑', 12999.00, 50, NOW() - INTERVAL '1 HOUR', NOW() + INTERVAL '2 HOUR', NOW()
WHERE NOT EXISTS (SELECT 1 FROM seckill_product WHERE id = 2);

INSERT INTO seckill_product (id, name, description, price, stock, start_time, end_time, created_at) 
SELECT 3, 'AirPods Pro', 'Apple AirPods Pro 无线耳机', 1999.00, 200, NOW() + INTERVAL '1 HOUR', NOW() + INTERVAL '3 HOUR', NOW()
WHERE NOT EXISTS (SELECT 1 FROM seckill_product WHERE id = 3);