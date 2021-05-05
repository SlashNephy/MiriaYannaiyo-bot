package blue.starry.glados.plugins

import blue.starry.glados.*
import blue.starry.glados.api.*
import blue.starry.glados.plugins.model.TweetResult.*
import blue.starry.jsonkt.delegation.*
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

object YahooTextParser: GLaDOSPlugin() {
    private val yahooApiKey by GLaDOS.secret.string("yahoo_api_key")
    
    fun parse(text: String): List<WordNode>? {
        val xml = retrieve(text) ?: return null

        return try {
            xml.getElementsByTagName("word").map {
                val word = it.childNodes
                WordNode(word.item(0).textContent, word.item(1).textContent, word.item(2).textContent)
            }
        } catch (e: Throwable) {
            logger.error(e) { "データの解析中にエラーが発生しました。XML の解析に失敗しました。" }
            return null
        }
    }
    
    private fun retrieve(text: String): Document? {
        val sentence = URLEncoder.encode(text, Charsets.UTF_8.name())
        val url = "https://jlp.yahooapis.jp/MAService/V1/parse?appid=$yahooApiKey&results=ma&response=surface,reading,feature&sentence=$sentence"
        
        return try { 
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url).also { 
                it.documentElement.normalize()
            }
        } catch (e: Throwable) {
            logger.error(e) { "データの取得中にエラーが発生しました。" }
            return null
        }
    }
    
    private fun <T> NodeList.map(block: (Node) -> T): List<T> {
        return (0 until length).map {
            block(item(it))
        }
    }
}
