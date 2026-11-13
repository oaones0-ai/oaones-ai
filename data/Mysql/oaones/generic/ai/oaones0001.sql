SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS uf_oaones_generic_ai_config (
    id BIGINT NOT NULL,
    api_url VARCHAR(500) NOT NULL DEFAULT '',
    api_key VARCHAR(1000) NOT NULL DEFAULT '',
    model VARCHAR(200) NOT NULL DEFAULT '',
    request_params TEXT NULL,
    update_time VARCHAR(30) NOT NULL DEFAULT '',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAones公共AI配置';
