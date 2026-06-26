# Changelog — minopp port

> 每 phase 收尾記一段：做了什麼、關鍵決策、產出。細節在 plans/ + api-changes + learnings。

## phase-3 runtime 完整驗證（2026-06-25 收尾）= port 主體完工
Phase 0/1/2 build 綠 + phase-2b runClient 能開局後，本 phase 深測完整玩法 / Bot / NBT 存讀 / 視覺 / 多人，逮 build-綠-騙不過的 runtime-only 洞。**全程 0 行 code change**——純驗證通過，逮到的「洞」全是 dev server 環境設定與 vanilla 機制，沒一個是 minopp 的。

- **S1 玩法**：完整 UNO 局（數字/SKIP/REVERSE/DRAW+2/WILD/WILD+4）、喊 Mino 三路徑（chat/keybind/`/minopp shout`）、doubt 抓賴、cut 插隊、勝負、破壞牌桌掉落——學長實玩全綠。WILD+4 forbid（`CardGame:84-91`）/ deck depleted（`CardGame:209-216` reshuffle 後 panic）走**邏輯審查**（minopp 無配牌 debug 指令、無法 runtime 穩定 setup）。**S1 首步查 setup 順帶撈到 plan 漏的玩法**：`/minopp set_table_award`（贏家獎勵，領獎 path=`GrantRewardEffectEvent` `CardGame:98`）、`set_table_demo`（展示桌擋開局）、`/minopp shout`、`cut` 插隊（`CardGame:77`，定位多人彩蛋，對 bot 出牌快窗口小）。
- **S2 Bot＋config**：五開關 NoWin/NoPlayerDraw/ForgetChance（0%/100% 兩端）/NoDelay/StartGame 實測行為對；YACL fallback（`S2CAutoPlayerScreenPacket:50-53`）走邏輯審查（dev 固定有 YACL）。
- **S3 NBT 存讀**：牌局進行中退出重進，手牌/檯面/輪到誰（主場景、S1 順手過）＋方向/棄牌堆/HUD 訊息列/Bot config（細節）全還原。**`ActionMessage` JSON-in-NBT 實測通過**——坐實 decision 4「保留不改」（同版本對稱穩）。
- **S4 視覺**：完整局＋render edge（GUI scale/graphics mode/亮暗/reload）/Screen 三態/多語言 zh_tw/音效字幕，學長人眼全綠。**B TODO（render 萃取）牌堆出牌瞬間不閃 → 定案保留**（玩家無感、忠實 port）。
- **S5 多人冒煙**：dedicated server 0 ERROR ＋ 雙 client 連上輪流玩（A1）/ server restart＋斷線重連續玩（A2）/ 雙 client 同步（A 出牌 B 即時見、私有手牌不互看，A3）/ 手持 item server 不炸（A4）——學長本機自起 server＋兩 client 驗，全綠。
- **忠實 port 邊界守住**：A（server 防呆）/ B（render 萃取）/ `ActionMessage`（JSON-in-NBT）/ nobewlr lang 全保留——查證皆「玩家無感、非洞」。
- **驗收**：五子批全綠、全程 log 0 ERROR；WARN 白名單＝YACL refmap（dev）＋ online-mode offline（dev 故意）。dedicated server 載 minopp 啟動乾淨，實錘 codex#6「dedicated server 載 minopp 炸」沒發生（A classloading lazy-load 駁過度成立）。
- **逮到的「洞」全非 minopp**：dev server 環境串（eula / online-mode / enforce-secure-profile / spawn-protection）+「入座要 OP」誤判（真因 vanilla spawn-protection、入座 code 零權限）——詳 learnings/pitfall-log phase-3。
- **意義**：verification-heavy / code-light phase 的典範——0 code change、純驗證，confirm port 主體玩法/視覺/存讀/多人全對，minopp 本體零問題。

## phase-2b client mixin runtime 對齊 + E3 視覺驗收（2026-06-25 收尾）= Phase 2 真收尾
Phase 2「編譯對齊」build 綠後，收尾 E3 runClient 啟動即 crash（MouseHandlerMixin `@Local` mojmap 失配）。本工作單元讓 runClient 起來 + 完成一直欠的 E3 視覺驗收 + 順手修兩個 runtime 才現形的 upstream 原生完整性洞。

- **mixin runtime 對齊（核心，真改只一行）**：`MouseHandlerMixin` 的 `@Local(name = "wheel")` → `@Local(index = 15)`。root cause：官方 mojmap LVT 把 `MouseHandler.onScroll` 的滾輪量 local 命名為 `k`（javap -l 實證 slot 15:I），upstream by-name "wheel" 來自 Parchment（NeoForge 環境補 local 名），我們 bare officialMojangMappings 無 Parchment → 對不上。改 `index` 直鎖 LVT slot，繞過 ordinal 的型別歧義（slot 7 是 boolean `Z`、唯一 int 是 slot 15，但 runtime 無 LVT 時 mixinextras frame 分析會把 boolean 當 int-category 數錯）。其餘 3 mixin（InventoryMixin 攤平後死碼 / KeyMappingAccessor / AbstractClientPlayerMixin）javap 證 OK，runClient `required=1` 統一蓋章（能進世界＝都 apply）。
- **E3 視覺驗收（Phase 2 核心、一直欠的）**：runClient 開完整一局 UNO（單人 + autoplayer 補人，autoplayer 右鍵啟用自動坐桌、startGame 門檻 ≥2 人）。滾輪翻牌正常、手牌牌面/方塊桌/autoplayer/HUD 全對；學長驗細（GUI scale / graphics mode / 方塊朝向 / 亮暗背景 / resource reload）零異常。
- **附帶修 1：coupon item definition**（runtime 視覺逮到的 upstream 原生洞）：`minopp:coupon` 顯示紫黑——缺 1.21.4+ `assets/minopp/items/coupon.json`（作者只給傳統 `models/item/coupon.json` + texture + 6 語言 lang，漏了新格式 item definition）。補一個指向現有 model 的 item definition（照 mino_table.json 範本）。upstream items/ 手寫 3 個、無 datagen、同樣漏 coupon。
- **附帶修 2：mino_table recipe ingredient 格式**（runtime log 逮到的 upstream 原生洞）：`recipe/mino_table.json` 的 key ingredient 用 1.21.1 物件格式 `{"item":...}`/`{"tag":...}`，1.21.11 解析失敗（log `Not a string`）。改字串格式 `"minecraft:smooth_stone_slab"` / tag `"#minecraft:logs"`。upstream recipe 同款舊格式；loot_table 無此病（字串 name + 標準 conditions）。
- **驗收**：runClient 進世界、滾輪翻牌、完整牌局零異常、coupon 不紫黑（學長親眼 + log 無 model error）、recipe log 0 parse error、全 log **0 ERROR**。log：run-phase2b-mixin.log / run-coupon-verify.log / run-recipe-verify.log。
- **意義**：runtime 行為＋視覺車道一口氣逮到 **2 個** green-build＋編譯全綠都抓不到的 upstream 原生完整性洞（coupon item definition、recipe ingredient 格式）——playbook blind spot 的實證。

## phase-2 client/render 編譯對齊（2026-06-24 收尾）
消掉 client/render 35 個編譯 error（A 手牌 7 + B render-state 9 + C HUD/Screen 19），compileJava + 完整 build 綠。**真實工作性質 = 26.1→1.21.11 逐個 class/泛型/命名小修**（非 render 範式大重構——v3 動工前 carve-out javap 推翻了 v2「範式①」的 yarn 誤判）。

- **A 手牌（HandCardsSpecialRenderer + MinoFabricClient）**：`SpecialModelRenderer.submit` 補 `ItemDisplayContext` 第2參（body 一行不動）+ `Unbaked` 去泛型；`MinoFabricClient` 的 `ID_MAPPER.put` 去泛型後型別自動對齊。35→28。
- **B render-state（BlockEntityMinoTableRenderer + EntityAutoPlayerRenderer）**：周邊 class 命名退回——`CameraRenderState` 去 `.level`（`renderer.state.level`→`renderer.state`）；`LightCoordsUtil.FULL_BRIGHT`→`LightTexture.FULL_BRIGHT`（26.1 改名、1.21.11 未跟，同 fabric-api/interact 模式）。介面層（submit/createRenderState/extractRenderState）javap 證與現狀全等、不動。28→19。
- **C HUD+Screen（GuiShim/GameOverlayLayer/SeatControlScreen/WildSelectionScreen）**：範式② C1 javap 重驗推翻 v2 自讀推測——型別 `GuiGraphicsExtractor`→`GuiGraphics` 對，但 body 有**二階命名陷阱**：`text`→`drawString`、`centeredText`→`drawCenteredString`、Screen `extractBackground`→`renderBackground`（26.1 新名 vs 1.21.11 舊名）。`fill`/`blit`/`pose`（Matrix3x2fStack）不變。19→0。
- **關鍵救援**：C1 強制 javap 擋下範式②「換型別後才暴露的 method 名」二階陷阱（v2 eng-review 只驗型別存在、沒驗 method 簽名）。HUD callback method-ref + Matrix3x2fStack 靠 compileJava 兜底通過。
- **驗收**：compileJava 35→0 + 完整 build SUCCESSFUL；E1 work-marker 0 新增（BlockEntityMinoTableRenderer:256 / ItemHandCards:101 兩個 upstream TODO 留 Phase 3）；E2 client 邊界乾淨（render 類只被 client entrypoint 引用）；E3 完整性車道 GUI texture（deck/arrow_down）齊。**⚠️ E3 正確性車道：runClient 啟動即 crash——`MouseHandlerMixin` 的 `@Local(name="wheel")` 在 mojmap 環境找不到 local、runtime mixin 注入失敗（非編譯問題、非本 phase 引入，phase-0 搬入未驗 runtime）。視覺驗收 BLOCKED；mixin runtime 對齊 + E3 歸新工作單元（學長拍板「記錄後 handoff」）。詳見 STATUS ⚠️專段 + learnings「build 綠≠mixin apply」。**
- **查證**：全程 javap minopp link 的 mojmap jar（`...layered+hash.2198-v2`）逐項釘死，不信 yarn/自讀推測。log：`compile-phase2-A/B/C-*.log`、`build-phase2-final.log`。

## phase-1 server-side 編譯對齊（2026-06-24 收尾）
消掉 server 3 個編譯 error，server-side 對齊 1.21.11。build 仍非綠（client 35 個屬 Phase 2，單 source set 整包編譯擋住，故 phase-1 無 runtime 驗收）。

- **修法（皆「26.1 加/改、1.21.11 未有」→ 退回 `<26.1`；逐 API 判斷、非全域規則）**：
  - `EntityAutoPlayer.interact`：26.1 的 3 參數 `(Player, InteractionHand, Vec3)` → 1.21.11 的 2 參數 `(Player, InteractionHand)`（編譯器 `required` 親證）。
  - `PlayerShim.sendSystemMessage`：`player.sendSystemMessage(Component)`（1.21.11 無）→ `player.displayClientMessage(message, true)`（編譯器查死存在）。
- **NBT 未知解除**：BlockEntityMinoTable/EntityAutoPlayer 的 `ValueInput/ValueOutput` NBT 簽名編譯通過 = 1.21.11 對齊、不需改；順手補回 `EntityAutoPlayer` 兩個 NBT method 的 `@Override`（codex+self-review 一致，強化編譯器保證、消 silent-failure 風險）。runtime 存讀正確性留 Phase 3。
- **stonecutter `//?` 清理**：選 C（改哪清哪）——清掉動到的 3 處（interact + 2 NBT method）+ PlayerShim sendSystemMessage 殘留；其餘 shim 的 `//?` 留收尾全面清。
- **驗收**：compileJava，3 個指定 server diag 精確消失 + 無新增 server diag + 剩餘逐條全 client/render（35）。log `compile-phase1-after.log`。
- **查證波折**：decompile MCP 壞（缺 bundled jar）、javap 撈不到 mojmap jar（cache 是 layered/intermediary 命名）→ 改編譯器查死（見 learnings）。

## phase-0 Stage-0 scoping spike（2026-06-24 收尾）
建好 Loom 1.21.11 Fabric 環境、定攤平起點、攤平複製、walking skeleton、爆+分類 1.21.11 API error 全貌。

- **環境**: 官方 fabric-example-mod 1.21.11 配置（`fabric-loom-remap` + bare `officialMojangMappings` + 單 source set，不採 skill 的 layered/split）。loader 0.19.3 / loom 1.17.12 / fabric-api 0.141.4+1.21.11 / YACL 3.8.1+1.21.11-fabric。runClient 空殼進主畫面（cache 缺 1.21.11 intermediary 用 `--refresh-dependencies` 補）。
- **攤平**: base=**B**（26.1.2-fabric 新碼）唯讀複製 60 class（54 共用 + 6 fabric）；丟 15 neoforge。base=B 經 5b 編譯器證實站得住（證偽閘未觸發）。
- **完整性**: 60 class + 資源母數核對通過（15 package 分佈全對、neoforge 0 殘留）。pack.mcmeta 純 Fabric 不需要、不補（官方模板實證）。
- **API error 全貌**: 178（搬 60 class）→ 45（加 YACL，補缺依賴雜訊）→ 38（修 fabric-api 7 個命名回退）。38 全真 MC API，Phase 1(3)/Phase 2(35) 切分（見 STATUS / api-changes）。
- **關鍵決策**: 跳 javap 直接拍 base=B（三方獨立佐證）；YACL 雙源驗版本選 3.8.1（3.8.2 未上 isxander maven）；fabric-api 7 個回退 `<26.1` 命名（itemgroup/keybinding/playC2S，jar class entry + javap + upstream 註解三方驗）；pack.mcmeta 跟官方不補。
- **學習**: base=B 混合基準（MC vs fabric-api 演進不同步）、pack.mcmeta 純 Fabric 非必要（見 learnings）。
- **產出**: 可 runClient 的環境 + 60 class 攤平 + 乾淨的 38 真 MC API error 分類（Phase 1/2 切分依據）。
