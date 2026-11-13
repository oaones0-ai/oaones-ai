IF OBJECT_ID('dbo.uf_oaones_generic_ai_call_log', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.uf_oaones_generic_ai_call_log (
        id BIGINT NOT NULL PRIMARY KEY,
        connector_id BIGINT NULL,
        connector_name NVARCHAR(100) NOT NULL DEFAULT N'',
        model NVARCHAR(200) NOT NULL DEFAULT N'',
        call_type VARCHAR(20) NOT NULL DEFAULT 'chat',
        status VARCHAR(20) NOT NULL DEFAULT '',
        http_status INT NOT NULL DEFAULT 0,
        prompt_tokens BIGINT NOT NULL DEFAULT 0,
        completion_tokens BIGINT NOT NULL DEFAULT 0,
        total_tokens BIGINT NOT NULL DEFAULT 0,
        duration_ms BIGINT NOT NULL DEFAULT 0,
        request_time VARCHAR(30) NOT NULL DEFAULT '',
        error_message NVARCHAR(1000) NOT NULL DEFAULT N''
    );
END
;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.uf_oaones_generic_ai_call_log')
      AND name = 'idx_oaones_ai_call_model'
)
BEGIN
    CREATE INDEX idx_oaones_ai_call_model
        ON dbo.uf_oaones_generic_ai_call_log(model);
END
;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.uf_oaones_generic_ai_call_log')
      AND name = 'idx_oaones_ai_call_time'
)
BEGIN
    CREATE INDEX idx_oaones_ai_call_time
        ON dbo.uf_oaones_generic_ai_call_log(request_time);
END
;
