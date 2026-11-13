IF OBJECT_ID('dbo.uf_oaones_generic_ai_config', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.uf_oaones_generic_ai_config (
        id BIGINT NOT NULL PRIMARY KEY,
        api_url NVARCHAR(500) NOT NULL DEFAULT '',
        api_key NVARCHAR(1000) NOT NULL DEFAULT '',
        model NVARCHAR(200) NOT NULL DEFAULT '',
        request_params NVARCHAR(MAX) NULL,
        update_time VARCHAR(30) NOT NULL DEFAULT ''
    );
END
;
