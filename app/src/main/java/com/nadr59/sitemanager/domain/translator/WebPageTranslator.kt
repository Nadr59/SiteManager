package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import org.json.JSONArray
import org.json.JSONTokener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebPageTranslator @Inject constructor() {

    /**
     * استخراج النصوص المرئية من الصفحة.
     *
     * يتم وضع data-ai-translate-id على العناصر حتى نستطيع
     * إعادة الترجمة إلى نفس المكان لاحقاً.
     */
    fun buildExtractScript(): String {
        return """
        (function() {
            const elements = document.querySelectorAll(
                'h1,h2,h3,h4,h5,h6,p,li,span,a,td,th,button,label,option,blockquote,figcaption,summary'
            );

            const result = [];
            let nodeIndex = 0;

            elements.forEach(function(element) {

                const tag = element.tagName.toLowerCase();

                if (
                    tag === 'script' ||
                    tag === 'style' ||
                    tag === 'noscript' ||
                    tag === 'template' ||
                    tag === 'svg' ||
                    tag === 'canvas' ||
                    tag === 'input' ||
                    tag === 'textarea' ||
                    tag === 'select'
                ) {
                    return;
                }

                if (element.hasAttribute('data-ai-translate-id')) {
                    return;
                }

                const style = window.getComputedStyle(element);

                if (
                    style.display === 'none' ||
                    style.visibility === 'hidden' ||
                    style.opacity === '0' ||
                    element.offsetParent === null
                ) {
                    return;
                }

                /*
                 * نترجم العناصر النهائية فقط.
                 * هذا يمنع ترجمة النص نفسه عدة مرات بسبب
                 * وجود p داخل article مثلاً.
                 */
                if (
                    element.children.length > 0 &&
                    !(
                        element.children.length === 1 &&
                        element.children[0].tagName === 'BR'
                    )
                ) {
                    return;
                }

                const text = element.innerText
                    .replace(/\s+/g, ' ')
                    .trim();

                if (text.length <= 1) {
                    return;
                }

                const nodeId = 'ai_node_' + nodeIndex++;

                element.setAttribute(
                    'data-ai-translate-id',
                    nodeId
                );

                element.setAttribute(
                    'data-ai-original-text',
                    text
                );

                result.push({
                    id: nodeId,
                    text: text
                });
            });

            return JSON.stringify(result);
        })();
        """.trimIndent()
    }

    /**
     * تثبيت مراقب للمحتوى الديناميكي.
     *
     * MutationObserver يراقب العناصر التي تضيفها الصفحة
     * بعد التحميل الأول.
     */
    fun buildInstallDynamicObserverScript(): String {
        return """
        (function() {
            try {

                if (window.__siteManagerTranslationObserver) {
                    window.__siteManagerTranslationObserver.disconnect();
                }

                window.__siteManagerTranslationQueue = [];
                window.__siteManagerTranslationCounter = 100000;

                function isIgnoredElement(element) {
                    if (!element || element.nodeType !== Node.ELEMENT_NODE) {
                        return true;
                    }

                    const tag = element.tagName.toLowerCase();

                    return (
                        tag === 'script' ||
                        tag === 'style' ||
                        tag === 'noscript' ||
                        tag === 'template' ||
                        tag === 'svg' ||
                        tag === 'canvas' ||
                        tag === 'input' ||
                        tag === 'textarea' ||
                        tag === 'select'
                    );
                }

                function isVisible(element) {
                    if (!element) return false;

                    const style = window.getComputedStyle(element);

                    return (
                        style.display !== 'none' &&
                        style.visibility !== 'hidden' &&
                        style.opacity !== '0' &&
                        element.offsetParent !== null
                    );
                }

                function alreadyProcessed(element) {
                    return (
                        element.hasAttribute('data-ai-translate-id') ||
                        element.hasAttribute('data-ai-observed')
                    );
                }

                function processElement(element) {

                    if (!element) return;

                    if (element.nodeType === Node.TEXT_NODE) {

                        const parent = element.parentElement;

                        if (
                            !parent ||
                            isIgnoredElement(parent) ||
                            !isVisible(parent)
                        ) {
                            return;
                        }

                        if (parent.hasAttribute('data-ai-translate-id')) {
                            return;
                        }

                        const text = element.textContent
                            .replace(/\s+/g, ' ')
                            .trim();

                        if (text.length <= 1) {
                            return;
                        }

                        const id =
                            'ai_dynamic_node_' +
                            window.__siteManagerTranslationCounter++;

                        const wrapper =
                            document.createElement('span');

                        wrapper.setAttribute(
                            'data-ai-translate-id',
                            id
                        );

                        wrapper.setAttribute(
                            'data-ai-observed',
                            'true'
                        );

                        wrapper.setAttribute(
                            'data-ai-original-text',
                            text
                        );

                        /*
                         * نستبدل TextNode بـ wrapper مع الاحتفاظ
                         * بالنص الأصلي داخله.
                         */
                        element.parentNode.insertBefore(
                            wrapper,
                            element
                        );

                        wrapper.appendChild(element);

                        window.__siteManagerTranslationQueue.push({
                            id: id,
                            text: text
                        });

                        return;
                    }

                    if (element.nodeType !== Node.ELEMENT_NODE) {
                        return;
                    }

                    if (isIgnoredElement(element)) {
                        return;
                    }

                    if (alreadyProcessed(element)) {
                        return;
                    }

                    const children =
                        Array.from(element.childNodes);

                    children.forEach(function(child) {
                        processElement(child);
                    });
                }

                window.__siteManagerTranslationObserver =
                    new MutationObserver(function(mutations) {

                        mutations.forEach(function(mutation) {

                            mutation.addedNodes.forEach(
                                function(node) {

                                    if (
                                        node.nodeType ===
                                        Node.ELEMENT_NODE
                                    ) {
                                        processElement(node);
                                    }
                                }
                            );
                        });
                    });

                if (document.body) {
                    window.__siteManagerTranslationObserver.observe(
                        document.body,
                        {
                            childList: true,
                            subtree: true
                        }
                    );
                }

                return 'observer_installed';

            } catch (e) {
                return 'observer_error';
            }
        })();
        """.trimIndent()
    }

    /**
     * استخراج العناصر الجديدة التي اكتشفها MutationObserver.
     */
    fun buildPollDynamicNodesScript(): String {
        return """
        (function() {
            try {

                if (
                    !window.__siteManagerTranslationQueue ||
                    window.__siteManagerTranslationQueue.length === 0
                ) {
                    return JSON.stringify([]);
                }

                const result =
                    window.__siteManagerTranslationQueue.splice(
                        0,
                        50
                    );

                return JSON.stringify(result);

            } catch (e) {
                return JSON.stringify([]);
            }
        })();
        """.trimIndent()
    }

    /**
     * إعادة النص المترجم إلى العناصر.
     *
     * textContent يستخدم بدلاً من innerHTML حتى لا يتم
     * تنفيذ HTML قادم من خدمة الترجمة.
     */
     fun buildInstallDynamicObserverScript(): String {
    return """
        (function() {
            try {
                if (window.__siteManagerTranslationObserver) {
                    window.__siteManagerTranslationObserver.disconnect();
                }

                window.__siteManagerTranslationQueue = [];

                var counter = window.__siteManagerTranslationCounter || 100000;

                function isIgnored(element) {
                    if (!element) return true;
                    var tag = element.tagName;
                    return tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT' ||
                           tag === 'TEXTAREA' || tag === 'INPUT' || tag === 'SELECT';
                }

                function isVisible(element) {
                    if (!element) return false;
                    var style = window.getComputedStyle(element);
                    return style.display !== 'none' &&
                           style.visibility !== 'hidden' &&
                           element.offsetParent !== null;
                }

                function collectElement(element) {
                    if (!element) return;

                    if (element.nodeType === Node.TEXT_NODE) {
                        var parent = element.parentElement;
                        if (!parent || isIgnored(parent) || !isVisible(parent)) return;

                        var text = element.textContent.trim();
                        if (text.length <= 1) return;

                        var wrapper = document.createElement('span');
                        var id = 'ai_dynamic_node_' + counter++;

                        wrapper.setAttribute('data-ai-translate-id', id);
                        wrapper.setAttribute('data-ai-translate-original', text);

                        element.parentNode.insertBefore(wrapper, element);
                        wrapper.appendChild(element);

                        window.__siteManagerTranslationQueue.push({
                            id: id,
                            text: text
                        });
                        return;
                    }

                    if (element.nodeType !== Node.ELEMENT_NODE) return;
                    if (isIgnored(element)) return;

                    var children = Array.from(element.childNodes);
                    children.forEach(function(child) {
                        collectElement(child);
                    });
                }

                window.__siteManagerTranslationObserver = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            collectElement(node);
                        });
                    });
                });

                if (document.body) {
                    window.__siteManagerTranslationObserver.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                }

                window.__siteManagerTranslationCounter = counter;

                return 'observer_installed';
            } catch (e) {
                return 'observer_error';
            }
        })();
    """.trimIndent()
     }
    fun buildReplaceScript(
        translations: List<TranslatedNode>
    ): String {

        val jsonArray = JSONArray()

        translations.forEach {
            jsonArray.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("translatedText", it.translatedText)
                }
            )
        }

        return """
        (function() {

            const translations =
                $jsonArray;

            translations.forEach(function(item) {

                const element =
                    document.querySelector(
                        '[data-ai-translate-id="' +
                        CSS.escape(item.id) +
                        '"]'
                    );

                if (!element) {
                    return;
                }

                /*
                 * إذا كان wrapper ديناميكيًا يحتوي على TextNode،
                 * textContent يحافظ على wrapper نفسه.
                 */
                element.textContent =
                    item.translatedText;

                element.setAttribute(
                    'data-ai-translated',
                    'true'
                );

                element.setAttribute(
                    'dir',
                    'auto'
                );

                element.style.direction = 'auto';
            });

        })();
        """.trimIndent()
    }

    fun buildSelectionScript(): String {
        return """
        (function() {

            const selection =
                window.getSelection();

            if (
                selection &&
                selection.toString().trim().length > 0
            ) {
                return JSON.stringify({
                    text: selection.toString().trim(),
                    rangeCount: selection.rangeCount
                });
            }

            return JSON.stringify({
                text: '',
                rangeCount: 0
            });

        })();
        """.trimIndent()
    }

    fun buildReplaceSelectionScript(
        translatedText: String
    ): String {

        val escapedText =
            JSONObject.quote(translatedText)

        return """
        (function() {

            const selection =
                window.getSelection();

            if (
                selection &&
                selection.rangeCount > 0
            ) {

                const range =
                    selection.getRangeAt(0);

                range.deleteContents();

                const wrapper =
                    document.createElement('span');

                wrapper.setAttribute(
                    'dir',
                    'auto'
                );

                wrapper.setAttribute(
                    'data-ai-selection-translation',
                    'true'
                );

                wrapper.style.backgroundColor =
                    '#FFF9C4';

                wrapper.style.padding =
                    '2px 4px';

                wrapper.style.borderRadius =
                    '3px';

                wrapper.style.fontSize =
                    '0.95em';

                wrapper.textContent =
                    $escapedText;

                range.insertNode(wrapper);

                selection.removeAllRanges();
            }

        })();
        """.trimIndent()
    }

    fun decodeJavascriptResult(
        rawResult: String?
    ): String {

        if (rawResult.isNullOrBlank()) {
            return ""
        }

        return try {

            val value =
                JSONTokener(rawResult).nextValue()

            when (value) {
                is String -> value
                is JSONArray -> value.toString()
                is JSONObject -> value.toString()
                else -> value?.toString() ?: ""
            }

        } catch (_: Exception) {

            rawResult
                .removePrefix("\"")
                .removeSuffix("\"")
        }
    }

    fun parseSelectionText(
        rawResult: String?
    ): String? {

        if (rawResult.isNullOrBlank()) {
            return null
        }

        return try {

            val decoded =
                decodeJavascriptResult(rawResult)

            if (decoded.isBlank()) {
                return null
            }

            JSONObject(decoded)
                .optString("text", "")
                .trim()
                .ifBlank { null }

        } catch (_: Exception) {
            null
        }
    }

    fun parseExtractedNodes(
        jsonString: String
    ): List<PageTextNode> {

        if (jsonString.isBlank()) {
            return emptyList()
        }

        return try {

            val decoded =
                decodeJavascriptResult(jsonString)

            if (decoded.isBlank()) {
                return emptyList()
            }

            val array =
                JSONArray(decoded)

            val nodes =
                mutableListOf<PageTextNode>()

            for (i in 0 until array.length()) {

                val obj =
                    array.optJSONObject(i)
                        ?: continue

                val id =
                    obj.optString("id", "")

                val text =
                    obj.optString("text", "")

                if (
                    id.isNotBlank() &&
                    text.isNotBlank()
                ) {
                    nodes.add(
                        PageTextNode(
                            id = id,
                            text = text
                        )
                    )
                }
            }

            nodes

        } catch (_: Exception) {
            emptyList()
        }
    }
}
