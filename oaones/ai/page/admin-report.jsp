<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  AI 管理页 - 报表统计。
  由 admin.jsp 或 usage.jsp 通过 jsp:include 引入，不单独作为完整页面访问。
--%>
<%
    if (!"true".equals(request.getAttribute("oaonesAiAdminInclude"))) {
        response.sendRedirect("/oaones/ai/page/admin.jsp?tab=report");
        return;
    }
%>
<div class="ai-admin-report">
    <div class="ai-report-head">
        <div>
            <h2 class="ai-usage-title">调用概览</h2>
            <p class="ai-usage-sub">按调用发生时的 AI 别名汇总；默认 AI 后续切换不会改写历史调用。</p>
        </div>
        <button id="aiUsageRefresh" type="button" class="btn">刷新数据</button>
    </div>

    <section class="oa-kpi-wrap ai-usage-kpis" aria-label="AI 调用概览">
        <div class="oa-kpi-card">
            <div class="oa-kpi-title">累计调用</div>
            <div class="ai-usage-kpi-value"><b id="aiUsageCalls">-</b><span>次</span></div>
        </div>
        <div class="oa-kpi-card">
            <div class="oa-kpi-title">累计 Token</div>
            <div class="ai-usage-kpi-value"><b id="aiUsageTokens">-</b><span>输入 + 输出</span></div>
        </div>
        <div class="oa-kpi-card">
            <div class="oa-kpi-title">成功率</div>
            <div class="ai-usage-kpi-value"><b id="aiUsageSuccessRate">-</b><span id="aiUsageSuccessDetail">成功 / 失败</span></div>
        </div>
        <div class="oa-kpi-card">
            <div class="oa-kpi-title">已使用 AI 别名</div>
            <div class="ai-usage-kpi-value"><b id="aiUsageAliases">-</b><span>个</span></div>
        </div>
    </section>

    <div id="aiUsageNotice" class="ai-usage-notice" hidden></div>

    <section class="card ai-usage-card">
        <div class="ai-usage-card-head">
            <div>
                <h2 class="ai-usage-title">AI 使用明细</h2>
                <p class="ai-usage-sub">报告按调用时记录的 AI 别名汇总；旧日志只写了“默认 AI”时，会按连接器补全实际别名，同一别名跨连接器的成功与失败调用会合并统计。</p>
            </div>
            <span class="oa-tag oa-tag-primary">按 AI 汇总</span>
        </div>

        <div class="filter-bar ai-usage-filter">
            <span class="filter-label">AI 别名</span>
            <input id="aiUsageKeyword" type="search" autocomplete="off" data-lpignore="true"
                   placeholder="输入 AI 别名筛选">
            <button id="aiUsageSearch" type="button" class="btn">查询</button>
            <button id="aiUsageReset" type="button" class="btn secondary">重置</button>
        </div>

        <div id="aiUsageLoading" class="ai-usage-loading">正在加载调用数据…</div>
        <div id="aiUsageEmpty" class="oa-empty" hidden>
            <strong>暂无调用记录</strong>
            <div>完成一次 AI 对话或连接测试后，这里会显示模型调用量和 token 用量。</div>
        </div>
        <div id="aiUsageTableWrap" class="table-wrap" hidden>
            <table class="grid ai-usage-table" id="aiUsageTable">
                <thead>
                <tr>
                    <th>AI 别名</th>
                    <th>调用次数</th>
                    <th>成功</th>
                    <th>失败</th>
                    <th>输入 Token</th>
                    <th>输出 Token</th>
                    <th>总 Token</th>
                    <th>平均耗时</th>
                    <th>最近调用</th>
                </tr>
                </thead>
                <tbody id="aiUsageBody"></tbody>
            </table>
        </div>
        <div class="pager" id="aiUsagePager"></div>
    </section>
</div>
