package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Builds safe JavaScript and parses WebView evaluateJavascript results. */
class WebPageTranslator {

    fun buildExtractScript(): String = """
        (function() {
            try {
                const selector = 'h1,h2,h3,h4,h5,h6,p,li,span,a,td,th,button,label,option,blockquote,figcaption,summary';
                const elements = document.querySelectorAll(selector);
                const result = [];
                let index = 0;

                elements.forEach(function(element) {
                    if (!element || element.children.length > 0) return;
                    if (element.offsetParent === null) return;
                    if (element.closest('script,style,noscript,template')) return;

                    const text = (element.innerText || element.textContent || '').trim();
                    if (text.length < 2) return;
                    if (element.getAttribute('data-ai-translate-id')) return;

                    const id = 'ai_node_' + index++;
                    element.setAttribute('data-ai-translate-id', id);
                    result.push({ id: id, text: text });
                });

                return JSON.stringify(result);
            } catch (e) {
                return JSON.stringify([]);
            }
        })();
    """.trimIndent()

    fun buildReplaceScript(translations: List<TranslatedNode>): String {
        val json = JSONArray().apply {
            translations.forEach { node ->
                put(JSONObject().apply {
                    put("id", node.id)
                    put("text", node.translatedText)
                })
            }
        }

        // JSON.stringify gives correct JS escaping for quotes, newlines, backslashes,
        // Unicode separators, etc. No hand-written JS string escaping is needed.
        val payload = JSONObject.quote(json.toString())
        return """
            (function() {
                try {
                    const items = JSON.parse($payload);
                    items.forEach(function(item) {
                        const selector = '[data-ai-translate-id="' + item.id + '"]';
                        const el = document.querySelector(selector);
                        if (!el) return;
                        el.textContent = item.text;
                        el.style.direction = 'rtl';
                        el.style.textAlign = 'right';
                    });
                    document.documentElement.setAttribute('data-ai-translated', 'true');
                } catch (e) {}
                return 'ok';
            })();
        """.trimIndent()
    }

    fun buildSelectionScript(): String = """
        (function() {
            try {
                const selection = window.getSelection();
                const text = selection ? selection.toString().trim() : '';
                return JSON.stringify({ text: text });
            } catch (e) {
                return JSON.stringify({ text: '' });
            }
        })();
    """.trimIndent()

    fun buildReplaceSelectionScript(translatedText: String): String {
        val payload = JSONObject.quote(translatedText)
        return """
            (function() {
                try {
                    const selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) return 'no-selection';
                    const range = selection.getRangeAt(0);
                    range.deleteContents();
                    const wrapper = document.createElement('span');
                    wrapper.textContent = $payload;
                    wrapper.style.direction = 'rtl';
                    wrapper.style.textAlign = 'right';
                    wrapper.style.backgroundColor = '#FFF9C4';
                    wrapper.style.padding = '2px 4px';
                    wrapper.style.borderRadius = '3px';
                    range.insertNode(wrapper);
                    selection.removeAllRanges();
                    return 'ok';
                } catch (e) {
                    return 'error';
                }
            })();
        """.trimIndent()
    }

    /** Decodes the JSON-string result returned by WebView.evaluateJavascript. */
    fun decodeJavascriptResult(rawResult: String?): String? {
        if (rawResult.isNullOrBlank() || rawResult == "null") return null
        return try {
            val decoded = JSONTokener(rawResult).nextValue()
            when (decoded) {
                is String -> decoded
                null -> null
                else -> decoded.toString()
            }
        } catch (_: Exception) {
            // Some WebView versions/plugins may return an already-decoded value.
            rawResult
        }
    }

    fun parseExtractedNodes(value: String?): List<PageTextNode> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(value)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val text = obj.optString("text").trim()
                    if (id.isNotBlank() && text.isNotBlank()) {
                        add(PageTextNode(id = id, text = text))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseSelectionText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(value)
            obj.optString("text").trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
