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

package blue.starry.glados.plugins.web

import blue.starry.glados.*
import blue.starry.glados.api.*
import blue.starry.glados.clients.web.*
import blue.starry.glados.clients.web.routing.layout.*
import blue.starry.glados.clients.web.routing.normal.*
import blue.starry.glados.plugins.*
import blue.starry.glados.plugins.extensions.mongodb.wrapper.*

object API: GLaDOSPlugin(), WebEventModel {
    private val collection by GLaDOS.mongodb.jsonCollection("MiriaYannaiyo")

    @WebRouting("/v1/miria/query", "api.ya.ru")
    override suspend fun onAccess(event: WebRoutingEvent) {
        val text by event.query()
        val via by event.query()
        val page by event.intQuery { 0 }
        val count by event.intQuery { 25 }

        val filters = mutableListOf<FilterBson>()

        if (!text.isNullOrBlank()) {
            filters += regexOf("status.text", text!!, "im") or regexOf("originalStatus.text", text!!, "im")
        }
        if (!via.isNullOrBlank()) {
            filters += regexOf("via", via!!, "im")
        }

        val filter = if (filters.isNotEmpty()) {
            allOf(filters)
        } else {
            null
        }

        event.call.respondJsonArray {
            collection.find(filter) {
                sort(descendingOf("_id"))
                skip(page * count)
                limit(count)
            }
        }
    }
}
