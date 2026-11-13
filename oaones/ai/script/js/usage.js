(function () {
    'use strict';

    var CFG = window.OAONES_AI_USAGE || {};
    var state = {
        summary: {},
        models: [],
        loading: false
    };
    var pager = null;

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

    function number(value) {
        var parsed = Number(value);
        if (!isFinite(parsed)) {
            parsed = 0;
        }
        return parsed.toLocaleString('zh-CN');
    }

    function duration(value) {
        var ms = Number(value);
        if (!isFinite(ms) || ms < 0) {
            ms = 0;
        }
        if (ms >= 1000) {
            return (ms / 1000).toFixed(2) + ' 秒';
        }
        return Math.round(ms) + ' ms';
    }

    function request(data) {
        var payload = { action: 'admin.ai.usage' };
        Object.keys(data || {}).forEach(function (key) {
            payload[key] = data[key];
        });
        var body = Object.keys(payload).map(function (key) {
            return encodeURIComponent(key) + '=' + encodeURIComponent(
                payload[key] == null ? '' : String(payload[key]));
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

    function notice(message, error) {
        var el = byId('aiUsageNotice');
        if (!el) {
            return;
        }
        el.textContent = message || '';
        el.hidden = !message;
        el.className = 'ai-usage-notice' + (error ? ' error' : '');
    }

    function renderSummary() {
        var summary = state.summary || {};
        var calls = Number(summary.calls) || 0;
        var success = Number(summary.successCalls) || 0;
        var failed = Number(summary.failedCalls) || 0;
        var rate = calls > 0 ? (success * 100 / calls) : 0;
        byId('aiUsageCalls').textContent = number(calls);
        byId('aiUsageTokens').textContent = number(summary.totalTokens);
        byId('aiUsageSuccessRate').textContent = rate.toFixed(rate >= 100 || rate === 0 ? 0 : 1) + '%';
        byId('aiUsageSuccessDetail').textContent = number(success) + ' 成功 / ' + number(failed) + ' 失败';
        byId('aiUsageAliases').textContent = number(state.models.length);
    }

    function filteredModels() {
        var keyword = String((byId('aiUsageKeyword') || {}).value || '')
            .replace(/^\s+|\s+$/g, '').toLowerCase();
        if (!keyword) {
            return state.models;
        }
        return state.models.filter(function (item) {
            return String(item.connectorName || '').toLowerCase().indexOf(keyword) >= 0;
        });
    }

    function rowHtml(item) {
        return '<tr>'
            + '<td><span class="ai-usage-model">' + esc(item.connectorName || '未记录 AI 别名') + '</span></td>'
            + '<td class="ai-usage-number">' + number(item.calls) + '</td>'
            + '<td class="ai-usage-number success">' + number(item.successCalls) + '</td>'
            + '<td class="ai-usage-number' + (Number(item.failedCalls) > 0 ? ' failed' : '') + '">'
            + number(item.failedCalls) + '</td>'
            + '<td class="ai-usage-number">' + number(item.promptTokens) + '</td>'
            + '<td class="ai-usage-number">' + number(item.completionTokens) + '</td>'
            + '<td class="ai-usage-number total">' + number(item.totalTokens) + '</td>'
            + '<td>' + duration(item.averageDurationMs) + '</td>'
            + '<td class="ai-usage-time">' + esc(item.lastCallTime || '-') + '</td>'
            + '</tr>';
    }

    function renderRows() {
        var rows = filteredModels();
        var pageRows = pager ? pager.apply(rows) : rows;
        var body = byId('aiUsageBody');
        var tableWrap = byId('aiUsageTableWrap');
        var empty = byId('aiUsageEmpty');
        if (body) {
            body.innerHTML = pageRows.map(rowHtml).join('');
        }
        if (tableWrap) {
            tableWrap.hidden = pageRows.length === 0;
        }
        if (empty) {
            empty.hidden = pageRows.length !== 0;
        }
        if (pager) {
            pager.render();
        }
    }

    function render() {
        var loading = byId('aiUsageLoading');
        if (loading) {
            loading.hidden = true;
        }
        renderSummary();
        if (pager) {
            pager.resetIfChanged('');
        }
        renderRows();
    }

    function load() {
        if (state.loading) {
            return;
        }
        state.loading = true;
        var button = byId('aiUsageRefresh');
        var loading = byId('aiUsageLoading');
        if (button) {
            button.disabled = true;
            button.textContent = '刷新中…';
        }
        if (loading) {
            loading.hidden = false;
        }
        notice('');
        request({}).then(function (data) {
            state.summary = data.summary || {};
            state.models = Array.isArray(data.models) ? data.models : [];
            render();
        }).catch(function (error) {
            var message = error.message || 'AI 调用数据加载失败';
            if (loading) {
                loading.hidden = true;
            }
            notice(message + '。请确认已执行 oaones0005.sql。', true);
            if (window.OaonesToast) {
                OaonesToast.error(message);
            }
        }).then(function () {
            state.loading = false;
            if (button) {
                button.disabled = false;
                button.textContent = '刷新数据';
            }
        });
    }

    if (window.OaonesTable && byId('aiUsagePager')) {
        pager = OaonesTable.pager({
            key: 'oaones.ai.usage',
            el: byId('aiUsagePager'),
            onChange: renderRows
        });
    }

    var refresh = byId('aiUsageRefresh');
    var search = byId('aiUsageSearch');
    var reset = byId('aiUsageReset');
    var keyword = byId('aiUsageKeyword');
    if (refresh) {
        refresh.addEventListener('click', load);
    }
    if (search) {
        search.addEventListener('click', renderRows);
    }
    if (reset) {
        reset.addEventListener('click', function () {
            if (keyword) {
                keyword.value = '';
                keyword.focus();
            }
            renderRows();
        });
    }
    if (keyword) {
        keyword.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                renderRows();
            }
        });
    }
    if (window.OaonesTable && byId('aiUsageTable')) {
        OaonesTable.resizable(byId('aiUsageTable'), { key: 'oaones.ai.usage.table' });
    }

    load();
})();
