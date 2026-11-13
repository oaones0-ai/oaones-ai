<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.alibaba.fastjson.JSONObject" %>
<%@ page import="com.customization.oaones.generic.ai.AiController" %>
<%@ page import="com.customization.oaones.generic.ai.AiLog" %>
<%@ page import="weaver.hrm.HrmUserVarify" %>
<%@ page import="weaver.hrm.User" %>
<%
    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("X-Content-Type-Options", "nosniff");

    User user = HrmUserVarify.getUser(request, response);
    if (user == null) {
        out.print("{\"code\":401,\"msg\":\"未登录或会话已失效\"}");
        return;
    }
    String action = request.getParameter("action");
    String normalizedAction = action == null ? "" : action.trim();
    JSONObject result;
    try {
        boolean mutation = normalizedAction.endsWith(".save")
                || normalizedAction.endsWith(".setDefault")
                || normalizedAction.endsWith(".setEnabled")
                || normalizedAction.endsWith(".test");
        if (mutation
                && !"XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            response.setStatus(403);
            out.print("{\"code\":403,\"msg\":\"非法请求\"}");
            return;
        }
        result = new AiController().handle(
                normalizedAction, request, String.valueOf(user.getUID()));
    } catch (Throwable t) {
        AiLog.error("公共 AI 接口处理失败 action=" + normalizedAction, t);
        String message = t.getMessage() == null ? "AI 配置请求处理失败" : t.getMessage().trim();
        result = new JSONObject();
        result.put("code", Integer.valueOf(-1));
        result.put("msg", message.length() == 0 ? "AI 配置请求处理失败" : message);
    }
    out.print(result == null ? "{\"code\":-1,\"msg\":\"无返回\"}" : result.toJSONString());
%>
