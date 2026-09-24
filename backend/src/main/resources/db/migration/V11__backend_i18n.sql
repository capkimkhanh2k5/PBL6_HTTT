-- Canonical backend locale data and localized content added by the i18n rollout.
UPDATE users
SET locale = CASE
    WHEN lower(replace(coalesce(locale, ''), '_', '-')) LIKE 'en%' THEN 'en'
    ELSE 'vi'
END;

ALTER TABLE users ALTER COLUMN locale TYPE varchar(2);
ALTER TABLE users ALTER COLUMN locale SET DEFAULT 'vi';
ALTER TABLE users ALTER COLUMN locale SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_locale CHECK (locale IN ('en', 'vi'));

ALTER TABLE services ADD COLUMN waiver_content_en text;

ALTER TABLE ai_conversations ADD COLUMN locale varchar(2);
UPDATE ai_conversations SET locale = 'vi' WHERE locale IS NULL;
ALTER TABLE ai_conversations ALTER COLUMN locale SET DEFAULT 'vi';
ALTER TABLE ai_conversations ALTER COLUMN locale SET NOT NULL;
ALTER TABLE ai_conversations ADD CONSTRAINT chk_ai_conversations_locale CHECK (locale IN ('en', 'vi'));

ALTER TABLE notifications ADD COLUMN locale varchar(2);
UPDATE notifications SET locale = 'vi' WHERE locale IS NULL;
ALTER TABLE notifications ALTER COLUMN locale SET DEFAULT 'vi';
ALTER TABLE notifications ALTER COLUMN locale SET NOT NULL;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_locale CHECK (locale IN ('en', 'vi'));
ALTER TABLE notifications ALTER COLUMN body TYPE text;

ALTER TABLE safety_rule_evaluations
    ADD COLUMN findings_json jsonb NOT NULL DEFAULT '[]'::jsonb;

DROP INDEX IF EXISTS idx_services_search_vector;
ALTER TABLE services DROP COLUMN IF EXISTS search_vector;
ALTER TABLE services ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(name_en, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(description_en, '')), 'B')
) STORED;
CREATE INDEX idx_services_search_vector ON services USING GIN(search_vector);
