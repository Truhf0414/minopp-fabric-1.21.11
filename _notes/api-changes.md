# API Changes — minopp port（1.21.1 → 1.21.11）

> 撞到的目標版本 API 改動，當下記；續接時先讀這裡、別重解。每條：症狀 → 改法 → 出處。

## phase-3：runtime 完整驗證 ＝ 0 code change、無新 API 改動（2026-06-25）
純驗證 phase，五子批（玩法/Bot/NBT/視覺/多人）全綠、**未改任何 code** → 無新增 API 改動。先前 phase 記的 API 改動（mixin `@Local` / 1.21.4+ item definition / recipe ingredient / render-state submit / NBT `ValueInput·ValueOutput` / fabric-api 回退…）runtime 全部驗證站得住。`ActionMessage` JSON-in-NBT（`ComponentSerialization.CODEC` + `JsonOps`，作者刻意 backward-compat）runtime 存讀對稱、實測通過、保留不改。逮到的環境問題（eula / online-mode / enforce-secure-profile / spawn-protection）屬 dev server 設定、非 API。

## phase-2b：mixin @Local mojmap 地雷 + 1.21.4+ item definition + 1.21.11 recipe ingredient（✅ 2026-06-25 runClient 實證）

### mixin `@Local` by-name 在 bare officialMojangMappings 失配（MouseHandlerMixin）
- **症狀**：runClient 啟動即 crash，`@WrapOperation(onScroll)` 的 `@Local(name="wheel")` → `Unable to find matching local`。
- **根因**：官方 mojmap LVT 把 `MouseHandler.onScroll(JDD)V` 的滾輪量 local 命名為 **`k`**（無語意，javap -l 實證 slot 15 sig `I`）。upstream "wheel" 來自 **Parchment**（NeoForge 環境給 mojmap 補 local 名），我們 bare `officialMojangMappings` 無 Parchment → by-name 對不上。jar 其實**有 LVT**（35 method），非「mojmap 剝光 local 名」，是「名字系統不同」。
- **修法**：`@Local(name="wheel")` → **`@Local(index = 15)`**（LVT slot 直鎖）。body 不動。
  - **不用 `ordinal`**：onScroll 唯一 `I`(int) local 是 slot 15，但 slot 7 是 boolean(`Z`)。純 `-c` bytecode 的 `istore` 看不出 boolean/int（同指令），必須 `javap -l` 看 LVT signature；ordinal 受「runtime 有無 LVT」影響——無 LVT 時 mixinextras frame 分析把 boolean 當 int-category 一起數 → wheel 飄成 ordinal 1、甚至 silent 抓錯 slot 7。index 無此歧義。
  - mixinextras 0.5.4 `@Local`：`ordinal()/index()/name()/type()/print()`（javap 實證）。沒命中可 `@Local(print=true)` 印 runtime 注入點實際 local 表 debug。
- **其餘 3 mixin runtime 健康**：InventoryMixin（攤平 base=B 後 `@Inject` 在 `//? if <26.1` 註解內、死碼 0 注入）/ KeyMappingAccessor（`@Accessor getKey` → `KeyMapping.key:InputConstants$Key` 在，vanilla 無 public getter 故 accessor 必要）/ AbstractClientPlayerMixin（`@ModifyReturnValue getFieldOfViewModifier`，1.21.11 `(boolean,float)→float` 單一 match、ModifyReturnValue 不綁參數）。runClient `required=1` 統一蓋章。

### 1.21.4+ item definition（client item model 新格式，porting 不可漏）
- **症狀**：`minopp:coupon` 物品顯示紫黑 missing model。
- **根因**：1.21.4+ item 顯示需要 `assets/<ns>/items/<id>.json`（item definition，指向 model），光有傳統 `models/item/<id>.json` **不夠**。
- **修法**：補 `items/coupon.json` = `{"model":{"type":"minecraft:model","model":"minopp:item/coupon"}}`（照現有 `mino_table.json` 範本；引用鏈 items→models/item/coupon.json→textures/item/coupon.png 全在）。
- **範圍**：upstream `items/` 手寫 3 個（hand_cards/hand_cards_nobewlr/mino_table）、無 datagen，同樣漏 coupon = **upstream 原生洞**（作者 1.21.1→1.21.4+ 升級時漏補 coupon 的 item definition）。green build + 編譯全綠抓不到，runClient 人眼才現形。

### 1.21.11 recipe ingredient 格式（物件 → 字串）
- **症狀**：runClient log `Couldn't parse data file 'minopp:mino_table' from 'minopp:recipe/mino_table.json' ... No key fabric:type ... Not a string`。
- **根因**：1.21.1 ingredient 物件格式 `{"item":"x"}`/`{"tag":"y"}` 在 1.21.11（1.20.5+ vanilla 變更）失效，要**字串格式**。
- **修法**：key ingredient `{"item":"minecraft:smooth_stone_slab"}` → `"minecraft:smooth_stone_slab"`；`{"tag":"minecraft:logs"}` → `"#minecraft:logs"`（tag 加 `#` 前綴）。`pattern`/`result`（`{"id":..,"count":..}`）不變、OK。
- **範圍**：upstream recipe 同款舊格式 = **upstream 原生洞**；`loot_table/blocks/mino_table.json` 無此病（字串 `name` + 標準 conditions、1.21.11 OK），不動。

## NBT / BlockEntity 存讀（✅ phase-1 已解：1.21.11 = ValueInput/ValueOutput，不需改）
- **BlockEntity save/load**: upstream `<26.1` 用 `CompoundTag` + 自寫 shim `cn.zbx1425.minopp.platform.multiver.ValueOutput`；`>=26.1` 用 `net.minecraft.world.level.storage.ValueInput` / `ValueOutput`（出處：`BlockEntityMinoTable.java` L44-49）。
  - **✅ 已解（phase-1 2026-06-24，編譯器證實、非 javap）**: 1.21.11 `BlockEntity.saveAdditional(ValueOutput)`/`loadAdditional(ValueInput)`、`Entity.addAdditionalSaveData(ValueOutput)`/`readAdditionalSaveData(ValueInput)` 簽名 = base=B 的 `ValueInput/ValueOutput`，**不需改**。證據：BlockEntityMinoTable 有 @Override+編過（Java 語言保證）；EntityAutoPlayer 原 active 無 @Override、phase-1 補回後仍編過（簽名真匹配、消 silent-failure 風險）。runtime 存讀正確性仍留 Phase 3。
  - **參考 commit（作者 1.21.1→26.1 遷移）**: `4dc00c3` + `57933db`（新建 `platform/multiver/ValueOutput.java` polyfill、`NbtIOShim.java`；`toTag()`→`nbtWriteTo(ValueOutput)`、`Card(CompoundTag)`→`Card(ValueInput)` 固定 pattern）。

## render-state 重構（Phase 2 參考食譜，**26.1 寫法**；1.21.11 待 javap 驗是否相同）
> 來源：作者 1.21.1→26.1 遷移 commit（upstream git）。**這是 26.1 的寫法，不保證 = 1.21.11**；Phase 2 逐項對 1.21.11 named jar javap 驗。
- **參考 commit**: `f1f70af`（BlockEntity render→submit 核心）、`9680199`（Entity render→submit）、`83f57ee`（RenderShim 手刻 renderLineBox，補回 26.1 移除的 `LevelRenderer.renderLineBox`）、`2fd1a3a`（HandCardsSpecialRenderer SpecialModelRenderer 路徑）、`d79f192`（線寬 setLineWidth）、`cd81287`（registry setId）。
- **API 改名 checklist（26.1 方向；逐項待驗 1.21.11）**:
  - BlockEntityRenderer: 單泛型 `<BE>` → 雙泛型 `<BE, RenderState>`；`render(...)` → `submit(state, PoseStack, SubmitNodeCollector, CameraRenderState)` + `createRenderState()`/`extractRenderState()`；`shouldRenderOffScreen(be)` → 去參數
  - `RenderType` → `RenderTypes`（package `net.minecraft.client.renderer.rendertype`）
  - `LightTexture.FULL_BRIGHT` → `LightCoordsUtil.FULL_BRIGHT`
  - `font.drawInBatch` → `sink.submitText`；`MultiBufferSource.getBuffer` → `sink.submitCustomGeometry(pose, renderType, (pose,buf)->...)`
  - `ItemRenderer`+BakedModel → `ItemModelResolver`+`ItemStackRenderState`（`ctx.itemModelResolver()` / `updateForTopItem`）
  - `client.model.PlayerModel` → `client.model.player.PlayerModel`；`client.resources.PlayerSkin` → `world.entity.player.PlayerSkin`；`ItemInHandLayer` → `PlayerItemInHandLayer`
  - HUD: `LayeredDraw.Layer`（<26.1）→ 無介面 class + render 吃 `GuiGraphicsExtractor`+`DeltaTracker`（>=26.1 fabric）；fabric HUD 註冊點不在 GameOverlayLayer 內，需另尋
  - `level.isClientSide`(field) → `level.isClientSide()`(getter)
- **client-only class 隔離**: 作者最後兩 commit（`006490c`/`a0dc41e`）專處理 `LocalPlayer`/`Minecraft.getInstance().player` 在 dedicated server 被誤載 → Fabric port 務必 `@Environment(CLIENT)` 隔離。

## SpecialModelRenderer / BlockEntityRenderer（✅ phase-2 2026-06-24 javap 實證 1.21.11 mojmap，推翻 plan v2 yarn 誤判）
> 上面「render-state 重構（26.1 寫法）食譜」的 1.21.11 校準結果。查證 = javap minopp link 的 mojmap jar（`minecraft-merged-1.21.11-loom.mappings.1_21_11.layered+hash.2198`，用 mojmap path `net/minecraft/world/item/ItemStack.class` 辨識）。**關鍵：plan v2 寫的「1.21.11 介面」是 yarn 名、非 mojmap；mojmap 才是 code 實際面對的（見 learnings「yarn 結構≠mojmap code」）。**
- **yarn↔mojmap 同物異名**（render 提交系統，全非 API 變化）: yarn `render`=mojmap `submit`；yarn `OrderedRenderCommandQueue`/`RenderCommandQueue`=mojmap `SubmitNodeCollector`/`OrderedSubmitNodeCollector`；yarn `collectVertices`=mojmap `getExtents`；yarn `getData`=mojmap `extractArgument`；yarn `getCodec`/`BakeContext`=mojmap `type`/`BakingContext`；yarn `updateRenderState`=mojmap `extractRenderState`。
- **SpecialModelRenderer（手牌）26.1→1.21.11 真實變化**: ① `submit` 7參→8參（補 `ItemDisplayContext` 第2參，state 後 PoseStack 前）；② `Unbaked` 去泛型（`bake` 回 `SpecialModelRenderer<?>`、`type()` 回 `MapCodec<? extends Unbaked>`）。內部 `submitCustomGeometry`（`SubmitNodeCollector` 繼承 `OrderedSubmitNodeCollector`，第2參 `RenderType`）、`ItemStackRenderState.submit(PoseStack,SubmitNodeCollector,int,int,int)` 5參 **不變**。
- **BlockEntityRenderer（方塊桌）26.1→1.21.11 真實變化**: 介面 **mojmap 全等現狀**（雙泛型 `<T extends BlockEntity, S extends BlockEntityRenderState>` / `submit(S,PoseStack,SubmitNodeCollector,CameraRenderState)` / `createRenderState()` / `extractRenderState(T,S,float,Vec3,CrumblingOverlay)` default / `shouldRenderOffScreen()`）。7 error 全在周邊 class mojmap 命名：`CameraRenderState` package `renderer.state.level`→`renderer.state`（去 `.level`，證實 api-changes「step5b C 段 `renderer.state.level` does not exist」）；`RenderTypes`(複)↔`RenderType`(單)、`LightCoordsUtil`、`BlockEntityRenderState.lightCoords` 欄位 **B 動工 javap 確認**。
- **前段「API 改名 checklist（26.1 方向）」校正**: 該 checklist 多條（submit/SubmitNodeCollector/CameraRenderState/RenderTypes/LightCoordsUtil）方向對，但**「1.21.11 的目標名」要用 mojmap、別抄成 yarn**；method 名（submit/extractRenderState）26.1↔1.21.11 其實沒變，變的是參數/泛型/周邊 class package。

## phase-2 動工實證：B 周邊命名 + C HUD/Screen 範式②（✅ 2026-06-24 javap + compileJava 釘死）
> 接上「SpecialModelRenderer/BlockEntityRenderer」段。B 周邊 class 命名與 C 範式② 動工時 javap 最終確認，全程查 minopp link 的 mojmap jar（`...layered+hash.2198-v2`，jar entry + javap）。

**B render-state 周邊 class（mojmap 釘死，編譯器 35→28→19 蓋章）**：
- `CameraRenderState`：`net.minecraft.client.renderer.state.level.CameraRenderState`（26.1）→ `net.minecraft.client.renderer.state.CameraRenderState`（1.21.11，**去 `.level`**）。jar entry 確認。影響 BlockEntityMinoTableRenderer:21/69 + EntityAutoPlayerRenderer:18/82。
- `FULL_BRIGHT`：`LightCoordsUtil.FULL_BRIGHT`（26.1，`net.minecraft.util.LightCoordsUtil`）→ `LightTexture.FULL_BRIGHT`（1.21.11，`net.minecraft.client.renderer.LightTexture`，`public static final int`）。LightCoordsUtil 整 jar 0 match=不存在；LightTexture 存在且有 FULL_BRIGHT。**26.1 改名、1.21.11 未跟**（同 fabric-api/interact）。影響 BlockEntityMinoTableRenderer:26/201/203/206/208。
- `RenderTypes`（複）/ `state.lightCoords` 欄位：**1.21.11 存在、不變**（編譯器現狀 0 報錯已證，未誤判為要改）。

**C HUD/Screen 範式②（javap mojmap 推翻 v2 自讀推測，C1 強制重驗）**：
- 型別：`GuiGraphicsExtractor`（26.1 GUI extract 物件）→ `net.minecraft.client.gui.GuiGraphics`（1.21.11）。GuiGraphicsExtractor 1.21.11 不存在。
- **method 名（26.1 新名 → 1.21.11 舊名，⚠️ 換型別後才暴露的二階陷阱）**：
  - `.text(Font, Component, x, y, color[, bool])` → `.drawString(...)`（javap：GuiGraphics 無 text、有 drawString 含 6 參 boolean overload）
  - `.centeredText(Font, Component, x, y, color)` → `.drawCenteredString(...)`
  - Screen override `extractBackground(GuiGraphics, int, int, float)` → `renderBackground(GuiGraphics, int, int, float)`（javap：Screen 有 `renderBackground(GuiGraphics,int,int,float)`、無 extractBackground）
- **不變（換型別即可）**：`.fill(int×5)`、`.blit(RenderPipeline, Identifier, …)`、`.pose()`（回 `org.joml.Matrix3x2fStack`，`pushMatrix/popMatrix/translate(x,y)/scale(x,y)` compileJava 兜底通過）。
- HUD 註冊：`HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD, id, GameOverlayLayer.INSTANCE::render)`，render 簽名 `(GuiGraphics, DeltaTracker)` 對上 fabric HUD callback（compileJava 蓋章，未另 javap fabric jar）。
- **教訓**：型別 `cannot find symbol` 是**一階**（編譯器直接報）；換型別後 method 名變是**二階**（換完才暴露）。javap 要連 method 簽名一起釘，別只查型別存在性——v2 eng-review 只驗型別、範式①②都栽在這。

## registry（1.21.2+，**已確定 1.21.11 需要**，作者已寫在 >=1.21.2 分支）
- Item/Block 的 `Item.Properties.setId(ResourceKey.create(Registries.ITEM/BLOCK, id))`；BlockItem `.useBlockDescriptionPrefix()`（取代舊 `useBlockPrefixedTranslationKey`）。出處：`GroupedItem:24`、`GroupedBlock:25`、`fabric/RegistriesWrapperImpl:40&42`。
- **1.21.4+ item model**: `assets/minopp/items/` 3 檔（select/model 結構，含自訂 special model loader `minopp:hand_cards_bewlr`）— porting 不可漏。

## phase-0 步驟5b 實測：1.21.11 API error 全貌（178 errors）〔實測 2026-06-24，完整 log: `H:\MC_Mods_Port\Minopp_Port\build-5b.log`〕
> 搬 60 class `gradlew build` 爆出。各檔分佈：AutoPlayerScreen 88 / BlockEntityMinoTableRenderer 14 / GuiShim 14 / GameOverlayLayer 12 / HandCardsSpecialRenderer 12 / RegistriesWrapperImpl 6 / SeatControlScreen 6 / WildSelectionScreen 6 / ClientPlatform 4 / CompatPacketRegistry 4 / EntityAutoPlayer 4 / EntityAutoPlayerRenderer 4 / MinoFabricClient 2 / PlayerShim 2。

**A. 缺第三方依賴（非 API 移植問題，補 build.gradle 即解）— 下個 session 第一件事**
- **YACL（`dev.isxander.yacl3`）**: AutoPlayerScreen 88 errors（最大宗）——`dev.isxander.yacl3.api` / `.api.controller` / `ConfigCategory` does not exist + 連帶大量 cannot find symbol。upstream config GUI 依賴 YACL，我們 build.gradle 沒加。→ **先加 YACL 1.21.11 依賴**（modImplementation，需查 1.21.11 對應版本），88 error 大半會消，再看 AutoPlayerScreen 剩餘真 API error。

**B. Fabric API package（待查 1.21.11 fabric-api 0.141.4 對應路徑/模組）**
- `net.fabricmc.fabric.api.creativetab.v1`（RegistriesWrapperImpl:12）— creative tab API
- `net.fabricmc.fabric.api.client.keymapping.v1`（ClientPlatform:15）— keybinding API

**C. 1.21.11 render-state 重構（真 API，Phase 2，對本檔既有食譜驗）**
- `net.minecraft.client.renderer.state.level` does not exist（BlockEntityMinoTableRenderer:21 / EntityAutoPlayerRenderer:18）— CameraRenderState package 移位
- render/HUD/GUI 大改：BlockEntityMinoTableRenderer 14 / EntityAutoPlayerRenderer 4 / GuiShim 14 / GameOverlayLayer 12（submit/RenderState/GuiGraphics 範式）

**D. SpecialModelRenderer.Unbaked 泛型（手牌渲染，plan 預期高風險點）**
- HandCardsSpecialRenderer `type Unbaked does not take parameters`(×4) + `is not abstract`(×2)；MinoFabricClient `MapCodec<Unbaked>` 不相容(×2) — SpecialModelRenderer.Unbaked 1.21.11 簽名變

**E. Entity/Screen 簽名（真 API，Phase 1/2）**
- EntityAutoPlayer:215 method does not override；:238 `interact` 簽名變
- SeatControlScreen:60 / WildSelectionScreen:54 method does not override（Screen API 變）

**F. 其他 cannot find symbol（130 總，含上述連帶）**：PlayerShim 2、CompatPacketRegistry 4 等，待逐一查（多數應在 A/B 補依賴後連帶消除）。

**處理順序**：A（加 YACL）重 build → 重新統計剩餘 → B（fabric-api package）→ C/D/E（真 1.21.11 API，對既有食譜 + javap 校準，多屬 Phase 2）。

## phase-0 步驟5b 後續：加 YACL + fabric-api 查證（實測 2026-06-24）
> 接上「步驟5b 178 errors」段。A（YACL）已做、B（fabric-api 7 個）已查證定性。完整 log: `H:\MC_Mods_Port\Minopp_Port\Minopp\build-5b-yacl.log`。

**A. YACL 已解決〔已驗證〕**：build.gradle 加 isxander maven repo（`https://maven.isxander.dev/releases`）+ `dev.isxander:yet-another-config-lib:${yacl_version}`（gradle.properties `yacl_version=3.8.1+1.21.11-fabric`）。**178 → 45**（消 133：88 AutoPlayerScreen + 連帶 symbol）。版本選定：modrinth 最新 3.8.2 **未上 isxander maven**（jar/pom HEAD 404），Gradle 從 maven 抓會炸；用 modrinth + maven 雙源都確認的 3.8.1。

**B. fabric-api 7 個 = 命名回退，非 MC API 變更〔三方驗證：jar class entry + javap 0.141.4 實際子模組 + upstream `<26.1` 註解〕**
- **root cause**: base=B（26.1.2-fabric）用「26.1 未來線 fabric-api 新命名」，但 1.21.11 fabric-api 0.141.4 仍是**舊命名**。修法 = 把 `>=26.1` 寫法回退成 upstream `<26.1` 寫法。
- 0.141.4 綁定子模組版本（查 fabric-api meta pom 確定，非版號大小猜）：item-group **4.2.36** / key-binding **1.1.7** / networking **5.1.6**。
- **RegistriesWrapperImpl**（:12 import、:49/:59 call）：`creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(tab)` → `itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(tab)`。〔javap: `modifyEntriesEvent(class_5321<class_1761>)`=`ResourceKey<CreativeModeTab>` 存在；creativetab.v1 整 package 不存在。修 code 時確認 `tabSupplier.get()` 回傳 `ResourceKey<CreativeModeTab>`〕
- **ClientPlatform**（:15 import、:32 call）：`keymapping.v1.KeyMappingHelper.registerKeyMapping(km)` → `keybinding.v1.KeyBindingHelper.registerKeyBinding(km)`。〔javap: `registerKeyBinding(class_304)`=`registerKeyBinding(KeyMapping)` 存在；keymapping.v1 不存在〕
- **CompatPacketRegistry**（:40,:42）：`PayloadTypeRegistry.serverboundPlay()/clientboundPlay()` → `playC2S()/playS2C()`。〔javap: PayloadTypeRegistry 有 `playC2S()`/`playS2C()`（回 `PayloadTypeRegistry<class_9129>`），無 serverboundPlay/clientboundPlay〕
- 三處 upstream 條件編譯 `//? if <26.1` 註解皆已寫好舊命名；本次另對 0.141.4 實際 jar javap 再確認，非僅信註解（信校驗不信宣告）。

**C. 剩 38 個 = 真 1.21.11 MC API（Phase 2 主菜為主，少數 Phase 1）**：render-state（BlockEntityMinoTableRenderer 7 + EntityAutoPlayerRenderer 2）、HUD/GuiGraphics（GuiShim 7 + GameOverlayLayer 6）、SpecialModelRenderer.Unbaked（HandCardsSpecialRenderer 6 + MinoFabricClient 1）、Screen override（SeatControlScreen 3 + WildSelectionScreen 3）、Entity（EntityAutoPlayer 2：:215 override + :238 interact 簽名）、Player（PlayerShim 1：sendSystemMessage(Component)）。= 38；+ B 的 7 = 45 ✓。

> **數字註腳**：build 自報 `45 errors`（權威）；strip-ANSI 統計抓到 90 是 gradle 把 error list 印兩次（compileJava stderr + BUILD FAILED summary），各檔數除 2 即 unique。

## phase-1 server 編譯對齊修法（✅ 2026-06-24 實施，編譯器驗證、38→35）
> server 3 個 error 全是「26.1 加/改、1.21.11 未有」→ 退回 `<26.1`。**逐 API 判斷、非全域規則**（permissions/NBT 在 1.21.11 就有、沒退）。
- **`EntityAutoPlayer.interact`（:215 @Override + :238 super）**: 26.1 三參數 `interact(Player, InteractionHand, Vec3 location)` → 1.21.11 兩參數 `interact(Player, InteractionHand)`。出處：編譯器 `:238 required: Player,InteractionHand / found: ...,Vec3`（親證 1.21.11 `Entity.interact` 簽名）。退回後 @Override 對上、`super.interact(player, hand)` 編過。
- **`PlayerShim.sendSystemMessage`（:17）**: `player.sendSystemMessage(Component)`（1.21.11 `Player` 無）→ `player.displayClientMessage(message, true)`（upstream `<26.1` 寫法）。⚠️ **語義非完全等價**：`displayClientMessage(.,true)`=actionbar/overlay、`sendSystemMessage`=系統聊天 → Phase 3 runtime 實測觀感、別當等價。`displayClientMessage(Component, boolean)` 存在性 = 編譯器查死（decompile MCP 壞 + javap 撈不到 mojmap jar，改編譯器；見 learnings）。
- **`EntityAutoPlayer` NBT @Override**: addAdditionalSaveData/readAdditionalSaveData 原 active 分支無 @Override（@Override 在被註解的 <26.1 分支）→ phase-1 補回（codex+self-review 一致；零行為變更、強化編譯器保證）。
- **stonecutter `//?` 清理**: 選 C（改哪清哪），清動到的 4 處；其餘 shim 的 `//?` 留收尾全面清。
