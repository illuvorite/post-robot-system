-- 库存锁定脚本
-- KEYS[1]: stock:available:{productId}
-- KEYS[2]: stock:locked:{productId}
-- ARGV[1]: 锁定数量
-- 返回: 1=成功, 0=库存不足, -1=Key不存在

local available = redis.call('GET', KEYS[1])
if not available then
    return -1
end

local availableNum = tonumber(available)
local quantity = tonumber(ARGV[1])

if availableNum < quantity then
    return 0
end

redis.call('DECRBY', KEYS[1], quantity)
redis.call('INCRBY', KEYS[2], quantity)

return 1
