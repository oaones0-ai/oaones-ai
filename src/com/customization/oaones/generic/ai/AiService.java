package com.customization.oaones.generic.ai;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 公共 AI 服务。业务模块不得直接实现供应商 HTTP 调用。
 */
public interface AiService {

    JSONObject getConfig();

    JSONObject saveConfig(JSONObject input);

    JSONObject listConnectors();

    JSONObject saveConnector(JSONObject input);

    JSONObject setConnectorEnabled(JSONObject input);

    JSONObject setDefaultConnector(JSONObject input);

    JSONObject testConnector(JSONObject input);

    JSONObject usageSummary();

    JSONObject connectorOptions();

    boolean isConfigured();

    boolean isConfigured(String connectorId);

    boolean supportsInputType(String inputType);

    String chat(String systemPrompt, String userPrompt) throws Exception;

    String chat(String connectorId, String systemPrompt, String userPrompt) throws Exception;

    String chatWithImages(String systemPrompt, String userPrompt,
                          List<String> imageDataUrls) throws Exception;
}
