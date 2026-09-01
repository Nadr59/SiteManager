package com.nadr59.sitemanager.domain.translator

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode

class WebPageTranslator {

    private val gson = Gson()

    fun buildExtractScript(): String {
        return """
            (function() {
                try {
                    var nodes = [];
                    var counter = 0;
                    var selectors = 'h1,h2,h3,h4,h5,h6,p,li,span,a,td,th,button,label,option,blockquote,figcaption,summary';
                    var elements = document.querySelectorAll(selectors);
                    elements.forEach(function(el) {
                        if (el.children.length === 0) {
                            var text = el.innerText.trim();
                            if (text.length > 0) {
                                var id = 'ai_node_' + counter;
                                el.setAttribute('data-ai-translate-id', id);
                                nodes.push({ id: id, text: text });
                                counter++;
                            }
                        }
                    });
                    return JSON.stringify(nodes);
                } catch(e) {
                    return '[]';
                }
            })();
        """.trimIndent()
    }

    fun parseExtractedNodes(jsonResult: String): List<PageTextNode> {
        return try {
            val cleanJson = jsonResult.trim('"').replace("\\\"", "\"")
            val nodesArray = gson.fromJson(cleanJson, Array<JsonObject>::class.java)
            nodesArray.map { obj ->
                PageTextNode(
                    id = obj.get("id")?.asString ?: "",
                    text = obj.get("text")?.asString ?: ""
                )
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun buildReplaceScript(translations: List<TranslatedNode>): String {
        val replacements = translations.joinToString(",\n") { node ->
            val escapedText = node.translatedText
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\"", "\\\"")
            "{ id: '${node.id}', text: '$escapedText' }"
        }

        return """
            (function() {
                try {
                    var translations = [$replacements];
                    translations.forEach(function(t) {
                        var el = document.querySelector('[data-ai-translate-id="' + t.id + '"]');
                        if (el) {
                            el.innerText = t.text;
                            el.style.direction = 'rtl';
                            el.style.textAlign = 'right';
                        }
                    });
                    return true;
                } catch(e) {
                    console.error('Translation error:', e);
                    return false;
                }
            })();
        """.trimIndent()
    }

    fun buildSelectionScript(): String {
        return """
            (function() {
                try {
                    var selection = window.getSelection();
                    var text = selection.toString().trim();
                    return JSON.stringify({ text: text, rangeCount: selection.rangeCount });
                } catch(e) {
                    return JSON.stringify({ text: '', rangeCount: 0 });
                }
            })();
        """.trimIndent()
    }

    fun parseSelectionJson(jsonResult: String): String {
        return try {
            val cleanJson = jsonResult.trim('"').replace("\\\"", "\"")
            val json = gson.fromJson(cleanJson, JsonObject::class.java)
            json.get("text")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
