# MiriaYannaiyo-bot

[Archived] みりあやんないよ bot のソースコード

## プラグイン

- BannedCollection.kt  
NG ワードや, 無視するクライアントなどを管理します。

- CheckTweet.kt  
条件を満たすツイートを判定します。

- DeleteBannedTweets.kt  
一定時間おきに NG ワードを含むツイートを検索し, 削除します。

- FFChecker.kt  
フォロー返しを行います。

- ImasDictionaryManager.kt  
アイマス DB の IME データのキャッシュを保持します。

- MiriaTweetLimitManager.kt  
ツイート数のリミットを管理します。

- MiriaUtils.kt  
Discord のコマンドインターフェイスです。`!checkWord <text>` で NG ワードを含むかどうか検査します。

- ReplyYannaiyo.kt  
`@MiriaYannaiyo` 宛へのリプライを処理します。

- ScheduledYannaiyo.kt  
15 分おきに「やんないよ」等をツイートします。

- SearchTweets.kt  
みりあやんないよ bot 関連のツイートを検索します。

- TextParseUtils.kt  
形態素解析関連のユーティリティです。

- `web`  
みりあやんないよ bot のプロジェクトページや, API などを処理します。

## NG フィルター

- `BanClient.json`  
NG クライアント

- `BanUser.json`  
NG ユーザ

- `BanWord.json`  
NG ワード
