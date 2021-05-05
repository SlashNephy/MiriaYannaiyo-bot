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
import blue.starry.glados.clients.web.*
import blue.starry.glados.clients.web.routing.meta.*
import blue.starry.glados.clients.web.routing.normal.*
import blue.starry.glados.plugins.extensions.web.*
import blue.starry.glados.plugins.extensions.web.addons.*
import blue.starry.glados.plugins.extensions.web.template.*
import kotlinx.html.*

object ProjectPage: GLaDOSPlugin(), WebEventModel {
    @WebRouting("/", "miria.ya.ru")
    override suspend fun onAccess(event: WebRoutingEvent) {
        event.respondLayout {
            meta {
                title = "みりあやんないよ bot"
                description = "Twitter bot 「みりあやんないよ bot (@MiriaYannaiyo)」についてまとめています。"
            }

            addon {
                install(GoogleAnalytics)
                install(Bootstrap4)
                install(Bootstrap4JSNative)
                install(FontAwesome)
                install(FontCSS)
            }

            contents {
                div(classes = "container") {
                    div(classes = "row") {
                        div(classes = "col-md-3") {
                            div(classes = "card") {
                                ul(classes = "list-group list-group-flush") {
                                    li(classes = "list-group-item") {
                                        a(href = "#welcome") {
                                            +"はじめに"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#features") {
                                            +"機能"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#result") {
                                            +"最近のツイート"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#api") {
                                            +"API"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#ask") {
                                            +"お問い合わせ"
                                        }
                                    }
                                }
                            }
                        }

                        div(classes = "col-md-9") {
                            div() {
attributes["id"] = "welcome"

                                h2 {
                                    i(classes = "fas fa-angle-double-right")
                                    +" はじめに"
                                }
                                p {
                                    +"このBotは, 15分に一度 フォローユーザーのツイートを拾って「みりあやんないよ」をツイートする Twitter Botです。"
                                    a(href = "https://twitter.com/MiriaYannaiyo", target = "_blank") {
                                        +"@MiriaYanniyo"
                                    }
                                }
                                p {
                                    +"©2003-2018 BNEI"
                                }

                                p {
                                    +"ソースコードは GitHub で管理しています。みりあやんないよbotは "
                                    a(href = "https://kotlinlang.org") {
                                        +"Kotlin"
                                    }
                                    +"製です。ことりんかわいい！みりあやんないよbotには MITライセンスが適用されます。"
                                }
                            }

                            hr()

                            div() {
attributes["id"] = "features"

                                h2 {
                                    i(classes = "fas fa-star")
                                    +" 機能"
                                }
                                ul {
                                    li {
                                        h3 {
                                            +"15分に一度 (毎時0分, 15分, 30分, 45分頃), "
                                            i {
                                                +"following"
                                            }
                                            +"のツイートから「みりあやんないよ」を投稿します"
                                        }
                                        p {
                                            +"TL のランダムなツイートを形態素解析して自然な日本語を切り取って, 「みりあ○○○やんないよ」の形にします。"
                                            +"みりあやんないよ bot は, 利用させていただいたツイートをお気に入り登録します。"
                                        }
                                        p {
                                            +"定期ツイートなどの自動ツイートの類いは除外しています。また 不適切ワードを DB 化し除外するようになっています。"
                                        }
                                        p {
                                            +"不適切な投稿を防止するため Yahoo! の感情解析の結果を利用しネガティブワードを投稿しにくくなりました。例えば 逝去や災害などの不幸なニュースには反応しにくくなっています。(2018/2/24)"
                                        }
                                        p {
                                            +"80% の確率で「みりあ○○やんないよ」選びますが, これだけではなくレアケースとして"
                                            p {
                                                b {
                                                    +"10% の確率で「みりあも○○やるー」"
                                                }
                                            }
                                            +"を さらに"
                                            b {
                                                +"5% の確率で「みりあも○○やーるー！」"
                                            }
                                            +"を そして残りの"
                                            b {
                                                +"5% の確率で「みりあも○○やーらない！」"
                                            }
                                            +"を選びます。"
                                        }
                                        br
                                        p {
                                            +"以下, 技術的備考です。"
                                        }
                                        p {
                                            +"形態素解析には, 2017/5/2 以前は MeCab を利用していましたが, より高い精度のため"
                                            a(href = "https://developer.yahoo.co.jp/webapi/jlp/", target = "_blank") {
                                                b {
                                                    +"Yahoo! のテキスト解析 API"
                                                }
                                            }
                                            +"を使用しています。"
                                        }
                                        p {
                                            +"MeCab 時代では, 単純に名詞のみを切り取っていましたが, 今では日本語が自然になるように品詞レベルで「みりあやんないよ」に接続するようなワードを連結するようにしています。"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"アイマス関連用語を重み付けし アイマス関連の話題に反応しやすくします"
                                        }
                                        p {
                                            a(href = "https://imas-db.jp/") {
                                                +"THE IDOLM@STER データベース"
                                            }
                                            +" 様が作成しているIME辞書データを組み込んでおり, アイマス関連用語を優先的に採用するようにしています。"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"リプライに反応します"
                                        }
                                        p {
                                            +"みりあやんないよ bot のツイートに対し「やって」「やんないで」などとリプライを飛ばすと反応します。"
                                        }
                                        p {
                                            +"スパムになってしまうのを防止するため, 1つのリプライに対し 5分間 のクールダウンを設けています。"
                                            +"多量のリプライを送るアカウントに対してはブラックリストに追加し, 反応しないように対処しますので予めご了承ください。(2018/11/14)"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"毎分フォロー返しを行います"
                                        }
                                        p {
                                            +"フォロー返しが行われていないときには「フォロバ」とリプライを送ってみてください。なお, フォロー規制に掛かっている場合がありますのでご了承ください。"
                                        }
                                    }
                                }
                            }

                            hr()

                            div() {
attributes["id"] = "result"

                                h2 {
                                    i(classes = "fas fa-list-ul")
                                    +" 最近の形態素解析 / 感情分析結果 (最新10件)"
                                }

                                div(classes = "card") {
                                    div(classes = "card-body", id = "miria-result")
                                }
                            }

                            hr()

                            div() {
attributes["id"] = "api"

                                h2 {
                                    i(classes = "fas fa-code")
                                    +" 公開API"
                                }
                                p {
                                    p { +"みりあやんないよ bot では 過去の形態素解析結果やツイート内容を取得できる JSON API を用意しています。" }
                                    p {
                                        +"詳しくは "
                                        a(href = "https://api.ya.ru") { +"API リファレンス" }
                                        +" をご覧ください。"
                                    }
                                }
                            }

                            hr()

                            div() {
attributes["id"] = "ask"

                                h2 {
                                    i(classes = "far fa-envelope")
                                    +" お問い合わせ"
                                }
                                p {
                                    code {
                                        +"akagi.miria※ya.ru"
                                    }
                                    +" までメールでお願いします。(※を@に置換してください)"
                                }
                            }
                        }
                    }
                }

                inlineStyle("/assets/miria.css")
                inlineScript("/assets/miria.js")
            }
        }
    }
}
