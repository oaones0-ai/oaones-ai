DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt FROM user_tables
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CONFIG';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE uf_oaones_generic_ai_config (
            id NUMBER(19) NOT NULL PRIMARY KEY,
            api_url VARCHAR2(500) NOT NULL,
            api_key VARCHAR2(1000) NOT NULL,
            model VARCHAR2(200) NOT NULL,
            request_params CLOB,
            update_time VARCHAR2(30) NOT NULL
        )';
    END IF;
END;
/
