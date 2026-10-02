## 技術要件

### プログラミング言語

| 言語 | バージョン | 用途 |
|------|-----------|------|
| Java | 21 (LTS) | サーバサイド実装 |
| JavaScript | ES2020 (ES11) | クライアントサイド実装（VanillaJS） |
| HTML | HTML5 | Thymeleaf テンプレート |
| CSS | CSS3 | UIデザイン |
| SQL | ANSI SQL標準(SQL:2023一部) | MyBatis マッピング |

### フレームワーク・ライブラリ

| カテゴリ | 技術・ツール | バージョン |
|----------|-------------|-----------|
| フレームワーク | Spring Boot | 4.0.6 |
| Web MVC フレームワーク | Spring MVC | 6.x (Spring Boot 同梱) |
| テンプレートエンジン | Thymeleaf | 3.1.5.RELEASE |
| O/Rマッパー | MyBatis | 4.0.1 |
| ユーティリティ | Lombok | 1.18.46 |
| データベース | H2 Database | 2.4.240 |
| テストフレームワーク | JUnit5 | 6.0.3 |
| モックライブラリ | Mockito | 5.20.0 |
| ビルドツール | Maven | — |

### 文字コード

| 対象 | 文字コード |
|------|----------|
| ソースコード | UTF-8 (BOMなし) |
| HTMLテンプレート | UTF-8 (`<meta charset="UTF-8">`) |
| リソースファイル | UTF-8 |
| データベース | UTF-8 |
| HTTPレスポンス | UTF-8 (`Content-Type: text/html; charset=UTF-8`) |

---

## アーキテクチャ構成

レイヤードアーキテクチャを採用する。

| レイヤー | 主な責務 |
|----------|---------|
| View | Thymeleafによるサーバーサイドレンダリング、VanillaJS、CSS |
| Controller / Form | URLマッピング、リクエスト/レスポンス処理、バリデーション/メッセージ処理、セッション管理、Service層への処理移譲 |
| Service | ビジネスロジック、Repositoryを使ったデータ処理 |
| Repository | データベースアクセス処理 |

## MyBatis マッピングファイル構成

MyBatis の SQL マッピングファイル（XML）はsrc/main/resources/mapper/ に配置する

### メッセージ方式

バリデーションエラーメッセージや画面表示メッセージは、`src/main/resources/messages.properties` に定義する。  
Thymeleaf テンプレートからは `#{key}` 記法で参照し、Controller では `MessageSource` を通じて解決する。

### 例外処理

`@ControllerAdvice` を付与したグローバル例外ハンドラクラスを `controller` パッケージに配置し、アプリケーション全体の例外を一元管理する。  
業務例外はカスタム例外クラス（`exception` パッケージ）として定義し、ハンドラでキャッチしてエラー画面へ遷移させる。

### レイヤー間のデータ受け渡し

- `repository/entity/` 配下のエンティティクラスは、View（Thymeleafテンプレート）に直接渡してはならない。
- Service層は `service/model/` 配下のモデルクラスに変換してからControllerに返す。
- Controllerは、Serviceから受け取ったモデルクラスをメソッド引数の `Model` に追加し、テンプレート名を `String` で返してViewに渡す。`ModelAndView` は使用しない。

### インジェクション

依存クラスのインジェクションする方法は、`@Autowired`を使用する

---

## フロントエンドファイル構成

### CSS
- CSSは外部ファイルに定義する。
- `src/main/resources/static/css/` フォルダに配置する。

### JavaScript
- JavaScriptは外部ファイルに定義する。
- `src/main/resources/static/js/` フォルダに配置する。

---

## パッケージ構成

```
com.example.ec
├─ config/              # Spring Bootの設定クラス
├─ exception/           # カスタム例外クラス
├─ controller/          # URLマッピング・リクエスト処理
|   └─ form/            # フォームオブジェクト（入力値バインディング・バリデーション）
├─ service/             # ビジネスロジック (インタフェースクラスは不要)
|   └─ model/           # Service層のビジネスロジックで使用するモデルクラス
└─ repository/          # MyBatisによるDBアクセス（Mapperインターフェース）
    └─ entity/          # DBテーブルに対応するエンティティクラス
```

## 命名

| レイヤー | 名前 |
|----------|---------|
| Thymeleaf Template | *.html |
| Controller | *Controller |
| Form | *Form |
| Service | *Service |
| Model | *Model |
| Repository | *Repository |
| Mapper XML | *Mapper.xml |
| Entity | *Entity |

## コーディング規約

### コメント
- 生成する全てのクラスおよびメソッドにJavaDocコメントを記載する。
- 処理の内容が分かりにくい箇所には、適宜インラインコメントを追加する。
