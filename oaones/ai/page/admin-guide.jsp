<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  AI 管理页 - 使用介绍。
  由 admin.jsp 通过 jsp:include 引入，不单独作为完整页面访问。
--%>
<%
    if (!"true".equals(request.getAttribute("oaonesAiAdminInclude"))) {
        response.sendRedirect("/oaones/ai/page/admin.jsp?tab=guide");
        return;
    }
%>
<div class="ai-admin-guide">
    <section class="card ai-guide-card">
        <h2 class="ai-section-title">运行方式</h2>
        <p class="ai-section-sub">AI 服务按“连接器”统一管理，业务只提交提示词和可选的连接器 ID。</p>
        <div class="ai-guide-grid">
            <div class="ai-guide-item">
                <span class="ai-guide-index">01</span>
                <strong>默认连接器</strong>
                <p>业务未指定连接器时，服务端自动选择当前默认 AI。默认项被禁用后会切换到其他已启用项。</p>
            </div>
            <div class="ai-guide-item">
                <span class="ai-guide-index">02</span>
                <strong>指定连接器</strong>
                <p>业务可以传入连接器 ID，使用特定模型或服务地址；不存在、已禁用的 ID 会被服务端拒绝。</p>
            </div>
            <div class="ai-guide-item">
                <span class="ai-guide-index">03</span>
                <strong>能力标记</strong>
                <p>文字、图片、视频、音频用于标记模型能力。扫描件翻译等场景可按“图片”能力选择模型。</p>
            </div>
        </div>
    </section>

    <section class="card ai-guide-card">
        <h2 class="ai-section-title">新增与维护连接器</h2>
        <ol class="ai-guide-steps">
            <li>进入“连接器列表”，点击“新增 AI”。</li>
            <li>填写易识别的别名、OpenAI Chat Completions 兼容 API 地址和模型名。</li>
            <li>填写 Bearer Token，选择支持类型，并按需设置默认项、附加 JSON 参数。</li>
            <li>先使用“测试连接”验证地址、Token 和模型可用，再保存配置。</li>
            <li>列表中的“启用/禁用”“设为默认”“编辑”“连接测试”用于后续维护。</li>
        </ol>
        <div class="ai-guide-note">
            Token 只保存在服务端，管理接口不会返回明文。编辑时留空表示不修改，勾选“清除已保存的 Token”才会删除。
        </div>
    </section>

</div>
