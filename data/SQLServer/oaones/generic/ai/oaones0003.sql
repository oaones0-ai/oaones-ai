IF COL_LENGTH('dbo.uf_oaones_generic_ai_config', 'create_time') IS NULL
BEGIN
    ALTER TABLE dbo.uf_oaones_generic_ai_config
        ADD create_time VARCHAR(30) NOT NULL
            CONSTRAINT DF_UF_OAONES_AI_CREATE_TIME DEFAULT '';
END
;

UPDATE dbo.uf_oaones_generic_ai_config
SET create_time = ISNULL(update_time, '')
WHERE create_time IS NULL OR create_time = '';
;

UPDATE dbo.uf_oaones_generic_ai_config
SET connector_name = N'默认 AI'
WHERE connector_name IS NULL OR LTRIM(RTRIM(connector_name)) = N'';
;

IF NOT EXISTS (
    SELECT 1 FROM dbo.uf_oaones_generic_ai_config
    WHERE enabled = 1 AND is_default = 1
)
BEGIN
    UPDATE dbo.uf_oaones_generic_ai_config
    SET is_default = 1
    WHERE id = (
        SELECT MIN(id) FROM dbo.uf_oaones_generic_ai_config WHERE enabled = 1
    );
END
;
