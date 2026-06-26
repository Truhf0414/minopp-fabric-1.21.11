# Phase 1 — server-side 編譯對齊（1.21.1 → 1.21.11）

> v1（2026-06-24 草稿）。狀態：草稿 → self-review → structured-plan-review → 學長拍板 → 動工。
> 偵察 ground truth：2026-06-24 重跑 `gradlew compileJava`（第一手），完整 log `compile-phase1.log`。

## 範圍 / Goal
讓 **server-side code 對齊 1.21.11 API**、消掉 server 的 **3 個編譯 error**。
**不追求 build 綠**——client 35 個 error 屬 Phase 2，單 source set 整包 `compileJava` 會被 client error 擋住。
產出：server 3 error 從 build log 消失（38 → 35），剩餘全為 client/render error。

## Context（第一手 ground truth，編譯器驗證，非腦補）
- **server 編譯 error 精確 = 3 個**（與 STATUS 38 一致、reproducible）：
  - `EntityAutoPlayer:215` — `@Override` does not override（interact 簽名變）
  - `EntityAutoPlayer:238` — `super.interact(player, hand, location)`；編譯器：**required `Player,InteractionHand` / found `Player,InteractionHand,Vec3`** → 1.21.11 `Entity.interact` = **2 參數**
  - `PlayerShim:17` — `player.sendSystemMessage(message)` cannot find symbol method `sendSystemMessage(Component)`
- **NBT 高優先未知已解（編譯器證實，非 javap 腦補）**：`BlockEntityMinoTable`（`saveAdditional(ValueOutput)`/`loadAdditional(ValueInput)`）、`EntityAutoPlayer`（`addAdditionalSaveData(ValueOutput)`/`readAdditionalSaveData(ValueInput)`）、`NbtIOShim`（`TagValueInput/TagValueOutput.create*`）**全部編譯通過、不在 error 列** → 1.21.11 NBT 簽名 = `ValueInput/ValueOutput`，與 base=B 一致、**不需改**。runtime 存讀正確性留 Phase 3。
- **permissions API**：`PlayerShim.hasPermissions` 用 26.1 的 `player.permissions()` / `PermissionLevel` **編譯通過** → 1.21.11 已有 permissions 系統（非 STATUS 計數錯）。
- 3 個 error 共性：**「26.1 加/改、1.21.11 尚無」→ 退回 `<26.1` 寫法**（印證 learnings「base=B 混合基準：部分 MC API 也要從 26.1 退回 <26.1」）。

## 拍板 decision（★ 待 grill 確認）
1. **範圍切法**：phase-1 = **只修 3 個 server error**，獨立小 phase 先走。
   - 理由：建立「退回 `<26.1`」的 pattern（Phase 2 client 的 GuiGraphicsExtractor/CameraRenderState 等同類退回會複用此經驗）+ 把 server 面確定下來、縮小 Phase 2 不確定範圍。
   - 替代（grill 時可推翻）：併入 Phase 2 一起做（反正都要 build 綠才驗收）。**推薦維持獨立**——小批、低風險、可單獨回退。
2. **★ stonecutter `//?` 註解處理（要你拍）**：這 2 個 error 點都帶攤平殘留的 `//? if >=26.1` 註解（專案已獨立、不再用 stonecutter，這些是死註解）。三選項：
   - **(A) 只改 active 行內容為 1.21.11 寫法、保留 `//?` 框架** — 最小 diff，但「註解標 >=26.1、內容卻是 <26.1」會自相矛盾（讀 code 誤導）。
   - **(B) 手動切分支**（<26.1 變 active、>=26.1 註解回去）— 語義一致，但手動配對 `/* */` 易錯、diff 較大。
   - **(C) 改哪清哪**：這 2 處直接清成裸 1.21.11 code、移除 `//?` 殘留 — 終態正確、無矛盾，但屬「改點順手清」。**推薦 (C)**（專案已脫離 stonecutter，`//?` 遲早全清；改點順手清這 2 處不算「全面清理」）。全面清理仍 out-of-scope。
3. **verification 受限認知**：phase-1 無法獨立跑 runServer/runClient（client error 擋整包編譯）。驗收 = build log server 3 error 消失。

## 原子步驟（bite-sized，一步一動作，每步做完等學長確認再下一步）
1. **動工首步：decompile 1.21.11 mojmap，confirm 2 個退回目標簽名**（carve-out 自查、100% cross-check，不丟 agent）：
   - `Entity.interact` 回傳型別 + 確認 2 參數 `(Player, InteractionHand)`（對編譯器 `required` 互證）。
   - `Player`/`Entity`/`LivingEntity` 有 `displayClientMessage(Component, boolean)`（退回 `sendSystemMessage` 的目標；upstream `<26.1` 用此寫法）。
   - 〔interact 簽名編譯器已給 ground truth；本步是對 `displayClientMessage` 補實證，別讓 plan 修法欄留腦補。〕
2. **修 `EntityAutoPlayer.interact`**（:215 + :238，按 decision 2 拍板的 A/B/C 處理 `//?`）：
   - active 簽名去掉第三參數 `final Vec3 location` → `interact(Player player, InteractionHand hand)`。
   - :238 `super.interact(player, hand, location)` → `super.interact(player, hand)`。
   - 回傳型別維持 `InteractionResult`（步驟1 confirm）。
3. **修 `PlayerShim.sendSystemMessage`**（:17，按 decision 2 處理 `//?`）：active `player.sendSystemMessage(message)` → `player.displayClientMessage(message, true)`（`<26.1` 寫法）。
4. **重跑 `gradlew compileJava` 驗收**：server 3 error 消失、總數 38 → 35、剩餘逐行確認全為 client/render（無新 server error）。

## Critical Files Reference（mutability — 動工/寫入前必對照本欄）
- `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`（canonical upstream clone）：**絕對唯讀**。動工/寫入前必先對照本清單；標唯讀者**禁止任何 Edit/Write 工具觸碰**，連 stonecutter 切 active target 都不行。僅供讀取 / javap / 參考。
- `H:\MC_Mods_Port\Minopp_Port\Minopp\src\main`：**會改**，且本 phase **只動 2 檔**：
  - `entity\EntityAutoPlayer.java`（interact）
  - `platform\multiver\PlayerShim.java`（sendSystemMessage）
  - 其餘 server 檔（game/ network/ block/ effect/ item/ platform 其餘）**本 phase 不動**（已編譯通過）。
- 1.21.11 mojmap 反編譯源（minecraft-dev MCP cache）：唯讀查證用。

## Verification（兩車道）
- **行為車道（smoke）**：**跳**。3 個改動屬機械退回——interact 是引擎呼叫的 override（callsite 不變）、sendSystemMessage→displayClientMessage 是 upstream `<26.1` 背書的等價；且 client 擋編譯、runtime 此 phase 跑不起來。NBT runtime 正確性留 Phase 3。
- **視覺車道**：本 phase 無可渲染資產上線，不適用（Phase 2/3 才開）。
- **編譯車道（本 phase 主驗收，gate 措辭採納 codex finding）**：重跑 `compileJava`，gate =
  1. 原本 **3 個指定 server diagnostics 精確消失**（EntityAutoPlayer:215/:238、PlayerShim:17）；
  2. **無新增任何 server/package diagnostic**；
  3. 剩餘 diagnostics **逐條盤點全為 client/render**。
  「總數 38 → 35」只是輔助訊號、**不是 gate**（避免「少 3 又新增 1，總數看似合理實則失敗」的假綠）。

## 風險
1. **低（行為差異，cross-model 點明）**：`displayClientMessage(msg, true)`（actionbar/overlay）與 `sendSystemMessage`（系統聊天）**語義非完全等價**，是使用者可見差異 → upstream `<26.1` 既用此寫法、作者已背書，但 **phase-1 報告禁寫「等價」**，Phase 3 runtime 必須實測觀感。
2. **低**：interact 退回後 `@Override` 對不上 → 編譯器已證 `Entity.interact(Player,InteractionHand)` 存在（:238 required），步驟1 decompile 再 confirm。
3. **低**：改 active 行誤動 `//?` 註解結構致假訊號 → 步驟4 重跑編譯即抓（編譯器是硬 gate）。

## Out-of-scope（後續 phase，本 plan 不做）
- **Phase 2**（最痛）：client/render 35 個 error — GuiGraphicsExtractor 17 / CameraRenderState 4 / LightCoordsUtil 5 / SpecialModelRenderer.Unbaked 6 / Screen override 2…（對 api-changes render 食譜 + 1.21.11 javap 校準）。
- stonecutter `//?` 註解**全面清理**（留收尾 chore；本 phase 僅按 decision 2 處理動到的 2 處）。
- NBT/遊戲狀態 runtime 正確性驗證（留 Phase 3 runServer/runClient）。
- **`EntityAutoPlayer` client-class 隔離隱患**（server entity 卻 import `Minecraft`/`AutoPlayerScreen`，dedicated-server class-loading 風險；api-changes「client-only class 隔離」已記）——編譯通過、非 phase-1 error，runtime 影響留 Phase 3 驗（作者用 inner `class Client` 隔離，Phase 3 對照）。

## 進入動工前 gate
草稿 → self-review → structured-plan-review（eng-review + codex 外部聲音）→ **學長親口拍板** → 才動步驟1。
（harness 訊號／我自問的選項都不算拍板。）

## review report
**self-review（2026-06-24）**：no-placeholder ✓；NBT 結論基於編譯器「編過」非腦補 ✓；mutability 命令式 ✓。補強：out-of-scope 加 `EntityAutoPlayer` client-class 隔離隱患。

**structured-plan-review（2026-06-24，4 lens + codex 外部聲音 xhigh）**：
- **Lens1 架構**：phase-1 只動 2 檔、低耦合、邊界乾淨。no issue。
- **Lens2 正確性（對 ground truth）**：server 3 error 逐行確認；`BlockEntityMinoTable` NBT 有 `@Override`+編過 = Java 語言保證簽名對齊；**`EntityAutoPlayer` NBT（addAdditionalSaveData/readAdditionalSaveData）缺 active `@Override`**，靠 `super` 呼叫編過間接證明簽名匹配——對齊成立但不夠硬。
- **Lens3 驗證**：phase-1 無 runtime 驗收（誠實揭露）；`sendSystemMessage→displayClientMessage` 語義變（chat→actionbar）非純機械。
- **Lens4 性能**：3 個簽名替換，無 hot path。no issue。
- **codex 外部聲音（cross-model 一致處 = 強信號）**：① 與我一致抓 `EntityAutoPlayer` 缺 `@Override` → 建議併入 phase-1 補回（零行為變更、強化編譯器保證，加了若編不過＝抓到真問題）；② 驗收別用「38→35」當 gate（已採納收緊）；③ 與我一致：displayClientMessage 語義非等價、報告別寫等價（已採納）；④ 提醒「退回 `<26.1`」**別升級成全域規則、逐 API 判斷**（permissions/NBT 已顯示 1.21.11 是混合狀態）；⑤ 移除 `Vec3` 參數後查 unused import → **已驗 `Vec3` 仍用於 EntityAutoPlayer:158（`lookAt`），不會 unused、不需清**。
- **處置**：驗收收緊 + 報告禁寫等價 = **已採納改 plan**；以下 3 項 = **待學長 grill 拍板**：(a) scope 獨立 vs 併 Phase 2；(b) stonecutter `//?` 處理 A/B/C；(c) 補回 `@Override` 到 2 個 Entity NBT method（codex+我建議採納）。

**★ 學長拍板（2026-06-24）+ 實施結果**：(a) 獨立 phase-1 ✅；(b) stonecutter 選 **C**（改哪清哪）✅；(c) 補 `@Override` ✅。已實施 5 處 Edit（interact 退 2 參 + 2 NBT @Override + 清 //? ／ PlayerShim displayClientMessage），compileJava **38→35**、server 3 個精確消失、剩餘逐條全 client。查證波折：decompile MCP 壞 + javap 撈不到 mojmap jar → 改編譯器查死 `displayClientMessage`（learnings 記）。**phase-1 收尾完成**（STATUS/changelog/api-changes/learnings 已更新）。
