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
import blue.starry.glados.clients.logger.*
import blue.starry.glados.clients.loop.*
import blue.starry.glados.clients.twitter.config.*
import blue.starry.glados.plugins.extensions.mongodb.wrapper.*
import blue.starry.penicillin.endpoints.search
import blue.starry.penicillin.endpoints.search.SearchResultType
import blue.starry.penicillin.endpoints.search.universal
import blue.starry.penicillin.extensions.await
import blue.starry.penicillin.extensions.models.text
import blue.starry.penicillin.extensions.via
import blue.starry.penicillin.models.Status
import java.util.concurrent.TimeUnit

object SearchTweets: GLaDOSPlugin() {
    private val account = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo_Official")
    private val searches by GLaDOS.mongodb.penicillinModelCollection<Status>("EgoSearchMiria")

    private const val searchQuery = "\"みりあやんないよ\" OR miriayannaiyo OR \"やんないよbot\" -from:MiriaYannaiyo -to:MiriaYannaiyo -from:CocoaYannaiyo -from:MIRIaYaNNAIYo__ -to:MIRIaYaNNAIYo__"
    private val replyRegex = "@MiriaYannaiyo\\s".toRegex(RegexOption.IGNORE_CASE)

    @Loop(30, TimeUnit.SECONDS)
    suspend fun search(event: LoopEvent) {
        val searchResult = account.officialClient.use {
            it.search.universal(query = searchQuery, modules = "tweet", resultType = SearchResultType.Recent).await()
        }

        for (it in searchResult.result.statuses) {
            val tweet = it.data
            if (searches.contains("id" eq tweet.id)) {
                continue
            }
            if (tweet.text.contains(replyRegex)) {
                continue
            }

            if ("みりあやんないよ" in tweet.user.name || "やんないよbot" in tweet.user.name || BannedCollection.checkClientRules(tweet.via.name) != null) {
                continue
            }
            if (tweet.user.screenName == "sirotennikap") {
                continue
            }

            SlackWebhook.message("#search-miria") {
                username = "${tweet.user.name} @${tweet.user.screenName}"
                icon = tweet.user.profileImageUrlHttps
                textBuilder {
                    appendln(tweet.text)
                    appendln("<https://twitter.com/${tweet.user.screenName}/status/${tweet.id}|${tweet.via.name}>")
                }
            }

            searches.insert(tweet)
        }
    }
}
