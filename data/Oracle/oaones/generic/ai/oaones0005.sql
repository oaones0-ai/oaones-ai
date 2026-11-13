DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt FROM user_tables
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CALL_LOG';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE uf_oaones_generic_ai_call_log (
            id NUMBER(19) NOT NULL PRIMARY KEY,
            connector_id NUMBER(19),
            connector_name VARCHAR2(100 CHAR) DEFAULT '''' NOT NULL,
            model VARCHAR2(200 CHAR) DEFAULT '''' NOT NULL,
            call_type VARCHAR2(20 CHAR) DEFAULT ''chat'' NOT NULL,
            status VARCHAR2(20 CHAR) DEFAULT '''' NOT NULL,
            http_status NUMBER(10) DEFAULT 0 NOT NULL,
            prompt_tokens NUMBER(19) DEFAULT 0 NOT NULL,
            completion_tokens NUMBER(19) DEFAULT 0 NOT NULL,
            total_tokens NUMBER(19) DEFAULT 0 NOT NULL,
            duration_ms NUMBER(19) DEFAULT 0 NOT NULL,
            request_time VARCHAR2(30 CHAR) DEFAULT '''' NOT NULL,
            error_message VARCHAR2(1000 CHAR) DEFAULT '''' NOT NULL
        )';
    END IF;
END;
/

DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt FROM user_indexes
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CALL_LOG'
      AND index_name = 'IDX_OAONES_AI_CALL_MODEL';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_oaones_ai_call_model ON uf_oaones_generic_ai_call_log(model)';
    END IF;

    SELECT COUNT(1) INTO v_cnt FROM user_indexes
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CALL_LOG'
      AND index_name = 'IDX_OAONES_AI_CALL_TIME';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_oaones_ai_call_time ON uf_oaones_generic_ai_call_log(request_time)';
    END IF;
END;
/

COMMIT;
