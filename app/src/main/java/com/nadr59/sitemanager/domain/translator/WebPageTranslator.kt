package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import org.json.JSONArray
import org.json.JSONObject

class WebPageTranslator {

    // ═══ استخراج النصوص من الصفحة ═══
    fun buildExtractScript(): String {
        return """
        (function() {
            const elements = document.querySelectorAll(
                'h1,h2,h3,h4,h5,h6,p,li,span,a,td,th,button,label,option,blockquote,figcaption,summary'
            );
            const result = [];
            let nodeIndex = 0;

            elements.forEach(function(element) {
                if (element.children.length === 0 ||
                    (element.children.length === 1 && element.children[0].tagName === 'BR')) {

                    const text = element.innerText.trim();

                    if (text.length > 1 &&
                        !element.hasAttribute('data-ai-translate-id') &&
                        element.offsetParent !== null) {

                        const nodeId = 'ai_node_' + nodeIndex;
                        element.setAttribute('data-ai-translate-id', nodeId);
                        result.push({
                            id: nodeId,
                            text: text
                        });
                        nodeIndex++;
                    }
                }
            });

            return JSON.stringify(result);
        })();
        """.trimIndent()
    }

    // ═══ استبدال النصوص المترجمة ═══
    fun buildReplaceScript(translations: List<TranslatedNode>): String {
        val js = StringBuilder()
        js.appendLine("(function() {")

        for (node in translations) {
            val escapedText = node.translatedText
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\"", "\\\"")

            js.appendLine("""
                try {
                    var el = document.querySelector('[data-ai-translate-id="${node.id}"]');
                    if (el) {
                        el.innerText = '$escapedText';
                        el.style.direction = 'rtl';
                        el.style.textAlign = 'right';
                    }
                } catch(e) {}
            """.trimIndent())
        }

        js.appendLine("})();")
        return js.toString()
    }

    // ═══ ترجمة النص المحدد ═══
    fun buildSelectionScript(): String {
        return """
        (function() {
            var selection = window.getSelection();
            if (selection && selection.toString().trim().length > 0) {
                return JSON.stringify({
                    text: selection.toString().trim(),
                    rangeCount: selection.rangeCount
                });
            }
            return JSON.stringify({text: '', rangeCount: 0});
        })();
        """.trimIndent()
    }

    // ═══ استبدال النص المحدد بالترجمة ═══
    fun buildReplaceSelectionScript(translatedText: String): String {
        val escaped = translatedText
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\"", "\\\"")

        return """
        (function() {
            var selection = window.getSelection();
            if (selection && selection.rangeCount > 0) {
                var range = selection.getRangeAt(0);
                range.deleteContents();

                var wrapper = document.createElement('span');
                wrapper.style.direction = 'rtl';
                wrapper.style.backgroundColor = '#FFF9C4';
                wrapper.style.padding = '2px 4px';
                wrapper.style.borderRadius = '3px';
                wrapper.style.fontSize = '0.95em';
                wrapper.innerText = '$escaped';

                range.insertNode(wrapper);
                selection.removeAllRanges();
            }
        })();
        """.trimIndent()
    }

    // ═══ تحليل JSON المستخرج ═══
    fun parseExtractedNodes(jsonString: String): List<PageTextNode> {
        return try {
            val cleanJson = jsonString
                .removePrefix("\"")
                .removeSuffix("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")

            val arr = JSONArray(cleanJson)
            val nodes = mutableListOf<PageTextNode>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                nodes.add(
                    PageTextNode(
                        id = obj.getString("id"),
                        text = obj.getString("text")
                    )
                )
            }
            nodes
        } catch (e: Exception) {
            emptyList()
        }
    }
    // ⭐ أضف هذه الدالة الجديدة
    fun parseSelectionJson(jsonResult: String): String {
        return try {
            val json = com.google.gson.Gson().fromJson(
                jsonResult, 
                com.google.gson.JsonObject::class.java
            )
            json.get("text")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
}
