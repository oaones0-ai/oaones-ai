package com.customization.oaones.generic.ai;

import weaver.general.BaseBean;

/**
 * 公共 AI 模块日志。
 */
public final class AiLog {
    private AiLog() {
    }

    public static void error(String message, Throwable throwable) {
        try {
            String suffix = throwable == null ? "" : " | " + throwable.getClass().getName()
                    + ": " + (throwable.getMessage() == null ? "" : throwable.getMessage());
            new BaseBean().writeLog("[oaones.generic.ai][ERROR] " + message + suffix);
        } catch (Throwable ignore) {
            // 日志组件不可用时不覆盖业务异常。
        }
    }
}
