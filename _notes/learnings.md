# Learnings — minopp port

> 踩坑→規則，當下 append。每條帶「坑→規則」可追溯。晉升節奏見 workflow skill 節6。
> **跨坑根因模式 / 檢討大會素材**另見 `pitfall-log.md`：本檔記「坑→規則」（可操作紀律），那檔記「坑→病因模式」（結案回顧）。每 session 收尾兩邊都補。

## phase-0 2026-06-23
- **坑**: prompt §2.4 推測「作者已把 1.21.2+ render-state 適配寫在 `>=1.21.2` 分支，攤平 `<26.1` 切片 ≈ 可用」。抽驗 `BlockEntityMinoTable.java`（L44-49）發現：NBT 新 API（`ValueInput`/`ValueOutput`）綁在 `>=26.1` 分支，`<26.1` 用 1.21.1 時代的自寫 shim `platform.multiver.ValueOutput`。
- **規則**: 攤平 `<26.1` 切片 **≠** 拿到 1.21.11-ready code。作者主分界是 `26.1`（為銜接 26.x），1.21.2~1.21.11 的 API 變化大多落在 `<26.1` 分支內、未適配。**「作者看起來寫好了」要逐檔 javap/runtime 驗，不可整體假設**（呼應 playbook 心法5：信校驗不信宣告）。
- **適用層**: 專案（此 port 的 upstream 結構認知）

## phase-0 2026-06-23（Stage-0 盤點後補）
- **坑**: prompt §7 以為「借 stonecutter chisel dump 出攤平 source」。盤點發現 Stonecutter 沒有 chisel-to-clean-source 動作——它是「設 active target → 就地重寫 `src/`」，無獨立輸出。若照 prompt 找那個 chisel 指令會撲空。
- **規則**: 攤平 = 切 active target（`src/` 就地攤平；upstream 當前已是 26.1.2-fabric）。攤平本身不難，真功夫在「選起點切片 + 校準 1.21.11」。
- **適用層**: 專案
- **坑**: prompt §2.4 推測作者 `>=1.21.2` 分支含 render-state 適配。盤點證實 `>=1.21.2`（12 處）只碰 registry setId + 翻譯鍵 + neoforge 封包，**render/NBT 適配全在 `>=26.1`**。
- **規則**: 1.21.11 的 render/NBT 不能靠攤平 `<26.1`（=1.21.1 老碼）免費取得；要嘛從 1.21.1 自爬，要嘛參考 `>=26.1`（26.1 新碼）降級——**選哪個須 javap 1.21.11 後定**。呼應 playbook 心法5：作者「看起來寫好了」要逐塊驗、不可整體假設。
- **適用層**: 專案

## phase-0 2026-06-24（eng-review 後補）
- **坑**: phase-0 plan v1 寫「攤平時切 upstream active target、用完還原」，自以為唯讀安全。eng-review（自我 lens2 + codex 一致）抓到：切 active target = stonecutter **就地重寫 upstream src/**，當下即違反唯讀；且「還原」本身是風險來源。
- **規則**: 「設定/切換操作」也可能是「寫入操作」——stonecutter 切 target 會改 `src/` 檔。涉及唯讀資源的任何「臨時切換」前，先確認該操作是否就地改檔；要切就在 disposable copy（worktree/另複製）做。
- **適用層**: 方法論候選（stonecutter / 條件編譯類 port 通用）——晉升全域前須問使用者（後果閘）。先記專案層。
- **救援時點**: eng-review gate（cross-model）擋下一個會違反唯讀護欄的設計（餵 DESIGN §12.3 成功度量）。

## phase-0 2026-06-24（跳 javap 決策）
- **坑**: plan v2（採 F2）排了完整 javap 偵察逐塊判 base + 列 override。動工前重審發現 javap 兩職責可拆：(a) 判 base A/B、(b) 逐塊產 override；而 (b) 與步驟5b「硬編 60 class 讓編譯器爆 error」資訊重疊、編譯器還更硬。
- **規則**: 查證步驟動手前先問「它扛幾個職責、有沒有更硬的工具已涵蓋其中一部分」。① base 大方向靠**多源獨立佐證**（我方食譜 + 外部對話 + 原傾向三方一致）即足，不必 javap confirm；② 逐塊 API 簽名靠**編譯器逐行爆**比人眼 javap 更硬更徹底、且本來就要做（5b）。把「人眼偵察」換「編譯器兜底」＝查證總量不減、工具升級、成本大降。**前提護欄**：跳的底氣來自 base 真有多源佐證；單一來源、尤其「一隊 AI 對冷門新版本異口同聲」不算數（那是共享盲區，呼應 playbook 心法5）。
- **適用層**: 方法論候選（條件編譯類 port 通用：javap-vs-編譯器、人眼偵察 vs 編譯器兜底的取捨）——晉升全域前須問使用者（後果閘）。先記專案層。

## phase-0 2026-06-24（build 設定三方查證）
- **坑**: 動工建環境前 invoke fabric-1.21.11-port skill，發現它的 Forbidden 清單（必用 layered mapping / remap / split）與專案 _notes 的 forbidden（禁這些）180 度對撞。我一開始因 skill version-stamped + 解釋具體，**傾向信 skill**——這個傾向本身也是個坑。
- **規則**: ① 兩個來源對撞時，別在兩者間二選一——**去找第三方更硬的 ground truth**（這裡＝官方 fabric-example-mod 1.21.11 分支，親抓 raw 確認）。結果 skill 與筆記**各錯一半**：筆記禁 remap/split 是錯的（官方用）；skill 的 layered mapping + 寫死版本(0.19.2/1.16.2)也與官方(bare officialMojangMappings + 0.19.3/1.17-SNAPSHOT)不符。② **「解釋具體」≠「解釋正確」**——skill 的「bare 會炸 class_310」講得很細，但查無官方佐證、官方正是用 bare。技術故事的細緻度不是可信度。③ 同一專案多份筆記抄同一個未驗來源，會「異口同聲」放大錯誤（呼應 playbook 心法5：信校驗不信宣告）。
- **救援時點**: 動工前 invoke skill 對 ground truth + workflow 三方對抗查證，擋下「照任一方 forbidden 盲建 → 卡死 class_310 或版本不符」一輪白工。
- **適用層**: 方法論候選（任何「skill/筆記宣稱的 build 配置」都該對官方模板驗）——晉升全域前問使用者。先記專案層。

## phase-0 2026-06-24（步驟1 build 綠，決策被實機驗證）
- **坑**: 照官方模板 bare officialMojangMappings + loom 1.17-SNAPSHOT 建環境，**BUILD SUCCESSFUL（1m22s）**——實機證實「照官方 bare」對、skill 的「bare 會炸 class_310 需 layered」是誤診。前面三方查證的拍板被 build 蓋章。loom 1.17-SNAPSHOT 解析為 1.17.12。
- **規則**: 「對官方模板查證 → 拍板 → 實機 build 驗證」這套走完＝決策有地基、不是紙面推論；residual unknown（版本相容/bare）由 build 一次清掉。賭官方對贏了，但贏的是流程不是運氣。
- **known issue（記著別困惑）**: build 過程 5 個 Fabric API **sources jar** remap 失敗（`Could not remap fabric-*-sources.jar fully!` + `org.cadixdev.mercury` NullPointerException）——是 loom 1.17 + `withSourcesJar()` 對部分 Fabric API 模組 sources 的已知毛病，**sources-only、不影響編譯/主 jar/runClient**。日後 IDE 看不到那幾個模組原始碼、或再見此 NPE，是同一件事、非新 bug。要根治可考慮關掉依賴 source 下載（但通常忍著就好）。
  - ⚠️ **2026-06-24 步驟5a 修正**：此「無害」判斷有誤。這個 mercury NPE / `class_2960 cannot be resolved` **不是無害 sources-only**，而是 cache 缺 1.21.11 intermediary 的早期徵兆，與後來 runClient crash 同根。`--refresh-dependencies` 補 cache 後應一併消失（待下次乾淨 build 確認）。詳見本檔最下方步驟5a runClient 條目。
- **適用層**: 專案（loom 1.17 + 官方 1.21.11 模板的實機行為）。

## phase-0 2026-06-24（步驟5a：PowerShell 搬資料夾多一層）
- **坑**: 唯讀複製 upstream resources，`Copy-Item "$up\assets" "$new\assets" -Recurse` 把 assets 複成 `assets\assets\minopp`（多一層），但同批的 `data` 卻正確。根因：官方 fabric 模板在 `src/main/resources` 預留**空的 `assets` 夾**（Glob 只列檔案、空夾沒顯示，故沒察覺它已存在），Copy-Item 碰「目標夾已存在」→ 把來源夾整個塞進去多一層；`data` 夾原本不存在故複製正確。同指令兩種結果，差在目標存不存在。
- **規則**: ① PowerShell `Copy-Item 來源夾 目標夾 -Recurse` 行為**取決於目標夾存不存在**（不存在=內容對齊；已存在=塞進去多一層）→ 複製資料夾前先確認目標不存在，或用 `Copy-Item "$src\*"` 複製內容、或先 `Remove-Item` 目標。② **搬完一定 Glob/tree 驗結構**，別假設複製對了（這次正是驗 tree 才抓到）。③ Glob `src/**/*` 不顯示空資料夾，別拿它判斷「目標乾淨」。
- **適用層**: 方法論候選（任何 port 搬 resources、跨 OS 的 cp/robocopy 同類問題通用）——晉升全域前問使用者。先記專案層。

## phase-0 2026-06-24（步驟5a runClient：cache 缺 intermediary 偽裝成配置錯）
- **坑**: 首次跑 runClient 驗 runtime，crash 於 `NoClassDefFoundError: net.minecraft.class_2960`（intermediary 名）一整片 Fabric API entrypoint 掛。一度循「bare mapping 錯？移除 split 害的？」兩條配置假設追。
- **診斷鏈（實機對照）**: 抓官方 build.gradle 原文比對→官方也 bare、唯一差異是我移除 split；缺「含 split 能跑」對照組故不敢斷→clone 官方原樣跑 runClient→**官方原樣（含 split、bare）也同款 crash**（排除 split + 排除我們的 code）→沙箱試 layered→也炸（排除 mapping 語法）→查 cache 發現 `net.fabricmc\intermediary` 本地只有 1.21.5、缺 1.21.11（fabric maven 明明有）→沙箱 `--refresh-dependencies`→**乾淨進主畫面**→我們專案同法→通過。
- **根因**: 這台機器 gradle/loom cache 缺 1.21.11 intermediary（前次下載未完整），runtime 對不上 Fabric API 的 intermediary 名。**與 mapping 配置、source set 全無關**。
- **規則**: ① **build 綠 ≠ runtime 綠**：intermediary 缺失只在 runClient 暴露，build 不啟動遊戲故被綠燈蓋住。環境就緒必跑一次 runClient 實證 runtime，別只信 build。② **「連官方原樣都炸」= 環境/cache 信號，不是配置錯**：遇反常先 clone 官方原樣對照 + 試 `--refresh-dependencies`，別一頭栽改配置。③ `class_XXXX not found at runtime` 標準第一招 = `--refresh-dependencies` 補 intermediary cache。
- **修正前判**: (a) 前 session「bare officialMojangMappings 對、skill 的 layered 錯」——bare 確實對（refresh 後 bare 能跑）、skill layered 確錯（實測 layered 也炸）；但「build 綠＝賭贏」是假象，build 綠當時就藏著 `class_2960 cannot be resolved` 的 remap ERROR。(b) 步驟1 記的「sources-jar remap mercury NPE = 無害 sources-only known issue」——**不是無害**，是同一個 cache 缺 intermediary 根因的早期徵兆（refresh 後應一併消失，待下次乾淨 build 確認）。
- **適用層**: 方法論候選（loom port 通用：build綠≠runtime綠、官方原樣對照、refresh 排 cache）——晉升全域前問使用者。先記專案層。

## phase-0 2026-06-24（步驟5b 後：base=B 是「混合基準」，fabric-api 要回退）
- **坑**: 步驟5b 加 YACL 後剩 45 error，其中 7 個（RegistriesWrapperImpl `creativetab.v1` / ClientPlatform `keymapping.v1` / CompatPacketRegistry `PayloadTypeRegistry.serverboundPlay`）是 fabric-api package/method「不存在」。形狀像「API 移植難點」，差點歸 Phase 1 硬啃。查證（jar class entry + javap 0.141.4 實際綁定子模組 item-group 4.2.36/key-binding 1.1.7/networking 5.1.6）發現：這是 fabric-api **命名演進**——base=B 抄的 26.1.2-fabric 用「26.1 未來線 fabric-api 新命名」（creativetab/keymapping/serverboundPlay），但 1.21.11 fabric-api 0.141.4 仍是舊命名（itemgroup/keybinding/playC2S），upstream 的 `//? if <26.1` 註解早寫好舊命名。
- **規則**: **base=B 不是單一基準，是「MC API 用 B、fabric-api 子集要回退到 <26.1」的混合**。fabric-api 演進時間線跟 MC 不同步：26.1 的 fabric-api 比 1.21.11 新（已改名），但 26.1 的 MC render API 跟 1.21.11 接近。→ 遇 fabric-api 的 `package does not exist`/`cannot find symbol`，先往 upstream `<26.1` 條件編譯註解找舊命名、再對 link 的 fabric-api 版本 javap 確認，**別當 MC API 移植硬啃**。這是 base=B「站得住但有局部代價」的具體形狀（證偽閘未觸發：MC 主體仍 B 對）。查 fabric-api 子模組 exact 版本看 meta pom 的 `<dependencies>`，別用 gradle cache 多版本的版號大小猜。
- **救援時點**: 步驟5b 讀 error 時對 fabric-api 7 個先查證定性，擋下「把 fabric-api 命名 mismatch 誤當真 MC API 移植、歸錯 phase 並硬啃」一輪白工。
- **適用層**: 專案（base=B 的 fabric-api 局部代價 + 確切回退修法）+ 方法論候選（多平台/條件編譯 port：依賴 API 與 MC API 演進不同步，base 對兩者可能要分開判）——晉升全域前問使用者。先記專案層。

## phase-0 2026-06-24（步驟6 核對：pack.mcmeta 非漏搬，純 Fabric 不需要）
- **坑**: 步驟6 檔案核對發現新專案無 pack.mcmeta，而 conventions 母數列 fabric 切片 templates=2（fabric.mod.json + pack.mcmeta），一度判「5a 漏搬破洞」要補。查證（github API 列官方 fabric-example-mod 1.21.11 `src/main/resources`）發現：官方純 Fabric 模板**根本不帶 pack.mcmeta**（只有 assets/ + fabric.mod.json + modid.mixins.json），且我們 5a runClient 無 pack.mcmeta 也進主畫面。
- **根因**: pack.mcmeta 是 upstream **modstitch 多平台** template（NeoForge/Forge 需 resource pack metadata）；純 Fabric 的 assets+data 靠 fabric-loader（fabric-resource-loader）自動生成 pack metadata 載入，**不需手寫 pack.mcmeta**。母數把「modstitch templates 目錄有的」當「純 Fabric 必需」=角度誤記（非真漏搬）。
- **規則**: 盤/核對完整性母數時，**區分「upstream 多平台 build 系統有的檔」vs「目標切片 runtime 實際需要的檔」**——modstitch/architectury 的多平台 template（pack.mcmeta、mods.toml）對純 Fabric 未必必要。判「漏搬」前先對**官方目標-loader 模板**核對該檔在不在（ground truth），別只憑 upstream 母數推。完整性結論「缺 X 是否破洞」若一時難定，留 runtime 車道（Phase 3 runClient 渲染真方塊）最終蓋章、別 silent 放過也別瞎補。pack.mcmeta 萬一要補：pack_format=75（1.21.11 version.json resource_major，注意 1.21.9+ major.minor 格式）。
- **救援時點**: 步驟6 檔案核對抓到 pack.mcmeta 缺，且進一步對官方模板查證、擋下「盲目補一個格式可能踩 1.21.11 schema 坑的 pack.mcmeta」。
- **適用層**: 專案（minopp 純 Fabric 切片不需 pack.mcmeta）+ 方法論候選（多平台→單平台 port：母數區分 build-template vs runtime-required）——晉升全域前問使用者。先記專案層。

## phase-1 2026-06-24（查證工具鏈 fallback：編譯器是最硬且永遠在的 ground truth）
- **坑**: phase-1 plan 排「動工首步 decompile 1.21.11 mojmap confirm `displayClientMessage`」。動工時 decompile MCP 壞（缺 bundled `mapping-io-cli.jar`）、改 javap 又撈不到——cache 的 `minecraft-merged…loom.mappings.layered` jar 是 layered/intermediary 命名，29068 entry 無 `net.minecraft.world.entity.Entity`（撈到中間產物非 bare mojmap）。兩條「預先查證」路徑都斷。
- **規則**: 查 API 存在性/簽名，工具鏈有 fallback、**編譯器排最後但最硬且永遠在**：對「API 在不在」這種二元問題，與其預先 decompile/javap，不如「直接改成目標寫法 → compileJava」——`cannot find symbol`＝不存在、編過＝存在且簽名匹配，且這是專案**實際 link 的環境**（比反編譯/外部 jar 更貼）。decompile/javap 的價值在「改之前先看全貌」（列 enum、找替代 API）；二元存在性問題編譯器更省更硬。呼應 phase-0「編譯器比人眼 javap 更硬」，再加一層「編譯器永遠在、是 fallback 終點」。**前提**：改動可還原（Edit 可逆）才敢「改後即驗」。
- **救援時點**: structured-plan-review 的 codex+self 一致抓到「EntityAutoPlayer NBT 缺 @Override 的 silent-failure 風險」→ phase-1 補回 @Override → 編譯器確認簽名真匹配（餵 DESIGN §12.3 成功度量）。
- **適用層**: 方法論候選（任何 port 的 API 查證工具鏈：decompile→javap→編譯器 fallback）——晉升全域前問使用者。先記專案層。

## phase-2 2026-06-24（動工前 carve-out：yarn 結構 ≠ mojmap code，差點照 yarn 名改 code）
- **坑**: phase-2 plan v2「範式①」說手牌/render-state 要把 `submit→render`、`SubmitNodeCollector→OrderedRenderCommandQueue`、`getExtents→collectVertices`、`extractRenderState→updateRenderState`、`type→getCodec`。動工前 carve-out 自查（javap minopp 實際 link 的 mojmap jar `...layered+hash.2198`）發現：左邊那些全是 **yarn 名**——前 session spike 用 minecraft-dev MCP 拉 **yarn 源**看結構（MCP mojmap 路徑壞），把「yarn↔mojmap 命名差異」誤當成「26.1→1.21.11 的 API 變化」。mojmap 真相：submit/SubmitNodeCollector/getExtents/extractArgument/type/extractRenderState **全沒改名**；真正變化只有 `SpecialModelRenderer.submit` 補一個 `ItemDisplayContext` 參數 + `Unbaked` 去泛型，而 `BlockEntityRenderer` 介面與現狀**全等**。照 v2 改 = 把一堆 mojmap 不存在的名字寫進 code、整批編譯失敗、白繞數趟 build。
- **規則**: **yarn 結構 ≠ mojmap code 名**（yarn 是綽號、mojmap 是本名，code 只認本名）。port 用 mojmap 時，查目標版本「有哪些 method / 結構」可借 yarn（MCP yarn 可用），但**寫進 code 的每個 class/method 名必須用 mojmap bytecode 釘死**——yarn↔mojmap 對同一介面的命名可差到全不同（render vs submit、OrderedRenderCommandQueue vs SubmitNodeCollector）。MCP mojmap 壞時 mojmap 名查法：① **javap loom cache 的 mojmap jar**（用含 mojmap path `net/minecraft/world/item/ItemStack.class` 辨識哪個 jar 是 mojmap；javap=`C:\Program Files\Java\jdk-17\bin\javap.exe`）；② 編譯器兜底（但 class 名兜不出、只兜 method 簽名 → class 名優先 javap）。呼應「26.1 食譜≠1.21.11」「信校驗不信宣告」「編譯器最硬」。
- **救援時點**: workflow carve-out（「目標版本 mapping/javap/簽名一律自查、100% cross-check agent/spike」）+ 「先更新 plan 再動工」角色閘——擋下照 v2 yarn 名盲改整批編譯失敗，並揪出 plan 範式①主軸（A+B 共用）需重寫。
- **適用層**: 方法論候選（mojmap-based port 用 yarn 工具偵察的「結構可借、名必查」紀律）——晉升全域前問使用者。先記專案層。

## phase-2 2026-06-24（收尾 retro：範式②「換型別後才暴露的二階命名陷阱」）
- **坑**: C HUD/Screen 動工，plan v3 已知「型別 `GuiGraphicsExtractor`→`GuiGraphics`」（v2 自讀推測、C1 待 javap 驗）。C1 javap 重驗發現：型別換對只是**一階**；換成 GuiGraphics 後，body 裡 26.1 的新 method 名（`text`/`centeredText`/Screen `extractBackground`）在 1.21.11 是舊名（`drawString`/`drawCenteredString`/`renderBackground`）——這些 method 陷阱**只有換完型別才暴露**（型別沒換前編譯器只報型別 cannot find symbol、看不到 method 層）。若照 v2 只換型別不改 method 名，body 會爆一輪新 cannot find symbol（範式①翻版）。
- **規則**: **「型別存在性」是一階查證、「換型別後 method 簽名對不對」是二階查證——javap 兩階一起釘，別只查型別在不在**。port 換一個中介/包裝型別（如「26.1 包裝物件 GuiGraphicsExtractor → 1.21.11 原生 GuiGraphics」）時，目標型別存在 ≠ 它的 method 名跟現狀呼叫一致；26.1 可能連 method 都改了名（text vs drawString）。動工前把「現狀呼叫的每個 method」逐一對目標型別 bytecode 驗，別停在「型別存在」就動手。補「26.1 食譜≠1.21.11」「yarn≠mojmap」之外的「型別對 ≠ method 對」這層。
- **救援時點**: plan v3「C1 動工前強制 javap 重驗範式②、別信 yarn/自讀」這個設計，擋下「照 v2 只換型別 → body method 名整批爆 → 白繞數趟 build」。配合 carve-out（mojmap 簽名 100% 自查、compileJava 逐階蓋章）。餵 DESIGN §12.3 成功度量。
- **適用層**: 方法論候選（任何 port 換中介/包裝型別：型別存在性 vs method 簽名兩階查證）——晉升全域前問使用者。先記專案層。

## phase-2 2026-06-24（收尾 E3 暴露：build 綠 ≠ mixin apply 成功，client runtime mixin 是 plan 漏切的工作面）
- **坑**: Phase 2「編譯對齊」三子批 35→0、完整 build SUCCESSFUL，自以為 client 端搞定。收尾 E3 runClient 一啟動就 crash：`MouseHandlerMixin` 的 `@Local(name="wheel")` 在 1.21.11 `MouseHandler.onScroll` 找不到 matching local（`Unable to find matching local!`，target `MouseHandler::onScroll(JDD)V` 注入點 `Inventory::setSelectedSlot(I)V`）→ mixin apply 失敗 → 遊戲 crash、視覺驗收進不去。mixin 的 Java code 編過了、build 全綠，但 mixin 是 **runtime 才注入**的，編譯器/build 完全測不到。
- **規則**: ① **`build 綠 ≠ mixin apply 成功`**——mixin（尤其 mixinextras `@Local`/`@WrapOperation`/`@Inject` 的注入點 match）是 runtime 才驗的，編譯對齊綠不代表 client 端能跑。port 切 phase 時 **client 工作面要拆「編譯對齊」+「mixin runtime 對齊」兩層**，別讓「編譯綠」假象蓋住 runtime mixin gap（這次 plan 就漏切了這層）。② **`@Local(name="...")` 在 mojmap 環境是地雷**——mojmap jar 通常剝離 local 變數名（只留參數名），upstream 在帶 local-name 環境開發的 by-name `@Local` 搬到 mojmap 會 `Unable to find matching local`；改用 `@Local(ordinal=N, type=...)` 或 `@Local(index=N)` 按型別序號/LVT 槽位抓（要 decompile target method 數 local）。③ **runClient 行為車道 smoke 是 mixin runtime 的唯一 gate**——只有真啟動遊戲才暴露，呼應 phase-0「build 綠≠runtime 綠」（那次 intermediary cache、這次 mixin apply）。
- **救援時點**: 收尾 E3 runClient（行為車道 smoke「進遊戲不 crash」）擋下「Phase 2 編譯綠就宣告 client 完成、把 runtime mixin 失敗帶進 Phase 3 才發現」。視覺車道的前置（行為 smoke）本身就是 gate。
- **適用層**: 方法論候選（mixin-based port：build 綠≠mixin apply、`@Local` by-name 在 mojmap 的通病、client 編譯/runtime 兩層切分）——晉升全域前問使用者。先記專案層。

## phase-2b 2026-06-25（@Local ordinal/index：要看 LVT 型別，別用 -c 的 istore 推）
- **坑**: 修 MouseHandlerMixin `@Local`，純 `javap -c` 看 `onScroll` bytecode 有 2 個 `istore`（slot 7、slot 15），一度判「2 個 int → wheel = ordinal 1」。
- **規則**: **`@Local` 的 `ordinal`/`type=int.class` 要看 LVT 的型別 signature（`javap -l`），不能用 `-c` 的 `istore` 推**——`istore` 對 boolean(`Z`)/int(`I`) 是同指令、分不出。`javap -l` 證 slot 7 是 `Z`(boolean)、唯一 `I` 是 slot 15 → ordinal 其實是 **0**（非 1）；ordinal 還受「runtime 有無 LVT」影響（無 LVT 時 frame 分析把 boolean 當 int-category）→ **`index` 直鎖 LVT slot 最穩、繞過型別歧義**。**附正前條**：phase-2 learnings ②「mojmap 剝離 local 名」**不準**——`javap -l` 證 jar 有 LVT（35 method），root cause 是「Parchment 名 `wheel` vs 官方 mojmap 名 `k`」不同、非剝離（詳 api-changes phase-2b）。
- **適用層**: 方法論候選（mixinextras `@Local` in mojmap port 的 discriminator 選擇）——晉升全域前問使用者。先記專案層。
- **救援時點**: structured-plan-review lens2「自己讀 ground truth」用 `javap -l` cross-check，推翻中途 ordinal=1 誤判。

## phase-2b 2026-06-25（mixin「真生效」驗證常需完整遊戲 setup，非「啟動不 crash」就完）
- **坑**: 以為 mixin 修好（index=15）+ runClient 不 crash = MouseHandlerMixin 驗完。實測「滾輪翻牌」要手持**綁定牌局的手牌**（`CardGameBindingComponent` + active game + 玩家在局裡），`/give` 空手牌必觸發不了（`handleScrollWheel` 5 道條件全不中 → false → 滾輪放行換 hotbar）。要單人 + autoplayer 開完整一局才驗得了。
- **規則**: **mixin（尤其攔截遊戲邏輯的）的「真生效」驗證，常需完整遊戲流程 setup，不是 build 綠/啟動不 crash 就完**。切 phase 估「mixin 功能驗證」工作量時，要算進「擺出觸發條件」的 setup 成本（這次 ≈ 開一局 UNO），可能跟 Phase 3「玩一局」重疊。呼應 phase-2「build 綠≠mixin apply」再加一層「apply≠真生效」。
- **適用層**: 方法論候選（mixin/行為 port 的功能驗收 setup 成本估算）——晉升全域前問使用者。先記專案層。
- **救援時點**: 學長實測 /give 空手牌滾輪沒翻牌 → 讀 `handleScrollWheel` code 釐清（非 index 抓錯、是 setup 不足，知識閘「沒查證別下斷言」），免於誤判 index=15 失敗。

## phase-2b 2026-06-25（runtime 行為+視覺車道逮到 2 個 upstream 原生完整性洞）
- **坑**: Phase 2b 收尾驗 runClient，意外逮到 2 個 green build + 編譯全綠都看不到的洞：① coupon 紫黑（缺 1.21.4+ item definition）；② mino_table recipe 1.21.11 解析失敗（ingredient 舊物件格式）。兩個對照 upstream 皆同款 → **upstream 原生洞**，非我們搬錯。
- **規則**: **runtime 行為/視覺驗收是完整性最後防線，逮得到靜態檢查（build/編譯/檔案交叉校驗）抓不到的「格式過時 / 新格式遺漏」洞**——尤其 data-driven 資源（item definition、recipe ingredient、loot table）的跨版本格式漂移，只有 runtime 載入才報。port 跨大版本（1.21.1→1.21.11）必跑 runClient 掃 log `ERROR` + 人眼視覺，別只信 build 綠。呼應 playbook「green build ≠ 完整」。
- **適用層**: 方法論候選（playbook 完整性層：runtime 車道逮 data 格式漂移）——晉升 playbook/cookbook 前問使用者。先記專案層。
- **救援時點**: E3 runClient 人眼（coupon 紫黑）+ log 掃 ERROR（recipe parse fail）逮到，免於把 2 個 upstream 原生洞帶進正式發布。

## phase-2b 2026-06-25（誠實：Glob 的 No files found 可能是路徑寫法假象）
- **坑**: 查 upstream item definition，`Glob(pattern="src/main/resources/assets/minopp/items/*.json", path=upstream)` 回 No files found，據此推「upstream items/ 空 → item definition 是 datagen 生成」。重查 `Glob(pattern="**/assets/minopp/items/*.json")` 才發現 upstream 其實有 3 個——第一次是路徑寫法假象，「datagen」推論整串錯。
- **規則**: **Glob 回 No files found 別急著當「真的沒有」往上推論**——先換 `**/` 寬鬆 pattern 或換寫法複驗，尤其當「沒有」會變成某個推論的地基。呼應防幻覺：當地基的否定斷言前對 ground truth、查不到先複驗別腦補。
- **適用層**: 方法論候選（工具使用 + 防幻覺）——晉升全域前問使用者。先記專案層。
- **救援時點**: 重查 Glob 自我推翻「datagen」推論、當場對學長更正，沒讓錯結論進收尾。

## phase-3 2026-06-25（多人 runtime 測試：agent background server 模式笨重，該讓使用者本機自起）
- **坑**: S5 多人冒煙，我用 agent 的 background task 跑 dedicated server。連環撞：① 搆不到 server console（background task 無互動 stdin、`gradlew runServer` 吃掉 console）→ 不能下 `op`，只能改 ops.json + 重啟；② 每改一個 server 設定（eula / online-mode / enforce-secure-profile）都要殺 server + 重發 + 學長重連，來回 5+ 次；③ 學長喊停「效率太差，我自己起」——本機自己起有 console，`op` 一句搞定。
- **規則**: **多人 / dedicated server 的 runtime 測試，別用 agent 的 background task 跑 server**——agent 搆不到互動 console（不能下 op/動態指令）、改設定要殺重啟、使用者反覆重連，效率極差。該一開始就讓**使用者本機自己起 server**（有 console），agent 只負責：① 預先把 server 設定鋪好（`eula=true` / `online-mode=false` / `enforce-secure-profile=false` / `spawn-protection=0`）、② 給驗證清單、③ 收回報。單機 runClient（agent 跑、使用者玩同畫面）沒此問題；多人（server＋多 client＋console 操作）才有。
- **適用層**: 方法論候選（需 dedicated server / 多 client / console 互動的 runtime 測試協作模式）——晉升全域前問使用者。先記專案層。
- **救援時點**: 學長喊停「我自己起」——把「多人測試該誰跑、環境怎麼鋪」釐清，止住 background server 的來回空轉。

## phase-3 2026-06-25（多人「要 OP 才能 X」先查 mod code，別假設是 mod 設計）
- **坑**: 學長多人測試報「玩家需要 OP 才能入座遊戲桌」，直覺像 minopp 權限設計。查 `BlockMinoTable.useWithoutItem`（入座/開座位 GUI，L170-191）發現**零 permission 檢查**——minopp 的 OP 門檻只在 award/demo 指令 + bot config（level 2），入座無門檻。真因是 vanilla `spawn-protection=16`：牌桌擺在 server 出生點 16 格內，原版 spawn protection 擋非 OP 玩家互動方塊、OP 繞過。
- **規則**: 多人測試遇「非 OP 玩家不能做 X」，**先查 mod code 那動作有無 permission 檢查，別假設是 mod 設計**——vanilla 機制（spawn-protection、op-permission-level、enforce-secure-profile）會在 mod 之外擋玩家。查 code 排除 mod 後，往 server.properties / vanilla 權限找。呼應防幻覺：歸因前對 ground truth（讀 code）、別腦補 mod 的鍋。
- **適用層**: 方法論候選（多人 mod 測試的「要 OP」歸因）——晉升全域前問使用者。先記專案層。
- **救援時點**: 查 BlockMinoTable 入座 code（無 permission）排除 minopp、指 spawn-protection；學長擺遠驗證坐實。
