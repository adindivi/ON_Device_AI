package com.example.backend

import java.io.File
import java.io.InputStream

data class TokenizerResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray
)

class BertTokenizer(
    private val vocabMap: Map<String, Int> = emptyMap(),
    private val maxSeqLength: Int = 128
) {
    val clsId: Long = (vocabMap["<s>"] ?: vocabMap["[CLS]"] ?: CLS_ID).toLong()
    val sepId: Long = (vocabMap["</s>"] ?: vocabMap["[SEP]"] ?: SEP_ID).toLong()
    val unkId: Long = (vocabMap["<unk>"] ?: vocabMap["[UNK]"] ?: UNK_ID).toLong()
    val padId: Long = (vocabMap["<pad>"] ?: vocabMap["[PAD]"] ?: PAD_ID).toLong()

    companion object {
        const val PAD_TOKEN = "[PAD]"
        const val UNK_TOKEN = "[UNK]"
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"

        const val PAD_ID = 0L
        const val UNK_ID = 100L
        const val CLS_ID = 101L
        const val SEP_ID = 102L

        fun loadFromStream(inputStream: InputStream, maxSeqLength: Int = 128): BertTokenizer {
            val vocab = mutableMapOf<String, Int>()
            inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        vocab[token] = index
                    }
                }
            }
            return BertTokenizer(vocab, maxSeqLength)
        }

        fun loadFromFile(file: File, maxSeqLength: Int = 128): BertTokenizer {
            if (!file.exists()) return BertTokenizer(emptyMap(), maxSeqLength)
            return loadFromStream(file.inputStream(), maxSeqLength)
        }
    }

    fun encode(text: String, isQuery: Boolean = false): TokenizerResult {
        val formattedText = text
        val tokens = tokenize(formattedText)
        val tokenIds = mutableListOf<Long>()

        tokenIds.add(clsId)
        for (token in tokens) {
            if (tokenIds.size >= maxSeqLength - 1) break
            val id = vocabMap[token]?.toLong()
                ?: if (isHangul(token)) {
                    // Map unknown Hangul syllables/characters to dynamic non-UNK token IDs in valid range (1..98) to prevent [UNK] flattening
                    (1 + (token.hashCode() and 0x7fffffff) % 98).toLong()
                } else {
                    unkId
                }
            tokenIds.add(id)
        }
        tokenIds.add(sepId)

        val length = tokenIds.size
        val inputIds = LongArray(maxSeqLength) { padId }
        val attentionMask = LongArray(maxSeqLength) { 0L }
        val tokenTypeIds = LongArray(maxSeqLength) { 0L }

        for (i in 0 until length) {
            inputIds[i] = tokenIds[i]
            attentionMask[i] = 1L
            tokenTypeIds[i] = 0L
        }

        return TokenizerResult(inputIds, attentionMask, tokenTypeIds)
    }

    private fun isHangul(text: String): Boolean {
        if (text.isEmpty()) return false
        val c = text[0].code
        return (c in 0xAC00..0xD7A3) || (c in 0x1100..0x11FF) || (c in 0x3130..0x318F)
    }

    private fun tokenize(text: String): List<String> {
        val cleanText = text.lowercase().trim()
        val words = cleanText.split(Regex("\\s+"))
        val resultTokens = mutableListOf<String>()

        for (word in words) {
            if (word.isBlank()) continue
            if (vocabMap.isEmpty()) {
                resultTokens.add(word)
                continue
            }

            var isBad = false
            var start = 0
            val subTokens = mutableListOf<String>()

            while (start < word.length) {
                var end = word.length
                var curSubstr = ""
                while (start < end) {
                    var substr = word.substring(start, end)
                    val prefixSubstr = if (start > 0) "##$substr" else substr
                    val spmSubstr = if (start == 0) "\u2581$substr" else substr
                    val spmSubstr2 = if (start == 0) " $substr" else substr

                    when {
                        vocabMap.containsKey(substr) -> curSubstr = substr
                        vocabMap.containsKey(spmSubstr) -> curSubstr = spmSubstr
                        vocabMap.containsKey(spmSubstr2) -> curSubstr = spmSubstr2
                        vocabMap.containsKey(prefixSubstr) -> curSubstr = prefixSubstr
                    }

                    if (curSubstr.isNotEmpty()) break
                    end--
                }

                if (curSubstr.isEmpty()) {
                    isBad = true
                    break
                }
                subTokens.add(curSubstr)
                start = end
            }

            if (isBad) {
                // For Korean / CJK words that fail subword matching, perform syllable-level breakdown instead of replacing whole word with [UNK]
                if (isHangul(word)) {
                    for (char in word) {
                        resultTokens.add(char.toString())
                    }
                } else {
                    resultTokens.add(UNK_TOKEN)
                }
            } else {
                resultTokens.addAll(subTokens)
            }
        }

        return resultTokens
    }
}
