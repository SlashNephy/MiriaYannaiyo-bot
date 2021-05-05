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
import blue.starry.glados.clients.chrono.*
import blue.starry.glados.clients.logger.*
import blue.starry.glados.clients.twitter.config.*
import blue.starry.glados.plugins.MiriaTweetLimitManager.RateLimitExceeded
import blue.starry.glados.plugins.TextParseUtils.filterWords
import blue.starry.glados.plugins.extensions.mongodb.wrapper.*
import blue.starry.glados.plugins.kusoripu.*
import blue.starry.glados.plugins.model.*
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.endpoints.favorites
import blue.starry.penicillin.endpoints.favorites.create
import blue.starry.penicillin.endpoints.statuses
import blue.starry.penicillin.endpoints.statuses.create
import blue.starry.penicillin.endpoints.statuses.show
import blue.starry.penicillin.endpoints.timeline
import blue.starry.penicillin.endpoints.timeline.homeTimeline
import blue.starry.penicillin.extensions.await
import blue.starry.penicillin.extensions.models.addToList
import blue.starry.penicillin.extensions.models.text
import blue.starry.penicillin.extensions.via
import blue.starry.penicillin.models.Status
import kotlinx.coroutines.*
import java.text.Normalizer
import java.util.*

object ScheduledYannaiyo: GLaDOSPlugin() {
    private val account = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo_Official")
    private val accountForPosting = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo")
    
    private const val maxRetries = 15

    @Schedule(minutes = [13, 28, 43, 58])
    suspend fun yannaiyo(event: ScheduleEvent) {
        val nextMinute = event.calendar.also {
            it.add(Calendar.MINUTE, 2)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }
        var sinceId: Long? = null

        account.officialClient.use { client ->
            for (i in 1 until maxRetries) {
                val timeline = try {
                    client.timeline.homeTimeline(count = 200, sinceId = sinceId).await()
                } catch (e: Throwable) {
                    logger.error(e) { "タイムライン取得中にエラーが発生しました。($i/$maxRetries)" }
                    continue
                }
                
                launch {
                    KusoripuCollector.register(timeline)
                }

                timeline.filterNot {
                    CheckTweet.shouldDisposeAsTweet(it)
                }.shuffled().forEach { status -> 
                    val result = try {
                        status.processTweet(nextMinute)
                    } catch (e: RateLimitExceeded) {
                        logger.warn { "通常ツイート数がリミットを超えています。" }
                        return
                    } catch (e: PenicillinTwitterApiException) {
                        when (e.error) {
                            TwitterApiError.DuplicateStatus -> {
                                logger.error { "ツイートが重複しました。" }
                                null
                            }
                            TwitterApiError.CannotPerformWriteActions -> {
                                logger.error { "書き込み操作が制限されています。" }
                                return
                            }
                            else -> {
                                logger.error(e) { "公式クライアントで定期ツイートに失敗しました。" }
                                null
                            }
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "ツイートの生成中にエラーが発生しました。" }
                        null
                    } ?: return@forEach

                    result.record()
                    return
                }

                sinceId = timeline.first().id
            }
        }
    }

    private val records by GLaDOS.mongodb.pojoCollection<TweetResult>("MiriaYannaiyo")
    private suspend fun TweetResult.record() {
        records.insertOne(this)

        SlackWebhook.message("#miriayannaiyo") {
            username = status.text
            icon = status.author.iconUrl
            
            textBuilder {
                appendln("オリジナルツイート: `${sourceStatus.text}` (${sourceStatus.via})")
                appendln("候補ワード: ${candidates.keys} -> ${pattern.text}")
                appendln("抽出ワード:\n```\n${candidates.map { node -> "${node.key} (${node.value.joinToString(" / ") { it.feature }})" }.joinToString("\n")}\n```")
                append("結合ノード:\n```\n${combinedNodes.joinToString("\n") { "${it.surface} (${it.feature})" }}\n```")
            }
        }
    }
    
    private suspend fun Status.processTweet(calendar: Calendar): TweetResult? {
        // 改行削除 -> URL削除 -> トリム -> 正規化
        val sentence = text.removeBreakLine().removeUrl().trim().normalize()

        val fullNodes = YahooTextParser.parse(sentence) ?: return null
        val (candidateNodes, combinedNodes) = filterWords(sentence, fullNodes)
        val deletedNodes = YahooFeelingAnalysis.check(candidateNodes)
        if (candidateNodes.isEmpty()) {
            return null
        }

        val pattern = TweetPattern.choose(candidateNodes.keys.maxBy { it.length }!!)

        if (GLaDOS.isDevelopmentMode) {
            return null
        }
        
        val shouldRetry = withTimeoutOrNull(calendar.timeInMillis - Date().time) {
            while (isActive) {
                try {
                    account.officialClient.use { client -> 
                        client.statuses.show(id = id).await()
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: PenicillinTwitterApiException) {
                    if (e.error == TwitterApiError.NoStatusFound) {
                        return@withTimeoutOrNull true
                    }
                }

                delay(5000)
            }

            null
        } ?: false
        
        if (shouldRetry) {
            return null
        }
        
        val status = accountForPosting.client.use { clientForPosting ->
            account.officialClient.use { client ->
                MiriaTweetLimitManager.checkNormalTweet()

                runCatching {
                    clientForPosting.statuses.create(pattern.text).await()
                }.recover {
                    logger.warn(it) { "非公式クライアントで定期ツイートに失敗しました。フォールバックします。" }

                    client.statuses.create(pattern.text).await()
                }.onSuccess {
                    runCatching {
                        client.favorites.create(id = id).await()
                    }.onFailure {
                        logger.warn(it) { "お気に入り登録に失敗しました。" }
                    }
                    
                    runCatching { 
                        user.addToList(1108269378791862272).await()
                    }.onFailure { 
                        logger.warn(it) { "リスト追加に失敗しました。" }
                    }
                }.onFailure {
                    MiriaTweetLimitManager.cancelLastNormalTweet()
                }.getOrThrow()
            }
        }

        return TweetResult(
            time = Date().time,

            status = TweetResult.Status(status.result.text, status.result.id, TweetResult.Status.Author(status.result.user.name, status.result.user.screenName, status.result.user.profileImageUrlHttps), status.result.via.name),
            sourceStatus = TweetResult.Status(text, id, TweetResult.Status.Author(user.name, user.screenName, user.profileImageUrlHttps), via.name),

            pattern = pattern,
            nodes = fullNodes,
            deletedNodes = deletedNodes,
            combinedNodes = combinedNodes,
            candidates = candidateNodes
        )
    }
    
    private fun String.removeBreakLine(): String {
        return replace("\n", " ")
    }

    private val urlRegex = "http(?:s)?://.+?(?:\\s|$)".toRegex()
    private fun String.removeUrl(): String {
        return replace(urlRegex, "")
    }
    
    private fun String.normalize(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFKC)
    }
}
