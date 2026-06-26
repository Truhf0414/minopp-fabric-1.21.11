# Phase 2b — client mixin runtime 對齊 + E3 視覺驗收

> Phase 2「編譯對齊」已綠（35→0 + build SUCCESSFUL），但收尾 E3 runClient 啟動即 crash（MouseHandlerMixin @Local mojmap 失配）→ 視覺驗收 BLOCKED。學長拍板「記錄後 handoff」，本 plan = 該新工作單元。**做完 = Phase 2 真收尾**，再進 Phase 3。
>
> **✅ 完成（2026-06-25）**：S1（`@Local(index=15)`）→ S2（compileJava 綠）→ S3（runClient 完整一局 UNO、滾輪翻牌正常、4 mixin 全 apply）→ S4（E3 視覺驗細零異常）→ E4（收尾五帳）。**範圍外附帶**（學長拍板修）：runtime 逮到 2 個 upstream 原生完整性洞——coupon item definition（紫黑）+ mino_table recipe ingredient 格式，皆已修並 runClient 驗（全 log 0 ERROR）。詳 changelog/api-changes/learnings/pitfall-log/STATUS phase-2b 段。

## 範圍 / Goal
讓 runClient 起得來（修 MouseHandlerMixin 的 `@Local` mojmap 失配）→ 確認其餘 mixin runtime 健康 → E3 視覺驗收（兩車道、對照 upstream）→ **Phase 2 真收尾**。
**真實工作量**：1 個 mixin 改一個 annotation 參數 + runClient 行為 smoke + E3 視覺人眼對照。非大改。

## Context（已驗 ground truth，全 javap mojmap jar `...layered+hash.2198-v2` + LVT 實證）

### 4 個 mixin 盤點 + runtime 風險分級
| Mixin | 注入 | 1.21.11 狀態 | 處置 |
|---|---|---|---|
| `mixin/MouseHandlerMixin` | `@WrapOperation(onScroll)` + `@Local(name="wheel")` | ❌ crash：by-name 抓不到 local | **本 plan 真修** |
| `mixin/InventoryMixin` | `@Inject(swapPaint)` 整段在 `//? if <26.1` 註解內 | ✅ 攤平 base=B 後是死碼、runtime 0 注入 | 不動 |
| `mixin/KeyMappingAccessor` | `@Accessor getKey()` | ✅ javap 證 `KeyMapping.key`(protected `InputConstants$Key`) 存在；vanilla 無 public getter 故 accessor 必要 | 不動，runClient 蓋章 |
| `fabric/mixin/AbstractClientPlayerMixin` | `@ModifyReturnValue(getFieldOfViewModifier)` | ✅ javap 證 `getFieldOfViewModifier(boolean,float)→float` 單一 match；`@ModifyReturnValue` 只改回傳值、不綁參數 | 不動，runClient 蓋章 |

> upstream 設計：滾輪攔截在 `<26.1` 用 `InventoryMixin.swapPaint`、`>=26.1` 改用 `MouseHandlerMixin`。攤平 base=B（`>=26.1`）後 InventoryMixin 自動空殼，活兒全在 MouseHandlerMixin。

### MouseHandler.onScroll(JDD)V — LVT 鐵證（javap -l mojmap jar）
LocalVariableTable（節錄關鍵）：
```
Slot  Name   Signature   語意
   0   this   MouseHandler
   1   l      J           param window(long)
   3   d      D           param horizontal(double)
   5   e      D           param vertical(double)
   7   bl     Z           discreteMouseScroll —— boolean，非 int
  15   k      I           ★ wheel —— 唯一的 int local
```
- bytecode：offset 248 `istore 15` = `vec.y!=0 ? vec.y : -vec.x`（滾輪合併量）；offset 363 `iload 15` 餵 `ScrollWheelHandler.getNextScrollWheelSelection(D,I,I)` → offset 377 `Inventory.setSelectedSlot(I)`（mixin `@WrapOperation` 包的就是這個 call）。slot 15 作用域 start250/length130 → 涵蓋注入點 377 ✓。
- **唯一 int(signature `I`) local = slot 15**。slot 7 是 `Z`(boolean)、不算 int。

### root cause（修正版，別再抄舊說法）
- 〔鐵證〕官方 mojmap LVT 把該 local 命名為 **`k`**（無語意單字母，javap -l 實證），upstream by-name `@Local(name="wheel")` 抓 "wheel" → 官方沒這名 → `Unable to find matching local`。
- 〔鐵證〕jar **有 LVT**（35 method 帶），**不是「mojmap 把 local 名全剝離」**——舊 STATUS/learnings 的「剝離 local 名」說法不準，是「名字系統不同」。
- 〔推論，未驗 upstream build 配置、但不影響修法〕"wheel" 八成來自 upstream 開發環境的 **Parchment**（NeoForge 圈給 mojmap 補 local 名的慣例）；我們 Loom bare officialMojangMappings 無 Parchment 故對不上。修法用 index、不靠任何 by-name，故此來源是真是假都不影響。

## 拍板 decision
1. **MouseHandlerMixin 修法：`@Local(name = "wheel")` → `@Local(index = 15)`**。
   - 理由：LVT slot 15 = `k`:int 鐵證；`index` 直鎖 LVT 槽位，**繞過 ordinal 的型別歧義**。
   - 不選 `ordinal = 0`：雖然「唯一 int」在「有 LVT」時 = ordinal 0，但 runtime jar **若無 LVT**，mixinextras 改 frame 分析、boolean(slot7) 被當 int-category 一起數 → wheel 飄成 ordinal 1，且可能 silent 抓錯 slot 7（discreteMouseScroll），滾輪翻牌靜默失效。index 無此風險。
   - 不選 `name = "k"`：官方名，但 runtime remap 後 local 名可能再變，by-name 最脆（正是這次 crash 的成因類型）。
   - body 一行不動（`ItemHandCards.Client.handleScrollWheel(wheel)` 等照舊；只動 annotation 參數）。
2. **其他 3 mixin 不動**：InventoryMixin 死碼、KeyMappingAccessor/AbstractClientPlayerMixin javap 證 OK → runClient 統一蓋章（required=1 會 fail-fast 揪出任何漏網）。
3. **E3 視覺驗收沿用 phase-2.md 固定場景清單**（手牌/箭頭/方塊桌/autoplayer/HUD/Screen），對照 upstream。

## 原子步驟（bite-sized）
- **S1.** 改 `MouseHandlerMixin.java`：`@Local(name = "wheel")` → `@Local(index = 15)`。其餘不動。
- **S2.** `gradlew compileJava`：確認改 annotation 0 編譯 error（mixinextras `@Local` import 不變）。
- **S3.** runClient **行為車道 smoke**（mixin runtime 唯一 gate）：
  - 啟動 → 進主畫面不 crash → 進單人 dev 世界。
  - **mixin apply 驗法**〔eng-review B1 修正：mixin 成功是**靜默**的、只在失敗才印 error，別期待逐條 success log〕：① log 無 mixin/local error（無 `Unable to find matching local`、無 `apply failed`）；② **required 兜底來源**〔eng-review A2〕＝ mixin config `minopp.mixins.json` 的 `"injectors":{"defaultRequire":1}`〔已讀證〕 ＋ mixinextras `@Local` 抓不到 local 直接拋 error〔這次 crash 即實證〕→ 漏 apply 必 crash，**能進世界＝都 apply 了**；③ 要逐條看則加 JVM arg `-Dmixin.debug=true` 看 verbose apply log。
  - **功能（驗真生效，非只 apply）**：
    - 手持手牌時滾輪**翻牌** vs 空手時滾輪正常**換 hotbar**（驗 MouseHandlerMixin `index=15` 抓對 wheel）。
    - **discrete mouse scroll 選項開/關各測一次**〔eng-review B2〕——slot 7 正是這 boolean、開關改變 onScroll 內部路徑，剛好驗 `index=15` 沒誤抓到 slot 7（直接打中我棄 ordinal 的理由）。
    - **開 GUI/Screen（背包 or mod screen）時滾輪不被手牌邏輯誤攔**〔eng-review B2〕——bytecode 證 onScroll 在 `screen != null` 走 `Screen.mouseScrolled` 分支、根本不到 `setSelectedSlot`，故開 GUI 時滾輪該完全正常；驗攔截範圍沒外溢。
- **S4.** **視覺車道**（獨立一級 gate、Phase 2 核心，對照 upstream 人眼。**學長拍板「驗細、視覺第一」→ 加 render 環境 edge**）。用 dev `run/`、不碰正式存檔：
  - **基本場景**（phase-2.md E3 清單）：手牌(1張/滿手/多色/特殊牌 skip·draw·reverse·wild/**每種顏色牌＋wild 選色後狀態**)、箭頭(順/逆時針)、方塊桌(空/遊戲中/有棄牌堆)、autoplayer(視野內≥1)、HUD(輪到自己/非自己回合)、Screen(SeatControl＋WildSelection 各開＋resize＋點主互動按鈕＋**hover/disabled/pressed 按鈕狀態**)。
  - **render 環境 edge**〔eng-review B3，學長要細〕：① **GUI scale** 改幾檔各看一次（非只 window resize）；② **graphics mode** Fast/Fancy/Fabulous 至少挑會影響透明/render layer 的看一次；③ **方塊桌不同朝向＋玩家從不同方向看**；④ 透明/染色元素放**亮背景與暗背景**前各看一次（抓 tint/render layer 錯）；⑤ **F3+T resource reload 後**材質仍正常。
  - **判準**：upstream MC/loader 版本可能不同 → 標準是**視覺語意一致**（orientation/state/透明/tint/render layer 對），**非 pixel-perfect**。
- **S5.** 收尾：changelog + api-changes（mixin `@Local` mojmap 地雷修法）+ learnings（ordinal LVT 型別陷阱、Parchment vs 官方名）+ STATUS（**Phase 2 真收尾**）+ 收尾 commit + 迷你 retro。

## Critical Files Reference（mutability — 動工/寫入前必對照本欄）
> 標唯讀者禁止任何 Edit/Write 工具觸碰。
- `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`：**絕對唯讀**，僅供讀取/javap/參考。
- **唯一會改的 code file**：`src\main\java\cn\zbx1425\minopp\mixin\MouseHandlerMixin.java`（只改 `@Local` 一個 annotation 參數）。
- **允許更新的文件（非 code，限 S5 收尾）**：`_notes/changelog.md`、`_notes/api-changes.md`、`_notes/learnings.md`、`_notes/STATUS.md`、`_notes/pitfall-log.md`。〔eng-review A1：避免「唯一會改」與 S5 文件更新自相矛盾〕
- 1.21.11 mojmap jar（loom cache `...layered+hash.2198-v2`）+ javap：**唯讀查證**。
- 其他 3 個 mixin：**本 plan 不改**（如 runClient 暴露問題再個別開步驟）。

## Verification（兩車道）
- **行為車道（smoke）**：runClient 進遊戲不 crash + 4 mixin apply（log 無 mixin error）+ 滾輪翻牌/換槽功能 = mixin runtime 對齊達成。
- **視覺車道（獨立一級 gate，Phase 2 核心、一直欠的）**：
  - 完整性：phase-2 E3 已過（render 資產齊、deck/arrow texture 在），不重跑。
  - 正確性：S4 固定場景對照 upstream（截圖/原版行為基準），抓 orientation/state variants/透明/tint/render layer「看起來沒壞但其實不對」。**此車道人眼對照需學長拍板**（視覺正確性優先是學長原則）。
- **協作分工**：S1/S2/S3-log（改 mixin、編譯、啟動 runClient 讀 log 確認不 crash + mixin apply）= 我做；S3-功能/S4 視覺正確性人眼對照 = 學長看畫面拍板（我可截圖輔助、但視覺對錯最終學長定）。

## 風險
1. **低**：index=15 修法——LVT slot 15 鐵證 + runClient required=1 fail-fast 驗（錯了立刻 crash 報錯，不 silent）。萬一沒命中：先用 `@Local(print = true)` 印 runtime 注入點實際 local 表確認 slot（mixinextras debug 工具，非改修法）再對。〔替代修法「wrap `getNextScrollWheelSelection` 從 arg 拿 wheel」eng-review 提過，學長拍板「真失敗再說」、不預寫進 plan〕
2. **中**：E3 視覺正確性主觀——固定場景 + 對照 upstream 基準降主觀；render 細節（tint/render layer）易「沒壞但不對」。
3. **低**：其他 3 mixin runtime——javap 證 OK，runClient 統一蓋章。
4. **低中**：runClient 在此機器的 runtime cache——phase-0 已 `--refresh-dependencies` 補 1.21.11 intermediary，預期不再 crash；若又 `class_XXXX not found` = cache 問題非 mixin（refresh 解、見 conventions）。

## Out-of-scope（Phase 3 / 本 plan 不做）
- Phase 3：runtime 完整驗證（玩完整一局 UNO + MinoBot + config GUI + NBT 重進世界 + 視覺深度對照）+ 2 個 upstream TODO（BlockEntityMinoTableRenderer:256 / ItemHandCards:101）。
- stonecutter `//?` 全面清理（留收尾 chore）。
- mod-fork-publish 推 private repo（全 phase 收尾後）。

## 進入動工前 gate
草稿 → self-review → 學長選「完整 eng-review」→ structured-plan-review（4 lens + codex xhigh）→ 學長 grill 拍 2 taste 點（E3 驗細·視覺第一；fallback 真失敗再說）→ 採納 findings 改 v2 → **gate 通過 ✓、動 S1**。

## review report
**self-review（2026-06-25）**：no-placeholder ✓ / mutability 命令式 ✓ / 兩車道 ✓ / bite-sized ✓ / ground truth 落檔 ✓ / 範圍完整（4 mixin 全分級、無漏）✓。修正 1：root cause 的 Parchment 來源標為〔推論〕、與〔鐵證〕分離（防圓謊）。標記：S4 E3 視覺正確性是需學長人眼參與的大頭、本 plan 僅推進到「能進 S4」，S4 內部迭代動工時展開。
**structured-plan-review（2026-06-25，4 lens + codex xhigh）**：方向確認、index=15 最佳、無 blocker。採納 findings → v2：A1 mutability 措辭分 code/文件、A2 required=1 來源寫清、B1 S3 log 期待修正（成功靜默）、B2 S3 加 discrete-scroll 開關＋GUI 滾輪不誤攔、B3 S4 加 render 環境 edge（學長要細）。codex 獨立認同 index=15＝強 signal（非背書，runClient 終驗）。taste 拍板：E3 驗細·視覺第一；fallback 真失敗再說（不預寫）。
**最重要的發現是自己 javap 抓的**：slot 7 是 `Z`(boolean) 非 int → ordinal 不可靠、改 index=15（codex 也獨立認同此盲點）。
