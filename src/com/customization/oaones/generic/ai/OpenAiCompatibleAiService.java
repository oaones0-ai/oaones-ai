package com.customization.oaones.generic.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Chat Completions 兼容实现。
 *
 * 一个实例绑定一个连接器 ID；未指定 ID 时始终解析默认连接器。
 * 显式指定的连接器不存在或已禁用时直接拒绝，不回退到默认连接器。
 */
public class OpenAiCompatibleAiService implements AiService {
    private static final String DEFAULT_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String DEFAULT_CONNECTOR_NAME = "默认 AI";
    private static final String UNRECORDED_CONNECTOR_NAME = "未记录 AI 别名";
    private static final int MAX_CONNECTORS = 50;
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final Object CREATE_LOCK = new Object();
    private static final String DEFAULT_SUPPORTED_TYPES = "text";
    private static final String[] SUPPORTED_TYPE_CODES = {"text", "image", "video", "audio"};

    private final String connectorId;
    private final int timeoutSeconds;
    private final boolean unlimitedTimeout;

    public OpenAiCompatibleAiService() {
        this(null, 0);
    }

    public OpenAiCompatibleAiService(String connectorId) {
        this(connectorId, 0);
    }

    public OpenAiCompatibleAiService(String connectorId, int timeoutSeconds) {
        this(connectorId, timeoutSeconds, false);
    }

    public OpenAiCompatibleAiService(String connectorId, int timeoutSeconds,
                                     boolean unlimitedTimeout) {
        this.connectorId = text(connectorId);
        this.timeoutSeconds = timeoutSeconds;
        this.unlimitedTimeout = unlimitedTimeout;
    }

    @Override
    public JSONObject getConfig() {
        return configView(effective(resolveDefault(loadAll())));
    }

    /**
     * 兼容旧版单连接器接口：保存默认连接器（没有默认连接器时创建第一个）。
     */
    @Override
    public JSONObject saveConfig(JSONObject input) {
        JSONObject source = input == null ? new JSONObject() : input;
        AiConfig existing = resolveDefault(loadAll());
        if (existing != null && !source.containsKey("id")) {
            source.put("id", existing.getId());
        }
        JSONObject result = saveConnector(source);
        JSONObject connector = result.getJSONObject("connector");
        return connector == null ? configView(effective(null)) : connector;
    }

    @Override
    public JSONObject listConnectors() {
        return connectorListView(loadAll());
    }

    @Override
    public JSONObject saveConnector(JSONObject input) {
        JSONObject source = input == null ? new JSONObject() : input;
        List<AiConfig> all = loadAll();
        String rawId = firstText(source.get("id"), source.get("connectorId"));
        Long id = parseId(rawId);
        if (rawId.length() > 0 && id == null) {
            throw new RuntimeException("AI 连接器 ID 格式不正确");
        }
        AiConfig existing = findById(all, id);
        if (id != null && existing == null) {
            throw new RuntimeException("AI 连接器不存在或已被移除");
        }
        boolean creating = existing == null;
        if (creating && all.size() >= MAX_CONNECTORS) {
            throw new RuntimeException("最多只能维护 " + MAX_CONNECTORS + " 个 AI 连接器");
        }

        AiConfig config = effective(existing);
        applyName(config, source, all);
        applyEnabled(config, source, creating);
        applyConnection(config, source, creating);
        applySupportedTypes(config, source, creating);
        applyApiKey(config, source);
        boolean makeDefault = source.containsKey("defaultConnector")
                && source.getBooleanValue("defaultConnector");
        if (makeDefault && !enabled(config)) {
            throw new RuntimeException("禁用的 AI 连接器不能设为默认");
        }
        if (creating) {
            config.setDefaultConnector(Integer.valueOf(0));
            config.setCreateTime(now());
        }
        config.setUpdateTime(now());

        if (creating) {
            synchronized (CREATE_LOCK) {
                insertWithGeneratedId(config);
            }
        } else {
            AiMyBatis.run(AiMapper.class, mapper ->
                    mapper.updateConfig(AiProps.configTable(), config));
        }

        if (makeDefault) {
            setDefault(config.getId());
        } else if (!hasPersistedDefault(loadAll()) && enabled(config)) {
            setDefault(config.getId());
        }

        List<AiConfig> saved = loadAll();
        JSONObject result = connectorListView(saved);
        result.put("connector", configView(effective(findById(saved, config.getId()))));
        result.put("message", creating ? "AI 连接器已新增" : "AI 连接器已保存");
        return result;
    }

    @Override
    public JSONObject setConnectorEnabled(JSONObject input) {
        JSONObject source = input == null ? new JSONObject() : input;
        Long id = parseId(firstText(source.get("id"), source.get("connectorId")));
        AiConfig target = findById(loadAll(), id);
        if (target == null) {
            throw new RuntimeException("AI 连接器不存在或已被移除");
        }
        boolean enable = source.containsKey("enabled")
                ? source.getBooleanValue("enabled") : !enabled(target);
        AiMyBatis.run(AiMapper.class, mapper ->
                mapper.updateEnabled(AiProps.configTable(), id, Integer.valueOf(enable ? 1 : 0)));

        List<AiConfig> after = loadAll();
        if (!hasPersistedDefault(after)) {
            AiConfig next = firstEnabled(after);
            if (next == null) {
                AiMyBatis.run(AiMapper.class, mapper -> mapper.clearDefaults(AiProps.configTable()));
            } else {
                setDefault(next.getId());
            }
        }

        List<AiConfig> saved = loadAll();
        JSONObject result = connectorListView(saved);
        result.put("message", enable ? "AI 连接器已启用" : "AI 连接器已禁用");
        return result;
    }

    @Override
    public JSONObject setDefaultConnector(JSONObject input) {
        JSONObject source = input == null ? new JSONObject() : input;
        Long id = parseId(firstText(source.get("id"), source.get("connectorId")));
        AiConfig target = findById(loadAll(), id);
        if (target == null) {
            throw new RuntimeException("AI 连接器不存在或已被移除");
        }
        if (!enabled(target)) {
            throw new RuntimeException("禁用的 AI 连接器不能设为默认");
        }
        setDefault(id);
        JSONObject result = connectorListView(loadAll());
        result.put("message", "默认 AI 已切换为「" + connectorName(target) + "」");
        return result;
    }

    @Override
    public JSONObject testConnector(JSONObject input) {
        JSONObject source = input == null ? new JSONObject() : input;
        List<AiConfig> all = loadAll();
        String rawId = firstText(source.get("id"), source.get("connectorId"));
        Long id = parseId(rawId);
        if (rawId.length() > 0 && id == null) {
            throw new RuntimeException("AI 连接器 ID 格式不正确");
        }
        AiConfig existing = findById(all, id);
        if (id != null && existing == null) {
            throw new RuntimeException("AI 连接器不存在或已被移除");
        }
        if (existing == null && !hasText(text(source.getString("name")))) {
            throw new RuntimeException("测试连接前请先填写 AI 别名");
        }

        AiConfig config = effective(existing);
        applyName(config, source, all);
        applyConnection(config, source, existing == null);
        applyApiKey(config, source);
        if (!ready(config)) {
            throw new RuntimeException("请先填写 API 地址、Token 和模型，再测试连接");
        }

        ChatResult chatResult;
        try {
            chatResult = executeChat(config,
                    "You are a connection test assistant.",
                    "Reply with exactly: OK",
                    AiProps.testTimeoutMs(),
                    true,
                    "test");
        } catch (Exception e) {
            String message = text(e.getMessage());
            throw new RuntimeException(message.length() == 0
                    ? "AI 连接测试失败" : "AI 连接测试失败：" + message, e);
        }

        JSONObject data = new JSONObject();
        data.put("success", Boolean.TRUE);
        data.put("message", "连接成功，模型已返回响应");
        data.put("latencyMs", Long.valueOf(chatResult.durationMs));
        data.put("reply", abbreviate(chatResult.content, 120));
        data.put("model", config.getModel());
        data.put("connectorId", config.getId() == null ? "" : String.valueOf(config.getId()));
        return data;
    }

    @Override
    public JSONObject usageSummary() {
        AiCallSummary summary = AiMyBatis.call(AiMapper.class, mapper ->
                mapper.getCallSummary(AiProps.callLogTable()));
        if (summary == null) {
            summary = new AiCallSummary();
        }
        List<AiCallSummary> models = AiMyBatis.call(AiMapper.class, mapper ->
                mapper.listCallSummaries(AiProps.callLogTable(), AiProps.configTable()));

        JSONObject totals = new JSONObject();
        totals.put("calls", safeLong(summary.getCalls()));
        totals.put("successCalls", safeLong(summary.getSuccessCalls()));
        totals.put("failedCalls", safeLong(summary.getFailedCalls()));
        totals.put("promptTokens", safeLong(summary.getPromptTokens()));
        totals.put("completionTokens", safeLong(summary.getCompletionTokens()));
        totals.put("totalTokens", safeLong(summary.getTotalTokens()));
        totals.put("averageDurationMs", summary.getAverageDurationMs() == null
                ? Double.valueOf(0) : summary.getAverageDurationMs());
        totals.put("modelCount", safeLong(summary.getModelCount()));

        JSONArray modelRows = new JSONArray();
        if (models != null) {
            for (AiCallSummary row : models) {
                JSONObject item = new JSONObject();
                item.put("model", defaultValue(row.getModel(), "未记录模型"));
                item.put("connectorName", defaultValue(row.getConnectorName(), "未记录 AI 别名"));
                item.put("calls", safeLong(row.getCalls()));
                item.put("successCalls", safeLong(row.getSuccessCalls()));
                item.put("failedCalls", safeLong(row.getFailedCalls()));
                item.put("promptTokens", safeLong(row.getPromptTokens()));
                item.put("completionTokens", safeLong(row.getCompletionTokens()));
                item.put("totalTokens", safeLong(row.getTotalTokens()));
                item.put("averageDurationMs", row.getAverageDurationMs() == null
                        ? Double.valueOf(0) : row.getAverageDurationMs());
                item.put("lastCallTime", defaultValue(row.getLastCallTime(), ""));
                modelRows.add(item);
            }
        }

        JSONObject data = new JSONObject();
        data.put("summary", totals);
        data.put("models", modelRows);
        return data;
    }

    @Override
    public JSONObject connectorOptions() {
        List<AiConfig> all = loadAll();
        List<AiConfig> enabledList = enabledConfigs(all);
        Long selectedDefaultId = resolvedDefaultId(all);
        JSONArray options = new JSONArray();
        for (AiConfig config : enabledList) {
            AiConfig effective = effective(config);
            JSONObject option = new JSONObject();
            option.put("id", String.valueOf(effective.getId()));
            option.put("name", connectorName(effective));
            option.put("model", effective.getModel());
            option.put("supportedTypes", supportedTypeArray(effective.getSupportedTypes()));
            option.put("configured", Boolean.valueOf(ready(effective)));
            option.put("defaultConnector", Boolean.valueOf(effective.getId().equals(selectedDefaultId)));
            options.add(option);
        }
        JSONObject data = new JSONObject();
        data.put("connectors", options);
        data.put("defaultId", selectedDefaultId == null ? "" : String.valueOf(selectedDefaultId));
        data.put("hasAvailable", Boolean.valueOf(!enabledList.isEmpty()));
        return data;
    }

    @Override
    public boolean isConfigured() {
        return isConfigured(connectorId);
    }

    @Override
    public boolean isConfigured(String requestedConnectorId) {
        try {
            return ready(effective(resolve(requestedConnectorId)));
        } catch (RuntimeException ignore) {
            return false;
        }
    }

    @Override
    public boolean supportsInputType(String inputType) {
        String type = text(inputType).toLowerCase(Locale.ROOT);
        if (supportedTypeIndex(type) < 0) {
            return false;
        }
        try {
            return supportsType(effective(resolve(connectorId)), type);
        } catch (RuntimeException ignore) {
            return false;
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) throws Exception {
        return chat(connectorId, systemPrompt, userPrompt);
    }

    @Override
    public String chat(String requestedConnectorId, String systemPrompt, String userPrompt)
            throws Exception {
        AiConfig config = effective(resolve(requestedConnectorId));
        requireReady(config);
        int readTimeoutMs = unlimitedTimeout ? 0 : timeoutSeconds > 0
                ? timeoutSeconds * 1000 : AiProps.readTimeoutMs();
        return executeChat(config, systemPrompt, userPrompt, readTimeoutMs,
                timeoutSeconds > 0 && !unlimitedTimeout, "chat").content;
    }

    @Override
    public String chatWithImages(String systemPrompt, String userPrompt,
                                 List<String> imageDataUrls) throws Exception {
        AiConfig config = effective(resolve(connectorId));
        requireReady(config);
        if (!supportsType(config, "image")) {
            throw new RuntimeException("AI 连接器「" + connectorName(config)
                    + "」未标记图片能力，不能执行 OCR");
        }
        if (imageDataUrls == null || imageDataUrls.isEmpty()) {
            throw new RuntimeException("AI 图片识别请求缺少图片");
        }
        JSONArray content = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", text(userPrompt));
        content.add(textPart);
        for (String imageDataUrl : imageDataUrls) {
            String url = text(imageDataUrl);
            if (!url.matches("^data:image/(?:png|jpeg|jpg|webp|gif|bmp);base64,.+$")) {
                throw new RuntimeException("AI 图片识别请求的图片格式不正确");
            }
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", url);
            imageUrl.put("detail", "auto");
            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);
            content.add(imagePart);
        }
        int readTimeoutMs = unlimitedTimeout ? 0 : timeoutSeconds > 0
                ? timeoutSeconds * 1000 : AiProps.readTimeoutMs();
        return executeChat(config, systemPrompt, content, readTimeoutMs,
                timeoutSeconds > 0 && !unlimitedTimeout, "vision", 5000).content;
    }

    private ChatResult executeChat(AiConfig config, String systemPrompt, String userPrompt,
                                   int readTimeoutMs, boolean hardCallTimeout, String callType)
            throws Exception {
        return executeChat(config, systemPrompt, (Object) text(userPrompt), readTimeoutMs,
                hardCallTimeout, callType);
    }

    private ChatResult executeChat(AiConfig config, String systemPrompt, Object userContent,
                                   int readTimeoutMs, boolean hardCallTimeout, String callType)
            throws Exception {
        return executeChat(config, systemPrompt, userContent, readTimeoutMs,
                hardCallTimeout, callType, null);
    }

    private ChatResult executeChat(AiConfig config, String systemPrompt, Object userContent,
                                   int readTimeoutMs, boolean hardCallTimeout, String callType,
                                   Integer maxTokens)
            throws Exception {
        long startedAt = System.nanoTime();
        ChatResult chatResult = new ChatResult();
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", text(systemPrompt));
        messages.add(system);
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userContent == null ? "" : userContent);
        messages.add(user);

        JSONObject payload = new JSONObject();
        payload.put("model", config.getModel());
        payload.put("messages", messages);
        payload.put("stream", Boolean.FALSE);
        payload.putAll(parseRequestParams(config.getRequestParams()));
        int configuredMaxTokens = payload.getIntValue("max_tokens");
        if (maxTokens != null && (configuredMaxTokens <= 0
                || configuredMaxTokens > maxTokens.intValue())) {
            payload.put("max_tokens", maxTokens);
        }

        int connectTimeoutMs = readTimeoutMs > 0
                ? Math.min(AiProps.connectTimeoutMs(), readTimeoutMs)
                : AiProps.connectTimeoutMs();
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
        if (hardCallTimeout) {
            clientBuilder.callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
        }
        OkHttpClient client = clientBuilder.build();
        Request request = new Request.Builder()
                .url(endpoint(config.getApiUrl()))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .post(RequestBody.create(JSON_MEDIA, payload.toJSONString()))
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            chatResult.httpStatus = response.code();
            ResponseBody body = response.body();
            String raw = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("OpenAI 兼容服务返回 HTTP " + response.code()
                        + "：" + abbreviate(raw, 500));
            }
            JSONObject result = JSONObject.parseObject(raw);
            JSONArray choices = result == null ? null : result.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("OpenAI 兼容服务响应缺少 choices："
                        + abbreviate(raw, 500));
            }
            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first == null ? null : first.getJSONObject("message");
            String content = messageContent(message);
            applyUsage(chatResult, result.getJSONObject("usage"));
            if (content.length() == 0) {
                throw new RuntimeException("OpenAI 兼容服务返回了空内容");
            }
            chatResult.content = content;
            chatResult.durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            recordCall(config, callType, "success", chatResult, null);
            return chatResult;
        } catch (Exception e) {
            chatResult.durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            recordCall(config, callType, "failed", chatResult, e);
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private boolean supportsType(AiConfig config, String type) {
        String normalized = text(type).toLowerCase(Locale.ROOT);
        for (String code : safeSupportedTypes(config == null
                ? "" : config.getSupportedTypes()).split(",")) {
            if (normalized.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private AiConfig resolve(String requestedConnectorId) {
        List<AiConfig> all = loadAll();
        String idText = text(requestedConnectorId);
        if (idText.length() == 0) {
            return resolveDefault(all);
        }
        Long id = parseId(idText);
        if (id == null) {
            throw new RuntimeException("AI 连接器 ID 格式不正确");
        }
        AiConfig config = findById(all, id);
        if (config == null) {
            throw new RuntimeException("指定的 AI 连接器不存在");
        }
        if (!enabled(config)) {
            throw new RuntimeException("指定的 AI 连接器已禁用，不能调用");
        }
        return config;
    }

    private AiConfig resolveDefault(List<AiConfig> all) {
        Long id = defaultId(all);
        if (id != null) {
            AiConfig config = findById(all, id);
            if (config != null && enabled(config)) {
                return config;
            }
        }
        return firstEnabled(all);
    }

    private Long defaultId(List<AiConfig> all) {
        for (AiConfig config : all) {
            if (enabled(config) && isDefault(config)) {
                return config.getId();
            }
        }
        return null;
    }

    private boolean hasPersistedDefault(List<AiConfig> all) {
        return defaultId(all) != null;
    }

    private Long resolvedDefaultId(List<AiConfig> all) {
        Long id = defaultId(all);
        if (id != null) {
            return id;
        }
        AiConfig fallback = firstEnabled(all);
        return fallback == null ? null : fallback.getId();
    }

    private void insertWithGeneratedId(AiConfig config) {
        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            config.setId(AiMyBatis.call(AiMapper.class, mapper ->
                    mapper.nextId(AiProps.configTable())));
            try {
                AiMyBatis.run(AiMapper.class, mapper ->
                        mapper.insertConfig(AiProps.configTable(), config));
                return;
            } catch (RuntimeException e) {
                lastError = e;
                if (!isDuplicateKey(e)) {
                    throw e;
                }
            }
        }
        throw lastError == null
                ? new RuntimeException("AI 连接器 ID 生成失败") : lastError;
    }

    private boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String type = current.getClass().getName();
            if (type.indexOf("DuplicateKey") >= 0
                    || type.indexOf("SQLIntegrityConstraintViolation") >= 0) {
                return true;
            }
            String message = text(current.getMessage()).toLowerCase(Locale.ROOT);
            if (message.indexOf("duplicate entry") >= 0
                    || message.indexOf("duplicate key") >= 0
                    || message.indexOf("unique constraint") >= 0
                    || message.indexOf("ora-00001") >= 0) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void setDefault(Long id) {
        AiMyBatis.run(AiMapper.class, mapper ->
                mapper.setDefault(AiProps.configTable(), id));
    }

    private List<AiConfig> loadAll() {
        List<AiConfig> rows = AiMyBatis.call(AiMapper.class, mapper ->
                mapper.listConfigs(AiProps.configTable()));
        return rows == null ? new ArrayList<AiConfig>() : rows;
    }

    private AiConfig findById(List<AiConfig> all, Long id) {
        if (id == null) {
            return null;
        }
        for (AiConfig config : all) {
            if (id.equals(config.getId())) {
                return config;
            }
        }
        return null;
    }

    private List<AiConfig> enabledConfigs(List<AiConfig> all) {
        List<AiConfig> result = new ArrayList<AiConfig>();
        for (AiConfig config : all) {
            if (enabled(config)) {
                result.add(config);
            }
        }
        return result;
    }

    private AiConfig firstEnabled(List<AiConfig> all) {
        for (AiConfig config : all) {
            if (enabled(config)) {
                return config;
            }
        }
        return null;
    }

    private void applyName(AiConfig config, JSONObject source, List<AiConfig> all) {
        String name = source.containsKey("name")
                ? text(source.getString("name")) : connectorName(config);
        if (name.length() == 0) {
            name = DEFAULT_CONNECTOR_NAME;
        }
        if (name.length() > 100) {
            throw new RuntimeException("AI 别名不能超过 100 个字符");
        }
        for (AiConfig other : all) {
            if (config.getId() != null && config.getId().equals(other.getId())) {
                continue;
            }
            if (name.equalsIgnoreCase(connectorName(other))) {
                throw new RuntimeException("AI 别名「" + name + "」已存在");
            }
        }
        config.setName(name);
    }

    private void applyEnabled(AiConfig config, JSONObject source, boolean creating) {
        if (source.containsKey("enabled")) {
            config.setEnabled(Integer.valueOf(source.getBooleanValue("enabled") ? 1 : 0));
        } else if (creating || config.getEnabled() == null) {
            config.setEnabled(Integer.valueOf(1));
        }
    }

    private void applyConnection(AiConfig config, JSONObject source, boolean creating) {
        boolean preset = AiProps.isMyOpenAi() == 0;
        if (preset) {
            String presetUrl = AiProps.presetAiUrl();
            config.setApiUrl(presetUrl.length() == 0
                    ? defaultValue(config.getApiUrl(), DEFAULT_API_URL) : presetUrl);
            config.setModel(AiProps.presetAiModel());
            if (!hasText(config.getRequestParams())) {
                config.setRequestParams("{}");
            }
            return;
        }

        String apiUrl = source.containsKey("apiUrl")
                ? text(source.getString("apiUrl"))
                : (creating ? "" : text(config.getApiUrl()));
        apiUrl = defaultValue(apiUrl, DEFAULT_API_URL);
        validateApiUrl(apiUrl);
        config.setApiUrl(apiUrl);

        String model = source.containsKey("model")
                ? text(source.getString("model")) : text(config.getModel());
        if (model.length() == 0) {
            throw new RuntimeException("模型名不能为空");
        }
        if (model.length() > 200) {
            throw new RuntimeException("模型名不能超过 200 个字符");
        }
        config.setModel(model);

        String requestParams = source.containsKey("requestParams")
                ? normalizeRequestParams(source.getString("requestParams"))
                : normalizeRequestParams(defaultValue(config.getRequestParams(), "{}"));
        config.setRequestParams(requestParams);
    }

    private void applySupportedTypes(AiConfig config, JSONObject source, boolean creating) {
        if (source.containsKey("supportedTypes")) {
            config.setSupportedTypes(normalizeSupportedTypes(source.get("supportedTypes")));
        } else if (creating || !hasText(config.getSupportedTypes())) {
            config.setSupportedTypes(DEFAULT_SUPPORTED_TYPES);
        }
    }

    private void applyApiKey(AiConfig config, JSONObject source) {
        String apiKey = text(source.getString("apiKey"));
        if ("****".equals(apiKey)) {
            apiKey = "";
        }
        if (apiKey.indexOf('\n') >= 0 || apiKey.indexOf('\r') >= 0) {
            throw new RuntimeException("API Token 格式不正确");
        }
        if (apiKey.length() > 1000) {
            throw new RuntimeException("API Token 不能超过 1000 个字符");
        }
        if (source.getBooleanValue("clearApiKey")) {
            config.setApiKey("");
        } else if (apiKey.length() > 0) {
            config.setApiKey(apiKey);
        } else if (config.getApiKey() == null) {
            config.setApiKey("");
        }
    }

    private JSONObject connectorListView(List<AiConfig> all) {
        JSONArray connectors = new JSONArray();
        int enabledCount = 0;
        int configuredCount = 0;
        Long resolvedDefaultId = resolvedDefaultId(all);
        for (AiConfig config : all) {
            AiConfig effective = effective(config);
            effective.setDefaultConnector(Integer.valueOf(
                    effective.getId() != null && effective.getId().equals(resolvedDefaultId) ? 1 : 0));
            connectors.add(configView(effective));
            if (enabled(effective)) {
                enabledCount++;
                if (ready(effective)) {
                    configuredCount++;
                }
            }
        }
        JSONObject data = new JSONObject();
        data.put("connectors", connectors);
        data.put("defaultId", resolvedDefaultId == null ? "" : String.valueOf(resolvedDefaultId));
        data.put("total", Integer.valueOf(all.size()));
        data.put("enabledCount", Integer.valueOf(enabledCount));
        data.put("configuredCount", Integer.valueOf(configuredCount));
        data.put("presetMode", Boolean.valueOf(AiProps.isMyOpenAi() == 0));
        return data;
    }

    private AiConfig effective(AiConfig stored) {
        AiConfig config = stored == null ? new AiConfig() : stored;
        config.setEnabled(Integer.valueOf(enabled(config) ? 1 : 0));
        config.setDefaultConnector(Integer.valueOf(isDefault(config) ? 1 : 0));
        if (AiProps.isMyOpenAi() == 0) {
            String url = AiProps.presetAiUrl();
            config.setApiUrl(url.length() == 0 ? defaultValue(config.getApiUrl(), DEFAULT_API_URL) : url);
            config.setModel(AiProps.presetAiModel());
        } else {
            config.setApiUrl(defaultValue(config.getApiUrl(), DEFAULT_API_URL));
            config.setModel(defaultValue(config.getModel(), ""));
        }
        config.setApiKey(defaultValue(config.getApiKey(), ""));
        config.setRequestParams(normalizeRequestParams(defaultValue(config.getRequestParams(), "{}")));
        config.setSupportedTypes(safeSupportedTypes(config.getSupportedTypes()));
        config.setCreateTime(defaultValue(config.getCreateTime(), ""));
        config.setUpdateTime(defaultValue(config.getUpdateTime(), ""));
        config.setName(connectorName(config));
        return config;
    }

    private JSONObject configView(AiConfig config) {
        JSONObject view = new JSONObject();
        boolean preset = AiProps.isMyOpenAi() == 0;
        view.put("id", config.getId() == null ? "" : String.valueOf(config.getId()));
        view.put("name", connectorName(config));
        view.put("apiUrl", config.getApiUrl());
        view.put("apiKey", "");
        view.put("apiKeyConfigured", Boolean.valueOf(hasText(config.getApiKey())));
        view.put("model", config.getModel());
        view.put("supportedTypes", supportedTypeArray(config.getSupportedTypes()));
        view.put("requestParams", config.getRequestParams());
        view.put("presetMode", Boolean.valueOf(preset));
        view.put("editable", Boolean.valueOf(!preset));
        view.put("configured", Boolean.valueOf(ready(config)));
        view.put("enabled", Boolean.valueOf(enabled(config)));
        view.put("defaultConnector", Boolean.valueOf(isDefault(config)));
        view.put("createTime", config.getCreateTime());
        view.put("updateTime", config.getUpdateTime());
        return view;
    }

    private String connectorName(AiConfig config) {
        if (config == null) {
            return DEFAULT_CONNECTOR_NAME;
        }
        String configured = text(config.getName());
        if (configured.length() > 0) {
            return configured;
        }
        if (isDefault(config)) {
            return DEFAULT_CONNECTOR_NAME;
        }
        if (config.getId() != null) {
            return "AI " + config.getId();
        }
        return DEFAULT_CONNECTOR_NAME;
    }

    private boolean ready(AiConfig config) {
        return config != null && hasText(config.getApiUrl()) && hasText(config.getApiKey())
                && hasText(config.getModel());
    }

    private void requireReady(AiConfig config) {
        if (config == null) {
            throw new RuntimeException("没有可用的 AI 连接器，请先在 AI 设置页启用并配置连接器");
        }
        if (!enabled(config)) {
            throw new RuntimeException("指定的 AI 连接器已禁用，不能调用");
        }
        if (!ready(config)) {
            throw new RuntimeException("AI 连接器「" + connectorName(config)
                    + "」尚未配置 API 地址、Token 或模型");
        }
    }

    private boolean enabled(AiConfig config) {
        return config != null && config.getEnabled() != null && config.getEnabled().intValue() == 1;
    }

    private boolean isDefault(AiConfig config) {
        return config != null && config.getDefaultConnector() != null
                && config.getDefaultConnector().intValue() == 1;
    }

    private String normalizeSupportedTypes(Object raw) {
        JSONArray values = new JSONArray();
        if (raw instanceof JSONArray) {
            values = (JSONArray) raw;
        } else {
            String rawText = text(raw);
            if (rawText.startsWith("[")) {
                try {
                    values = JSONArray.parseArray(rawText);
                } catch (Exception e) {
                    throw new RuntimeException("支持类型格式不正确", e);
                }
            } else if (rawText.length() > 0) {
                for (String item : rawText.split(",")) {
                    values.add(item);
                }
            }
        }

        boolean[] selected = new boolean[SUPPORTED_TYPE_CODES.length];
        for (int i = 0; i < values.size(); i++) {
            String code = text(values.get(i)).toLowerCase(Locale.ROOT);
            if (code.length() == 0) {
                continue;
            }
            int index = supportedTypeIndex(code);
            if (index < 0) {
                throw new RuntimeException("不支持的 AI 能力类型：" + code);
            }
            selected[index] = true;
        }

        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < SUPPORTED_TYPE_CODES.length; i++) {
            if (!selected[i]) {
                continue;
            }
            if (normalized.length() > 0) {
                normalized.append(',');
            }
            normalized.append(SUPPORTED_TYPE_CODES[i]);
        }
        if (normalized.length() == 0) {
            throw new RuntimeException("请至少选择一种 AI 支持类型");
        }
        return normalized.toString();
    }

    private int supportedTypeIndex(String code) {
        for (int i = 0; i < SUPPORTED_TYPE_CODES.length; i++) {
            if (SUPPORTED_TYPE_CODES[i].equals(code)) {
                return i;
            }
        }
        return -1;
    }

    private String safeSupportedTypes(String value) {
        try {
            return normalizeSupportedTypes(value);
        } catch (RuntimeException ignore) {
            return DEFAULT_SUPPORTED_TYPES;
        }
    }

    private JSONArray supportedTypeArray(String value) {
        JSONArray result = new JSONArray();
        String normalized = safeSupportedTypes(value);
        for (String code : normalized.split(",")) {
            if (code.length() > 0) {
                result.add(code);
            }
        }
        return result;
    }

    private JSONObject parseRequestParams(String requestParams) {
        String value = normalizeRequestParams(requestParams);
        JSONObject result = new JSONObject();
        JSONObject parsed = JSONObject.parseObject(value);
        if (parsed != null) {
            for (String key : parsed.keySet()) {
                if (!"model".equals(key) && !"messages".equals(key) && !"stream".equals(key)) {
                    result.put(key, parsed.get(key));
                }
            }
        }
        return result;
    }

    private String normalizeRequestParams(String requestParams) {
        String value = defaultValue(requestParams, "{}");
        JSONObject parsed;
        try {
            parsed = JSONObject.parseObject(value);
        } catch (Exception e) {
            throw new RuntimeException("附加请求参数必须是合法的 JSON 对象", e);
        }
        if (parsed == null) {
            throw new RuntimeException("附加请求参数必须是 JSON 对象");
        }
        if (parsed.containsKey("model") || parsed.containsKey("messages")
                || parsed.containsKey("stream")) {
            throw new RuntimeException("附加请求参数不能覆盖 model、messages 或 stream");
        }
        String normalized = parsed.toJSONString();
        if (normalized.length() > 20000) {
            throw new RuntimeException("附加请求参数不能超过 20000 个字符");
        }
        return normalized;
    }

    private void validateApiUrl(String apiUrl) {
        if (apiUrl.length() > 500) {
            throw new RuntimeException("AI API 地址不能超过 500 个字符");
        }
        HttpUrl parsed = HttpUrl.parse(apiUrl);
        if (parsed == null || (!"http".equals(parsed.scheme()) && !"https".equals(parsed.scheme()))) {
            throw new RuntimeException("AI API 地址必须是合法的 HTTP 或 HTTPS 地址");
        }
    }

    private HttpUrl endpoint(String apiUrl) {
        HttpUrl parsed = HttpUrl.parse(text(apiUrl));
        if (parsed == null) {
            throw new RuntimeException("AI API 地址格式不正确");
        }
        String path = parsed.encodedPath();
        int length = path.length();
        while (length > 1 && path.charAt(length - 1) == '/') {
            length--;
        }
        String normalizedPath = path.substring(0, length);
        if (normalizedPath.endsWith("/chat/completions")) {
            return parsed;
        }
        return parsed.newBuilder()
                .addPathSegment("chat")
                .addPathSegment("completions")
                .build();
    }

    private String messageContent(JSONObject message) {
        if (message == null || message.get("content") == null) {
            return "";
        }
        Object content = message.get("content");
        if (content instanceof String) {
            return text(content);
        }
        if (content instanceof JSONArray) {
            StringBuilder result = new StringBuilder();
            JSONArray parts = (JSONArray) content;
            for (int i = 0; i < parts.size(); i++) {
                Object part = parts.get(i);
                if (part instanceof JSONObject) {
                    String value = ((JSONObject) part).getString("text");
                    if (value != null) {
                        result.append(value);
                    }
                } else if (part != null) {
                    result.append(part);
                }
            }
            return result.toString();
        }
        return text(content);
    }

    private void applyUsage(ChatResult result, JSONObject usage) {
        if (result == null || usage == null) {
            return;
        }
        result.promptTokens = firstLong(usage, "prompt_tokens", "input_tokens");
        result.completionTokens = firstLong(usage, "completion_tokens", "output_tokens");
        result.totalTokens = firstLong(usage, "total_tokens");
        if (result.totalTokens == 0L) {
            result.totalTokens = result.promptTokens + result.completionTokens;
        }
    }

    private long firstLong(JSONObject source, String firstKey, String... otherKeys) {
        long value = source.getLongValue(firstKey);
        if (value != 0L) {
            return value;
        }
        for (String key : otherKeys) {
            value = source.getLongValue(key);
            if (value != 0L) {
                return value;
            }
        }
        return 0L;
    }

    private void recordCall(AiConfig config, String callType, String status,
                            ChatResult result, Throwable error) {
        try {
            AiConfig loggedConfig = config;
            if (loggedConfig == null) {
                try {
                    loggedConfig = effective(resolve(null));
                } catch (RuntimeException ignore) {
                    // 没有可解析的默认连接器时，才保留“未记录 AI 别名”的兜底日志。
                }
            }
            AiCallLog log = new AiCallLog();
            log.setConnectorId(loggedConfig == null ? null : loggedConfig.getId());
            log.setConnectorName(loggedConfig == null
                    ? UNRECORDED_CONNECTOR_NAME : connectorName(loggedConfig));
            log.setModel(loggedConfig == null ? "" : defaultValue(loggedConfig.getModel(), ""));
            log.setCallType(callType);
            log.setStatus(status);
            log.setHttpStatus(Integer.valueOf(result == null ? 0 : result.httpStatus));
            log.setPromptTokens(Long.valueOf(result == null ? 0L : result.promptTokens));
            log.setCompletionTokens(Long.valueOf(result == null ? 0L : result.completionTokens));
            log.setTotalTokens(Long.valueOf(result == null ? 0L : result.totalTokens));
            log.setDurationMs(Long.valueOf(result == null ? 0L : result.durationMs));
            log.setRequestTime(now());
            log.setErrorMessage(error == null ? "" : abbreviate(error.getMessage(), 1000));
            insertCallLogWithGeneratedId(log);
        } catch (Throwable t) {
            AiLog.error("AI 调用日志写入失败 model="
                    + (config == null ? "" : config.getModel()), t);
        }
    }

    private void insertCallLogWithGeneratedId(AiCallLog log) {
        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            log.setId(AiMyBatis.call(AiMapper.class, mapper ->
                    mapper.nextCallLogId(AiProps.callLogTable())));
            try {
                AiMyBatis.run(AiMapper.class, mapper ->
                        mapper.insertCallLog(AiProps.callLogTable(), log));
                return;
            } catch (RuntimeException e) {
                lastError = e;
                if (!isDuplicateKey(e)) {
                    throw e;
                }
            }
        }
        throw lastError == null
                ? new RuntimeException("AI 调用日志 ID 生成失败") : lastError;
    }

    private Long parseId(String value) {
        String id = text(value);
        if (id.length() == 0) {
            return null;
        }
        try {
            long parsed = Long.parseLong(id);
            return parsed > 0L ? Long.valueOf(parsed) : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String firstText(Object first, Object second) {
        String value = text(first);
        return value.length() > 0 ? value : text(second);
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private String defaultValue(String value, String fallback) {
        return hasText(value) ? text(value) : fallback;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String abbreviate(String value, int max) {
        String text = text(value);
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value.longValue();
    }

    private static final class ChatResult {
        private String content = "";
        private int httpStatus;
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
        private long durationMs;
    }
}
