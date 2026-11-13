package com.customization.oaones.generic.ai;

/**
 * AI Service 工厂。业务模块通过接口获取实现。
 */
public final class AiServiceFactory {
    private AiServiceFactory() {
    }

    public static AiService create() {
        return new OpenAiCompatibleAiService();
    }

    /**
     * 创建指定连接器的服务；connectorId 为空时使用默认连接器。
     */
    public static AiService create(String connectorId) {
        return new OpenAiCompatibleAiService(connectorId);
    }

    /**
     * 创建指定连接器和调用超时的服务；timeoutSeconds 小于等于 0 时使用全局配置。
     */
    public static AiService create(String connectorId, int timeoutSeconds) {
        return new OpenAiCompatibleAiService(connectorId, timeoutSeconds);
    }

    /**
     * 创建不限制读取和整体调用超时的服务；连接建立仍使用全局连接超时。
     */
    public static AiService createUnlimited(String connectorId) {
        return new OpenAiCompatibleAiService(connectorId, 0, true);
    }
}
