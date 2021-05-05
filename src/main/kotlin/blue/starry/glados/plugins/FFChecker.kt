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
import blue.starry.glados.clients.loop.*
import blue.starry.glados.clients.twitter.config.*
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.endpoints.followers
import blue.starry.penicillin.endpoints.followers.listIds
import blue.starry.penicillin.endpoints.followers.listUsers
import blue.starry.penicillin.endpoints.friends
import blue.starry.penicillin.endpoints.friends.listIds
import blue.starry.penicillin.endpoints.friends.listUsers
import blue.starry.penicillin.endpoints.friendships
import blue.starry.penicillin.endpoints.friendships.createByUserId
import blue.starry.penicillin.endpoints.friendships.destroyByUserId
import blue.starry.penicillin.extensions.await
import blue.starry.penicillin.extensions.cursor.allIds
import blue.starry.penicillin.extensions.cursor.untilLast
import java.util.concurrent.TimeUnit

object FFChecker: GLaDOSPlugin() {
    private val account = GLaDOS.config.twitter.twitterAccount("MiriaYannaiyo_Official")

    @Loop(1, TimeUnit.MINUTES)
    suspend fun checkShallowly(event: LoopEvent) {
        account.checkShallowly()
    }

    @Loop(3, TimeUnit.MINUTES)
    suspend fun checkDeeply(event: LoopEvent) {
        account.checkDeeply()
    }

    private suspend fun TwitterAccount.checkShallowly() {
        officialClient.use { client ->
            val follows = client.friends.listUsers(count = 200, skipStatus = true, includeUserEntities = false).await().result.users
            val followers = client.followers.listUsers(count = 200, skipStatus = true, includeUserEntities = false).await().result.users
            val (shouldUnfollow, shouldFollow) = follows.filter { it.followedBy == false } to followers.filter { !it.following && !it.followRequestSent }

            if (shouldUnfollow.size < 100) {
                for (target in shouldUnfollow) {
                    client.wrapUnfollow(target.id, user.screenName)
                }

                for (target in shouldFollow) {
                    if (!client.wrapFollow(target.id, user.screenName)) {
                        break
                    }
                }
            } else {
                logger.error { "アカウントロックと考えられるため, リムーブ/フォロー処理を中止します. (@${user.screenName})" }
            }
        }
    }

    private suspend fun TwitterAccount.checkDeeply() {
        officialClient.use { client ->
            val follows = client.friends.listIds(count = 5000).untilLast().allIds
            val followers = client.followers.listIds(count = 5000).untilLast().allIds
            val (shouldUnfollow, shouldFollow) = (follows - followers) to (followers - follows)

            if (shouldUnfollow.size < 100) {
                for (userId in shouldUnfollow) {
                    client.wrapUnfollow(userId, user.screenName)
                }

                for (userId in shouldFollow) {
                    if (!client.wrapFollow(userId, user.screenName)) {
                        break
                    }
                }
            } else {
                logger.error { "アカウントロックと考えられるため, リムーブ/フォロー処理を中止します. (@${user.screenName})" }
            }
        }
    }

    private suspend fun ApiClient.wrapFollow(id: Long, sn: String): Boolean {
        try {
            val user = friendships.createByUserId(userId = id).await()
            logger.info { "@${user.result.screenName} をフォローしました。(@$sn)" }
            return true
        } catch (e: PenicillinTwitterApiException) {
            return when (e.error) {
                TwitterApiError.UnableToFollowAtThisTime -> {
                    false
                }
                TwitterApiError.AlreadyRequestedToFollowUser, TwitterApiError.CannotFindSpecifiedUser, TwitterApiError.UserNotFound -> {
                    true
                }
                else -> {
                    logger.error(e) { "UserID: $id のフォローに失敗しました。(@$sn)" }
                    true
                }
            }
        }
    }

    private suspend fun ApiClient.wrapUnfollow(id: Long, sn: String) {
        try {
            val user = friendships.destroyByUserId(userId = id).await()
            logger.info { "@${user.result.screenName} のフォローを解除しました。(@$sn)" }
        } catch (e: PenicillinTwitterApiException) {
            when (e.error) {
                TwitterApiError.UserNotFound, TwitterApiError.ResourceNotFound -> {
                }
                else -> {
                    logger.error(e) { "UserID: $id のフォロー解除に失敗しました。(@$sn)" }
                }
            }
        }
    }
}
