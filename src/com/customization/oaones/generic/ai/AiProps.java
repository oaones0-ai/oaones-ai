package com.customization.oaones.generic.ai;

import weaver.general.BaseBean;

/**
 * 公共 AI 配置文件，默认读取 oaones.generic.ai.properties。
 */
public final class AiProps {
    private static final String PROP_FILE = "oaones.generic.ai";

    private AiProps() {
    }

    private static String prop(String key, String def) {
        try {
            String value = new BaseBean().getPropValue(PROP_FILE, key);
            return value == null || value.trim().length() == 0 ? def : value.trim();
        } catch (Throwable t) {
            return def;
        }
    }

    private static int propInt(String key, int def) {
        String value = prop(key, "");
        try {
            return value.length() == 0 ? def : Integer.parseInt(value);
        } catch (Exception ignore) {
            return def;
        }
    }

    public static String configTable() {
        return prop("ai.config_table", "uf_oaones_generic_ai_config");
    }

    public static String callLogTable() {
        return prop("ai.call_log_table", "uf_oaones_generic_ai_call_log");
    }

    public static String adminIds() {
        return prop("ai.admin.ids", "");
    }

    public static int adminRoleId() {
        return propInt("ai.admin.roleid", 0);
    }

    public static int isMyOpenAi() {
        return propInt("ismyopenai", 1);
    }

    public static String presetAiUrl() {
        return prop("modelurl", "");
    }

    public static String presetAiModel() {
        return prop("modelname", "deepseek-flash");
    }

    public static int connectTimeoutMs() {
        int value = propInt("ai.http.connect_timeout_ms", 8000);
        return value < 1000 ? 8000 : value;
    }

    public static int readTimeoutMs() {
        int value = propInt("ai.http.read_timeout_ms", 45000);
        return value < 1000 ? 45000 : value;
    }

    public static int testTimeoutMs() {
        int value = propInt("ai.http.test_timeout_ms", 20000);
        return value < 1000 ? 20000 : value;
    }
}
