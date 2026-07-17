-- 库存扣减脚本（支付成功后从锁定库存扣减）
-- KEYS[1]: stock:locked:{productId}
-- ARGV[1]: 扣减数量
-- 返回: 1=成功, 0=锁定库存不足, -1=Key不存在

local locked = redis.call('GET', KEYS[1])
if not locked then
    return -1
end

local lockedNum = tonumber(locked)
local quantity = tonumber(ARGV[1])

if lockedNum < quantity then
    return 0
end

redis.call('DECRBY', KEYS[1], quantity)

return 1
