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

import blue.starry.glados.GLaDOSPlugin
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

object MiriaTweetLimitManager: GLaDOSPlugin() {
    private const val normalTweetLimitPerHour = 5
    private const val replyTweetLimitPerHour = 100
    private const val hourInMillis = 60 * 60 * 1000

    private val normalTweetLogs = CopyOnWriteArraySet<Long>()
    private val replyTweetLogs = CopyOnWriteArraySet<Long>()

    fun checkNormalTweet() {
        val current = Date().time
        normalTweetLogs += current

        normalTweetLogs.filter { current - it > hourInMillis }.forEach {
            normalTweetLogs.remove(it)
        }

        if (normalTweetLogs.size > normalTweetLimitPerHour) {
            throw RateLimitExceeded()
        }

        logger.trace { "RateLimit: ${normalTweetLogs.size}/$normalTweetLimitPerHour" }
    }
    
    fun cancelLastNormalTweet() {
        normalTweetLogs.remove(normalTweetLogs.last())
    }

    fun checkReplyTweet() {
        val current = Date().time
        replyTweetLogs += current

        replyTweetLogs.filter { current - it > hourInMillis }.forEach {
            replyTweetLogs.remove(it)
        }

        if (replyTweetLogs.size > replyTweetLimitPerHour) {
            throw RateLimitExceeded()
        }

        logger.trace { "RateLimit: ${replyTweetLogs.size}/$replyTweetLimitPerHour" }
    }
    
    fun cancelLastReplyTweet() {
        replyTweetLogs.remove(replyTweetLogs.last())
    }

    class RateLimitExceeded: Exception("Miria tweet limit exceeded.")
}
