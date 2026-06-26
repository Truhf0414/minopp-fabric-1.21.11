# Port 踩坑總帳 — minopp（檢討大會素材）

> **用途**：累積整個 port **每個 session 的坑**，供 **port 完成後的檢討大會**做後設回顧。學長觀察「這次 port 坑特別多」——本檔專門挖**為什麼**，不只記單坑。
> **與 `learnings.md` 的分工**：learnings 記「坑→**規則**」（即時、可操作、之後遵守）；本檔記「坑→**根因模式**」（回顧、找跨坑的共同病因 + 成本 + 攔截點）。本檔每條引用 learnings、**不重抄細節**。
> **維護紀律**：每 session/phase 收尾，把該段的坑歸進下方「逐 phase 坑帳」+ 視情況更新「根因模式總表」。**正面攔截**（紀律奏效、擋下白工）也要記——檢討大會不只看踩進去的，也看「什麼擋住了」。

---

## 為什麼這次 port 坑特別多？（總論 — 檢討大會核心論點）

**一句話：這個 port 同時踩中四個高坑率因子的疊加，一般 port 通常只攤上其中一兩個。**

| 因子 | 這個 port 的具體形狀 | 一般 port 有嗎 |
|---|---|---|
| **逆向 port（未來→過去）** | base=B 是 26.1.2-fabric（比目標 1.21.11 **更新**的 snapshot），等於拿「未來碼」往回退到 1.21.11，每個 26.1 新增/重構都是潛在退回點 | 少見，多數是舊→新 |
| **工具鏈半殘** | minecraft-dev MCP 的 mojmap remap 壞（缺 mapping-io-cli.jar），逼著用 yarn 看結構 + javap/編譯器兜 mojmap | 通常 mapping 工具是好的 |
| **冷門新版本、資訊不可靠** | 1.21.11 Fabric 資料少，skill/筆記/yarn spike 容易「異口同聲」共享同一盲區 | 熱門版本社群驗證足 |
| **多平台→單平台** | upstream 是 modstitch 多平台（NeoForge/Forge/Fabric），切純 Fabric 時「多平台有的」未必「Fabric 要的」 | 單平台→單平台無此落差 |

**檢討大會 framing**：坑多不是執行不力，是**起點選擇（base=B）疊加環境限制（工具鏈/冷門版本）**的必然。四個因子兩兩交互還會放大——例如「逆向 port × 工具鏈半殘」就生出了 phase-2 的 yarn/mojmap 命名大坑（P2-1）。真正該檢討的是：**這些因子在 Stage-0 選 base 時可預見多少？哪些該在開工前就標為高風險？**

---

## 根因模式總表

### 模式 A — base=B「未來碼往回 port」的逆向落差　★最大坑源
從比目標**更新**的 26.1 退回 1.21.11，導致大量「26.1 加的/改的、1.21.11 還沒有，要逐個退回」。
- 命中坑：P0-1、P0-3、P0-8、P1-1（部分）、**P2-1**
- 共同形狀：「作者看起來在 `>=X` 分支寫好了」→ 其實綁在更未來的分支、對目標版未適配。
- 檢討問題：base=B 是 Stage-0 三方佐證後拍的（站得住），但「站得住」≠「沒代價」。代價分佈（哪些 API 退、退多少）能否在選 base 時更早估出來？

### 模式 B — 工具鏈半殘（mojmap 路徑壞）
MCP mojmap remap 壞 → 被迫 yarn 看結構 + javap loom cache jar + 編譯器兜底。
- **真根因（2026-06-24 查證 npm 包實際狀態）**：`@mcdxai/minecraft-dev-mcp@1.2.0` 發布包頂層只有 dist/node_modules/LICENSE/package.json/README，**`tools/` 整個目錄不存在**（Test-Path False）。mojmap remap 要的 `tools/mapping-io-cli/build/libs/mapping-io-cli-1.0.0.jar` 連放它的目錄、連 build 它的 source 都沒打包；package.json 無 build/postinstall step、README 無說明。→ **不是「裝壞了」，是這版發布包對 mojmap 功能本來就缺料**（yarn 路徑不需這 jar 故能用、mojmap 從第一秒缺）。error 訊息叫你 `cd tools/mapping-io-cli && gradlew shadowJar` 自 build，但 tools/ 不在包裡、照做也生不出來。〔作者為何這樣發布未查、不斷言〕修法：去 MCP repo 拿 tools/mapping-io-cli source 自 build jar 放對位置 / 回報 issue / 等新版；本 port 已用 javap loom cache jar 更硬的路繞過、不阻塞。
- 命中坑：P0-7、P1-1、**P2-1**
- 連鎖效應：工具半殘**直接製造**了「yarn↔mojmap 命名差異」這個坑類（P2-1）——若 mojmap 工具是好的，spike 根本不會拿 yarn 名當 code 名。
- 檢討問題：是否該早點投資修 MCP（建 mapping-io-cli.jar）而非整 port 用 javap 繞？繞的累積成本 vs 修一次的成本。

### 模式 C — 多源「異口同聲」≠ 對（冷門新版本的共享盲區）
冷門新版本資料少，多個來源（skill、筆記、yarn spike、AI review）會共享同一個錯誤盲區、互相壯膽。
- 命中坑：P0-5、**P2-1**
- 共同形狀：「好幾個來源都這樣說 / 解釋得很具體」→ 仍錯。**解釋的細緻度 ≠ 可信度。**
- 攔截法（已驗）：去找**第三方更硬的 ground truth**（官方模板、bytecode/javap、編譯器），別在多個軟來源間二選一。

### 模式 D — 「看起來 OK」的隱藏副作用
表面正常、底下有雷：設定操作其實是寫入、build 綠其實 runtime 炸、工具行為依賴環境。
- 命中坑：P0-2、P0-4、P0-6、P0-7
- 子型：①「設定/切換」即「寫入」（stonecutter 切 target 改 upstream src）；②「build 綠」≠「runtime 綠」（cache 缺 intermediary）；③ 工具行為依賴環境狀態（Copy-Item 看目標夾存不存在）。
- 檢討問題：這類只能靠「動手後驗結構/驗 runtime」抓，無法純推理預防——驗證紀律的投報率最高的地方。

### 模式 E — 多平台→單平台的 template/母數混淆
upstream 多平台 build 系統（modstitch）有的檔/命名，純 Fabric 未必需要。
- 命中坑：P0-8（fabric-api 命名）、P0-9（pack.mcmeta）
- 共同形狀：拿「upstream 母數」當「目標切片必需」→ 誤判漏搬 / 誤啃假 API 問題。
- 攔截法：對**官方目標-loader 模板**核對（ground truth），別只憑 upstream 母數推。

### 模式 F — 忠實 port 連 upstream 原生洞一起搬（data 格式漂移、runtime 才現形）　★phase-2b 新增
忠實複製 upstream 時，upstream 自己**沒跟上目標版本的「原生洞」**會被一起搬進來——尤其 data-driven 資源的跨版本格式漂移（item definition 新格式遺漏、recipe ingredient 物件→字串、loot table schema），編譯/build/檔案交叉校驗全綠，只有 runtime 載入才報。
- 命中坑：P2b-2（coupon item definition）、P2b-3（recipe ingredient）
- 共同形狀：**對照 upstream 同款壞** → 非我們搬錯、是 upstream 原生未適配目標版本；green build 騙過，runClient log `ERROR` / 人眼紫黑才現形。
- 與模式 D② 區別：D② 是「我們的配置/環境」隱藏副作用（cache/設定）；F 是「**upstream 內容本身**」帶洞、被忠實搬入。
- 攔截法（已驗）：**runtime 行為+視覺車道**（playbook 完整性最後防線）——跨大版本 port 必跑 runClient 掃 log `ERROR` + 人眼視覺，別只信 build 綠。
- **★ 反向校準（phase-3 規劃期 2026-06-25 新增）**：F 有**對偶面**——upstream 原生的「**非洞**」不該補。`hand_cards_nobewlr`（手牌 3D 渲染的 fallback/base model 載體 item）upstream 本就無 lang 翻譯、runtime 露 raw translation key；但它**玩家正常遊玩永遠碰不到**（全 src 無 game logic 發放、發到手上的是主牌 `hand_cards`、只能 /give、不進 creative tab）→ 補 lang ＝ 憑空加一條 upstream 沒有的字串、反而**偏離**忠實 port。**判據＝玩家正常遊玩可達性**：可達且壞（coupon 紫黑 P2b-2）→ 修；不可達（nobewlr 露 key）→ 不補。⚠️ **切割**：lang（名字、玩家不可見）不補，但其 model＋texture（主牌 GUI/第三人稱的底圖、**玩家可見**、查證在：model `parent item/generated` + texture 共用 `minopp:item/hand_cards`）仍須完整。教訓：別把「不補名字」誤推成「整個 item 可無視」。

---

## 逐 phase 坑帳

> 格式：**坑** ｜ 模式 ｜ 盲點（當時為何踩）｜ 代價 ｜ 攔截點 ｜ →learnings 對應條。

### phase-0（環境 + 攤平 + 爆 error 分類）
- **P0-1 攤平 `<26.1` 切片 ≠ 1.21.11-ready** ｜【A】｜ 盲點：信 prompt §2.4「作者已把 render-state 寫在 `>=1.21.2`」｜ 代價：差點整體假設可用｜ 攔截：抽驗 BlockEntityMinoTable L44-49 ｜ →learnings phase-0「攤平切片≠1.21.11-ready」
- **P0-2 stonecutter 無 chisel-dump、就地重寫 src** ｜【D】｜ 盲點：prompt §7 預設有 chisel-to-clean-source 指令｜ 代價：照找會撲空｜ 攔截：Stage-0 盤點機制｜ →learnings phase-0「攤平=切 active target」
- **P0-3 `>=1.21.2` 只碰 registry、render/NBT 在 `>=26.1`** ｜【A】｜ 盲點：同 P0-1｜ 攔截：盤點 12 處實查｜ →learnings phase-0「render/NBT 不能靠攤平免費取得」
- **P0-4 stonecutter 切 active target = 就地改 upstream（違反唯讀）** ｜【D①】｜ 盲點：以為「切換+還原」唯讀安全｜ 代價：差點當下違反唯讀護欄｜ 攔截：eng-review（自 lens2 + codex 一致）｜ →learnings phase-0「設定操作也可能是寫入操作」
- **P0-5 build 設定 skill-forbidden vs 筆記-forbidden 180°撞、傾向信 skill** ｜【C】｜ 盲點：skill version-stamped + 解釋具體 → 傾向信｜ 代價：照任一方盲建會卡 class_310 或版本不符一輪白工｜ 攔截：去抓官方 fabric-example-mod raw（第三方硬 ground truth）｜ →learnings phase-0「兩來源對撞去找第三方」
- **P0-6 PowerShell Copy-Item 資料夾多一層** ｜【D③】｜ 盲點：同指令對 data 夾正確、對 assets 夾多一層（差在目標夾存不存在）｜ 攔截：搬完 Glob/tree 驗結構｜ →learnings phase-0「Copy-Item 行為取決於目標夾存不存在」
- **P0-7 runClient cache 缺 intermediary、偽裝成配置錯（build 綠騙過）** ｜【B+D②】｜ 盲點：循「bare mapping 錯？split 害的？」兩條配置假設追｜ 代價：一度想改對的 bare 配置｜ 攔截：clone 官方原樣對照（也炸）+ `--refresh-dependencies`｜ →learnings phase-0「build綠≠runtime綠、官方原樣對照」
- **P0-8 base=B 混合基準、fabric-api 7 個要回退** ｜【A+E】｜ 盲點：fabric-api `package does not exist` 形狀像 API 移植難點、差點歸 Phase 1 硬啃｜ 代價：差點誤當真 MC API 移植｜ 攔截：jar class entry + javap 0.141.4 + upstream 註解三方驗｜ →learnings phase-0「base=B 混合基準」
- **P0-9 pack.mcmeta 純 Fabric 不需要（非漏搬）** ｜【E】｜ 盲點：conventions 母數列 templates=2、判 5a 漏搬｜ 代價：差點盲補一個可能踩 1.21.11 schema 坑的檔｜ 攔截：對官方 fabric-example-mod 模板核對｜ →learnings phase-0「區分 build-template vs runtime-required」

### phase-1（server 編譯對齊）
- **P1-1 查證工具鏈三斷頭（decompile MCP 壞 + javap 撈不到 mojmap + 改用編譯器）** ｜【B】｜ 盲點：plan 排「動工首步 decompile confirm」，兩條預先查證路徑都斷｜ 代價：查證計畫卡住｜ 攔截：改「直接改→compileJava」二元查死｜ →learnings phase-1「編譯器是最硬且永遠在的 ground truth」

### phase-2（client/render，2026-06-24 本 session）★坑的集大成
- **P2-1 yarn 結構 ≠ mojmap code，差點照 yarn 名整批改 code** ｜【A×B×C 三因子疊加】｜
  - **盲點**：前 session spike 用 MCP **yarn 源**看結構（因 mojmap 路徑壞 = 模式B），把 yarn↔mojmap 命名差異（render/OrderedRenderCommandQueue/collectVertices/updateRenderState…）誤當成 26.1→1.21.11 的 API 變化（逆向 port = 模式A）；spike + structured-plan-review + 學長 v2 拍板**三關都沒抓到**（多源異口同聲 = 模式C）。
  - **真相**：mojmap 的 submit/SubmitNodeCollector/getExtents/extractArgument/type/extractRenderState **26.1↔1.21.11 全沒改名**；A 真實差異 = submit 補 ItemDisplayContext + Unbaked 去泛型，B 介面與現狀**全等**。
  - **代價（被擋下，未發生）**：照 v2 會把一堆 mojmap 不存在的名字寫進 code → 整批編譯失敗、白繞數趟 build；且 plan 範式①主軸（A+B 共用）整條建在錯誤前提上。
  - **攔截點 ★**：workflow carve-out（「目標版本 API 簽名一律自查、100% cross-check agent/spike」）→ 動工前 javap minopp link 的 mojmap jar，並用 **phase-0 已記錄的實測編譯 error log × javap 三組交叉驗證**確認 jar 對。→ 角色閘「偏離 plan 字面前先喊」→ 學長拍「先更新 plan 再動工」。
  - →learnings phase-2「yarn 結構≠mojmap code」｜ →api-changes「SpecialModelRenderer/BlockEntityRenderer javap 實證」
  - **檢討大會看點**：這一坑是四因子的活標本——它同時是逆向 port（要退回）、工具半殘（逼用 yarn）、多源異口同聲（三關沒抓），**唯一擋住的是「不信 spike、自己 javap」這條 carve-out 紀律**。若沒這條紀律，這是會真的釀成白工的坑。

### phase-2b（mixin runtime 對齊 + E3 視覺驗收，2026-06-25）★runtime 車道逮洞集大成
- **P2b-1 mixin `@Local` by-name 在 bare mojmap 失配（runClient 啟動即 crash）** ｜【D② + 跨環境 mapping 名】｜ 盲點：upstream `@Local(name="wheel")` 在它的 Parchment（NeoForge）環境有效，搬到我們 bare officialMojangMappings（無 Parchment）官方名是 `k`、對不上；build 全綠完全測不到（mixin runtime 注入）｜ 代價：runClient 進不去、E3 視覺驗收 BLOCKED（卡在收尾、未帶進 Phase 3）｜ 攔截：runClient 行為 smoke（唯一 gate）+ javap -l 看 LVT 定 `index=15`｜ →learnings phase-2b「@Local 看 LVT 型別」+ phase-2「build綠≠mixin apply」
- **P2b-2 coupon 紫黑（缺 1.21.4+ item definition）** ｜【F】｜ 盲點：coupon 有完整 ItemCoupon+texture+傳統 model+6 語言 lang，獨缺 1.21.4+ `items/coupon.json`；upstream 同樣漏（原生洞）｜ 代價：紫黑（學長 runtime 人眼逮到、未進發布）｜ 攔截：E3 視覺車道人眼｜ →learnings phase-2b「runtime 車道逮 upstream 原生洞」
- **P2b-3 mino_table recipe 1.21.11 解析失敗（ingredient 舊物件格式）** ｜【F】｜ 盲點：1.21.1 `{"item":...}`/`{"tag":...}` 1.21.11 失效；upstream 同款（原生洞）；loot_table 無此病｜ 代價：方塊桌生存合不出（log ERROR 逮到、未進發布）｜ 攔截：runClient log 掃 ERROR｜ →learnings phase-2b「runtime 車道逮 data 格式漂移」
- **正面攔截（紀律奏效，檢討大會看「什麼擋住了」）**：
  - ① **javap -l cross-check 推翻中途 ordinal=1 誤判**：純 -c 看 istore 以為 slot 7+15 兩 int → ordinal 1；javap -l 證 slot 7 是 boolean(`Z`) → 唯一 int 是 slot 15 → ordinal 0，改用更穩的 `index=15`。carve-out「自查目標簽名」+ structured-plan-review lens2「自讀 ground truth」奏效。
  - ② **runtime 行為+視覺車道一口氣逮 2 個 upstream 原生洞**（coupon+recipe）——green build 全綠都看不到，是 runtime 車道存在價值的強案例（模式 F 攔截點）。
  - ③ **誠實更正 Glob No files found 假象**：基於假 No files 推「datagen 生成」，重查 `**/` pattern 自我推翻、當場對學長更正，沒讓錯結論進收尾（防幻覺紀律奏效）。

### phase-3（runtime 完整驗證，2026-06-25）★新根因類【G】測試 harness／環境坑（A-F 都是 port code，這是首次「坑全在測試環境、非 mod」）
> Phase 3 **0 code change**、五子批全綠；逮到的「坑」**沒一個是 minopp**——全是 dev server 測試環境與協作模式。
- **P3-1 agent background server 跑多人測試＝低效死路** ｜【G 測試協作模式】｜ 盲點：想用 agent 的 background task 跑 dedicated server 驗 S5 多人｜ 病灶：① 搆不到 server console（background 無互動 stdin、`gradlew runServer` 吃掉 console）→ 不能 `op`、只能改 ops.json 重啟；② 每改一個 server 設定都殺 server＋重發＋學長重連、來回 5+ 次｜ 代價：大量來回空轉、學長喊停「效率太差我自己起」｜ 攔截：學長喊停→改使用者本機自起（有 console）｜ →learnings phase-3「多人測試別用 agent background server」
- **P3-2 dev dedicated server 環境設定串擋測試** ｜【G】｜ 盲點：以為 server 起來就能玩｜ 病灶：`eula` → `online-mode`（離線帳號被擋）→ `enforce-secure-profile`（離線無簽名）→ `spawn-protection`（非 OP 不能互動方塊）一連串、一個個撞｜ 代價：每關一次重啟＋重連｜ 攔截：逐個解；教訓＝多人測試前一次鋪好（learnings phase-3 清單）
- **P3-3「入座要 OP」誤判 minopp（真因 vanilla spawn-protection）** ｜【G + 歸因】｜ 盲點：學長報「入座要 OP」、直覺像 minopp 權限設計｜ 真相：查 `BlockMinoTable.useWithoutItem`（入座/開座位 GUI）**零 permission 檢查**；真因 vanilla `spawn-protection=16`（牌桌在出生點 16 格內、擋非 OP 互動方塊）｜ 代價：差點誤把 vanilla 機制當 mod 鍋｜ 攔截：查 code 排除 minopp→指 spawn-protection→學長擺遠坐實｜ →learnings phase-3「多人『要 OP』先查 mod code」
- **正面攔截**：① **0 code change＝逮到的洞全非 minopp**——dev 環境串＋vanilla 機制，minopp 本體零問題（驗證 phase 的乾淨結果）；② **忠實 port 邊界全守住**——A/B/ActionMessage/nobewlr 查證後全保留（玩家無感、非洞），ActionMessage JSON-in-NBT 還 runtime 實測過；③ **S1 首步查 setup 順帶撈到 plan 漏的 4 個玩法功能**（award/demo/shout/cut）——decision 8「先查 setup」的價值實證。

---

## 待檢討大會回答的後設問題（隨 port 累積）
1. base=B 的退回代價，Stage-0 選 base 時能更早估出多少？（模式 A）
2. 工具鏈半殘該「早修一次」還是「整 port 繞」？累積繞路成本 vs 修 MCP 成本。（模式 B）
3. 哪些坑是「純推理可預防」、哪些「只能動手後驗證才抓」？驗證紀律投報率最高處在哪？（模式 D）
4. carve-out（自查目標版本簽名、不信 agent/spike）擋下幾次真實白工？這條紀律的成功度量。（P2-1 是強案例）
5. **忠實 port 的邊界**——upstream 原生洞（模式 F）「順手修」vs「保持一致」怎麼拿捏？能否在搬入時就對目標版本格式 schema 預檢、不等 runtime 才逮？（phase-2b coupon/recipe 拍板「**修**」；phase-3 規劃期 nobewlr lang 拍板「**不補**」——一正一反夾出判據雛形「**玩家正常遊玩可達性**」：可達且壞才修、不可達的露 key/缺名不補。但「修 upstream bug」是否每次都該做、會不會偏離忠實複製，仍值得回顧；模式 F「反向校準」已記此案例）。phase-3 驗證又添佐證：A/B/ActionMessage/nobewlr 全因「玩家無感」保留，邊界判據站得住。
6. **多人 / dedicated server 的 runtime 測試協作模式**（模式 G）——agent background server 被證實低效死路（P3-1：搆不到 console、改設定狂重啟、使用者重連 5+ 次）。「**使用者本機自起 ＋ agent 預鋪 server 設定 ＋ 給清單收回報**」是否該定為多人測試的標準協作模式？單機 runClient（agent 跑、使用者玩同畫面）vs 多人（server＋多 client＋console 操作）的協作分界線在哪？
