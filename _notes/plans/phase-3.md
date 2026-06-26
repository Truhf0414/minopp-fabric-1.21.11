# Phase 3 — runtime 完整驗證（玩法 · Bot · NBT 存讀 · 視覺 · 多人冒煙）

> 承接 phase-2b（Phase 0/1/2 全收尾、`build` SUCCESSFUL、單機 runClient 已開完一局 UNO 零異常）。本 phase＝**深測完整玩法 ＋ 多人冒煙**、逮 build-綠-騙不過的 **runtime-only 洞**、確認行為/視覺對照**學長基準**正確。**＝ port 主體最後一個內容 phase**；過了 = mod 本體 port 完工（之後只剩「要不要發布」+ 長期維護，皆 out-of-scope）。
>
> **狀態：v2（eng-review 完成、structured-plan-review＋codex findings 已採納、學長 grill 拍板）。gate 通過，待動 S0。**

## 範圍 / Goal
深度遊玩驗證已 port 的 minopp，把「build 全綠但 runtime 悄悄錯」的洞逮出來並修掉，標準＝**玩起來行為對、視覺對、存讀不掉狀態、多人服起得來且同步**。驗證線：完整 UNO 局玩法 → MinoBot ＋ config → NBT 存讀（單機關世界重進）→ 視覺深度對照 → **多人冒煙（dedicated server ＋ 第二 client）**。
**真實工作量**：**verification-heavy / code-light**——主體是「學長實玩 ＋ 我跑環境/掃 log/檔案校驗 ＋ 逮到洞當場修小的」。預設 code 改動少；改多少取決於逮到幾個洞（phase-2b 同型 phase 逮了 2 個 upstream 原生洞）。

## Context（已驗 ground truth）

### 承接狀態〔查過 STATUS〕
- `build` SUCCESSFUL（phase-2 35→0）；單機 runClient phase-2b 開**完整一局 UNO 零異常**、4 mixin 全 apply、滾輪翻牌正常、E3 視覺驗細過、全 log 0 ERROR。
- phase-2b 附帶修掉 2 個 upstream 原生完整性洞（coupon item definition 紫黑 / mino_table recipe ingredient 格式）→ 模式 F。

### NBT 存讀鏈盤點〔查過 grep，全 26.1 `ValueInput/ValueOutput` 範式〕— 3c 核心
| 層 | 存/讀入口 | 檔:行 |
|---|---|---|
| 牌桌（總入口） | `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` | `block/BlockEntityMinoTable.java:78` / `:100` |
| ↳ client 同步 | `getUpdateTag` → `NbtIOShim.pourOne(this::saveAdditional)` | `BlockEntityMinoTable.java:335` |
| 玩家 | `CardPlayer(ValueInput)` / `nbtWriteTo` | `game/CardPlayer.java:42` / `:50` |
| 牌局 | `CardGame(ValueInput)` / `nbtWriteTo` | `game/CardGame.java:256` / `:271` |
| 單張牌 | `Card(ValueInput)` / `nbtWriteTo` | `game/Card.java:152` / `:161` |
| Bot config | `useConfigNbt(ValueInput)` / `writeConfigNbt` | `game/AutoPlayer.java:99` / `:107` |
| 動作訊息 | `ActionMessage(ValueInput)` / `nbtWriteTo` | `game/ActionMessage.java:20` / `:29` |

- **`ActionMessage` 註記**〔查過完整 code，已從「高風險」降級〕：作者刻意 JSON-in-NBT（backward-compat）；存↔讀對稱（同顆 1.21.11 `ComponentSerialization.CODEC`）→ 同版本自洽；JSON 純文字跨版本反比 NBT 結構穩。保留不改（decision 4）。殘留風險僅「跨版本舊存檔」，本 port 大概率無。S3 一般驗證（重進 HUD 訊息列正常）。

### 牌局規則機制〔查過 CardGame.java / AutoPlayer.java，eng-review lens2 全數對照通過〕
- 牌型：NUMBER(0–9) / SKIP / REVERSE / DRAW(+2) / WILD / WILD+4；suit：red·green·blue·yellow·wild。
- **REVERSE 2 人局**〔`CardGame.java:111-116`〕：`players.size()==2` → `isSkipping=true`（＝跳過語意）；否則 `isAntiClockwise` 翻轉。
- **rule_forbid 綁 WILD+4**〔`CardGame.java:84-89`〕：`WILD && DRAW` 且手上還有別張 `canPlayOn` → fail `rule_forbid`（無其他可出才能打 WILD+4）。
- Bot config 5 欄位〔`AutoPlayer.java:19-23` + NBT key `:100-112`〕：`aiNoWin`(NoWin) / `aiNoPlayerDraw`(NoPlayerDraw) / `aiForgetChance`(ForgetChance) / `aiNoDelay`(NoDelay) / `aiStartGame`(StartGame)。
- Bot：`entity/EntityAutoPlayer`（右鍵啟用→坐桌）＋ `game/AutoPlayer.playAtGame`。

### 2 個 upstream 作者 TODO〔查過 code + 學長拍板〕— 保留原樣
| 代號 | 位置 | 性質 | 處置 |
|---|---|---|---|
| **A** | `item/ItemHandCards.java:101` `appendHoverText` 內呼叫 client-only `Minecraft.getInstance().player` | server 防呆；玩家正常遊玩零感 | **保留**；S5 dedicated server 持有該 item 順帶驗不炸（codex#6，見下校準） |
| **B** | `render/BlockEntityMinoTableRenderer.java:256` `// Do the extraction`；`submit` 即時讀活 `state.blockEntity.game.deck.size()` | render 萃取技術債；玩家幾乎無感 | **保留**，掛 runtime 例外：S4 牌桌視覺真閃爍/錯位才升級修 |

- **A 的 classloading 校準**〔ground truth，駁 codex#6 過度〕：`ItemHandCards` 是 common class，但用 `Minecraft.getInstance()` 的是內部 lazy-load `Client` static class → dedicated server 單純載入 `ItemHandCards` **不連帶載入 `Client`**，要執行到 tooltip 路徑才碰，server 正常不畫 tooltip → 不碰。codex「載入就炸/發不了服」風險被打折；但 S5 順帶驗（便宜保險），不為它改 A。

## 拍板 decision（已與學長對齊）
1. **基準＝學長**（human oracle）：視覺/手感對錯最終學長定，我截圖/掃 log 輔助。
2. **驗證 ＋ 小洞當場修**；牽動架構大洞 → 記錄另議（格式見 S6）。
3. **A、B upstream TODO 保留原樣**；B 唯一例外＝S4 視覺真閃爍/錯位才破例。
4. **`ActionMessage` JSON-in-NBT 保留不改**（作者刻意 backward-compat、玩家無感）。
5. **human-in-the-loop**：學長實玩（含開第二 client/runServer）；我跑環境、掃 log、檔案校驗、修洞。缺學長不可。
6. **nobewlr lang 不補**（已結案）。
7. **多人冒煙 gate 納入 Phase 3**〔A 選 (a)〕：dedicated server 起得來 ＋ 第二 client 基本同步即過；**完整多人並發壓測（多真人 race／網路延遲丟包）out-of-scope**。
8. **可控 setup 紀律**〔codex#3/#7〕：edge case **需明確 setup 才算「已驗」**。**S1 首步查證已完成（2026-06-25）**：minopp **無配牌 debug 指令**（發牌 `Collections.shuffle` 純隨機 `CardGame:50`）→ WILD+4 forbid／deck depleted（deck 另有 reshuffle `CardGame:211` 幾不觸發）**改邏輯審查（讀 code 確認 path）＋非 gate**〔學長拍板〕；2人REVERSE 正常 2 人局可測，其餘機會性＋記錄。**正面攔截**：此查證順帶撈到 plan 漏的 `award`/`demo`/`shout` 三個玩法功能（已納 S1）——decision 8「先查 setup」的價值實證。
9. **log 判準＝0 ERROR ＋ WARN 分類白名單**〔codex#9〕：WARN 也掃，已知無害者記白名單，未知 WARN 當疑點追（phase-2b 教訓：資源/codec/packet 常只 WARN）。

## 原子步驟（bite-sized；每子批＝一個「玩→驗→修」可驗收接縫，做完等學長確認才動下一個）
- **S0（前置 sanity）**：`gradlew build` 確認仍綠 ＋ 單機 runClient 起得來進 dev 世界（log 判準＝decision 9）。不過＝環境退化，先查非往下。
- **S1 ＝ 玩法**（我跑 runClient＋掃 log、學長實玩判對錯）：
  - **首步查證已完成**〔2026-06-25〕：minopp **無配牌 debug 指令**（發牌純隨機）；牌效果 edge 多無法可控 setup（見 decision 8）；順帶撈到 3 個 plan 漏的指令功能（↓喊 Mino 行）。
  - 逐牌效果（setup 現實）：**2人REVERSE**＝正常 2 人局出 REVERSE 即觸發 ✓；SKIP／DRAW+2／WILD／數字＝機會性（多打幾局）＋記錄實際觸發；**WILD+4 forbid／deck depleted**＝無法 setup → **邏輯審查（讀 code path）＋非 gate**〔學長拍板〕。
  - 喊 Mino（剩1張）／忘喊被 doubt 抓賴／亂喊被罰；**喊 Mino 三路徑**：chat 打「mino/uno/minopp」〔`Mino.java:87`〕＋ keybind（shout_modifier）＋ **`/minopp shout`**〔`MinoCommand:33`〕。
  - **指令玩法功能**〔S1 首步撈到、學長拍板納〕：**`/minopp set_table_award`**（OP，手持物站桌上設桌子 `award`＝贏家獎勵〔完整領獎流程待 S1 追〕）、**`/minopp set_table_demo <bool>`**（OP，設展示桌 → 不開真局＋`table_in_demo` 提示）。
  - 勝負結算（game_won）；**deck depleted 獨立 micro-case，標長測·非 gate**（decision 8）。
  - **loot**〔D2〕：破壞 mino_table 掉落正確（`data/minopp/loot_table/blocks/mino_table.json`）。
  - **順帶記視覺異常**〔D1〕：視覺觀察貫穿 S1–S5，逮到先記、S4 集中對照。
  - 判準：行為符 UNO 規則＋學長記憶；log 0 ERROR＋WARN 白名單。
- **S2 ＝ MinoBot ＋ config GUI**：
  - 召喚 Bot → 右鍵啟用 → 自動坐桌 → ≥2 人（aiStartGame 開則自動提議）startGame → Bot 自動出牌/選色/喊 Mino。
  - 各開關**最小可驗場景**〔codex#4〕：**NoWin**（Bot 剩可出最後一張，開→必不出）／**NoPlayerDraw**（Bot 有 Draw 且下家真人，開→不打 Draw）／**ForgetChance 設 0%／100% 兩端**（不驗中間隨機）／**NoDelay** 目測 Normal vs Instant／**StartGame** 開關自動提議。
  - **YACL fallback**〔B3〕：**移除/禁用 YACL 啟動一次**驗 fallback 提示（`gui.minopp.bot_config.yacl_missing`）；不做則此判準刪。
  - 判準：每開關開/關行為有對應差異。
- **S3 ＝ NBT 存讀（單機）**（行為車道 smoke）：
  - 牌局進行中存檔 → 退出 → 重進 → 還原：players/各人手牌/currentPlayer/牌堆/方向(isAntiClockwise)/isSkipping/topCard/棄牌堆/Bot config。
  - `ActionMessage` 還原（一般驗證）：重進後 HUD 訊息列不亂碼/不丟。
  - 判準：重進可無縫接續；log 無 NBT parse error。
- **S4 ＝ 視覺深度對照**（獨立一級 gate，對照學長基準。phase-2 E3 清單 ＋ 完整局新場景）：
  - 手牌（1張/滿手/多色/特殊牌/wild 選色後）、牌堆＋棄牌堆（空/遊戲中/多張堆疊）、HUD（輪到/非輪到/方向/draw 累積/top card）、Bot（視野內）、Screen（SeatControl＋WildSelection：開/resize/hover·disabled·pressed）、方塊桌朝向、GUI scale 各檔、graphics mode Fast/Fancy/Fabulous、亮暗背景、F3+T reload。
  - **B TODO 觀察點**：出牌/抽牌瞬間盯牌堆，看高度有無閃爍/跳動（決定 B 例外）。
  - **音效/subtitles**〔D2〕：play/draw/sigh/pass/turn_notice/win/mino_shout 等觸發時音效＋字幕正常。
  - **多語言**〔P2〕：切 `zh_tw` 掃一遍翻譯無缺字/亂碼（學長中文基準）。
  - 判準：視覺語意一致（orientation/state/透明/tint/render layer），非 pixel-perfect。
- **S5 ＝ 多人冒煙 gate**〔A 選 (a)，新增〕（需學長開 runServer＋第二 client）：
  - **A1**：`gradlew runServer` 啟動 ＋ 載入世界 ＋ 0 ERROR（揪 server-only 啟動炸/client class 誤混）。
  - **A1**：第二 client 連 localhost → 兩端入桌、輪流出牌/抽牌、喊 Mino、勝負。
  - **A2**：進行中 → 停 server → 開 server → client 回來繼續至少一輪（server-side 存讀，異於單機路徑）；client 斷線重連後 HUD/手牌/座位同步。
  - **A3 雙 client 一致**：A 出牌 → B 即時見棄牌堆/top card/HUD/方向更新；B 私有手牌內容 A 不可見、但公共狀態變；換座/離桌/重連雙方桌面一致。
  - **A4**：dedicated server 持有 `hand_cards` item 流程不炸（順帶覆蓋 A classloading 保險）。
  - 判準：server 0 ERROR＋WARN 白名單；server-authoritative 狀態正確 ＋ 雙 client 視覺/HUD 一致。
- **S6 收尾**：changelog ＋ api-changes（runtime 驗證結論＋逮到/修掉的洞）＋ learnings（迷你 retro，conditional）＋ STATUS（**Phase 3 收尾＝port 主體完工**）＋ 收尾 commit ＋ pitfall-log（逮到的洞歸模式）。
  - **大洞記錄格式**〔C2〕：另議的大洞一律記＝重現步驟／環境（單機 or server）／log·截圖／預期vs實際／是否阻擋 Phase 3。

## Critical Files Reference（mutability — 動工/寫入前必對照本欄）
> 標唯讀者禁止任何 Edit/Write 工具觸碰。
- `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`：**絕對唯讀**，僅供讀取/對照/javap。
- **Phase 3 預設不改 code（驗證 phase）**。「逮到洞才動」候選熱點（動前逐檔確認、小洞才當場改）：
  - NBT 鏈：`block/BlockEntityMinoTable.java`、`game/{CardGame,Card,CardPlayer,AutoPlayer}.java`
  - 視覺：`render/BlockEntityMinoTableRenderer.java`、`gui/GameOverlayLayer.java` 等
  - **網路/多人**：`network/**`（S5 冒煙若炸出 server-only/sync 問題才動）
  - 資源：`assets/minopp/**`、`data/minopp/**`（若再現 phase-2b 型格式漂移）
- **保留、本 phase 不動**：`game/ActionMessage.java`（decision 4）、`item/ItemHandCards.java:101`（A）；`render/BlockEntityMinoTableRenderer.java:256`（B）僅 S4 視覺真壞才破例。
- **允許更新文件（限 S6 收尾）**：`_notes/{changelog,api-changes,learnings,STATUS,pitfall-log}.md`。
- 1.21.11 mojmap jar（loom cache）＋ javap：唯讀查證。

## Verification（兩車道）
- **行為車道（smoke）**：S1 玩法零異常 ＋ S2 Bot/config 開關生效 ＋ S3 NBT 還原 ＋ **S5 多人冒煙（server 起得來＋雙 client 同步）** ＋ 全程 0 ERROR＋WARN 白名單。
- **視覺車道（獨立一級 gate）**：完整性（phase-2 E3 已過＋完整局新場景＋多語言）；正確性（S4 對照學長基準＋S5 雙 client 一致）。
- **協作分工**：我＝build/runClient/runServer/掃 log/檔案校驗/修洞；學長＝實玩、開第二 client、Bot 行為判讀、視覺與手感對錯**最終拍板**。

## 風險
1. **中**：runtime 洞未知數——完整玩法＋多人面比 phase-2b 大，NBT 還原/資源漂移/server-only 問題可能現形。對策：小洞當場修、大洞記錄（S6 格式）。
2. **中**：S5 多人冒煙環境——runServer 首次啟動可能需 eula/server 配置；dedicated server 缺 client class 隔離若沒做好會啟動炸（但這正是 gate 要抓的，非壞消息）。對策：S5 動工先確認 runServer task 可用。
3. **中低**：human-in-loop 需學長時間（完整局＋config 組合＋第二 client）。對策：子批切細、分次玩。
4. **中**：可控 setup 手段未定〔誠實標〕——B1 的 edge case 給牌手段 S1 動工首步才查 minopp 有無 debug 入口；查不到則高成本 edge 降為長測·非 gate（decision 8）。
5. **低**：B TODO 閃爍／`ActionMessage` 存讀／runClient cache——均低或已降級（見各處註）。

## Out-of-scope（本 phase 不做）
- **完整多人並發壓測**：多真人同時操作的 race、網路延遲/丟包、大量並發 client（S5 只做基本同步冒煙）。
- A／B upstream TODO 重構（保留；B 僅視覺真壞破例）。
- `ActionMessage` 改漂亮（decision 4）。
- nobewlr lang（已拍板不補）。
- 發布 / mod-fork-publish 推 repo（port 主體收尾後另議）。
- upstream 長期維護迴圈（diff-backport，另一條線）。
- stonecutter `//?` 全面清理（留最終收尾 chore）。

## 進入動工前 gate
草稿 ✅ → self-review ✅ → eng-review（structured-plan-review 4 lens ＋ codex xhigh）✅ → 學長 grill 拍板（A=a／B 全採／C／D／P1·P2）✅ → 採納 findings 改 **v2** ✅ → **gate 通過 ✓、待動 S0**。

## review report
**self-review（2026-06-25）**：no-placeholder ✓／mutability 命令式 ✓／兩車道 ✓／bite-sized ✓／ground truth 落檔 ✓。**草稿補正**：`ActionMessage` 初評「最高風險」讀完 code 後收回（對稱序列化）、改保留＋降級（誠實記錄：被「ugly＋backward compat」字眼觸發的術語直覺）。

**structured-plan-review（2026-06-25，4 lens ＋ codex xhigh）**：
- **Lens 1 架構**：子批切法/順序合理；小 gap → D1（視覺貫穿）、NBT 兩路徑（client 同步靠 S5＋phase-2b 隱含）。
- **Lens 2 ground truth（自讀，最重要）**：AutoPlayer 5 config 欄位（含漏讀的 `aiStartGame`）、REVERSE 2人局＝isSkipping、rule_forbid 綁 WILD+4 —— **全對照 code 通過、無矛盾**。
- **Lens 3 覆蓋**：最大洞＝多人 server 未驗（我 lens3 ＋ codex#1 **獨立一致＝強 signal**）；次＝edge case 無可控 setup。
- **Lens 4 perf**：無 issue（驗證 phase 不引入 hot path；B 每幀 `deck.size()` 為 O(1)）。
- **codex（10 findings）處置**：A(a) 多人冒煙 gate 納入（#1/#2/#5→S5）；B 全採（#3/#7→decision 8 可控 setup、#4→S2 0%·100%、#8→B3 YACL）；C（#9→decision 9 WARN 白名單、#10→S6 大洞格式）；**#6 A classloading 用 ground-truth（lazy classloading）降溫**——不為它改 A、S5 順帶驗。codex 最有價值補刀＝#3（「各走一遍」假設玩就會遇到、edge 靠運氣測不穩）。
- **carve-out 守住**：codex 當 signal 非 verdict；A4 我用 ground truth 駁回其過度斷言；多人 gate 範圍（a vs b vs c）交學長拍。

**handoff to grill ✅**：學長拍 A=(a)／B 全採／C 同意／D 採／P1·P2 納 → 本 v2。
