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
import blue.starry.penicillin.extensions.lang
import blue.starry.penicillin.extensions.models.text
import blue.starry.penicillin.extensions.via
import blue.starry.penicillin.models.Status
import java.text.Normalizer
import java.text.Normalizer.Form.NFKC

object CheckTweet: GLaDOSPlugin() {
    suspend fun shouldDisposeAsTweet(status: Status): Boolean {
        /*
            次のツイートは破棄:
                自分のツイート, @を含むツイート, ふぁぼ済み, 非公開アカウントのツイート, 非日本語圏のツイート
                アルファベットのみのツイート, BANクライアントからのツイート, BANユーザからのツイート, BANワードを含むツイート
         */
        return status.isMyTweet() || status.isReply() || status.isAlreadyFavorited() || status.isProtected() || status.isForeignLanguage() || status.isAlphabets() || status.isFromBannedClient() || status.isFromBannedUser() || status.containsBannedWords()
    }

    suspend fun shouldDisposeAsReply(status: Status): Boolean {
        /*
            次のリプライは破棄:
                リプライ情報がないツイート, 自分のツイート, ふぁぼ済み, 非日本語圏のツイート, アルファベットのみのツイート, BANクライアントからのツイート
                BANユーザからのツイート, BANワードを含むツイート
         */
        return status.hasNullInReplyToStatusId() || status.isMyTweet() || status.isAlreadyFavorited() || status.isForeignLanguage() || status.isAlphabets() || status.isFromBannedClient() || status.isFromBannedUser() || status.containsBannedWords()
    }

    suspend fun shouldDisposeAsMyTweet(status: Status): Boolean {
        /*
            次の自分のツイートは破棄:
                @を含むツイート, BANワードを含むツイート
         */
        return status.isReply() || status.containsBannedWords()
    }

    private fun Status.hasNullInReplyToStatusId(): Boolean {
        return inReplyToStatusId == null
    }

    private fun Status.isMyTweet(): Boolean {
        return user.screenName == "MiriaYannaiyo"
    }

    private fun Status.isReply(): Boolean {
        return "@" in text
    }

    private fun Status.isAlreadyFavorited(): Boolean {
        return favorited
    }

    private fun Status.isProtected(): Boolean {
        return user.protected
    }

    private fun Status.isForeignLanguage(): Boolean {
        return lang.value != "ja"
    }

    private val alphabetRegex = "^\\w+$".toRegex()
    private fun Status.isAlphabets(): Boolean {
        return alphabetRegex.matches(text)
    }

    private suspend fun Status.containsBannedWords(): Boolean {
        val bannedWord = BannedCollection.checkWordRules(Normalizer.normalize(text, NFKC)) ?: return false

        logger.trace { "https://twitter.com/${user.screenName}/status/$id を無視しました。\n理由: `${bannedWord.word}` (${bannedWord.category}) を含むツイートであるため。" }
        return true
    }

    private suspend fun Status.isFromBannedClient(): Boolean {
        val bannedClient = BannedCollection.checkClientRules(via.name) ?: return false

        logger.trace { "https://twitter.com/${user.screenName}/status/$id を無視しました。\n理由: `${bannedClient.name}` (${bannedClient.category}) からのツイートであるため。" }
        return true
    }

    private suspend fun Status.isFromBannedUser(): Boolean {
        val bannedUser = BannedCollection.checkUserRules(user.screenName) ?: return false

        logger.trace { "https://twitter.com/${user.screenName}/status/$id を無視しました。\n理由: @${bannedUser.screen_name} (${bannedUser.reason}) のツイートであるため。" }
        return true
    }
}
