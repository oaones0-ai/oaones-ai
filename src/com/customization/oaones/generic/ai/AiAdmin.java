package com.customization.oaones.generic.ai;

import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import weaver.conn.RecordSet;

/**
 * 公共 AI 配置管理权限。
 */
public final class AiAdmin {
    private AiAdmin() {
    }

    public static boolean isAdmin(String operatorId) {
        int userId = toInt(operatorId);
        if (userId <= 0) {
            return false;
        }
        if (userId == 1 || inConfiguredIds(userId)) {
            return true;
        }
        return inRole(userId, AiProps.adminRoleId());
    }

    public static void requireAdmin(String operatorId) {
        if (!isAdmin(operatorId)) {
            throw new RuntimeException("只有 AI 服务管理员可以执行此操作");
        }
    }

    private static boolean inConfiguredIds(int userId) {
        String ids = AiProps.adminIds();
        if (ids.length() == 0) {
            return false;
        }
        for (String part : ids.split(",")) {
            if (toInt(part) == userId) {
                return true;
            }
        }
        return false;
    }

    private static boolean inRole(int userId, int roleId) {
        if (userId <= 0 || roleId <= 0) {
            return false;
        }
        try {
            HrmCommonService service = new HrmCommonServiceImpl();
            String condition = service.getHrmRoleMembers_queryConditionSql(userId, "");
            if (condition == null || condition.trim().length() == 0) {
                return false;
            }
            RecordSet rs = new RecordSet();
            rs.executeQuery("select count(1) from HrmRoleMembers where roleid = "
                    + roleId + " and " + condition);
            return rs.next() && rs.getInt(1) > 0;
        } catch (Throwable t) {
            AiLog.error("AI 管理员角色判定失败 userId=" + userId + ", roleId=" + roleId, t);
            return false;
        }
    }

    private static int toInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignore) {
            return 0;
        }
    }
}
