# YTVP9 - YouTube VP9ダウンロード・再生アプリ

個人用Androidアプリ。YouTube動画をVP9/WebMでダウンロードし、オフライン再生する。

## 構成

- `app/src/main/java/com/ytvp9/app/`
  - `App.kt` - Applicationクラス、Room DB初期化
  - `Entities.kt` - Roomエンティティ (Video, Queue, BlockState)
  - `Database.kt` - DAOとデータベース定義
  - `MainActivity.kt` - ホーム／ライブラリ画面
  - `PlayerActivity.kt` - Media3/ExoPlayer再生画面
  - `DownloadService.kt` - ダウンロード実行（フォアグラウンドサービス、ブロック回避ロジック）
  - `MusePushService.kt` - FCM経由でMuseからのダウンロード指示を受信
- `app/src/main/AndroidManifest.xml` - 権限、Activity、Service、FCM設定

## 最上位原則

YouTubeからブロックされないことを最優先。速度・利便性と競合する場合はブロック回避を優先。

- 同時ダウンロード1本、動画間にクールダウン間隔（既定3分）
- 403/bot確認/PO Token要求を検知したら即時停止＋冷却期間（既定6時間）
- 冷却期間中は新規取得を開始しない

## Muse連携

FCM経由でダウンロード指示を受信:
```json
{
  "action": "download",
  "url": "https://www.youtube.com/watch?v=...",
  "title": "任意のメモ"
}
```

## ビルド

```bash
export ANDROID_HOME=~/android-sdk
./gradlew assembleDebug
```

## 未実装（TODO）

- youtubedl-android AARの組み込み（DownloadService.downloadOne）
- FFmpeg結合処理
- RecyclerViewアダプタ（ライブラリ一覧）
- 設定画面
- yt-dlp単独更新機構
- FCMトークンのMuse側への登録フロー

## GitHub Actionsでのビルド（推奨）

この環境でのローカルビルドが困難なため、GitHub Actionsでのクラウドビルドを推奨します：

1. このプロジェクトをGitHubリポジトリにプッシュ
2. GitHub上で Actions タブを開く
3. "Build APK" ワークフローを手動実行（またはmainブランチへのプッシュで自動実行）
4. ビルド完了後、Artifactsから `ytvp9-debug-apk` をダウンロード
5. APKをタブレットに転送してインストール

### 必要な準備

- GitHubアカウント
- Firebaseプロジェクト（FCM用）- `google-services.json` を `app/` に配置してからプッシュ

## Muse連携の仕組み

1. ユーザーがチャットでMuseに「この動画をダウンロードして」と指示
2. MuseがFCM経由でアプリにプッシュ通知を送信
3. `MusePushService` がプッシュを受信し、ダウンロードキューに追加
4. `DownloadService` がブロック回避ルールに従って順次ダウンロード

### FCMペイロード形式
```json
{
  "action": "download",
  "url": "https://www.youtube.com/watch?v=...",
  "title": "動画タイトル"
}
```
