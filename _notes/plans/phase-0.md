# Phase 0 — Stage-0 scoping spike（建環境 + 定攤平起點 + 攤平 + walking skeleton）

> **v3（2026-06-24，跳 javap：base=B 直接拍板、override 清單改由步驟5b 編譯器 error 產出，見文末 review report v3 增補）**
> v2（2026-06-24，已採納 eng-review 全部 5 條 findings）

## 範圍 / Goal
建好 Loom 1.21.11 Fabric 環境 → **直接拍板 base=B（跳 javap，三方佐證見步驟2）** → 從 upstream **唯讀複製** base=B 的 fabric source 進新專案 → 分兩步編（骨架先通 → 再爆 API error），**讓編譯器逐行爆出 override 全貌取代人眼 javap 偵察**。
**目標不是零 error，是戳破最大不確定**：①Loom+Mojmap+mixin build 管線通不通、②60 class 對 1.21.11 的 API error 全貌（哪些 import 不存在/簽名變了 = override 清單）。產出乾淨的「API error 全貌分類」當後續 phase 切分依據。（base 大方向 A/B 不再是本 phase 的未知——三方佐證已定 B；本 phase 的未知收斂為「逐塊 override 有哪些」。）

## Context（已驗證 ground truth，細節見 _notes）
- 命名 **Mojmap**，不翻 yarn〔查過〕。
- 母數 **60 class**（54 共用 + 6 fabric；丟 15 neoforge）+ 資源（見 conventions）。
- **攤平 = stonecutter 就地重寫**：upstream `src/` 當前已是 `26.1.2-fabric`（active = `>=26.1` 活碼）〔查過〕。
- **render/NBT 適配全在 `>=26.1`**；`>=1.21.2`（12 處）只碰 registry/翻譯鍵；`<26.1` 是 1.21.1 老碼〔查過〕。
- **stonecutter 版本條件對 target 的 mcVersion 求值**：versions/ 只有 1.20.1 / 1.21.1 / 26.1.2 三 target；要 `(>=1.21.2 && <26.1)` 切片得**新增** mcVersion∈[1.21.2,26.1) 的 target〔查過邏輯〕。
- 作者 1.21.1→26.1 遷移留了逐行食譜 commit（api-changes.md 列）；舊碼（<26.1）與新碼（>=26.1）兩份都在 git，可雙向參考。

## 拍板 decision
- **路線 B**（獨立 Fabric、無 stonecutter / modstitch）— 使用者已拍板。
- **mod metadata** — 已確認：`mod_id=minopp`、`maven_group=cn.zbx1425`（建環境時對 upstream build.gradle.kts 再核）、`mod_name=Mino++`、`mod_version=1.4.0-1.21.11`、working_dir=`H:\MC_Mods_Port\Minopp_Port\Minopp`。
- **攤平起點 = base slice + override 清單**（採納 eng-review F3 框架；使用者 2026-06-24 拍板 base=B 並跳 javap）：
  - **base slice（主起點）= B（`>=26.1`，當前 active 的 26.1.2-fabric 新碼）— 已拍板**。理由：render-state/NBT 新範式（submit/RenderState/ValueInput）是 1.21.6+ 產物、比 1.21.1 老碼接近 1.21.11；當前已攤平、複製最省事；registry setId 在 B 本來就含（26.1>1.21.2）。**三方獨立佐證指向 B**（見步驟2），base 大方向把握已足，不跑 javap confirm。〔base=B 拍板；唯一翻盤條件 = 步驟5b 編譯器爆出「B 站不住」的 error 形狀 → 角色閘回報〕
  - **override 清單**：**不前置 javap 列**，改由**步驟5b 編譯器 error 全貌**逐行爆出（哪些 import 不存在/簽名變 = override）。已知高風險 override 候選仍當讀 error 的眼睛：**手牌渲染**（1.21.11 有無 `SpecialModelRenderer`？無則退回 BEWLR 路徑）、item model resolver 形狀、HUD 註冊方式。
  - 🛑 **角色閘（base 證偽）**：若步驟5b error 顯示 base=B 站不住（26.1 專屬 API 大量不存在、反而像 A）或 override 牽動大改方向，**動後續前先回報使用者確認**。

## 原子步驟（bite-sized，一步一動作，每步做完等確認再下一步）
1. **建環境（base 基準＝官方 fabric-example-mod 1.21.11 分支，非 skill 模板）**〔2026-06-24 拍板「整組照官方」〕：配置 = `fabric-loom-remap` + `splitEnvironmentSourceSets()` + **bare** `loom.officialMojangMappings()` + 官方版本（loader 0.19.3 / loom 1.17-SNAPSHOT / fabric-api 0.141.4+1.21.11；版本相容性待實證）。`fabric-1.21.11-port` skill 僅當 scaffold 佈局參考（source set / entrypoint / fabric.mod.json），**其 layered mapping 與寫死版本不採用**（與官方不符，詳見 conventions + learnings + 全域記憶 fabric-1211-port-skill-needs-fix）。餵現成 metadata（別空問）。產出：能 `runClient` 進遊戲的空殼 Fabric mod（mapping/管線通）。
2. **拍板 base=B（跳 javap，採納 2026-06-24 決策）**：base slice 直接定 **B（`>=26.1`，當前 active 的 26.1.2-fabric 新碼）**，不跑 javap 偵察。
   - **三方佐證（為何敢不驗就拍）**：① 我方 `_notes/api-changes.md` 記的 render/NBT 食譜方向、② 外部 ChatGPT 對話獨立列舉 1.21.5 RenderPipeline / 1.21.6 GuiRenderState / 1.21.9 `getEntityWorld` 一連串渲染重寫，使 1.21.11 遠離 1.21.1 老碼、③ 本 plan 原傾向——三方一致指向 base=B。base 大方向把握已足。
   - **為何 javap 可跳（成本論）**：javap 原有兩職責——(a) 判 base A/B、(b) 逐塊產 override 清單。(a) 三方佐證已足；(b) 反正步驟5b 硬編 60 class 時，**編譯器逐行爆出每個不存在/簽名變的 import，比人眼 javap 更硬更徹底**，且滿足原 F2「render/4 檔每個 import 驗存在」。base=B 攤平＝複製 active，不需 javap 提前量。故 javap 淨價值＝提前量，此情境不痛 → 跳，省成本。
   - 〔base=B 已拍板；override 清單 = 步驟5b 編譯器 error 全貌（見步驟3/5b）。版本號 1.21.11 使用者宇宙確定，步驟1 Loom 抓 jar 自然兜底，不另查。〕
   - **保留 javap 為「按需工具」非「必經步驟」**：步驟5b 讀 error 時若某塊 import 簽名編譯訊息講不清（如多載歧義），可臨時對該 class javap 釐清——carve-out 自查、不丟 agent。
3. **override 清單來源 + base 證偽閘**：override 清單**不前置 javap**，由**步驟5b 編譯器 error 全貌**產出（按 render/NBT/registry/item-model/mixin/其他分類，記進 STATUS + api-changes）。已知高風險 override 候選當讀 error 的眼睛：**手牌渲染**（1.21.11 有無 `SpecialModelRenderer`？無則退 BEWLR）、item model resolver 形狀、HUD 註冊方式。
   - 🛑 **角色閘**：若步驟5b error 顯示 base=B 站不住（26.1 專屬 API 大量不存在、反而像 A）或 override 牽動大改方向 → **動後續前先回報使用者**，別自己翻 base。
4. **攤平抽 source（採納 F1：canonical 永遠唯讀）**：
   - base=B：從 canonical upstream **唯讀複製**當前 active（26.1.2-fabric）的共用 + fabric package（**不複製** `neoforge/` package、neoforge mixin、mods.toml）→ 新專案 `src/`。此為純讀取複製，canonical 不動。
   - override 參考：`<26.1` 老碼直接從 git 歷史 / 當前 src 的 `/* */` 註解區塊**唯讀讀取**參考，不切 target。
   - **若真需在非-active target 攤平出某切片**（如要看純 1.21.1 或新增 [1.21.2,26.1) target 攤平）→ 在 **disposable upstream copy**（git worktree 或另複製一份）做，**canonical `Minopp-upstream` 的 .java 永不寫、不切 active**。
   - `//?` / `/* */` 殘留可先帶著編（合法 Java 註解），清理留步驟6 後或後續。
5. **walking skeleton（採納 F4：拆兩步，分離骨架噪音與 API error）**：
   - **5a 骨架先通**：把 metadata（fabric.mod.json 實值化）+ 全部 resources + mixin config（common+fabric）+ **最小 stub entrypoint** 搬進去，`gradlew build` 骨架通 + `runClient` 空殼進遊戲。確認 Loom/Fabric/mixin/AW/模板替換無誤。
   - **5b 爆 API error**：再搬 60 class，`gradlew build`（不接 `| tail`），讓 **1.21.11 API error 乾淨地一次爆出**、按類別分（render / NBT / registry / item-model / mixin / 其他）記進 STATUS + api-changes，當 phase 1/2 切分依據。
6. **編後核對（採納 F5）**：核對攤平殘留沒造成假訊號——class 數對母數（60）、import 無假陰性、resource/mixin 分支沒錯搬。

## Critical Files Reference（mutability — 動工/寫入前必對照本欄）
- `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`（canonical upstream clone）：**絕對唯讀**。禁止任何 Edit/Write 觸碰其檔；**連 stonecutter 切 active target 都不行**（那會就地重寫它的 src/）。所有攤平/切 target 一律在 disposable copy 做。讀取/複製/javap 參考 OK。
- `H:\MC_Mods_Port\Minopp_Port\Minopp`（新專案）：**會寫**（環境 + 攤平 source + 資源）。
- disposable upstream copy（步驟4 視需要建，如 `Minopp-upstream-scratch` 或 git worktree）：**可寫**，用完即丟。
- **build 基準＝官方 1.21.11 模板**（非 skill forbidden）〔2026-06-24 拍板更正〕：**用** `fabric-loom-remap` + `splitEnvironmentSourceSets()` + **bare** `loom.officialMojangMappings()`。舊誤記「禁 remap/split/layered」已證錯——官方模板實證該用 remap+split+bare，**勿回退到禁用**。skill 的 layered mapping 與寫死版本不採。

## Verification（兩車道）
- **行為車道**：步驟1 末 + 步驟5a 末 `runClient` 空殼/骨架進得了遊戲（mapping/管線通）。步驟5b 編譯**容許非零 error**，但要能跑完 build 流程拿到完整 error list。
- **視覺車道**：本 phase 無可渲染資產上線，暫不適用（Phase 2/3 才開）。
- **完整性**：步驟6 核對 60 class + 資源母數沒漏搬、無攤平殘留假訊號（對 conventions 母數清單）。

## 風險
1. **最高**：1.21.11 render/item-model 的 override 全貌（尤其手牌 SpecialModelRenderer vs BEWLR）→ base=B 跳 javap（三方佐證，見步驟2），改由步驟5b 編譯器硬編逐行爆出，不靠猜。base 大方向已收斂（不再是本 phase 未知）。
2. **高**：Loom + Mojmap + mixin（4 個 mixin）build 管線通不通 → 步驟5a 骨架先通戳破（與 API error 分離）。
3. **中**：base/override 判斷錯 → 因 git 有雙份碼可雙向參考、override 清單可逐塊回滾，降低不可逆性。
4. **中**：誤在 canonical upstream 切 target/寫入 → Critical Files 明令「canonical 連切 active 都不行，攤平用 disposable copy」。
5. **低**：`//?` 殘留造成假訊號 → 步驟6 核對。

## Out-of-scope（後續 phase，此 plan 不做）
- **Phase 1**：核心 code 編譯對齊（server 優先）：`game/`、`network/`、`block` NBT 存讀、registry setId。
- **Phase 2**（最痛）：client render 對齊：`render/` 4 檔、`gui/`（HUD）、`EntityAutoPlayerRenderer`、各 Screen，對著作者 >=26.1 食譜 + javap 1.21.11 校準 + override 清單。
- **Phase 3**：runtime 驗證兩車道：玩完整一局 UNO + MinoBot + config GUI + NBT 重進世界；視覺對照 upstream。
- **收尾**：build 綠 + 0 work-marker + 完整性核對 + `mod-fork-publish` 推 private repo。

## 進入動工前 gate
草稿 → self-review → structured-plan-review（eng-review + codex）✅ → 使用者親口確認 → 才動步驟1。（harness 訊號／我自問的選項都不算確認。）

## review report
**eng-review（2026-06-24，structured-plan-review：4 lens + codex 外部聲音）**：cross-model 高度一致。5 條 findings：
- **F1[高]**: flatten 切 active target 會就地重寫 upstream src/（違反唯讀護欄）+ (A) `(>=1.21.2,<26.1)` 切片無現成 target（1.21.1-fabric 是 `<1.21.2`）→ flatten 一律在 disposable upstream copy 做、canonical 永遠唯讀；(A) 須在副本新增 mcVersion∈[1.21.2,26.1) 的 target。
- **F2[中]**: javap 清單漏手牌渲染岔路（SpecialModelRenderer vs BEWLR）+ item renderer 全套 + Fabric HUD/custom item renderer 註冊 API → 擴充清單，並驗 render/4 檔實際 import 的每個 MC 類。
- **F3[中/策略]**: 攤平起點「A/B 二選一」是錯誤二分法 → 改輸出「base slice + override 清單」。
- **F4[中]**: walking skeleton 一次搬 60 class 編，error 被骨架噪音（模板/mixin/entrypoint/AW）淹沒 → 拆 5a（骨架+空 entrypoint 先通）+ 5b（再搬 60 class 爆 API error）。
- **F5[低]**: `//?` 殘留「可先帶著編」太樂觀 → 編後驗無假訊號。
- **處置**: 使用者 2026-06-24 拍板「F1/F2/F4/F5 全採納；F3 用 base+overrides」→ **v2 已全部反映**。

**v3 增補（2026-06-24，跳 javap 決策）**：使用者拍板「跳 javap 步驟、base=B 直接定、override 清單改由步驟5b 編譯器 error 產出」。理由：javap 兩職責中 (a) 選 base 已有三方獨立佐證（我方食譜 + 外部 ChatGPT 對話列舉 1.21.5/1.21.6/1.21.9 渲染重寫 + 原 plan 傾向）、(b) 產 override 由步驟5b 編譯器逐行爆出更硬更徹底。**不違背 F2/F3**：F2 要的「逐 import 驗存在」由編譯器達成（比人眼 javap 更徹底）；F3 的 base+override 框架保留，只是 override 來源從 javap 改為編譯器。改動成本 << 走完整 javap。javap 降級為「步驟5b 讀 error 釐清歧義」的按需工具。版本號 1.21.11 使用者確認（步驟1 Loom 自然兜底），不另查。
