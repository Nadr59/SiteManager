package com.nadr59.sitemanager.domain.translator

object TextChunker {

    fun split(text: String, maxLength: Int = 3000): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n\n")

        val currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            if (currentChunk.length + paragraph.length + 2 > maxLength) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }

                if (paragraph.length > maxLength) {
                    val sentences = paragraph.split(Regex("(?<=[.!?。！؟])\\s+"))
                    for (sentence in sentences) {
                        if (currentChunk.length + sentence.length + 1 > maxLength) {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString().trim())
                                currentChunk.clear()
                            }
                            if (sentence.length > maxLength) {
                                var i = 0
                                while (i < sentence.length) {
                                    val end = minOf(i + maxLength, sentence.length)
                                    chunks.add(sentence.substring(i, end).trim())
                                    i = end
                                }
                            } else {
                                currentChunk.append(sentence).append(" ")
                            }
                        } else {
                            currentChunk.append(sentence).append(" ")
                        }
                    }
                } else {
                    currentChunk.append(paragraph).append("\n\n")
                }
            } else {
                currentChunk.append(paragraph).append("\n\n")
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }
}


