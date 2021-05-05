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

package blue.starry.glados.plugins.model

data class TweetResult(
    val time: Long,
    val pattern: TweetPattern,

    val status: Status,
    val sourceStatus: Status,

    val nodes: List<WordNode>,
    val deletedNodes: List<WordNode>,
    val combinedNodes: List<WordNode>,
    val candidates: Map<String, List<WordNode>>
) {
    data class Status(
        val text: String,
        val id: Long,
        val author: Author,
        val via: String
    ) {
        data class Author(val name: String, val screenName: String, val iconUrl: String)
    }

    data class WordNode(
        val surface: String,  // 単語
        val reading: String,  // 単語読み
        val feature: String,  // 品詞情報
        val description: String? = null,  // アイマス関連名詞の説明
        var feeling: YahooFeelingScore? = null,  // Yahoo 感情解析結果
        var deleted: Boolean = false
    ) {
        data class YahooFeelingScore(val active: String, val positive: Int, val neutral: Int, val negative: Int)
    }
}
