# mise — 共有 EC システム（catalog / cart / pricing / inventory / checkout / order）

**Status: R2**

純粋な `.cljc` の EC ライブラリ。runtime dep は `shitsuke`（design system）と
`chobo`（audit ledger / subscription / invoice）だけで、ネットワーク・I/O・
時計への依存を持たない。ホストが port を注入する。

## 成熟度の梯子（この repo における R-tier の意味）

| tier | 意味 | 現在地 |
|---|---|---|
| R0 | 契約のスケッチのみ | — |
| R1 | 純粋な契約 + テスト | — |
| **R2** | **ドメイン契約が一通り揃い、test / lint / CI が緑。git 座標で消費可能。ただし本番の消費者で実証されていない** | **← いまここ** |
| R3 | 実際の消費者が本番でこれを叩いている | 未達 |
| R4 | 本番負荷で実証され、運用記録がある | 未達 |
| R5 | API が安定し、非互換変更に廃止手順がある | 未達 |

**R3 に上がるための具体的な不足は 1 つ**: `checkout/IPaymentPort` に**実装が
mock しかない**（`MockPaymentPort` は常に authorize する）。決済を通す消費者が
現れるまで、このライブラリは「決済のあるEC」を実証していない。

> ⚠ murakumo.cloud の物販は mise を使っていない（ADR-2608040100）。Stripe Checkout は
> HTTP API + ホスト型 UI で、`IPaymentPort` の authorize/capture 抽象にも
> `kessai` の銀行 rail 抽象にも当てはまらないため。**mise に Stripe adapter を
> 足すなら、まず「Checkout 型（ホスト型リダイレクト）」を port の形として
> 表現できるかを決める** —— authorize/capture の 2 段は card-present 由来の
> モデルで、リダイレクト決済はその形をしていない。

## 触るときの注意（実測で分かっていること）

- **`pricing/multiply` の nil 許容は仕様**。`cart/net-total` が「割引なし」を
  `(multiply nil -1)` で表現しており、ここで例外を投げると割引のない
  カートが全部落ちる。一方 `pricing/line-total` は nil 単価を**拒否する** ——
  明細行に単価が無いのはデータ不整合で、¥0 として描画すると「正当な無料商品」と
  区別がつかないまま代金が動く。
- **`inventory/reserve` は在庫不足を拒否して nil を返す**（2026-08-04 に変更）。
  以前は 0 に clamp していて、在庫割れを黙って吸収していた。clamp が要る用途には
  `reserve-clamped` を明示的に呼ぶ。`reserve-many` は all-or-nothing。
- **`checkout/postal-required?`** は郵便番号制度を持たない国（HK / AE / 香港・
  UAE 等）を知っている。郵便番号を無条件必須にすると、それは厳しい検証ではなく
  「誰も書いていない国別ブロックリスト」になる。
- テストは意図を日本語のコメントで持っている。**古い挙動を守っているテストを
  見つけたら、意図ごと書き換えてよい**（実例: `reserve-many` の旧テストは
  oversell を仕様として固定していた）。

## 検証

```bash
clojure -M:test   # 74 tests / 210 assertions
clojure -M:lint   # errors 0（warning は既存分が残っている）
```
