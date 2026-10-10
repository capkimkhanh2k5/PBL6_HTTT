-- Merge duplicate conversations without losing their messages.
CREATE TEMPORARY TABLE conversation_merge ON COMMIT DROP AS
SELECT id, FIRST_VALUE(id) OVER (
    PARTITION BY customer_id, vendor_id, master_order_id ORDER BY created_at, id
) AS canonical_id
FROM conversations
WHERE customer_id IS NOT NULL AND vendor_id IS NOT NULL AND master_order_id IS NOT NULL;

UPDATE messages m SET conversation_id = c.canonical_id
FROM conversation_merge c WHERE m.conversation_id = c.id AND c.id <> c.canonical_id;

UPDATE conversations c SET updated_at = merged.latest_update
FROM (
    SELECT m.canonical_id, MAX(c.updated_at) AS latest_update
    FROM conversation_merge m JOIN conversations c ON c.id = m.id GROUP BY m.canonical_id
) merged WHERE c.id = merged.canonical_id;

DELETE FROM conversations c USING conversation_merge m
WHERE c.id = m.id AND m.id <> m.canonical_id;

CREATE UNIQUE INDEX uq_conversations_participants_order
ON conversations(customer_id, vendor_id, master_order_id);

ALTER TABLE conversations ADD COLUMN last_message_sequence BIGINT NOT NULL DEFAULT 0;
ALTER TABLE messages ADD COLUMN message_sequence BIGINT;

WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY conversation_id ORDER BY created_at, id) AS sequence
    FROM messages
)
UPDATE messages m SET message_sequence = n.sequence FROM numbered n WHERE m.id = n.id;

ALTER TABLE messages ALTER COLUMN message_sequence SET NOT NULL;
ALTER TABLE messages ADD CONSTRAINT chk_messages_positive_sequence CHECK (message_sequence > 0);
CREATE UNIQUE INDEX uq_messages_conversation_sequence ON messages(conversation_id, message_sequence);

UPDATE conversations c SET last_message_sequence = latest.sequence
FROM (
    SELECT conversation_id, MAX(message_sequence) AS sequence FROM messages GROUP BY conversation_id
) latest WHERE c.id = latest.conversation_id;
