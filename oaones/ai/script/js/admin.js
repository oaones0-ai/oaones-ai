(function () {
    'use strict';

    var CFG = window.OAONES_AI_ADMIN || {};
    var TYPE_LABELS = {
        text: '文字',
        image: '图片',
        video: '视频',
        audio: '音频'
    };
    var state = {
        connectors: [],
        defaultId: '',
        total: 0,
        enabledCount: 0,
        configuredCount: 0,
        presetMode: false
    };
    var editingId = '';
    var picker = null;
    var connectorPager = null;

    function byId(id) {
        return document.getElementById(id);
    }

    function esc(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function syncTabQuery(tab) {
        if (!window.history || !window.history.replaceState || !window.URL) {
            return;
        }
        try {
            var url = new window.URL(window.location.href);
            url.searchParams.set('tab', tab);
            window.history.replaceState(null, '', url.toString());
        } catch (ignore) {
            // URL 更新失败不影响页签切换。
        }
    }

    function activateTab(tab) {
        window.setTimeout(function () {
            if (!window.OaonesTable) {
                return;
            }
            if (tab === 'connectors' && byId('aiConnectorTable')) {
                OaonesTable.resizable(byId('aiConnectorTable'), { key: 'oaones.ai.connectors.table' });
            } else if (tab === 'report' && byId('aiUsageTable')) {
                OaonesTable.resizable(byId('aiUsageTable'), { key: 'oaones.ai.usage.table' });
            }
        }, 0);
    }

    function bindAdminTabs() {
        if (!window.OaonesTabs || !byId('aiAdminTabs')) {
            return;
        }
        var initial = CFG.activeTab || 'guide';
        OaonesTabs.bind({
            container: '#aiAdminTabs',
            buttons: '#aiAdminTabs [data-tab]',
            panels: '[data-tab-panel]',
            initial: initial,
            onChange: function (tab) {
                syncTabQuery(tab);
                activateTab(tab);
            }
        });
        activateTab(initial);
    }

    function bindConnectorTabs() {
        if (!window.OaonesTabs || !byId('aiConnectorTabs')) {
            return;
        }
        OaonesTabs.bind({
            container: '#aiConnectorTabs',
            buttons: '#aiConnectorTabs [data-connector-tab]',
            panels: '#aiConnectorPanelList, #aiConnectorPanelAccess',
            buttonAttribute: 'data-connector-tab',
            panelAttribute: 'data-connector-panel',
            initial: 'list'
        });
    }

    function request(action, data) {
        var payload = { action: action };
        Object.keys(data || {}).forEach(function (key) {
            payload[key] = data[key];
        });
        var body = Object.keys(payload).map(function (key) {
            return encodeURIComponent(key) + '=' + encodeURIComponent(payload[key] == null
                ? '' : String(payload[key]));
        }).join('&');
        return fetch(CFG.apiUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body
        }).then(function (response) {
            return response.json().catch(function () {
                throw new Error('服务返回了无法识别的数据（HTTP ' + response.status + '）');
            }).then(function (result) {
                if (!response.ok || !result || result.code !== 0) {
                    throw new Error((result && result.msg) || ('HTTP ' + response.status));
                }
                return result.data || {};
            });
        });
    }

    function toast(message, mode) {
        if (window.OaonesToast) {
            if (mode === 2) {
                OaonesToast.error(message);
            } else if (mode === 1) {
                OaonesToast.warn(message);
            } else {
                OaonesToast.info(message);
            }
            return;
        }
        if (window.console && console.log) {
            console.log(message);
        }
    }

    function javaString(value) {
        return '"' + String(value == null ? '' : value)
            .replace(/\\/g, '\\\\')
            .replace(/"/g, '\\"') + '"';
    }

    function updateAccessExamples(item) {
        var javaCode = byId('aiGuideJavaCode');
        var pickerCode = byId('aiGuidePickerCode');
        if (!javaCode && !pickerCode) {
            return;
        }

        var id = item && item.id != null ? String(item.id) : '';
        var name = item && item.name ? String(item.name) : '跟随默认';
        var javaText = id
            ? '// 使用「' + name + '」连接器\n'
                + 'AiService ai = AiServiceFactory.create(' + javaString(id) + ');\n'
                + 'String content = ai.chat("系统提示词", "用户内容");'
            : '// 使用默认连接器\n'
                + 'AiService ai = AiServiceFactory.create();\n'
                + 'String content = ai.chat("系统提示词", "用户内容");';
        var pickerText = 'var picker = OaonesAiPicker.create({\n'
            + '    el: "#aiPicker",\n'
            + '    apiUrl: "/oaones/ai/page/api.jsp",\n'
            + (id ? '    value: ' + JSON.stringify(id) + ',\n' : '')
            + '    onChange: function (item) {\n'
            + '        // item.id 为空表示跟随默认 AI\n'
            + '        console.log(item.id || "");\n'
            + '    }\n'
            + '});';

        if (javaCode) { javaCode.textContent = javaText; }
        if (pickerCode) { pickerCode.textContent = pickerText; }
    }

    function normalize(data) {
        data = data || {};
        state.connectors = Array.isArray(data.connectors) ? data.connectors : [];
        state.defaultId = String(data.defaultId || '');
        state.total = parseInt(data.total, 10) || state.connectors.length;
        state.enabledCount = parseInt(data.enabledCount, 10) || 0;
        state.configuredCount = parseInt(data.configuredCount, 10) || 0;
        state.presetMode = !!data.presetMode;
    }

    function rowById(id) {
        for (var i = 0; i < state.connectors.length; i++) {
            if (String(state.connectors[i].id) === String(id)) {
                return state.connectors[i];
            }
        }
        return null;
    }

    function defaultRow() {
        if (state.defaultId) {
            return rowById(state.defaultId);
        }
        for (var i = 0; i < state.connectors.length; i++) {
            if (state.connectors[i].defaultConnector && state.connectors[i].enabled) {
                return state.connectors[i];
            }
        }
        return null;
    }

    function normalizedTypes(value) {
        var source = value;
        if (typeof source === 'string') {
            source = source.replace(/^\s+|\s+$/g, '');
            if (source.charAt(0) === '[') {
                try {
                    source = JSON.parse(source);
                } catch (ignore) {
                    source = source.split(',');
                }
            } else {
                source = source ? source.split(',') : [];
            }
        }
        if (!Array.isArray(source)) {
            source = [];
        }
        var seen = {};
        var result = [];
        source.forEach(function (item) {
            var code = String(item == null ? '' : item).replace(/^\s+|\s+$/g, '').toLowerCase();
            if (TYPE_LABELS[code] && !seen[code]) {
                seen[code] = true;
                result.push(code);
            }
        });
        return result.length ? result : ['text'];
    }

    function typesHtml(row) {
        return '<div class="ai-type-tags">' + normalizedTypes(row.supportedTypes).map(function (code) {
            return '<span class="oa-tag ai-type-tag ai-type-' + esc(code) + '">'
                + esc(TYPE_LABELS[code]) + '</span>';
        }).join('') + '</div>';
    }

    function rowHtml(row) {
        var id = esc(row.id);
        var isDefault = String(row.id) === String(state.defaultId) || !!row.defaultConnector;
        var actionHtml = '';
        actionHtml += row.enabled
            ? '<button type="button" class="link danger" data-ai-action="disable" data-id="' + id + '">禁用</button>'
            : '<button type="button" class="link" data-ai-action="enable" data-id="' + id + '">启用</button>';
        if (row.enabled && !isDefault) {
            actionHtml += '<button type="button" class="link" data-ai-action="default" data-id="' + id + '">设为默认</button>';
        }
        actionHtml += '<button type="button" class="link" data-ai-action="test" data-id="' + id + '">连接测试</button>';

        return '<tr data-row-id="' + id + '" class="' + (row.enabled ? '' : 'ai-row-disabled') + '">'
            + '<td><span class="ai-id">' + id + '</span></td>'
            + '<td><div class="ai-name-line"><button type="button" class="ai-name-link" data-ai-action="edit" data-id="' + id + '" title="编辑连接器">' + esc(row.name) + '</button>'
            + (isDefault ? '<span class="oa-tag oa-tag-primary">默认</span>' : '')
            + (row.apiKeyConfigured ? '' : '<span class="oa-tag oa-tag-warning">未配置 Token</span>')
            + '</div></td>'
            + '<td><span class="ai-url" title="' + esc(row.apiUrl || '') + '">' + esc(row.apiUrl || '-') + '</span></td>'
            + '<td><span class="ai-model">' + esc(row.model || '-') + '</span></td>'
            + '<td>' + typesHtml(row) + '</td>'
            + '<td><div class="ai-row-actions">' + actionHtml + '</div></td>'
            + '</tr>';
    }

    function renderNotice() {
        var notice = byId('aiNotice');
        if (!notice) {
            return;
        }
        var text = '';
        if (state.total === 0) {
            text = '尚未添加 AI 连接器。';
        } else if (state.enabledCount === 0) {
            text = '当前没有已启用的 AI 连接器，业务侧暂时无法调用 AI。';
        } else if (state.configuredCount === 0) {
            text = '已启用的 AI 连接器还没有完整的地址、Token 和模型配置。';
        }
        notice.textContent = text;
        notice.className = 'ai-notice' + (text ? ' warning' : '');
        notice.hidden = !text;
    }

    function render() {
        var total = byId('aiKpiTotal');
        var enabled = byId('aiKpiEnabled');
        var configured = byId('aiKpiConfigured');
        var defaultText = byId('aiKpiDefault');
        var body = byId('aiTableBody');
        var table = byId('aiConnectorTable');
        var tableWrap = byId('aiTableWrap');
        var pagerEl = byId('aiConnectorPager');
        var empty = byId('aiEmpty');
        var loading = byId('aiLoading');
        var selectedDefault = defaultRow();
        var pageRows = state.connectors;

        if (window.OaonesTable && pagerEl && !connectorPager) {
            connectorPager = OaonesTable.pager({
                key: 'oaones.ai.connectors',
                el: pagerEl,
                onChange: render
            });
        }
        if (connectorPager) {
            pageRows = connectorPager.apply(state.connectors);
        }

        if (total) { total.textContent = String(state.total); }
        if (enabled) { enabled.textContent = String(state.enabledCount); }
        if (configured) { configured.textContent = String(state.configuredCount); }
        if (defaultText) {
            defaultText.innerHTML = selectedDefault
                ? '<b>' + esc(selectedDefault.name) + '</b><span> 未指定 AI 时调用</span>'
                : '<span class="ai-muted">尚未设置默认 AI</span>';
        }
        if (body) {
            body.innerHTML = pageRows.map(rowHtml).join('');
        }
        if (loading) { loading.hidden = true; }
        if (tableWrap) { tableWrap.hidden = state.connectors.length === 0; }
        if (empty) { empty.hidden = state.connectors.length !== 0; }
        if (connectorPager) {
            if (state.connectors.length) {
                connectorPager.show();
                connectorPager.render();
            } else {
                connectorPager.hide();
            }
        }
        if (window.OaonesTable && table && state.connectors.length) {
            OaonesTable.resizable(table, { key: 'oaones.ai.connectors.table' });
        }
        renderNotice();
        refreshPicker();
    }

    function setBusy(busy) {
        var refresh = byId('aiRefresh');
        var add = byId('aiAdd');
        if (refresh) { refresh.disabled = !!busy; refresh.textContent = busy ? '刷新中…' : '刷新'; }
        if (add) { add.disabled = !!busy; }
    }

    function load(showLoading) {
        var loading = byId('aiLoading');
        if (showLoading && loading) {
            loading.hidden = false;
            loading.textContent = '正在加载 AI 连接器…';
        }
        setBusy(true);
        request('admin.connector.list', {}).then(function (data) {
            normalize(data);
            render();
        }).catch(function (error) {
            if (loading) {
                loading.hidden = false;
                loading.textContent = error.message || 'AI 连接器加载失败';
            }
            toast(error.message || 'AI 连接器加载失败', 2);
        }).then(function () {
            setBusy(false);
        });
    }

    function refreshPicker() {
        if (!window.OaonesAiPicker || !byId('aiPickerPreview')) {
            return;
        }
        if (!picker) {
            picker = OaonesAiPicker.create({
                el: byId('aiPickerPreview'),
                apiUrl: CFG.apiUrl,
                placeholder: '跟随默认 AI',
                onChange: updateAccessExamples,
                onLoad: function (connectors, instance) {
                    updateAccessExamples(instance.getItem());
                }
            });
        } else {
            picker.refresh();
        }
    }

    function setSecretState(row) {
        var key = byId('aiApiKey');
        var clearWrap = byId('aiClearKeyWrap');
        var clear = byId('aiClearKey');
        var keyTip = byId('aiKeyTip');
        var hasKey = !!(row && row.apiKeyConfigured);
        if (window.OaonesSecret) {
            OaonesSecret.reset(key, hasKey);
        } else if (key) {
            key.value = '';
        }
        if (clear) { clear.checked = false; }
        if (clearWrap) { clearWrap.hidden = !row; }
        if (keyTip) {
            keyTip.textContent = hasKey
                ? '已配置 Token，留空表示不修改。'
                : 'Bearer 认证 Token，只保存在服务端。';
        }
    }

    function applyFormMode(row) {
        var preset = state.presetMode;
        var fields = [byId('aiApiUrl'), byId('aiModel'), byId('aiRequestParams')];
        fields.forEach(function (field) {
            if (field) { field.disabled = preset; }
        });
        var tip = byId('aiPresetTip');
        if (tip) { tip.hidden = !preset; }
        var defaultBox = byId('aiConnectorDefault');
        var enabledBox = byId('aiConnectorEnabled');
        if (defaultBox) {
            if (row) { defaultBox.checked = !!row.defaultConnector; }
            defaultBox.disabled = !!(row && row.defaultConnector) || !!(enabledBox && !enabledBox.checked);
        }
        var title = byId('aiModalTitle');
        if (title) { title.textContent = row ? '编辑 AI 连接器' : '新增 AI 连接器'; }
    }

    function typeCheckboxes() {
        return document.querySelectorAll('#aiConnectorForm input[name="supportedTypes"]');
    }

    function selectedTypes() {
        var result = [];
        var boxes = typeCheckboxes();
        for (var i = 0; i < boxes.length; i++) {
            if (boxes[i].checked) {
                result.push(boxes[i].value);
            }
        }
        return result;
    }

    function setSelectedTypes(value) {
        var types = {};
        normalizedTypes(value).forEach(function (code) {
            types[code] = true;
        });
        var boxes = typeCheckboxes();
        for (var i = 0; i < boxes.length; i++) {
            boxes[i].checked = !!types[boxes[i].value];
        }
    }

    function clearTestResult() {
        var result = byId('aiTestResult');
        if (result) {
            result.hidden = true;
            result.className = 'ai-test-result';
            result.textContent = '';
        }
    }

    function showTestResult(message, success) {
        var result = byId('aiTestResult');
        if (!result) {
            return;
        }
        result.hidden = false;
        result.className = 'ai-test-result ' + (success ? 'success' : 'error');
        result.textContent = message;
    }

    function openEditor(row) {
        var form = byId('aiConnectorForm');
        if (!form) {
            return;
        }
        form.reset();
        editingId = row ? String(row.id) : '';
        byId('aiConnectorId').value = editingId;
        byId('aiConnectorName').value = row ? (row.name || '') : '';
        byId('aiApiUrl').value = row ? (row.apiUrl || '') : '';
        byId('aiModel').value = row ? (row.model || '') : '';
        byId('aiRequestParams').value = row ? (row.requestParams || '{}') : '{}';
        byId('aiConnectorEnabled').checked = row ? !!row.enabled : true;
        byId('aiConnectorDefault').checked = row ? !!row.defaultConnector : !defaultRow();
        setSelectedTypes(row ? row.supportedTypes : ['text']);
        clearTestResult();
        setSecretState(row);
        applyFormMode(row);
        OaonesModal.open('aiConnectorModal', { focus: true });
    }

    function closeEditor() {
        OaonesModal.close('aiConnectorModal');
    }

    function save() {
        var name = String(byId('aiConnectorName').value || '').trim();
        if (!name) {
            toast('请填写 AI 别名', 1);
            byId('aiConnectorName').focus();
            return;
        }
        var enabled = !!byId('aiConnectorEnabled').checked;
        var defaultConnector = !!byId('aiConnectorDefault').checked;
        var supportedTypes = selectedTypes();
        if (!supportedTypes.length) {
            toast('请至少选择一种 AI 支持类型', 1);
            return;
        }
        if (!enabled) {
            defaultConnector = false;
        }
        var payload = {
            id: editingId,
            name: name,
            apiUrl: byId('aiApiUrl').value,
            apiKey: window.OaonesSecret ? OaonesSecret.value(byId('aiApiKey')) : byId('aiApiKey').value,
            model: byId('aiModel').value,
            supportedTypes: supportedTypes,
            requestParams: byId('aiRequestParams').value,
            enabled: enabled,
            defaultConnector: defaultConnector,
            clearApiKey: !!byId('aiClearKey').checked
        };
        var button = byId('aiConnectorSave');
        if (button) {
            button.disabled = true;
            button.textContent = '保存中…';
        }
        request('admin.connector.save', { payload: JSON.stringify(payload) }).then(function (data) {
            normalize(data);
            render();
            closeEditor();
            toast(data.message || 'AI 连接器已保存', 0);
        }).catch(function (error) {
            toast(error.message || '保存失败', 2);
        }).then(function () {
            if (button) {
                button.disabled = false;
                button.textContent = '保存 AI 连接器';
            }
        });
    }

    function testPayload() {
        var types = selectedTypes();
        if (!types.length) {
            toast('请至少选择一种 AI 支持类型', 1);
            return null;
        }
        return {
            id: editingId,
            name: byId('aiConnectorName').value,
            apiUrl: byId('aiApiUrl').value,
            apiKey: window.OaonesSecret ? OaonesSecret.value(byId('aiApiKey')) : byId('aiApiKey').value,
            model: byId('aiModel').value,
            supportedTypes: types,
            requestParams: byId('aiRequestParams').value,
            clearApiKey: !!byId('aiClearKey').checked
        };
    }

    function testConnector(payload, editorMode, button) {
        if (!payload) {
            return;
        }
        var originalText = button ? button.textContent : '';
        if (button) {
            button.disabled = true;
            button.textContent = '测试中…';
        }
        if (editorMode) {
            showTestResult('正在连接 AI 服务，请稍候…', true);
        }
        request('admin.connector.test', { payload: JSON.stringify(payload) }).then(function (data) {
            var latency = parseInt(data.latencyMs, 10);
            var message = data.message || 'AI 连接测试成功';
            if (!isNaN(latency)) {
                message += '，耗时 ' + latency + ' ms';
            }
            if (data.reply) {
                message += '，返回：' + data.reply;
            }
            if (editorMode) {
                showTestResult(message, true);
            }
            toast(message, 0);
        }).catch(function (error) {
            var message = error.message || 'AI 连接测试失败';
            if (editorMode) {
                showTestResult(message, false);
            }
            toast(message, 2);
        }).then(function () {
            if (button) {
                button.disabled = false;
                button.textContent = originalText;
            }
        });
    }

    function setDefault(id) {
        var row = rowById(id);
        request('admin.connector.setDefault', {
            payload: JSON.stringify({ id: id })
        }).then(function (data) {
            normalize(data);
            render();
            toast(data.message || '默认 AI 已切换', 0);
        }).catch(function (error) {
            toast(error.message || '设置默认 AI 失败', 2);
        });
    }

    function toggleEnabled(row) {
        var enabling = !row.enabled;
        if (!enabling) {
            var message = row.defaultConnector
                ? '「' + row.name + '」当前是默认 AI。禁用后将自动选择其他已启用 AI；若没有其他 AI，业务侧将暂时无法调用。确定禁用吗？'
                : '禁用后「' + row.name + '」不会出现在 AI 选择器中，也不能被业务接口调用。确定禁用吗？';
            OaonesToast.confirm(message, function () {
                changeEnabled(row, false);
            }, {
                title: '禁用 AI 连接器',
                okText: '禁用',
                danger: true
            });
            return;
        }
        changeEnabled(row, true);
    }

    function changeEnabled(row, enabled) {
        request('admin.connector.setEnabled', {
            payload: JSON.stringify({ id: row.id, enabled: enabled })
        }).then(function (data) {
            normalize(data);
            render();
            toast(data.message || (enabled ? 'AI 连接器已启用' : 'AI 连接器已禁用'), 0);
        }).catch(function (error) {
            toast(error.message || '更新 AI 状态失败', 2);
        });
    }

    function onClick(event) {
        var target = event.target;
        if (!target || !target.closest) {
            return;
        }
        var button = target.closest('[data-ai-action]');
        if (!button) {
            return;
        }
        var action = button.getAttribute('data-ai-action');
        var id = button.getAttribute('data-id');
        var row = rowById(id);
        if (!row) {
            return;
        }
        if (action === 'edit') {
            openEditor(row);
        } else if (action === 'test') {
            testConnector({ id: row.id }, false, button);
        } else if (action === 'default') {
            setDefault(id);
        } else if (action === 'disable' || action === 'enable') {
            toggleEnabled(row);
        }
    }

    if (CFG.canAdmin) {
        updateAccessExamples({ id: '', name: '跟随默认 AI' });
        bindAdminTabs();
        bindConnectorTabs();
        var addButton = byId('aiAdd');
        var refreshButton = byId('aiRefresh');
        var form = byId('aiConnectorForm');
        var tableBody = byId('aiTableBody');
        var enabledBox = byId('aiConnectorEnabled');
        var defaultBox = byId('aiConnectorDefault');
        var testButton = byId('aiConnectorTest');

        if (addButton) { addButton.addEventListener('click', function () { openEditor(null); }); }
        if (refreshButton) { refreshButton.addEventListener('click', function () { load(true); }); }
        if (testButton) {
            testButton.addEventListener('click', function () {
                testConnector(testPayload(), true, testButton);
            });
        }
        if (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                save();
            });
        }
        if (tableBody) { tableBody.addEventListener('click', onClick); }
        if (enabledBox) {
            enabledBox.addEventListener('change', function () {
                if (!enabledBox.checked) {
                    defaultBox.checked = false;
                    defaultBox.disabled = true;
                } else {
                    defaultBox.disabled = !!(editingId && rowById(editingId) && rowById(editingId).defaultConnector);
                }
            });
        }
        if (window.OaonesSecret && byId('aiApiKey')) {
            OaonesSecret.bind(byId('aiApiKey'));
        }
        load(true);
    }
})();
