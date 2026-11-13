<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  AI 管理页 - 连接器列表及编辑弹窗。
  由 admin.jsp 通过 jsp:include 引入，不单独作为完整页面访问。
--%>
<%
    if (!"true".equals(request.getAttribute("oaonesAiAdminInclude"))) {
        response.sendRedirect("/oaones/ai/page/admin.jsp?tab=connectors");
        return;
    }
%>
<div class="ai-admin-connectors">
    <div id="aiNotice" class="ai-notice" hidden></div>

    <nav class="sub-tabs ai-connector-tabs" id="aiConnectorTabs" role="tablist"
         aria-label="连接器管理">
        <button type="button" id="aiConnectorTabList" class="active"
                data-connector-tab="list" role="tab" aria-controls="aiConnectorPanelList"
                aria-selected="true">连接器列表</button>
        <button type="button" id="aiConnectorTabAccess"
                data-connector-tab="access" role="tab" aria-controls="aiConnectorPanelAccess"
                aria-selected="false">业务接入</button>
    </nav>

    <section id="aiConnectorPanelList" class="tab-panel active"
             data-connector-panel="list" role="tabpanel" aria-labelledby="aiConnectorTabList">
    <div class="card ai-list-card">
        <div class="ai-card-head">
            <div>
                <h2 class="ai-section-title">连接器列表</h2>
                <p class="ai-section-sub">Token 只保存在服务端；禁用项不会出现在其他页面的 AI 选择器中。</p>
            </div>
            <div class="ai-card-actions">
                <button id="aiRefresh" type="button" class="btn secondary small">刷新</button>
                <button id="aiAdd" type="button" class="btn">新增 AI</button>
            </div>
        </div>

        <div id="aiLoading" class="ai-loading">正在加载 AI 连接器…</div>
        <div id="aiEmpty" class="oa-empty" hidden>
            <strong>还没有 AI 连接器</strong>
            <div>新增一个连接器并配置 API 地址、Token 与模型后即可使用。</div>
        </div>
        <div id="aiTableWrap" class="table-wrap" hidden>
            <table class="grid ai-table" id="aiConnectorTable">
                <thead>
                <tr>
                    <th class="ai-col-id">ID</th>
                    <th class="ai-col-name">AI别名</th>
                    <th class="ai-col-url">API地址</th>
                    <th class="ai-col-model">模型名称</th>
                    <th class="ai-col-types">支持类型</th>
                    <th class="ai-col-actions">操作</th>
                </tr>
                </thead>
                <tbody id="aiTableBody"></tbody>
            </table>
        </div>
        <div class="pager" id="aiConnectorPager"></div>
    </div>
    </section>

    <section id="aiConnectorPanelAccess" class="tab-panel"
             data-connector-panel="access" role="tabpanel" aria-labelledby="aiConnectorTabAccess">
        <section class="card ai-guide-card">
            <h2 class="ai-section-title">业务接入</h2>
            <p class="ai-section-sub">业务可按连接器 ID 指定 AI，也可留空跟随当前默认 AI。</p>
            <div class="ai-picker-demo ai-guide-picker-demo">
                <div class="ai-picker-demo-label">选择器预览</div>
                <div id="aiPickerPreview"></div>
            </div>
            <div class="ai-guide-code-grid">
                <div>
                    <h3>Java 调用</h3>
                    <pre class="ai-code"><code id="aiGuideJavaCode">// 使用默认连接器
AiService ai = AiServiceFactory.create();
String content = ai.chat("系统提示词", "用户内容");</code></pre>
                </div>
                <div>
                    <h3>前端选择器</h3>
                    <pre class="ai-code"><code id="aiGuidePickerCode">var picker = OaonesAiPicker.create({
    el: "#aiPicker",
    apiUrl: "/oaones/ai/page/api.jsp",
    onChange: function (item) {
        // item.id 为空表示跟随默认 AI
        console.log(item.id || "");
    }
});</code></pre>
                </div>
            </div>
            <p class="ai-guide-note">选择器只展示已启用且配置完整的连接器；业务接口仍应按连接器 ID 做最终校验。</p>
        </section>
    </section>
</div>

<div class="modal-mask" id="aiConnectorMask"></div>
<div class="modal modal-wide" id="aiConnectorModal" data-modal-mask="aiConnectorMask" aria-hidden="true">
    <form id="aiConnectorForm">
        <div class="modal-head">
            <span id="aiModalTitle">新增 AI 连接器</span>
            <div class="modal-head-actions">
                <button type="button" class="modal-x" data-modal-close="aiConnectorModal" aria-label="关闭">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M6 6l12 12M18 6L6 18"></path>
                    </svg>
                </button>
            </div>
        </div>
        <div class="modal-body">
            <input id="aiConnectorId" type="hidden">
            <div class="form-group">
                <div class="form-group-title">基本信息</div>
                <div class="form-row">
                    <label for="aiConnectorName">AI 别名<span class="req-mark">*</span></label>
                    <div class="field">
                        <input id="aiConnectorName" name="name" type="text" maxlength="100" autocomplete="off"
                               placeholder="例如：翻译专用、研发测试 AI" required>
                        <p class="field-note">别名用于设置页和 AI 选择器展示，请保持简短且易识别。</p>
                    </div>
                </div>
                <div class="form-row">
                    <label>使用状态</label>
                    <div class="field ai-check-line">
                        <label class="ai-check"><input id="aiConnectorEnabled" type="checkbox" checked><span>启用此 AI</span></label>
                        <label class="ai-check"><input id="aiConnectorDefault" type="checkbox"><span>设为默认 AI</span></label>
                        <p class="field-note">禁用的 AI 不会出现在选择器中，也不能被业务接口调用。</p>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <div class="form-group-title">模型能力</div>
                <div class="form-row">
                    <label>支持类型<span class="req-mark">*</span></label>
                    <div class="field ai-check-line ai-type-options">
                        <label class="ai-check"><input name="supportedTypes" type="checkbox" value="text" checked><span>文字</span></label>
                        <label class="ai-check"><input name="supportedTypes" type="checkbox" value="image"><span>图片</span></label>
                        <label class="ai-check"><input name="supportedTypes" type="checkbox" value="video"><span>视频</span></label>
                        <label class="ai-check"><input name="supportedTypes" type="checkbox" value="audio"><span>音频</span></label>
                        <p class="field-note">可多选，用于标记该模型具备的主要输入或生成能力；
                            文档翻译会在扫描件场景使用勾选“图片”的模型执行 OCR。</p>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <div class="form-group-title">服务连接</div>
                <div class="form-row">
                    <label for="aiApiUrl">API 地址<span class="req-mark">*</span></label>
                    <div class="field">
                        <input id="aiApiUrl" name="apiUrl" type="url" autocomplete="off"
                               placeholder="https://api.openai.com/v1/chat/completions">
                        <p class="field-note">支持完整 `/chat/completions` 地址或 `/v1` 等 base URL。</p>
                    </div>
                </div>
                <div class="form-row">
                    <label for="aiApiKey">API Token<span class="req-mark">*</span></label>
                    <div class="field">
                        <input id="aiApiKey" name="apiKey" type="password" autocomplete="new-password" data-lpignore="true">
                        <p id="aiKeyTip" class="field-note">Bearer 认证 Token，只保存在服务端。</p>
                        <label class="ai-check ai-clear-key" id="aiClearKeyWrap" hidden>
                            <input id="aiClearKey" type="checkbox"><span>清除已保存的 Token</span>
                        </label>
                    </div>
                </div>
                <div class="form-row">
                    <label for="aiModel">模型名<span class="req-mark">*</span></label>
                    <div class="field">
                        <input id="aiModel" name="model" type="text" autocomplete="off"
                               placeholder="例如 gpt-4.1-mini、deepseek-chat">
                    </div>
                </div>
                <div class="form-row">
                    <label for="aiRequestParams">附加参数</label>
                    <div class="field">
                        <textarea id="aiRequestParams" name="requestParams" rows="5" spellcheck="false"
                                  placeholder='{"temperature":0,"max_tokens":4096}'></textarea>
                        <p class="field-note">JSON 对象，不能覆盖 `model`、`messages`、`stream`。</p>
                    </div>
                </div>
                <div id="aiPresetTip" class="ai-preset-tip" hidden>当前为系统预设模式，API 地址、模型和附加参数由配置文件锁定。</div>
            </div>
            <div id="aiTestResult" class="ai-test-result" role="status" aria-live="polite" hidden></div>
        </div>
        <div class="modal-foot">
            <button type="button" class="btn secondary" data-modal-close="aiConnectorModal">取消</button>
            <button id="aiConnectorTest" type="button" class="btn secondary">测试连接</button>
            <button id="aiConnectorSave" type="submit" class="btn">保存 AI 连接器</button>
        </div>
    </form>
</div>
