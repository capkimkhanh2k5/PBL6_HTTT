CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE services ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B')
) STORED;

CREATE INDEX idx_services_search_vector ON services USING GIN(search_vector);
