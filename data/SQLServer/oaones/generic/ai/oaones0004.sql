IF COL_LENGTH('dbo.uf_oaones_generic_ai_config', 'supported_types') IS NULL
    BEGIN
        ALTER TABLE dbo.uf_oaones_generic_ai_config
            ADD supported_types NVARCHAR(200) NOT NULL
                CONSTRAINT DF_UF_OAONES_AI_SUPPORTED_TYPES DEFAULT N'text';
    END
;

UPDATE dbo.uf_oaones_generic_ai_config
SET supported_types = N'text'
WHERE supported_types IS NULL OR LTRIM(RTRIM(supported_types)) = N'';
;
