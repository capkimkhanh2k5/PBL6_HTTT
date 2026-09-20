-- release_hold.lua
-- Batch inventory release script
-- KEYS: slot hold keys, e.g. KEYS[1] = "inventory:slot:" .. slotId1 .. ":holds", KEYS[2] = ...
-- ARGV[1]: holdId (string)

local holdId = ARGV[1]

for i = 1, #KEYS do
    redis.call('HDEL', KEYS[i], holdId)
end

return "OK"
