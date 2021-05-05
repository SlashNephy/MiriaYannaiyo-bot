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
import blue.starry.glados.clients.discord.command.DiscordCommand
import blue.starry.glados.clients.discord.command.error.embedError
import blue.starry.glados.clients.discord.command.events.DiscordCommandEvent
import blue.starry.glados.clients.discord.command.events.argumentString
import blue.starry.glados.clients.discord.command.policy.PermissionPolicy
import blue.starry.glados.clients.discord.extensions.ColorPresets
import blue.starry.glados.clients.discord.extensions.await
import blue.starry.glados.clients.discord.extensions.messages.embed
import blue.starry.glados.clients.discord.extensions.messages.reply
import blue.starry.glados.clients.discord.extensions.result.embedResult
import blue.starry.glados.clients.discord.extensions.result.reject
import blue.starry.glados.plugins.MiriaUtils.description

object MiriaUtils: GLaDOSPlugin() {
    @DiscordCommand(description = "みりあやんないよbot の禁止ワードと照合します。", arguments = ["テキスト"], checkArgumentsSize = false)
    suspend fun checkWord(event: DiscordCommandEvent) {
        reject(event.arguments.isEmpty()) {
            event.embedError {
                "判定するテキストが空です。"
            }
        }

        val text = event.argumentString
        val banned = BannedCollection.checkWordRules(text)
        if (banned == null) {
            event.reply {
                embed {
                    title("チェック結果")
                    description { "`$text` は BANワードを含んでいません。" }
                    color(ColorPresets.Good)
                    timestamp()
                }
            }
        } else {
            event.reply {
                embed {
                    title("チェック結果")
                    description { "`$text` は BANワードを含んでいます。\n  ワード: ${banned.word}\n  カテゴリ: ${banned.category}" }
                    color(ColorPresets.Bad)
                    timestamp()
                }
            }
        }.await()
    }

    @DiscordCommand(description = "みりあやんないよbot の禁止ワードを追加します。", arguments = ["ワード", "カテゴリ"], permission = PermissionPolicy.MainGuildAdminOnly)
    suspend fun addBanWord(event: DiscordCommandEvent) {
        val (word, category) = event.arguments

        reject(!BannedCollection.banWord(word, category)) {
            event.embedError {
                "すでに `$word` は登録済です。"
            }
        }

        event.embedResult {
            "`$word` ($category) を追加しました。"
        }.await()
    }

    @DiscordCommand(description = "みりあやんないよbot の禁止クライアントを追加します。", arguments = ["クライアント名", "カテゴリ"], permission = PermissionPolicy.MainGuildAdminOnly)
    suspend fun addBanClient(event: DiscordCommandEvent) {
        val (client, category) = event.arguments

        reject(!BannedCollection.banClient(client, category)) {
            event.embedError {
                "すでに `$client` は登録済です。"
            }
        }

        event.embedResult {
            "`$client` ($category) を追加しました。"
        }.await()
    }

    @DiscordCommand(description = "みりあやんないよbot のBANユーザを追加します。", arguments = ["スクリーンネーム", "理由"], permission = PermissionPolicy.MainGuildAdminOnly)
    suspend fun addBanUser(event: DiscordCommandEvent) {
        val (screenName, reason) = event.arguments

        reject(!BannedCollection.banUser(screenName, reason)) {
            event.embedError {
                "すでに `@$screenName` は登録済です。"
            }
        }

        event.embedResult {
            "`@$screenName` ($reason) を追加しました。"
        }.await()
    }
}
