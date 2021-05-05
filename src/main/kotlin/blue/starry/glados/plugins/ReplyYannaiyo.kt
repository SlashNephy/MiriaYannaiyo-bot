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

import blue.starry.glados.*
import blue.starry.glados.api.*
import blue.starry.glados.api.annotations.*
import blue.starry.glados.clients.loop.*
import blue.starry.glados.clients.twitter.config.*
import blue.starry.glados.plugins.MiriaTweetLimitManager.RateLimitExceeded
import blue.starry.glados.plugins.extensions.mongodb.store.*
import blue.starry.glados.plugins.extensions.mongodb.wrapper.contains
import blue.starry.glados.plugins.extensions.mongodb.wrapper.eq
import blue.starry.glados.plugins.extensions.mongodb.wrapper.insertOne
import blue.starry.glados.plugins.model.*
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.core.streaming.listener.FilterStreamListener
import blue.starry.penicillin.endpoints.friendships
import blue.starry.penicillin.endpoints.friendships.createByUserId
import blue.starry.penicillin.endpoints.statuses
import blue.starry.penicillin.endpoints.statuses.create
import blue.starry.penicillin.endpoints.statuses.delete
import blue.starry.penicillin.endpoints.statuses.show
import blue.starry.penicillin.endpoints.stream
import blue.starry.penicillin.endpoints.stream.filter
import blue.starry.penicillin.endpoints.timeline
import blue.starry.penicillin.endpoints.timeline.mentionsTimeline
import blue.starry.penicillin.extensions.await
import blue.starry.penicillin.extensions.listen
import blue.starry.penicillin.extensions.models.text
import blue.starry.penicillin.models.Status
import java.util.*
import java.util.concurrent.TimeUnit.*
import kotlin.math.roundToInt

object ReplyYannaiyo: GLaDOSPlugin() {
    private val account = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo_Official")
    private val accountForPosting = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo")

    private var lastMiriaReplyId by MongoDynamicConfigStore.Long
    private val replyHistories by GLaDOS.mongodb.collection("MiriaYannaiyoReplyHistory")

    @Loop(1, MINUTES)
    @DisabledFeature
    suspend fun stream(event: LoopEvent) {
        account.client.use { client ->
            client.stream.filter(track = listOf("MiriaYannaiyo")).listen(object: FilterStreamListener {
                override suspend fun onStatus(status: Status) {
                    if (status.inReplyToUserId == account.user.id) {
                        if (status.isDone()) {
                            return
                        }
                        
                        // 不適切なリプライを破棄
                        if (CheckTweet.shouldDisposeAsReply(status)) {
                            return
                        }
                        
                        // クールダウンを加味
                        if (!ReplyYannaiyo.Cooldowns.isAllowed(status)) {
                            return
                        }

                        status.respondYatteReply()
                    }
                }
            }).await(reconnect = false)
        }
    }
    
    @Loop(12, SECONDS)
    suspend fun reply(event: LoopEvent) {
        account.officialClient.use { client ->
            val mentions = client.timeline.mentionsTimeline(count = 50, sinceId = lastMiriaReplyId).await()
            if (mentions.isEmpty()) {
                return
            }

            for (status in mentions) {
                // 既にリプライの処理が完了
                if (status.isDone()) {
                    break
                }

                // 不適切なリプライを破棄
                if (CheckTweet.shouldDisposeAsReply(status)) {
                    continue
                }

                // クールダウンを加味
                if (!ReplyYannaiyo.Cooldowns.isAllowed(status)) {
                    continue
                }

                if (status.respondFollowBackReply()) {
                    continue
                } else {
                    status.respondYatteReply()
                }
            }

            lastMiriaReplyId = mentions.first().id
        }
    }

    private suspend fun Status.isDone(): Boolean {
        if (replyHistories.contains("id" eq id)) {
            return true
        }

        replyHistories.insertOne("id" to id, "user_id" to user.id, "text" to text)
        return false
    }

    private val followBackWordsInReply = arrayOf("フォロバ", "フォロー", "ふぉろば", "ふぉろー", "follow")
    private suspend fun Status.respondFollowBackReply(): Boolean {
        if (followBackWordsInReply.any { it in text }) {
            if (!user.following) {
                account.officialClient.use { client ->
                    runCatching {
                        client.friendships.createByUserId(userId = user.id).await()
                    }.onFailure {
                        logger.error(it) { "フォロー失敗: @${user.screenName}" }
                    }
                }
            }
            return true
        }

        return false
    }

    private val replyWords = arrayOf("やって", "やろう", "やんなよ", "やめろ", "やんないで", "やれ", "やめて", "やる気", "やりな", "せえや", "しろ", "して", "しよう", "せよ", "やるな")
    private val yannaiyoRegex1 = "^みりあ(.+?)やんないよ$".toRegex()
    private val yannaiyoRegex2 = "^みりあも(.+?)(やるー|やーるー！|やーらない！)$".toRegex()
    private suspend fun Status.respondYatteReply() {
        if (replyWords.any { it in text }) {
            accountForPosting.client.use { clientForPosting ->
                account.officialClient.use { client ->
                    val targetStatus = client.statuses.show(id = inReplyToStatusId!!).await()
                    // @MiriaYannaiyoのツイート
                    if (targetStatus.result.user.id == account.user.id) {
                        if (CheckTweet.shouldDisposeAsMyTweet(targetStatus.result)) {
                            return
                        }

                        val previousWord = yannaiyoRegex1.matchEntire(targetStatus.result.text)?.groupValues?.get(1) ?: yannaiyoRegex2.matchEntire(targetStatus.result.text)?.groupValues?.get(1) ?: return

                        try {
                            MiriaTweetLimitManager.checkReplyTweet()
                        } catch (e: RateLimitExceeded) {
                            logger.warn { "リプライツイート数がリミットを超えています。" }
                            return
                        }

                        val text = TweetPattern.choose(previousWord).text

                        runCatching {
                            clientForPosting.statuses.create("@${user.screenName} $text", inReplyToStatusId = id).await()
                        }.recover {
                            logger.warn(it) { "非公式クライアントでリプライに失敗しました。フォールバックします。" }

                            client.statuses.create(text, inReplyToStatusId = id).await()
                        }.onSuccess {
                            if (!it.result.text.startsWith("@")) {
                                client.statuses.delete(it.result.id).await()
                            }

                            logger.info { "@${user.screenName} へのリプライ: $text" }
                        }.onFailure { e ->
                            if (e is PenicillinTwitterApiException) {
                                if (e.error == TwitterApiError.DuplicateStatus) {
                                    logger.error { "リプライが重複しました: $text" }
                                }
                            } else {
                                logger.error(e) { "リプライ中にエラーが発生しました." }
                            }
                        }
                    }
                }
            }
        }
    }

    private object Cooldowns {
        private val data by MongoConfigMap<Long, Long?>("MiriaYannaiyoReplyCooldown") { null }
        private const val thresholdSeconds = 300

        private fun update(id: Long) {
            data[id] = Date().time
        }

        private fun remainingSeconds(id: Long): Int {
            val previous = data[id] ?: return 0
            return (thresholdSeconds - (Date().time - previous) / 1000.0).roundToInt()
        }

        fun isAllowed(status: Status): Boolean {
            val remaining = remainingSeconds(status.user.id)
            if (remaining > 0) {
                logger.debug { "クールダウン発動: @${status.user.screenName} 残り${remaining}秒" }
                return false
            }

            update(status.user.id)
            return true
        }
    }
}
