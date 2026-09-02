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
                    tag === 'textarea'
                ) {
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

                if (
                    element.children.length > 0 &&
                    !(
                        element.children.length === 1 &&
                        element.children[0].tagName === 'BR'
                    )
                ) {
                    return;
                }

                if (element.hasAttribute('data-ai-translate-id')) {
                    return;
                }

                const text = element.innerText
                    .replace(/\s+/g, ' ')
                    .trim();

                if (text.length <= 1) {
                    return;
                }

                const nodeId = 'ai_node_' + nodeIndex;

                element.setAttribute(
                    'data-ai-translate-id',
                    nodeId
                );

                result.push({
                    id: nodeId,
                    text: text
                });

                nodeIndex++;
            });

            return JSON.stringify(result);
        })();
        """.trimIndent()
    }

    fun buildReplaceScript(
        translations: List<TranslatedNode>
    ): String {

        val items = translations.map {
            JSONObject().apply {
                put("id", it.id)
                put("translatedText", it.translatedText)
            }.toString()
        }

        return """
        (function() {
            const translations = [
                ${items.joinToString(",")}
            ];

            const elements = document.querySelectorAll(
                '[data-ai-translate-id]'
            );

            elements.forEach(function(element) {

                const id = element.getAttribute(
                    'data-ai-translate-id'
                );

                const item = translations.find(
                    function(t) {
                        return t.id === id;
                    }
                );

                if (!item) {
                    return;
                }

                element.innerText = item.translatedText;

                element.setAttribute('dir', 'auto');
                element.style.direction = 'auto';
            });
        })();
        """.trimIndent()
    }

    fun buildSelectionScript(): String {
        return """
        (function() {
            const selection = window.getSelection();

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

        val escapedText = JSONObject.quote(translatedText)

        return """
        (function() {
            const selection = window.getSelection();

            if (
                selection &&
                selection.rangeCount > 0
            ) {
                const range = selection.getRangeAt(0);

                range.deleteContents();

                const wrapper = document.createElement('span');

                wrapper.setAttribute('dir', 'auto');
                wrapper.style.backgroundColor = '#FFF9C4';
                wrapper.style.padding = '2px 4px';
                wrapper.style.borderRadius = '3px';
                wrapper.style.fontSize = '0.95em';

                wrapper.innerText = $escapedText;

                range.insertNode(wrapper);

                selection.removeAllRanges();
            }
        })();
        """.trimIndent()
    }

    /**
     * يحول نتيجة evaluateJavascript من WebView
     * إلى النص الحقيقي الموجود داخلها.
     *
     * مثال:
     * WebView يعيد:
     * "\"hello world\""
     *
     * والنتيجة هنا:
     * "hello world"
     */
    fun decodeJavascriptResult(
        rawResult: String?
    ): String {

        if (rawResult.isNullOrBlank()) {
            return ""
        }

        return try {
            val value = JSONTokener(rawResult).nextValue()

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

    /**
     * يستخرج النص من نتيجة buildSelectionScript().
     */
    fun parseSelectionText(
        rawResult: String?
    ): String {

        if (rawResult.isNullOrBlank()) {
            return ""
        }

        return try {
            val decoded = decodeJavascriptResult(rawResult)

            if (decoded.isBlank()) {
                return ""
            }

            val json = JSONObject(decoded)

            json.optString("text", "")
                .trim()

        } catch (_: Exception) {
            ""
        }
    }

    fun parseExtractedNodes(
        jsonString: String
    ): List<PageTextNode> {

        if (jsonString.isBlank()) {
            return emptyList()
        }

        return try {
            val decoded = decodeJavascriptResult(jsonString)

            if (decoded.isBlank()) {
                return emptyList()
            }

            val array = JSONArray(decoded)
            val nodes = mutableListOf<PageTextNode>()

            for (i in 0 until array.length()) {

                val obj = array.optJSONObject(i)
                    ?: continue

                val id = obj.optString("id", "")
                val text = obj.optString("text", "")

                if (id.isNotBlank() && text.isNotBlank()) {
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
