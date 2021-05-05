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

import blue.starry.glados.api.GLaDOS
import blue.starry.glados.plugins.extensions.mongodb.wrapper.*

object BannedCollection {
    private val banWords by GLaDOS.mongodb.pojoCollection<BanWord>("MiriaYannaiyoBanWord")
    suspend fun checkWordRules(tweetText: String): BanWord? {
        return banWords.find().find { it.word in tweetText }
    }

    suspend fun banWord(word: String, category: String): Boolean {
        if (banWords.contains { BannedCollection.BanWord::word eq word }) {
            return false
        }

        banWords.insertOne(BanWord(word, category))
        return true
    }

    private val banUsers by GLaDOS.mongodb.pojoCollection<BanUser>("MiriaYannaiyoBanUser")
    suspend fun checkUserRules(screenName: String): BanUser? {
        return banUsers.findOne(BannedCollection.BanUser::screen_name eq screenName)
    }

    suspend fun banUser(screenName: String, reason: String): Boolean {
        if (banUsers.contains { BannedCollection.BanUser::screen_name eq screenName }) {
            return false
        }

        banUsers.insertOne(BanUser(screenName, reason))
        return true
    }

    private val banClients by GLaDOS.mongodb.pojoCollection<BanClient>("MiriaYannaiyoBanClient")
    suspend fun checkClientRules(name: String): BanClient? {
        return banClients.findOne(BannedCollection.BanClient::name eq name)
    }

    suspend fun banClient(name: String, category: String): Boolean {
        if (banClients.contains { BannedCollection.BanClient::name eq name }) {
            return false
        }

        banClients.insertOne(BanClient(name, category))
        return true
    }

    data class BanWord(val word: String, val category: String)

    data class BanUser(val screen_name: String, val reason: String)

    data class BanClient(val name: String, val category: String)
}
