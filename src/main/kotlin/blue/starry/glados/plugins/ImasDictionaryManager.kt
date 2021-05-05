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

import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import blue.starry.glados.api.GLaDOS
import blue.starry.glados.api.httpClient
import blue.starry.glados.plugins.model.TweetResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

object ImasDictionaryManager {
    private val mutex = Mutex()
    private val cache = mutableListOf<TweetResult.WordNode>()
    private var lastUpdated: Long? = null
    private const val updateThresholdMillis = 5 * 60 * 1000

    private suspend fun fetch(): List<TweetResult.WordNode> {
        val text = GLaDOS.httpClient.get<HttpStatement>("https://raw.githubusercontent.com/maruamyu/imas-ime-dic/master/dic.txt").execute {
            it.content.readRemaining().readText(charset = Charsets.UTF_16LE)
        }

        return text.lines().map { it.trim().split("\t") }.filter { it.size == 4 }.map {
            TweetResult.WordNode(it[1], it[0], "名詞,${it[2]},アイマス関連名詞", it[3])
        }
    }

    suspend fun get(): List<TweetResult.WordNode> {
        return mutex.withLock {
            if (lastUpdated == null || Date().time - lastUpdated!! > updateThresholdMillis) {
                cache.clear()
                cache += fetch()
                lastUpdated = Date().time
            }

            cache
        }
    }
}
