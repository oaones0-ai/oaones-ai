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
    request.setAttribute("oaonesAiAdminInclude", "true");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>AI 调用统计</title>
    <link rel="stylesheet" href="/oaones/scripts/css/style.css?v=<%=aiAssetV%>">
    <link rel="stylesheet" href="/oaones/ai/script/css/usage.css?v=<%=aiAssetV%>">
</head>
<body>
<main class="page ai-usage">
    <header class="page-head ai-usage-head">
        <div class="page-head-main">
            <span class="ai-usage-eyebrow">OAONES · AI USAGE</span>
            <h1 class="page-title">AI 调用统计</h1>
            <p class="page-sub">按调用发生时记录的 AI 别名汇总实际调用次数、成功失败情况与 token 用量；旧日志缺少别名时按连接器补全。默认 AI 切换不会改变历史归属，连接测试也会计入调用记录。</p>
        </div>
        <div class="page-actions">
            <a class="btn secondary" href="/oaones/ai/page/admin.jsp?tab=connectors">连接器列表</a>
        </div>
    </header>

    <% if (!canAdmin) { %>
    <section class="card">
        <div class="oa-empty">当前账号没有 AI 调用统计查看权限。</div>
    </section>
    <% } else { %>
    <jsp:include page="/oaones/ai/page/admin-report.jsp"/>
    <% } %>
</main>

<% if (canAdmin) { %>
<script>
    window.OAONES_AI_USAGE = {
        apiUrl: "/oaones/ai/page/api.jsp"
    };
</script>
<script src="/oaones/scripts/js/toast.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/scripts/js/table.js?v=<%=aiAssetV%>"></script>
<script src="/oaones/ai/script/js/usage.js?v=<%=aiAssetV%>"></script>
<% } %>
</body>
</html>
