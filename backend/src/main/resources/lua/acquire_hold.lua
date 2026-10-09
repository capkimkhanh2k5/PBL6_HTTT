-- Atomically plan all items, including multiple options on the same departure.
-- Structured descriptors: PERSON_LIMIT|quantity|remainingCapacity or
-- SHARED_CAPACITY_UNITS|optionType|quantity|maxPackagePax|unit:capacity:booked;...|allowSplit.
local now = tonumber(ARGV[1])
local holdId = ARGV[2]
local expireAt = ARGV[3]
local states = {}
local results = {}

local function split(value, delimiter)
    local parts = {}
    for part in string.gmatch(value, "([^" .. delimiter .. "]+)") do
        table.insert(parts, part)
    end
    return parts
end

local function readHolds(key)
    local total = 0
    local seats = {}
    local private = {}
    local entries = redis.call('HGETALL', key)
    for i = 1, #entries, 2 do
        local value = entries[i + 1]
        local bar = string.find(value, '|')
        local colon = string.find(value, ':')
        local expiry = bar and tonumber(string.sub(value, 1, bar - 1))
            or (colon and tonumber(string.sub(value, colon + 1)))
        if entries[i] ~= holdId and expiry and expiry > now then
            if bar then
                local detail = string.sub(value, bar + 1)
                if string.sub(detail, 1, 13) == 'PERSON_LIMIT:' then
                    total = total + (tonumber(string.sub(detail, 14)) or 0)
                elseif string.sub(detail, 1, 6) == 'UNITS:' then
                    for _, encoded in ipairs(split(string.sub(detail, 7), ';')) do
                        local allocation = split(encoded, ':')
                        local unit = tonumber(allocation[1])
                        local count = tonumber(allocation[2]) or 0
                        seats[unit] = (seats[unit] or 0) + count
                        total = total + count
                        if allocation[3] == '1' then private[unit] = true end
                    end
                end
            elseif colon then
                total = total + (tonumber(string.sub(value, 1, colon - 1)) or 0)
            end
        end
    end
    return total, seats, private
end

for i, key in ipairs(KEYS) do
    local descriptor = ARGV[3 + i]
    local mode = 'PERSON_LIMIT'
    local quantity, capacity, optionType, packagePax, allowSplit, units
    if descriptor and string.find(descriptor, '|') then
        local parts = split(descriptor, '|')
        mode = parts[1]
        if mode == 'PERSON_LIMIT' then
            quantity = tonumber(parts[2])
            capacity = tonumber(parts[3])
        else
            optionType = parts[2]
            quantity = tonumber(parts[3])
            packagePax = tonumber(parts[4]) or 1
            units = parts[5] or ''
            allowSplit = parts[6] == '1'
        end
    else
        quantity = tonumber(ARGV[3 + (i - 1) * 2 + 1])
        capacity = tonumber(ARGV[3 + (i - 1) * 2 + 2])
    end

    local state = states[key]
    if not state then
        local held, unitHeld, unitPrivate = readHolds(key)
        state = { mode = mode, held = held, requested = 0, allocations = {}, units = {} }
        if mode == 'SHARED_CAPACITY_UNITS' then
            for _, encoded in ipairs(split(units, ';')) do
                local parts = split(encoded, ':')
                local number = tonumber(parts[1])
                local cap = tonumber(parts[2])
                local booked = tonumber(parts[3]) or 0
                local activeHeld = unitHeld[number] or 0
                table.insert(state.units, {
                    number = number, capacity = cap,
                    available = unitPrivate[number] and 0 or math.max(0, cap - booked - activeHeld),
                    empty = booked == 0 and activeHeld == 0 and not unitPrivate[number],
                    private = unitPrivate[number] or false
                })
            end
        end
        states[key] = state
    end

    local itemAllocations = {}
    if mode == 'PERSON_LIMIT' then
        local available = math.max(0, capacity - state.held - state.requested)
        if quantity > available then
            return { 'INSUFFICIENT', key, tostring(quantity), tostring(available) }
        end
        state.requested = state.requested + quantity
    elseif optionType == 'PRIVATE' then
        local eligible = {}
        for _, unit in ipairs(state.units) do
            if unit.empty and unit.capacity >= packagePax then table.insert(eligible, unit) end
        end
        if #eligible < quantity then
            return { 'INSUFFICIENT', key, tostring(quantity), tostring(#eligible) }
        end
        for j = 1, quantity do
            local unit = eligible[j]
            table.insert(itemAllocations, unit.number .. ':' .. unit.capacity .. ':1')
            unit.available = 0
            unit.empty = false
            unit.private = true
        end
    else
        local chosen = nil
        local total = 0
        for _, unit in ipairs(state.units) do
            total = total + unit.available
            if not chosen and not unit.empty and not unit.private and unit.available >= quantity then
                chosen = unit
            end
        end
        if not chosen then
            for _, unit in ipairs(state.units) do
                if unit.empty and unit.available >= quantity then chosen = unit; break end
            end
        end
        if chosen then
            table.insert(itemAllocations, chosen.number .. ':' .. quantity .. ':0')
            chosen.available = chosen.available - quantity
            chosen.empty = false
        elseif not allowSplit or total < quantity then
            return { 'INSUFFICIENT', key, tostring(quantity), tostring(total) }
        else
            local remaining = quantity
            for pass = 1, 2 do
                for _, unit in ipairs(state.units) do
                    local matches = (pass == 1 and not unit.empty) or (pass == 2 and unit.empty)
                    if matches and not unit.private and remaining > 0 and unit.available > 0 then
                        local take = math.min(remaining, unit.available)
                        table.insert(itemAllocations, unit.number .. ':' .. take .. ':0')
                        unit.available = unit.available - take
                        unit.empty = false
                        remaining = remaining - take
                    end
                end
            end
        end
    end
    for _, allocation in ipairs(itemAllocations) do table.insert(state.allocations, allocation) end
    table.insert(results, table.concat(itemAllocations, ';'))
end

-- No Redis mutations until every item is feasible. Write exactly one record per departure.
for key, state in pairs(states) do
    local entries = redis.call('HGETALL', key)
    for i = 1, #entries, 2 do
        local value = entries[i + 1]
        local bar = string.find(value, '|')
        local colon = string.find(value, ':')
        local expiry = bar and tonumber(string.sub(value, 1, bar - 1))
            or (colon and tonumber(string.sub(value, colon + 1)))
        if expiry and expiry <= now then redis.call('HDEL', key, entries[i]) end
    end
    local detail = state.mode == 'PERSON_LIMIT' and ('PERSON_LIMIT:' .. state.requested)
        or ('UNITS:' .. table.concat(state.allocations, ';'))
    redis.call('HSET', key, holdId, expireAt .. '|' .. detail)
    redis.call('EXPIRE', key, 86400)
end
return { 'OK', table.concat(results, '@') }
