# 公開手順（PaaS）

Docker に対応した PaaS（例: Render、Railway、Fly.io）で動かす手順です。
画面の名称や料金は各社で変わるため、契約時に公式の案内で確認してください。

## 用意するもの
- アプリ本体（このリポジトリ。Dockerfile でビルドされる）
- PostgreSQL（PaaS の管理DBサービス）

## 手順
1. GitHub などにリポジトリを置き、PaaS に接続する（種別は Docker / Dockerfile）。
2. PaaS で PostgreSQL を作成する。リージョンは日本に近い所（例: シンガポール）を選ぶ。
3. Web サービスに次の環境変数を設定する。

| 環境変数 | 内容 |
|---|---|
| `DB_URL` | `jdbc:postgresql://ホスト:5432/DB名`（PaaS の接続情報から組み立てる。`postgres://` ではなく `jdbc:postgresql://` で始める） |
| `DB_USERNAME` | DBのユーザー名 |
| `DB_PASSWORD` | DBのパスワード |
| `APP_INITIAL_MANAGER_USER_NUMBER` | 初期管理者のユーザー番号（半角数字8桁） |
| `APP_INITIAL_MANAGER_PASSWORD` | 初期管理者のパスワード（半角英数字8文字以上。推測されにくい長いもの） |

`DB_*` が未設定だとアプリは起動しません（意図した動作）。

4. デプロイして、PaaS が発行する `https://〜` のURLを開く。
5. `https://〜/manager/login` から初期管理者でログインし、アルバイトのアカウントを登録する。
6. 利用者には `https://〜/user/login` を案内する。このURLはブックマークされるので、後から変えない。
7. 初期管理者を作成できたら、`APP_INITIAL_MANAGER_*` の環境変数は削除してよい。

## 公開後に必ずやること
- DBの自動バックアップを有効にする（PaaS の有料プランに含まれることが多い。契約時に確認）。
- 管理者のパスワードを定期的に変更する。
- 独自ドメインを使う場合は、その名前をブックマークに案内する。

## 現時点の注意（調査で指摘済みの未対応事項）
- ログインの試行回数制限がない。
- 管理者画面（/manager）を営業所のIPに絞る設定がない。
- スキーマ管理が `ddl-auto=update`（Flyway未導入）。
- 氏名・シフトを扱うため、公開前に会社の了承を得る。
