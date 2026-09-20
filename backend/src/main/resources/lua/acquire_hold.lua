-- acquire_hold.lua
-- Batch inventory holding script
-- KEYS: slot hold keys, e.g. KEYS[1] = "inventory:slot:" .. slotId1 .. ":holds", KEYS[2] = ...
-- ARGV[1]: currentTimeMillis (number)
-- ARGV[2]: holdId (string)
-- ARGV[3]: expireAtMillis (number)
-- ARGV[4..]: pairs of (requestedQty, maxCapacity) for each slot key

local now = tonumber(ARGV[1])
local holdId = ARGV[2]
local expireAt = ARGV[3]
local numSlots = #KEYS

-- Phase 1: Verify capacity across ALL slots (All-or-Nothing check)
for i = 1, numSlots do
    local key = KEYS[i]
    local reqQty = tonumber(ARGV[3 + (i - 1) * 2 + 1])
    local maxCap = tonumber(ARGV[3 + (i - 1) * 2 + 2])

    local activeHeld = 0
    local rawHolds = redis.call('HGETALL', key)

    for j = 1, #rawHolds, 2 do
        local hId = rawHolds[j]
        local val = rawHolds[j + 1]

        local colonPos = string.find(val, ":")
        if colonPos then
            local qty = tonumber(string.sub(val, 1, colonPos - 1))
            local exp = tonumber(string.sub(val, colonPos + 1))
            if exp and exp > now then
                if hId ~= holdId then
                    activeHeld = activeHeld + qty
                end
            end
        end
    end

    if (activeHeld + reqQty) > maxCap then
        local avail = math.max(0, maxCap - activeHeld)
        return { "INSUFFICIENT", key, tostring(reqQty), tostring(avail) }
    end
end

-- Phase 2: All slots passed! Commit the holds and clean up any expired entries
for i = 1, numSlots do
    local key = KEYS[i]
    local reqQty = tonumber(ARGV[3 + (i - 1) * 2 + 1])

    local rawHolds = redis.call('HGETALL', key)
    for j = 1, #rawHolds, 2 do
        local hId = rawHolds[j]
        local val = rawHolds[j + 1]
        local colonPos = string.find(val, ":")
        if colonPos then
            local exp = tonumber(string.sub(val, colonPos + 1))
            if exp and exp <= now then
                redis.call('HDEL', key, hId)
            end
        end
    end

    local recordVal = tostring(reqQty) .. ":" .. tostring(expireAt)
    redis.call('HSET', key, holdId, recordVal)
    redis.call('EXPIRE', key, 86400)
end

return { "OK" }
