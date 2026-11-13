package com.customization.oaones.generic.ai;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * 公共 AI 配置与选择器接口。
 */
public class AiController {

    public JSONObject handle(String action, HttpServletRequest request, String operatorId) {
        String name = action == null ? "" : action.trim();
        AiService service = AiServiceFactory.create();

        if ("admin.connector.list".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.listConnectors());
        }
        if ("admin.connector.save".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.saveConnector(payload(request)));
        }
        if ("admin.connector.setDefault".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.setDefaultConnector(payload(request)));
        }
        if ("admin.connector.setEnabled".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.setConnectorEnabled(payload(request)));
        }
        if ("admin.connector.test".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.testConnector(payload(request)));
        }
        if ("admin.ai.usage".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            return ok(service.usageSummary());
        }

        // 兼容旧页面和旧集成；未指定连接器时均操作默认连接器。
        if ("admin.config.get".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            JSONObject data = new JSONObject();
            data.put("config", service.getConfig());
            return ok(data);
        }
        if ("admin.config.save".equals(name)) {
            AiAdmin.requireAdmin(operatorId);
            JSONObject data = new JSONObject();
            data.put("config", service.saveConfig(payload(request)));
            data.put("message", "AI 服务配置已保存");
            return ok(data);
        }

        // 其他页面读取可选连接器；只返回已启用项，不返回 Token 或请求地址。
        if ("ai.connector.options".equals(name)) {
            return ok(service.connectorOptions());
        }
        return fail("未知 action：" + name);
    }

    private JSONObject payload(HttpServletRequest request) {
        String payload = request.getParameter("payload");
        if (payload == null || payload.trim().length() == 0) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(payload);
        } catch (Exception e) {
            throw new RuntimeException("AI 配置格式不正确", e);
        }
    }

    private JSONObject ok(JSONObject data) {
        JSONObject result = new JSONObject();
        result.put("code", Integer.valueOf(0));
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }

    private JSONObject fail(String message) {
        JSONObject result = new JSONObject();
        result.put("code", Integer.valueOf(-1));
        result.put("msg", message);
        return result;
    }
}
