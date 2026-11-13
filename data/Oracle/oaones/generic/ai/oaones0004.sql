DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_cnt FROM user_tab_cols
    WHERE table_name = 'UF_OAONES_GENERIC_AI_CONFIG'
      AND column_name = 'SUPPORTED_TYPES';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE uf_oaones_generic_ai_config ADD (supported_types VARCHAR2(200 CHAR) DEFAULT ''text'' NOT NULL)';
    END IF;
END;
/

UPDATE uf_oaones_generic_ai_config
SET supported_types = 'text'
WHERE supported_types IS NULL OR TRIM(supported_types) IS NULL;

COMMIT;
