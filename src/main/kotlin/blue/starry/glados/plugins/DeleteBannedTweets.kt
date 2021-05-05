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
import blue.starry.glados.api.GLaDOS
import blue.starry.glados.api.config
import blue.starry.glados.clients.loop.*
import blue.starry.glados.clients.twitter.config.officialClient
import blue.starry.glados.clients.twitter.config.twitter
import blue.starry.glados.clients.twitter.config.twitterAccount
import blue.starry.glados.plugins.extensions.mongodb.wrapper.delete
import blue.starry.glados.plugins.extensions.mongodb.wrapper.eq
import blue.starry.glados.plugins.model.TweetResult
import blue.starry.penicillin.core.exceptions.PenicillinException
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.endpoints.statuses
import blue.starry.penicillin.endpoints.statuses.delete
import blue.starry.penicillin.endpoints.timeline
import blue.starry.penicillin.endpoints.timeline.userTimeline
import blue.starry.penicillin.extensions.await
import blue.starry.penicillin.extensions.models.text
import java.util.concurrent.TimeUnit

object DeleteBannedTweets: GLaDOSPlugin() {
    private val account = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo_Official")
    private val results by GLaDOS.mongodb.pojoCollection<TweetResult>("MiriaYannaiyo")

    @Loop(5, TimeUnit.MINUTES)
    suspend fun check(event: LoopEvent) {
        var maxId: Long? = null

        account.officialClient.use { client ->
            repeat(10) { i ->
                val timeline = client.timeline.userTimeline(count = 200, maxId = maxId).await()
                for (status in timeline) {
                    val banned = BannedCollection.checkWordRules(status.text.split("みりあ").drop(1).joinToString("みりあ")) ?: continue

                    try {
                        client.statuses.delete(status.id).await()
                        logger.info { "ツイート: `${status.text}` を削除しました。(${i + 1}/10)\n理由: `${banned.word}` (${banned.category}) を含むため。" }

                        if (status.user.id == account.user.id) {
                            results.delete("status.text" eq status.text)
                        }
                    } catch (e: PenicillinTwitterApiException) {
                        if (e.error != TwitterApiError.NoStatusFound) {
                            logger.error(e) { "ツイートの削除に失敗しました: `${status.text}`" }
                        }
                    }
                }

                maxId = timeline.lastOrNull()?.id ?: return@repeat
            }
        }
    }
}
