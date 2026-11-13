SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS uf_oaones_generic_ai_call_log (
    id BIGINT NOT NULL,
    connector_id BIGINT NULL,
    connector_name VARCHAR(100) NOT NULL DEFAULT '',
    model VARCHAR(200) NOT NULL DEFAULT '',
    call_type VARCHAR(20) NOT NULL DEFAULT 'chat',
    status VARCHAR(20) NOT NULL DEFAULT '',
    http_status INT NOT NULL DEFAULT 0,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    request_time VARCHAR(30) NOT NULL DEFAULT '',
    error_message VARCHAR(1000) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    KEY idx_oaones_ai_call_model (model),
    KEY idx_oaones_ai_call_time (request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAones公共AI调用日志';
