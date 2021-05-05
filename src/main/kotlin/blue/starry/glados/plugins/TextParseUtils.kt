/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nep
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.starry.glados.plugins

import blue.starry.glados.plugins.WordRules.basicRule
import blue.starry.glados.plugins.WordRules.passRules
import blue.starry.glados.plugins.WordRules.skipWhenSingleRules
import blue.starry.glados.plugins.model.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object TextParseUtils {
    suspend fun filterWords(text: String, nodes: List<TweetResult.WordNode>): Pair<ConcurrentHashMap<String, List<TweetResult.WordNode>>, List<TweetResult.WordNode>> {
        val moddedNodes = nodes.toCollection(CopyOnWriteArrayList())
        repeat(5) {
            passRules.forEach { rule -> rule.join(moddedNodes) }  // 5回フィルタを実行
        }

        val imasNodes = ImasDictionaryManager.get().asSequence().filter { it.surface in text }.map { listOf(it) }.toList()

        return (basicRule.finalize(moddedNodes) + imasNodes).asSequence().map { filteredNodes ->
            val word = filteredNodes.joinToString("") { it.surface }

            // ハッシュタグが動作するように空白を両端につける
            if (filteredNodes.first().surface.startsWith("#")) {
                " $word "
            } else {
                word.trim()
            } to filteredNodes
        }.filterNot { (_, value) ->
            // 単一品詞のみで構成されるエントリーを削除
            skipWhenSingleRules.any { rule -> value.all { it.feature.startsWith(rule) } }
        }.filterNot {
            // 1文字のエントリーを削除
            it.first.length == 1
        }.filterNot { (_, value) ->
            // 同じ単語が3回以上連続するエントリーを削除
            value.any { (surface) -> value.count { it.surface == surface } >= 3 }
        }.toList().toMap(ConcurrentHashMap()) to moddedNodes
    }

    class WordRule(private vararg val wordClasses: String) {
        private val first = wordClasses.first()
        private val lastRuleIndex = wordClasses.size - 1

        private var matching = false
        private var ruleIndex = 0
        private val currentCandidate = arrayListOf<TweetResult.WordNode>()

        private fun reset() {
            matching = false
            ruleIndex = 0
            currentCandidate.clear()
        }

        fun join(nodes: CopyOnWriteArrayList<TweetResult.WordNode>) {
            reset()

            nodes.toList().forEach { node ->
                if (lastRuleIndex < ruleIndex) {  // ルールのサイズを超過
                    reset()
                } else if (!matching && node.feature.startsWith(first)) {  // マッチ開始
                    matching = true
                    ruleIndex++
                    currentCandidate.add(node)
                } else if (matching && node.feature.startsWith(wordClasses[ruleIndex])) {  // ruleIndex番目のルールをチェック
                    currentCandidate.add(node)
                    if (ruleIndex == lastRuleIndex) {  // ルールの最後
                        val startIndex = nodes.indexOf(currentCandidate.first())
                        if (startIndex != -1) {
                            nodes[startIndex] = TweetResult.WordNode(currentCandidate.joinToString("") { it.surface }, currentCandidate.joinToString("") { it.reading }, "名詞(結合[${currentCandidate.joinToString(" / ") { it.feature }}])")
                            nodes.removeAll(currentCandidate)
                        }

                        reset()
                    } else {
                        ruleIndex++
                    }
                } else {
                    reset()
                }
            }

            // 固める
            if (this != basicRule) {
                basicRule.join(nodes)
            }
        }

        fun finalize(nodes: MutableList<TweetResult.WordNode>): List<List<TweetResult.WordNode>> {
            val found = arrayListOf<List<TweetResult.WordNode>>()
            reset()

            fun append() {
                if (currentCandidate.isNotEmpty()) {
                    val startIndex = nodes.indexOf(currentCandidate.first())
                    if (startIndex != -1) {
                        nodes[startIndex] = TweetResult.WordNode(currentCandidate.joinToString("") { it.surface }, currentCandidate.joinToString("") { it.reading }, "名詞(結合[${currentCandidate.joinToString(" / ") { it.feature }}])")
                        nodes.removeAll(currentCandidate)
                        found.add(currentCandidate.toList())
                    }
                }
            }

            for (it in nodes) {
                if (it.feature.startsWith(first)) {
                    currentCandidate.add(it)
                } else {
                    append()
                    reset()
                }
            }
            append()

            return found.toSet().toList()
        }
    }
}
