package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مسؤول عن التفاعل بين WebView ونظام الترجمة.
 *
 * الوظائف الرئيسية:
 * 1. استخراج النصوص القابلة للترجمة من الصفحة.
 * 2. إعطاء كل عنصر معرفاً ثابتاً.
 * 3. إعادة النصوص المترجمة إلى عناصرها الأصلية.
 * 4. ترجمة النص المحدد.
 * 5. تحليل نتائج JavaScript القادمة من WebView.
 *
 * متوافق مع:
 * - PageTextNode
 * - TranslatedNode
 * - TranslationRepository
 * - BrowserViewModel
 */
@Singleton
class WebPageTranslator @Inject constructor() {

    // =========================================================
    // استخراج النصوص من الصفحة
    // =========================================================

    /**
     * يبني JavaScript لاستخراج النصوص القابلة للترجمة.
     *
     * ملاحظات:
     * - لا نعدل النصوص أثناء الاستخراج.
     * - لا نضيف معرفات مكررة.
     * - نتجنب العناصر المخفية.
     * - نتجنب عناصر الإعلانات والقوائم والعناصر التقنية.
     * - نستخدم عناصر النص النهائية Leaf Nodes لتقليل التكرار.
     */
    fun buildExtractScript(): String {
        return """
            (function() {
                try {
                    const result = [];
                    let nodeIndex = 0;

                    const selectors = [
                        'h1',
                        'h2',
                        'h3',
                        'h4',
                        'h5',
                        'h6',
                        'p',
                        'li',
                        'span',
                        'a',
                        'td',
                        'th',
                        'button',
                        'label',
                        'option',
                        'blockquote',
                        'figcaption',
                        'summary'
                    ];

                    const excludedSelectors = [
                        'script',
                        'style',
                        'noscript',
                        'template',
                        'svg',
                        'canvas',
                        'textarea',
                        'input',
                        '[aria-hidden="true"]',
                        '[hidden]',
                        '[data-ai-ignore="true"]',
                        '.ad',
                        '.ads',
                        '.advert',
                        '.advertisement',
                        '.cookie',
                        '.popup',
                        '.modal',
                        '.sidebar',
                        '.social',
                        '.share'
                    ];

                    function isExcluded(element) {
                        for (let i = 0; i < excludedSelectors.length; i++) {
                            try {
                                if (element.matches(excludedSelectors[i])) {
                                    return true;
                                }
                            } catch (e) {}
                        }

                        try {
                            if (element.closest(
                                'script,style,noscript,template,svg,canvas,textarea,input,' +
                                '[aria-hidden="true"],[hidden],' +
                                '[data-ai-ignore="true"]'
                            )) {
                                return true;
                            }
                        } catch (e) {}

                        return false;
                    }

                    function isVisible(element) {
                        try {
                            const style = window.getComputedStyle(element);

                            if (
                                style.display === 'none' ||
                                style.visibility === 'hidden' ||
                                style.opacity === '0'
                            ) {
                                return false;
                            }

                            if (
                                element.offsetWidth <= 0 ||
                                element.offsetHeight <= 0
                            ) {
                                return false;
                            }

                            return true;

                        } catch (e) {
                            return true;
                        }
                    }

                    function normalizeText(text) {
                        return (text || '')
                            .replace(/\u00A0/g, ' ')
                            .replace(/[ \t]+/g, ' ')
                            .replace(/\n{3,}/g, '\n\n')
                            .trim();
                    }

                    const elements =
                        document.querySelectorAll(selectors.join(','));

                    elements.forEach(function(element) {

                        try {
                            if (isExcluded(element)) {
                                return;
                            }

                            if (!isVisible(element)) {
                                return;
                            }

                            /*
                             * نترجم العناصر التي تحتوي على نص نهائي.
                             *
                             * إذا كان العنصر يحتوي على عناصر نصية
                             * داخلية كثيرة، نتركها للعناصر الداخلية
                             * حتى لا تتكرر الترجمة.
                             */
                            const childElements =
                                element.querySelectorAll(
                                    'h1,h2,h3,h4,h5,h6,p,li,span,a,td,th,' +
                                    'button,label,option,blockquote,figcaption,summary'
                                );

                            if (childElements.length > 0) {

                                /*
                                 * إذا كان هناك عناصر ترجمة داخلية،
                                 * لا نأخذ العنصر الأب إلا إذا كان
                                 * يحتوي على نص مباشر مهم.
                                 */
                                let hasDirectText = false;

                                for (let i = 0; i < element.childNodes.length; i++) {
                                    const child = element.childNodes[i];

                                    if (
                                        child.nodeType === Node.TEXT_NODE &&
                                        normalizeText(child.textContent).length > 1
                                    ) {
                                        hasDirectText = true;
                                        break;
                                    }
                                }

                                if (!hasDirectText) {
                                    return;
                                }
                            }

                            const text =
                                normalizeText(element.innerText);

                            /*
                             * تجاهل النصوص القصيرة جداً.
                             */
                            if (text.length < 2) {
                                return;
                            }

                            /*
                             * تجاهل النصوص التي تبدو كروابط تقنية
                             * أو مسارات أو كود.
                             */
                            if (
                                text.length > 10000
                            ) {
                                return;
                            }

                            /*
                             * منع تكرار نفس النص الموجود في عناصر
                             * متجاورة بشكل واضح.
                             */
                            const nodeId =
                                'ai_node_' + nodeIndex;

                            element.setAttribute(
                                'data-ai-translate-id',
                                nodeId
                            );

                            result.push({
                                id: nodeId,
                                text: text
                            });

                            nodeIndex++;

                        } catch (e) {}
                    });

                    return JSON.stringify(result);

                } catch (error) {

                    return JSON.stringify([]);

                }
            })();
        """.trimIndent()
    }

    // =========================================================
    // استبدال النصوص المترجمة
    // =========================================================

    /**
     * يبني JavaScript لاستبدال النصوص الأصلية بالترجمة.
     *
     * استخدام JSON.stringify/JSON.parse هنا أكثر أماناً
     * من إدخال النص مباشرة داخل علامات اقتباس JavaScript.
     */
    fun buildReplaceScript(
        translations: List<TranslatedNode>
    ): String {

        if (translations.isEmpty()) {
            return "void(0);"
        }

        val script = StringBuilder()

        script.append(
            """
            (function() {
                try {
                    const translations = [
            """.trimIndent()
        )

        translations.forEachIndexed { index, node ->

            val idJson =
                JSONObject.quote(node.id)

            val translatedJson =
                JSONObject.quote(node.translatedText)

            script.append(
                """
                    {
                        id: $idJson,
                        text: $translatedJson
                    }
                """.trimIndent()
            )

            if (index < translations.lastIndex) {
                script.append(",")
            }

            script.append("\n")
        }

        script.append(
            """
                    ];

                    translations.forEach(function(item) {

                        try {

                            const selector =
                                '[data-ai-translate-id="' +
                                CSS.escape(item.id) +
                                '"]';

                            const element =
                                document.querySelector(selector);

                            if (!element) {
                                return;
                            }

                            element.innerText =
                                item.text;

                            element.setAttribute(
                                'data-ai-translated',
                                'true'
                            );

                            /*
                             * اتجاه النص المترجم.
                             *
                             * dir=auto أفضل من فرض RTL
                             * لأن النظام قد يترجم إلى لغة LTR.
                             */
                            element.setAttribute(
                                'dir',
                                'auto'
                            );

                            element.style.unicodeBidi =
                                'plaintext';

                        } catch (e) {}

                    });

                } catch (e) {}
            })();
            """.trimIndent()
        )

        return script.toString()
    }

    // =========================================================
    // استخراج النص المحدد
    // =========================================================

    /**
     * الحصول على النص الذي حدده المستخدم في WebView.
     */
    fun buildSelectionScript(): String {
        return """
            (function() {
                try {

                    const selection =
                        window.getSelection();

                    if (
                        selection &&
                        selection.rangeCount > 0
                    ) {

                        const text =
                            selection.toString()
                                .replace(/\u00A0/g, ' ')
                                .trim();

                        if (text.length > 0) {

                            return JSON.stringify({
                                text: text,
                                rangeCount: selection.rangeCount
                            });
                        }
                    }

                } catch (e) {}

                return JSON.stringify({
                    text: '',
                    rangeCount: 0
                });

            })();
        """.trimIndent()
    }

    // =========================================================
    // استبدال النص المحدد
    // =========================================================

    /**
     * يستبدل النص المحدد بالترجمة.
     *
     * لا نضع الترجمة مباشرة داخل JavaScript.
     * يتم تمريرها كقيمة JSON آمنة.
     */
    fun buildReplaceSelectionScript(
        translatedText: String
    ): String {

        val translatedJson =
            JSONObject.quote(translatedText)

        return """
            (function() {
                try {

                    const selection =
                        window.getSelection();

                    if (
                        !selection ||
                        selection.rangeCount === 0
                    ) {
                        return;
                    }

                    const translated =
                        $translatedJson;

                    const range =
                        selection.getRangeAt(0);

                    range.deleteContents();

                    const wrapper =
                        document.createElement('span');

                    wrapper.innerText =
                        translated;

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

                    wrapper.style.unicodeBidi =
                        'plaintext';

                    range.insertNode(wrapper);

                    selection.removeAllRanges();

                } catch (e) {}

            })();
        """.trimIndent()
    }

    // =========================================================
    // تحليل JSON المستخرج
    // =========================================================

    /**
     * يحلل نتيجة evaluateJavascript.
     *
     * WebView قد يعيد:
     *
     * 1. JSON array مباشرة.
     * 2. JSON string يحتوي على JSON array.
     *
     * لذلك نعالج الحالتين.
     */
    fun parseExtractedNodes(
        jsonString: String
    ): List<PageTextNode> {

        if (jsonString.isBlank() ||
            jsonString == "null"
        ) {
            return emptyList()
        }

        return try {

            val jsonValue =
                JSONTokener(jsonString).nextValue()

            val array =
                when (jsonValue) {

                    is JSONArray ->
                        jsonValue

                    is String ->
                        JSONArray(jsonValue)

                    else ->
                        return emptyList()
                }

            val nodes =
                ArrayList<PageTextNode>(
                    array.length()
                )

            for (i in 0 until array.length()) {

                val obj =
                    array.optJSONObject(i)
                        ?: continue

                val id =
                    obj.optString("id")
                        .trim()

                val text =
                    obj.optString("text")
                        .trim()

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

            /*
             * توافق إضافي مع بعض إصدارات WebView
             * التي قد تعيد JSON escaped.
             */
            parseEscapedJson(jsonString)
        }
    }

    // =========================================================
    // فك JSON escaped احتياطياً
    // =========================================================

    private fun parseEscapedJson(
        value: String
    ): List<PageTextNode> {

        return try {

            val decoded =
                JSONTokener(value).nextValue()
                    ?.toString()
                    ?: return emptyList()

            val array =
                JSONArray(decoded)

            val result =
                ArrayList<PageTextNode>(
                    array.length()
                )

            for (i in 0 until array.length()) {

                val obj =
                    array.optJSONObject(i)
                        ?: continue

                val id =
                    obj.optString("id")
                        .trim()

                val text =
                    obj.optString("text")
                        .trim()

                if (
                    id.isNotBlank() &&
                    text.isNotBlank()
                ) {

                    result.add(
                        PageTextNode(
                            id = id,
                            text = text
                        )
                    )
                }
            }

            result

        } catch (_: Exception) {
            emptyList()
        }
    }
}
