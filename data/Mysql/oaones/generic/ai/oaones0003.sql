SET NAMES utf8mb4;

SET @has_create_time = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'uf_oaones_generic_ai_config'
      AND COLUMN_NAME = 'create_time'
);
SET @sql = IF(@has_create_time = 0,
    'ALTER TABLE uf_oaones_generic_ai_config ADD COLUMN create_time VARCHAR(30) NOT NULL DEFAULT '''' COMMENT ''创建时间''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE uf_oaones_generic_ai_config
SET create_time = update_time
WHERE (create_time IS NULL OR create_time = '') AND update_time IS NOT NULL;

UPDATE uf_oaones_generic_ai_config
SET connector_name = '默认 AI'
WHERE connector_name IS NULL OR TRIM(connector_name) = '';

UPDATE uf_oaones_generic_ai_config t
SET is_default = 1
WHERE t.id = (
        SELECT min_id FROM (
            SELECT MIN(x.id) AS min_id
            FROM uf_oaones_generic_ai_config x
            WHERE x.enabled = 1
        ) picked
    )
  AND NOT EXISTS (
        SELECT 1 FROM (
            SELECT y.id
            FROM uf_oaones_generic_ai_config y
            WHERE y.enabled = 1 AND y.is_default = 1
            LIMIT 1
        ) existing_default
    );
