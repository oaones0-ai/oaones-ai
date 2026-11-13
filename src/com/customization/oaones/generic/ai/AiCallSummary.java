package com.customization.oaones.generic.ai;

/**
 * AI 调用量与 token 汇总。
 */
public class AiCallSummary {
    private String model;
    private String connectorName;
    private Long calls;
    private Long successCalls;
    private Long failedCalls;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Double averageDurationMs;
    private Long modelCount;
    private String lastCallTime;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getConnectorName() { return connectorName; }
    public void setConnectorName(String connectorName) { this.connectorName = connectorName; }
    public Long getCalls() { return calls; }
    public void setCalls(Long calls) { this.calls = calls; }
    public Long getSuccessCalls() { return successCalls; }
    public void setSuccessCalls(Long successCalls) { this.successCalls = successCalls; }
    public Long getFailedCalls() { return failedCalls; }
    public void setFailedCalls(Long failedCalls) { this.failedCalls = failedCalls; }
    public Long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }
    public Long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public Double getAverageDurationMs() { return averageDurationMs; }
    public void setAverageDurationMs(Double averageDurationMs) { this.averageDurationMs = averageDurationMs; }
    public Long getModelCount() { return modelCount; }
    public void setModelCount(Long modelCount) { this.modelCount = modelCount; }
    public String getLastCallTime() { return lastCallTime; }
    public void setLastCallTime(String lastCallTime) { this.lastCallTime = lastCallTime; }
}
