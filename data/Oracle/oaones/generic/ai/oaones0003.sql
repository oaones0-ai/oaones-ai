DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt FROM user_tab_cols
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CONFIG' AND column_name = 'CREATE_TIME';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE uf_oaones_generic_ai_config ADD (create_time VARCHAR2(30))';
    END IF;
END;
/

UPDATE uf_oaones_generic_ai_config
SET create_time = update_time
WHERE create_time IS NULL AND update_time IS NOT NULL;

UPDATE uf_oaones_generic_ai_config
SET connector_name = '默认 AI'
WHERE connector_name IS NULL OR TRIM(connector_name) IS NULL;

DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt
    FROM uf_oaones_generic_ai_config
    WHERE enabled = 1 AND is_default = 1;

    IF v_cnt = 0 THEN
        UPDATE uf_oaones_generic_ai_config
        SET is_default = 1
        WHERE id = (
            SELECT MIN(id) FROM uf_oaones_generic_ai_config WHERE enabled = 1
        );
    END IF;
END;
/

COMMIT;
/
