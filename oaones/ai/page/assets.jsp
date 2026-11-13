<%--
  AI 项目静态资源版本片段。

  本文件只依赖 AI 项目自身资源，以及 /oaones/scripts 下的公共组件；
  不依赖 appsyn 项目，可随 AI 功能单独部署。

  用法：在 AI 页面中静态引入本文件，并使用 aiAssetV：
      <%@ include file="/oaones/ai/page/assets.jsp" %>
--%>
<%
    long aiAssetV = 0L;
    String[] aiAssetPaths = {
        // 公共组件与公共样式
        "/oaones/scripts/css/style.css",
        "/oaones/scripts/js/toast.js",
        "/oaones/scripts/js/modal.js",
        "/oaones/scripts/js/secret.js",
        "/oaones/scripts/js/table.js",
        "/oaones/scripts/js/tabs.js",
        "/oaones/scripts/js/aiPicker.js",
        // AI 项目本地资源
        "/oaones/ai/script/css/admin.css",
        "/oaones/ai/script/css/usage.css",
        "/oaones/ai/script/js/admin.js",
        "/oaones/ai/script/js/usage.js"
    };
    try {
        for (String aiAssetPath : aiAssetPaths) {
            java.net.URL aiAssetUrl = application.getResource(aiAssetPath);
            if (aiAssetUrl != null) {
                aiAssetV = Math.max(aiAssetV, aiAssetUrl.openConnection().getLastModified());
            }
        }
    } catch (Exception aiAssetIgnore) {
        aiAssetV = 0L;
    }
    if (aiAssetV <= 0L) {
        aiAssetV = System.currentTimeMillis();
    }
%>
