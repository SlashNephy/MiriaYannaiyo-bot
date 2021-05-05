package blue.starry.glados.plugins

import blue.starry.glados.plugins.TextParseUtils.WordRule

object WordRules {
    // 基本ルール
    val basicRule = WordRule("名詞")
    
    // 形態素解析後にやんないよツイートとして採用する連続する品詞
    val passRules = arrayOf(
        // 体言
        WordRule("特殊,記号,*,#,#,#", "名詞"),  // #xxx
        WordRule("名詞", "特殊,単漢", "名詞"),  // xxx～yyy
        WordRule("接頭辞", "名詞"), WordRule("名詞", "接尾辞"), WordRule("名詞", "助詞,格助詞,*,と,と,と", "名詞"),  // xxxとyyy
        WordRule("形容詞,形容", "接尾辞,接尾さ"),  // ~さ
        WordRule("連体詞,連体", "名詞"),  // あのxxx
        WordRule("動詞", "名詞"),  // ~するxxx
        WordRule("動詞", "助動詞,助動詞た", "名詞"),  // ~したxxx
        WordRule("動詞", "助動詞,助動詞ない", "名詞"),  // ~しないxxx
        WordRule("名詞", "助詞,助詞その他", "助動詞,助動詞だ,体言接続", "名詞"),  // ~みたいなxxx
        WordRule("名詞", "助動詞,助動詞だ,体言接続", "名詞"), // xxxなxxx
        WordRule("名詞", "接尾辞,接尾", "助動詞,助動詞だ,体言接続", "名詞"), // xxxなxxx
        WordRule("形容詞", "名詞"),  // ~なxxx
        WordRule("形容動詞,形動", "助動詞,助動詞だ,体言接続,な,な,だ", "名詞"),  // ~なxxx
        WordRule("名詞,数詞", "接尾辞,助数"),  // ~月
        
        // 助詞
        WordRule("名詞", "助詞,係助詞,*,しか"),  // xxxしか
        WordRule("名詞", "助詞,助詞連体化,*,の", "名詞"),  // xxxのyyy
        WordRule("名詞", "助詞,格助詞", "名詞,名サ自"),  // xxxをxxx
        WordRule("名詞", "助詞,並立助詞,*,とか", "名詞"),  // xxxとかyyy
        WordRule("動詞", "助詞,接続助詞,*,て,て,て"),  // ~して
        WordRule("名詞", "助詞,格助詞,*,から", "名詞"),  // xxxからyyy
        WordRule("名詞", "助詞,格助詞,*,で", "名詞"),  // xxxでyyy
        
        // 副詞
        WordRule("副詞,副詞,*", "名詞")  // 絶対xxx
    )
    
    // 単一品詞で構成される場合スキップする品詞
    val skipWhenSingleRules = arrayOf("名詞,数詞", "名詞,人姓")
    
    // Yahoo感情分析スキップ品詞
    val skipCheckFeelingRules = arrayOf("特殊", "助詞", "接尾辞", "接頭辞", "助動詞", "名詞,数詞")
}
