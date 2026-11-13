<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.customization.oaones.generic.ai.AiAdmin" %>
<%@ page import="weaver.hrm.HrmUserVarify" %>
<%@ page import="weaver.hrm.User" %>
<%@ include file="/oaones/ai/page/assets.jsp" %>
<%
    request.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    User user = HrmUserVarify.getUser(request, response);
    if (user == null) {
        response.sendRedirect("/wui/index.html");
        return;
    }
    boolean canAdmin = AiAdmin.isAdmin(String.valueOf(user.getUID()));

    String aiAdminTab = request.getParameter("tab");
    aiAdminTab = aiAdminTab == null ? "" : aiAdminTab.trim().toLowerCase();
    if (!"report".equals(aiAdminTab)
            && !"connectors".equals(aiAdminTab)
            && !"guide".equals(aiAdminTab)) {
        aiAdminTab = "guide";
    }

    request.setAttribute("oaonesAiAdminInclude", "true");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>AI 服务管理</title>
    <link rel="stylesheet" href="/oaones/scripts/css/style.css?v=<%=aiAssetV%>">
    <link rel="stylesheet" href="/oaones/ai/script/css/admin.css?v=<%=aiAssetV%>">
    <link rel="stylesheet" href="/oaones/ai/script/css/usage.css?v=<%=aiAssetV%>">
</head>
<body>
<main class="page ai-admin">
    <header class="page-head ai-page-head">
        <div class="page-head-main">
            <span class="ai-eyebrow">OAONES · AI CONNECTORS</span>
            <h1 class="page-title">AI 服务管理</h1>
            <p class="page-sub">统一查阅业务接入方式、维护多个 OpenAI Chat Completions 兼容连接器，并查看调用统计。</p>
        </div>
        <% if (canAdmin) { %>
        <div class="page-actions">
            <span class="oa-tag oa-tag-primary">多连接器</span>
        </div>
        <% } %>
    </header>

    <% if (!canAdmin) { %>
    <section class="card">
        <div class="ai-denied">当前账号没有公共 AI 配置权限。</div>
    </section>
    <% } else { %>
    <nav class="sub-tabs ai-admin-tabs" id="aiAdminTabs" role="tablist" aria-label="AI 服务管理">
        <button type="button" id="aiAdminTabGuide"
                class="<%= "guide".equals(aiAdminTab) ? "active" : "" %>"
                data-tab="guide" role="tab" aria-controls="aiAdminPanelGuide"
                aria-selected="<%= "guide".equals(aiAdminTab) %>">使用介绍</button>
        <button type="button" id="aiAdminTabConnectors"
                class="<%= "connectors".equals(aiAdminTab) ? "active" : "" %>"
                data-tab="connectors" role="tab" aria-controls="aiAdminPanelConnectors"
                aria-selected="<%= "connectors".equals(aiAdminTab) %>">连接器列表</button>
        <button type="button" id="aiAdminTabReport"
                class="<%= "report".equals(aiAdminTab) ? "active" : "" %>"
                data-tab="report" role="tab" aria-controls="aiAdminPanelReport"
                aria-selected="<%= "report".equals(aiAdminTab) %>">统计报表</button>
    </nav>

    <div id="aiAdminPanelGuide"
         class="tab-panel<%= "guide".equals(aiAdminTab) ? " active" : "" %>"
         data-tab-panel="guide" role="tabpanel" aria-labelledby="aiAdminTabGuide">
        <jsp:include page="/oaones/ai/page/admin-guide.jsp"/>
    </div>

    <div id="aiAdminPanelConnectors"
         class="tab-panel<%= "connectors".equals(aiAdminTab) ? " active" : "" %>"
         data-tab-panel="connectors" role="tabpanel" aria-labelledby="aiAdminTabConnectors">
        <jsp:include page="/oaones/ai/page/admin-connectors.jsp"/>
    </div>

    <div id="aiAdminPanelReport"
         class="tab-panel<%= "report".equals(aiAdminTab) ? " active" : "" %>"
         data-tab-panel="report" role="tabpanel" aria-labelledby="aiAdminTabReport">
        <jsp:include page="/oaones/ai/page/admin-report.jsp"/>
    </div>
    <% } %>
</main>

<% if (canAdmin) { %>
<script>
    window.OAONES_AI_ADMIN = {
        canAdmin: true,
        apiUrl: "/oaones/ai/page/api.jsp",
        activeTab: "<%=aiAdminTab%>"
    };
    window.OAONES_AI_USAGE = {
        apiUrl: "/oaones/ai/page/api.jsp"
    };
</script>
<script src="/oaones/scripts/js/toast.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/modal.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/secret.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/table.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/tabs.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/aiPicker.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/ai/script/js/usage.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/ai/script/js/admin.js?v=<%=aiAssetV%>"></script>
<% } %>
</body>
</html>
