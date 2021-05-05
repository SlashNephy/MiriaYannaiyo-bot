package blue.starry.glados.plugins

import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import blue.starry.glados.*
import blue.starry.glados.api.*
import blue.starry.glados.plugins.WordRules.skipCheckFeelingRules
import blue.starry.glados.plugins.model.*
import blue.starry.glados.plugins.model.TweetResult.*
import blue.starry.glados.plugins.model.TweetResult.WordNode.*
import blue.starry.jsonkt.*
import java.util.concurrent.ConcurrentHashMap

object YahooFeelingAnalysis: GLaDOSPlugin() {
    private val jsonRegex = "YAHOO.JP.srch.rt.sentiment = (.+?)</script>".toRegex()
    
    suspend fun check(nodes: MutableMap<String, List<WordNode>>): List<WordNode> {
        val cache = ConcurrentHashMap<String, YahooFeelingScore>()
        val deleted = arrayListOf<WordNode>()

        nodes.values.flatten().filterNot { node ->
            // 感情解析をスキップ
            skipCheckFeelingRules.any { node.feature.startsWith(it) }
        }.forEach {
            it.feeling = cache.getOrPut(it.surface) {
                retrieve(it.surface) ?: return@forEach
            }

            if (it.feeling!!.positive < 10 && it.feeling!!.negative > 60) {
                ScheduledYannaiyo.logger.info { "ワード: ${it.surface} (${it.feature}) を取り除きました。(スコア: ${it.feeling})" }
                it.deleted = true

                nodes.remove(it.surface)
            }
        }

        return deleted
    }
    
    private suspend fun retrieve(word: String): YahooFeelingScore? {
        val result = try {
            GLaDOS.httpClient.get<String>("https://search.yahoo.co.jp/realtime/search?p=${word.encodeURLParameter()}")
        } catch (e: Throwable) {
            logger.error(e) { "データの取得中にエラーが発生しました。" }
            return null
        }
        
        val json = jsonRegex.find(result)?.groupValues?.get(1)?.parseOrNull<YahooFeelingModel>()
        if (json == null) {
            logger.warn { "データの解析中にエラーが発生しました。想定された正規表現がマッチしませんでした。" }
            return null
        }

        return YahooFeelingScore(json.active, json.scores.positive, json.scores.neutral, json.scores.negative)
    }
}
