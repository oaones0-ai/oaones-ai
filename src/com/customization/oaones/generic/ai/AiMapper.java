package com.customization.oaones.generic.ai;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公共 AI 配置 Mapper。
 */
public interface AiMapper {
    AiConfig getConfig(@Param("table") String table);

    List<AiConfig> listConfigs(@Param("table") String table);

    AiConfig getConfigById(@Param("table") String table, @Param("id") Long id);

    Long nextId(@Param("table") String table);

    int insertConfig(@Param("table") String table, @Param("config") AiConfig config);

    int updateConfig(@Param("table") String table, @Param("config") AiConfig config);

    int updateEnabled(@Param("table") String table, @Param("id") Long id,
                      @Param("enabled") Integer enabled);

    int setDefault(@Param("table") String table, @Param("id") Long id);

    int clearDefaults(@Param("table") String table);

    Long nextCallLogId(@Param("table") String table);

    int insertCallLog(@Param("table") String table, @Param("log") AiCallLog log);

    AiCallSummary getCallSummary(@Param("table") String table);

    List<AiCallSummary> listCallSummaries(@Param("callLogTable") String callLogTable,
                                          @Param("configTable") String configTable);
}
