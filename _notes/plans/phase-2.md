# Phase 2 — client/render 編譯對齊（26.1 → 1.21.11）

> **v3（2026-06-24，javap mojmap bytecode 推翻 v2 範式①）**。狀態：v2 草稿→self-review→structured-plan-review→學長拍板「都照推薦走」→**動工前 carve-out 自查 mojmap，javap 推翻範式①→ v3 重寫 A/B 步驟、學長拍板「先更新 plan 再動工」**。
> **v2 範式①錯在哪**：它用 yarn 名（前 session spike 拉 MCP yarn 源看結構，沒換 mojmap）當成 1.21.11 的 API 變化。javap minopp 實際 link 的 mojmap jar（`minecraft-merged-1.21.11-loom.mappings.1_21_11.layered+hash.2198`）證實全是同物異名，見下「範式修正」。
> 偵察 ground truth：**A 手牌 mojmap 介面 javap 已釘死**；**B 方塊桌 BlockEntityRenderer mojmap 介面 javap 已釘死（與現狀全等）**；C HUD/Screen 範式②**尚未 javap 重驗**（動工 C1 前必驗）。

## 範圍 / Goal
消掉 **35 個 client/render 編譯 error**、build 綠、runClient 視覺驗收（兩車道）。
**真實工作性質（v3 修正）**：不是 render 範式大重構，是「26.1→1.21.11 逐個 class/泛型/命名小修」（同 phase-1 退 fabric-api/interact）。介面提交範式（submit + SubmitNodeCollector）26.1↔1.21.11 幾乎沒變。

35 個按檔案分**三子批**（v2 後 HUD+Screen 合併，見 decision 1）：
- **A 手牌 7**（HandCardsSpecialRenderer 6 + MinoFabricClient 1）
- **B render-state 9**（BlockEntityMinoTableRenderer 7 + EntityAutoPlayerRenderer 2）
- **C HUD+Screen 19**（GuiShim 7 + GameOverlayLayer 6 + SeatControlScreen 3 + WildSelectionScreen 3）

## Context（已驗 ground truth）

### ★ 範式修正（v3，javap mojmap 推翻 v2 範式①）
**yarn↔mojmap 同物異名對照**（v2 把左欄當成「1.21.11 要改成的目標」，其實右欄才是 mojmap code 實際面對的；兩欄是同一個 class/method 的不同 mapping 命名，**非 API 變化**）：

| v2 寫的（yarn 名） | mojmap 真名（javap 實證） | 26.1 現狀 code | 26.1→1.21.11 真要改？ |
|---|---|---|---|
| `render` | `submit` | `submit` | 名不變 |
| `OrderedRenderCommandQueue` | `SubmitNodeCollector` | `SubmitNodeCollector` | 不變 |
| `collectVertices` | `getExtents` | `getExtents` | 不變 |
| `getData` | `extractArgument` | `extractArgument` | 不變 |
| `getCodec()` | `type()` | `type()` | 不變 |
| `BakeContext` | `BakingContext` | `BakingContext` | 不變 |
| `updateRenderState`（B） | `extractRenderState` | `extractRenderState` | 不變 |

**真正的 26.1→1.21.11 差異（mojmap 視角，才是 code 要改的）**：
- **A（手牌 SpecialModelRenderer）**：`submit` 7參→8參（**補 `ItemDisplayContext` 第2參**，state 之後 PoseStack 之前）+ **`Unbaked` 去泛型**（連帶 `bake` 回 `SpecialModelRenderer<?>`、`type()` 回 `MapCodec<? extends Unbaked>`）。內部 `submitCustomGeometry`/`cardItemModel.submit` **原樣不動**〔javap: `ItemStackRenderState.submit(PoseStack, SubmitNodeCollector, int,int,int)` 5參全等、`SubmitNodeCollector` 繼承 `OrderedSubmitNodeCollector.submitCustomGeometry`〕。
- **B（方塊桌 BlockEntityRenderer）**：介面 **mojmap 完全等於現狀 26.1**（雙泛型 `<T extends BlockEntity, S extends BlockEntityRenderState>` / `submit(S,PoseStack,SubmitNodeCollector,CameraRenderState)` / `createRenderState` / `extractRenderState(T,S,float,Vec3,CrumblingOverlay)` default / `shouldRenderOffScreen()` 全等）。7 error 全在**周邊 class 的 mojmap 命名**，逐個 compileJava + javap 兜（見 B 步驟）。

### A 手牌 SpecialModelRenderer（javap mojmap 已釘死）
mojmap 1.21.11（`net.minecraft.client.renderer.special.SpecialModelRenderer`，javap bytecode）：
- `void submit(T, ItemDisplayContext, PoseStack, SubmitNodeCollector, int light, int overlay, boolean glint, int)` — 8參
- `void getExtents(Consumer<Vector3fc>)` / `T extractArgument(ItemStack)`
- `interface Unbaked`（**raw 無泛型**）：`SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext)` / `MapCodec<? extends SpecialModelRenderer.Unbaked> type()`
- 範本 mojmap `DecoratedPotSpecialRenderer`（同 package）；yarn 範本 `DecoratedPotModelRenderer` 已驗 `record Unbaked() implements Unbaked { MapCodec.unit(...); getCodec/bake 回 wildcard }`。
- 註冊：`SpecialModelRenderers.ID_MAPPER.put(id, Unbaked.MAP_CODEC)`（MinoFabricClient:47；去泛型後型別對齊，編譯器確認）。

現狀 26.1（HandCardsSpecialRenderer.java）vs 1.21.11 差異 = `submit` 缺 ItemDisplayContext + `Unbaked<T>` 帶泛型。**只改這兩點，其餘原樣。**

### B render-state（javap mojmap 已釘死：介面與現狀全等）
mojmap 1.21.11 `BlockEntityRenderer`（javap bytecode）= 現狀 26.1 code 介面，**逐項全等**（見上表）。`BlockEntityRenderState` 存在於 `net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState`（現狀 import:16 正確）。
**7 error 來源（周邊 class mojmap 命名，逐個動工 javap/compileJava 釘）**：
- `CameraRenderState`：現狀 import:21 `...renderer.state.level.CameraRenderState` → javap 證實 mojmap 是 `...renderer.state.CameraRenderState`（去 `.level`）。
- `RenderTypes` vs `RenderType`：現狀用 `RenderTypes.lines()`/`RenderTypes.entityCutout(...)`〔mojmap submitCustomGeometry 第2參型別是 `RenderType`(單數)；1.21.11 是否有 `RenderTypes` 工具類 **B2 javap 確認**——無則改 `RenderType.xxx()`〕。
- `LightCoordsUtil.FULL_BRIGHT`（import:26）：1.21.11 存在性 **B2 javap 確認**。
- `state.lightCoords`（:108/:118）：`BlockEntityRenderState` 是否有此欄位 **B2 javap 確認**。
- `RenderShim.renderLineBox`（:82）、`submitText`(:201 10參)、`cardItemModel.submit`(:108 5參)：javap 顯示簽名與現狀一致，預期不動（compileJava 兜底）。
- ⚠️ EntityAutoPlayerRenderer（entity 線）生命週期可能與 BlockEntity 線不同 → B3 對 entity 線範本 javap（見步驟）。

### HUD+Screen（C，範式②尚未 javap 重驗）
〔v2 自讀推測，**未經 javap**〕1.21.11 DrawContext（mojmap GuiGraphics）存在；Screen 背景 method = `renderBackground(GuiGraphics, int, int, float)`；GuiGraphicsExtractor 1.21.11 不存在（26.1 extract 產物）。
**★ v3 強制**：範式①的教訓是「yarn 結構 ≠ mojmap code」。C1 動工前**必 javap mojmap 驗證** GuiGraphicsExtractor 是否真不存在、GuiGraphics/Screen.renderBackground/HudElementRegistry callback 的 mojmap 真名與簽名，**別信任何 yarn 或自讀推測**。
- Screen（SeatControl/WildSelection）：override 背景 method 簽名退回（C1 javap 定）。
- HUD（GameOverlayLayer）：render 吃 GuiGraphics（C1 javap 定）；HUD 註冊 HudElementRegistry.attachElementAfter callback 型別（fabric API）。
- **GuiShim 是樞紐**：改它一處，GameOverlayLayer + 兩 Screen 大半連帶解決。

### 退路（A spike 證實不需動用）
ItemHandCardsNoBewlr + HandCardsWithoutLevelRenderer（<26.1 BEWLR，/* */ 註解）。SpecialModelRenderer 主路徑走到底、BEWLR 不啟用。

## 拍板 decision
1. **HUD+Screen 合併為一子批 C**〔v2 eng-review #1〕：共用 GuiShim 樞紐 → 合併最省。**但範式②本身 C1 javap 重驗後才定改法**（v3）。
2. **攻擊順序**：A 手牌（介面差異最大、先收）→ B render-state（介面全等、修周邊命名）→ C HUD+Screen（範式②獨立、待 javap）。
3. **偵察深度**：A/B 的 mojmap 介面 javap 已釘死；B 周邊 class 命名 B2/B3 compileJava+javap 逐個兜；C1 javap 重驗範式②。
4. **stub 取捨**〔v2 eng-review #7〕：不放空 TODO stub——保留 render 內部原邏輯、最小改。**0 work-marker 檢查提到每子批末**（A末/B末/C末）。
5. **子批粒度 + 內部 checkpoint**：三子批為回退單位（等學長確認）；子批內每檔 compileJava checkpoint 盤點錯誤數，不必每步等學長、只盤點不混。
6. **stonecutter //? 殘留**：沿用 phase-1 選 C（改哪清哪）。
7. **查證工具鏈**（v3 釘死）：mojmap 名查證 = **javap minopp link 的 mojmap jar**（`...layered+hash.2198`，含 mojmap `net/minecraft/world/item/ItemStack`）；javap = `C:\Program Files\Java\jdk-17\bin\javap.exe`。minecraft-dev MCP **mojmap 路徑壞**（缺 mapping-io-cli.jar），yarn 可用但**只看結構、不可當 mojmap code 名**（範式①教訓）。不修 MCP（out-of-scope）。

## 原子步驟（bite-sized，每**子批**做完等學長確認再下一個；子批內 checkpoint 自走）

### 子批 A：手牌渲染（mojmap javap 已釘死）
- **A1.** HandCardsSpecialRenderer.submit：簽名插入 `ItemDisplayContext displayContext` 第2參（state 之後、poseStack 之前）；`hasFoil`→`glint`（語意對齊，非必須）。**body 完全不動**（submitCustomGeometry/cardItemModel.submit 原樣）。`ItemDisplayContext` 已 import（:26）。
- **A2.** HandCardsSpecialRenderer.Unbaked 去泛型：`implements SpecialModelRenderer.Unbaked<HandCardRenderState>`→`SpecialModelRenderer.Unbaked`；`bake` 回傳 `SpecialModelRenderer<HandCardRenderState>`→`SpecialModelRenderer<?>`；`type()` 回傳 `MapCodec<? extends SpecialModelRenderer.Unbaked<HandCardRenderState>>`→`MapCodec<? extends SpecialModelRenderer.Unbaked>`。`MAP_CODEC`/`MapCodec.unit`/`BakingContext` 不變。
- **A3.** 移除 26.1-only 殘留 import 若有（現狀無多餘；SubmitNodeCollector 保留——1.21.11 仍用）。
- **A4.** MinoFabricClient:47 `SpecialModelRenderers.ID_MAPPER.put(id, Unbaked.MAP_CODEC)`：Unbaked 去泛型後型別對齊，compileJava 確認（預期不用改或微調）。
- **A5.** compileJava + **0 work-marker 檢查**：手牌 7 error（HandCardsSpecialRenderer 6 + MinoFabricClient 1）精確消失 + 無新增 + 無殘留 TODO/stub（預期 35→28）。

### 子批 B：render-state（介面全等，修周邊命名）
- **B1.** javap 補偵察（**自查、不丟 agent**）：`RenderTypes` 工具類在不在 1.21.11 / `LightCoordsUtil` 存在性 / `BlockEntityRenderState.lightCoords` 欄位 / EntityRenderer 範本（entity 線 vs block-entity 線）+ EntityAutoPlayerRenderer 現狀讀檔。
- **B2.** 修 BlockEntityMinoTableRenderer（7）：CameraRenderState import 去 `.level`；RenderTypes/LightCoordsUtil/lightCoords 依 B1 結果對齊。**介面層（submit/createRenderState/extractRenderState 簽名）不動**。compileJava checkpoint 盤點。
- **B3.** 修 EntityAutoPlayerRenderer（2）：對 entity 線範本（B1 javap）。compileJava checkpoint 盤點。
- **B末.** compileJava + 0 work-marker：render-state 9 消失、無新增（預期 28→19）。

### 子批 C：HUD+Screen（範式② C1 javap 重驗後才定改法）
- **C1.** **javap mojmap 重驗範式②**（v3 強制，別信 yarn）：GuiGraphicsExtractor 是否存在 / GuiGraphics(=DrawContext) mojmap 真名 / Screen.renderBackground 簽名 / GameOverlayLayer HUD render 簽名 / HudElementRegistry.attachElementAfter callback 型別（fabric API）。讀現狀 GuiShim/GameOverlayLayer/兩 Screen。
- **C2.** 改 **GuiShim**（樞紐）：依 C1 結果對齊包裝對象。compileJava checkpoint。
- **C3.** 改 GameOverlayLayer + MinoFabricClient HUD 註冊 callback。compileJava checkpoint。
- **C4.** 改 SeatControlScreen + WildSelectionScreen 背景 method。compileJava checkpoint。
- **C末.** compileJava + 0 work-marker：HUD+Screen 19 消失、**build 綠**（預期 19→0）。

### 收尾（→ workflow 節4.4 + 節5 + 節6）
- **E1.** build 綠 + 全專案 0 殘留 work-marker（TODO/FIXME/編譯 stub）。
- **E2. 跨子批整合靜態檢查清單**〔eng-review #3〕（非 runtime、人工 diff 檢查）：
  - 每個 render path 同一套 submit 流程；pose push/pop 成對；light/overlay/renderLayer/transparency 來源明確；
  - extractRenderState 只讀必要資料、不偷用 live BE 狀態；HUD 與 world render 沒混座標系；
  - **client-only 類沒被 common/server path 靜態引用**〔eng-review #8〕（EntityAutoPlayer 隔離）。
- **E3. runClient 視覺驗收（兩車道 + 固定場景清單）**〔eng-review #4〕，見 Verification。
- **E4.** changelog + api-changes + learnings + STATUS 更新 + 收尾 commit + 迷你 retro。

## Critical Files Reference（mutability — 動工/寫入前必對照本欄）
> 動工/寫入前必先對照本清單；標唯讀者禁止任何 Edit/Write 工具觸碰，連 stonecutter 切 active target 都不行。
- H:\MC_Mods_Port\Minopp_Port\Minopp-upstream（canonical upstream clone）：**絕對唯讀**。僅供讀取 / javap / 參考。
- H:\MC_Mods_Port\Minopp_Port\Minopp\src\main\java\cn\zbx1425\minopp：**會改**，本 phase 動的檔：
  - A 手牌：render\HandCardsSpecialRenderer.java、fabric\MinoFabricClient.java
  - B render-state：render\BlockEntityMinoTableRenderer.java、render\EntityAutoPlayerRenderer.java
  - C HUD+Screen：platform\multiver\GuiShim.java、gui\GameOverlayLayer.java、gui\SeatControlScreen.java、gui\WildSelectionScreen.java
  - 連帶可能動（編譯器報才動）：platform\multiver\RenderShim.java（renderLineBox）、各 render 檔 LightCoordsUtil/RenderTypes import、MinoFabricClient HUD 註冊行
  - 其餘 server 檔（已 phase-1 編過）本 phase 不動。
- 1.21.11 mojmap minecraft jar（loom cache `...layered+hash.2198`）+ minecraft-dev MCP（yarn 可用 / mojmap 路徑壞）：**唯讀查證**。

## Verification（兩車道）
- **行為車道（smoke）**：render 是 runtime hot path → 收尾 runClient 進遊戲開一局，確認手牌/HUD/方塊桌/autoplayer 都畫得出來、不 crash、不 NoClassDefFound。
- **視覺車道（獨立一級 gate，本 phase 核心）**：
  - 完整性：純檔案交叉校驗每個註冊 render 對象 → model/blockstate/texture 齊（抓紫黑缺貼圖）。批次掃一次。
  - 正確性（**固定場景清單，eng-review #4，逐項對照 upstream**）：
    - 手牌：1 張 / 滿手牌 / 多色牌 / 特殊牌（skip/draw/reverse/wild）/ 選色後狀態；
    - 箭頭：順時針 / 逆時針方向；
    - 方塊桌：空桌 / 遊戲中 / 有棄牌堆；
    - autoplayer：至少一個實體在視野內；
    - HUD：遊戲中 / 輪到自己 / 非自己回合；
    - Screen：SeatControlScreen + WildSelectionScreen 各開一次 + **resize 一次** + **點一次主互動按鈕確認 callback 活著**。
  - 用 dev run/、不碰正式存檔。
- **編譯車道（子批 + 內部 checkpoint gate）**：每子批/每檔 compileJava，指定 error 精確消失 + 無新增 + 剩餘逐條盤點（總數遞減只是輔助訊號、非 gate）。

## 風險
1. **低（v3 降級，原高）**：手牌 submit 簽名——javap 已釘死（補 ItemDisplayContext + 去泛型），不再是「行級編譯器兜底」的高不確定。compileJava A5 蓋章。
2. **低中（v3 降級，原高）**：B render-state——介面 javap 證實與現狀全等，剩周邊命名（CameraRenderState/RenderTypes/LightCoordsUtil/lightCoords）B2 javap+compileJava 兜。EntityAutoPlayerRenderer entity 線 B3 對範本（仍需 B1 偵察確認生命週期）。
3. **中（eng-review #3）**：跨子批整合——多 render path 都能編、但 pose/light/overlay/transparency 某環不一致致畫面錯，只靠收尾 runClient 太晚 → E2 靜態整合檢查清單補。
4. **中（v3 上升）**：C HUD+Screen 範式②**尚未 javap 重驗**——v2 自讀推測 GuiGraphicsExtractor 不存在，可能又是 yarn/命名陷阱。C1 強制 javap 先驗，別重蹈範式①覆轍。
5. **低中（eng-review #8）**：EntityAutoPlayer client-class 隔離 → 改 renderer import 可能弄壞 client/server 邊界，E2 diff review 專看。

## Out-of-scope（後續 phase / 本 plan 不做）
- **Phase 3**：runtime 完整驗證（玩完整一局 UNO + MinoBot + config GUI + NBT 重進世界）+ 視覺深度對照。
- stonecutter //? 全面清理（留收尾 chore；本 phase 僅按 decision 6 處理動到的）。
- 修 minecraft-dev MCP 的 mojmap 路徑（mapping-io-cli.jar）。
- mod-fork-publish 推 private repo（全 phase 收尾後）。

## 進入動工前 gate
草稿 → self-review → structured-plan-review（eng-review + codex）→ 學長拍板 v2「都照推薦走」→ **動工前 carve-out javap 推翻範式① → v3 重寫 → 學長拍板「先更新 plan 再動工」** → 待學長 review v3 → 動子批 A。

## review report
**self-review（2026-06-24，v1）**：no-placeholder ✓ / mutability 命令式 ✓ / 兩車道 ✓ / bite-sized ✓ / spike 落檔 ✓。

**structured-plan-review（2026-06-24，v2，4 lens + codex xhigh）**：見 git 歷史 / STATUS。v2 findings #1~#8 已納入（HUD/Screen 合併、跨子批 gate、固定場景、雙樣本偵察、子批內 checkpoint、不放空 stub、client 邊界、Screen resize/按鈕）。

**v3 javap 自查（2026-06-24，carve-out 動工前查證）**：
- **推翻 v2 範式①**：javap minopp link 的 mojmap jar 證實 v2「26.1 SubmitNodeCollector→1.21.11 OrderedRenderCommandQueue+render+queue.submitXXX」是 **yarn 名誤當 API 變化**。mojmap 真相：A `submit`/`SubmitNodeCollector` 沒改名（差 ItemDisplayContext 參數 + Unbaked 泛型）；B `BlockEntityRenderer` 介面與現狀**全等**（差周邊 class 命名）。
- **救援時點**：carve-out「目標版本簽名 100% 自查、不信 agent/spike」+ 不丟 agent 自己 javap，擋下「照 v2 把 submit 改 render、把不存在的 OrderedRenderCommandQueue/collectVertices/getData 寫進 code → 整批編譯失敗、白繞數趟 build」。對應 learnings「yarn 結構 ≠ mojmap code」「信校驗不信宣告」。
- **未竟**：C 範式②尚未 javap 重驗（C1 強制補）；B 周邊 class 命名 B1/B2 javap 補。
