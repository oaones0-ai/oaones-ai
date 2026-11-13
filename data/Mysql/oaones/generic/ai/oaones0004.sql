SET NAMES utf8mb4;

SET @has_supported_types = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'uf_oaones_generic_ai_config'
      AND COLUMN_NAME = 'supported_types'
);
SET @sql = IF(@has_supported_types = 0,
    'ALTER TABLE uf_oaones_generic_ai_config ADD COLUMN supported_types VARCHAR(200) NOT NULL DEFAULT ''text'' COMMENT ''支持类型：text,image,video,audio''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE uf_oaones_generic_ai_config
SET supported_types = 'text'
WHERE supported_types IS NULL OR TRIM(supported_types) = '';
