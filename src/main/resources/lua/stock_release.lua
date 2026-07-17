-- 库存释放脚本（取消订单/超时回滚）
-- KEYS[1]: stock:available:{productId}
-- KEYS[2]: stock:locked:{productId}
-- ARGV[1]: 释放数量
-- 返回: 1=成功, 0=锁定库存不足, -1=Key不存在

local locked = redis.call('GET', KEYS[2])
if not locked then
    return -1
end

local lockedNum = tonumber(locked)
local quantity = tonumber(ARGV[1])

if lockedNum < quantity then
    return 0
end

redis.call('INCRBY', KEYS[1], quantity)
redis.call('DECRBY', KEYS[2], quantity)

return 1
