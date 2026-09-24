WITH ranked_images AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY service_id
               ORDER BY sort_order NULLS LAST, created_at, id
           )::smallint AS normalized_sort_order
    FROM service_images
)
UPDATE service_images image
SET sort_order = ranked.normalized_sort_order
FROM ranked_images ranked
WHERE image.id = ranked.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_images_service_sort_order
    ON service_images (service_id, sort_order);
