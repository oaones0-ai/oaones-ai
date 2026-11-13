package com.customization.oaones.generic.ai;

/**
 * 公共 OpenAI Chat Completions 兼容服务配置。
 */
public class AiConfig {
    private Long id;
    private String name;
    private String apiUrl;
    private String apiKey;
    private String model;
    private String supportedTypes;
    private String requestParams;
    private Integer enabled;
    private Integer defaultConnector;
    private String createTime;
    private String updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSupportedTypes() { return supportedTypes; }
    public void setSupportedTypes(String supportedTypes) { this.supportedTypes = supportedTypes; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getDefaultConnector() { return defaultConnector; }
    public void setDefaultConnector(Integer defaultConnector) { this.defaultConnector = defaultConnector; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
