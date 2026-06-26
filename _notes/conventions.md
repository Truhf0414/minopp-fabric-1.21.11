# minopp port — Conventions（專案專屬）

> 通用流程紀律在全域 skill `minecraft-mod-port-workflow`。這裡只放此 port 專屬、無法通用的東西。

## 專案標識
- mod-id / namespace: `minopp` / `cn.zbx1425.minopp`
- maven_group: `cn.zbx1425`（待建環境時對 upstream `build.gradle.kts` 再確認）
- mod_name: `Mino++`
- mod_version: `1.4.0-1.21.11`
- 目標版本 / loader: Minecraft 1.21.11 / Fabric（Loom）。**build 基準 = 官方 `fabric-example-mod` 1.21.11 分支**〔2026-06-24 拍板，已親抓 raw 驗證〕：`net.fabricmc.fabric-loom-remap` plugin + `splitEnvironmentSourceSets()` + **bare** `loom.officialMojangMappings()`（無 layered/intermediary）。版本對官方：MC 1.21.11 / loader 0.19.3 / loom 1.17-SNAPSHOT / fabric-api 0.141.4+1.21.11〔版本相容性待建環境實證〕。⚠️ **舊筆記曾誤記「officialMojangMappings()+intermediary、禁 fabric-loom-remap、禁 split」——已證錯（與官方相反），勿回退**。fabric-1.21.11-port skill 僅當 scaffold 佈局參考，其 layered/寫死版本不採（見全域記憶 fabric-1211-port-skill-needs-fix）。
- **source set: 單一（src/main）**〔2026-06-24 步驟5a 拍板〕——**不採**官方模板的 `splitEnvironmentSourceSets()`。理由：upstream 本來就單 source set（client 靠 `@Environment` + 兩 entrypoint 分流 + mixin config 的 `client` 陣列隔離），1:1 平移最省、phase-0 爆 API error 最乾淨。build.gradle 已移除 `splitEnvironmentSourceSets()` 與 client sourceSet。
- **⚠️ runtime / intermediary cache**〔2026-06-24 步驟5a runClient 實證〕：bare `officialMojangMappings()` 配置**正確**（runClient 空殼已進主畫面、Sound engine started、無 crash）。但 runtime 需要完整的 1.21.11 intermediary cache。**若 runClient crash 於 `NoClassDefFoundError: net.minecraft.class_XXXX`（intermediary 名、一整片 Fabric API entrypoint 掛）= gradle/loom cache 缺 intermediary**，跑 `gradlew <task> --refresh-dependencies` 補上即解（與 mapping/source set 配置無關，**別因此改 bare→layered**，實測 layered 也炸）。本機曾只有 1.21.5 intermediary、缺 1.21.11，已 refresh 補上。連官方 `fabric-example-mod` 原樣在補 cache 前也同款 crash。
- 命名系統: **Mojmap**（upstream 與新環境一致，不翻 yarn）〔查過：BlockEntityMinoTable import 全 Mojmap〕

## upstream 規格源
- repo: `zbx1425/minopp`，clone HEAD = `a0dc41e`（2026-06-01，"fix: try fix client class loading"）
- 攤平切片 = **(fabric, >=1.21.2, <26.1)**：保留 `fabric` / `>=1.21.2` / `<26.1` 及組合；移除 `neoforge` / `<1.21.2` / `>=26.1` 及組合
- ⚠️ **關鍵認知**：作者主版本分界是 `<26.1` / `>=26.1`（為銜接 26.x WIP 而寫），**不是** `>=1.21.2`。`<26.1` 分支內容大致停在 1.21.1 時代；1.21.2~1.21.11 的 API 變化大多落在 `<26.1` 分支內、**未適配**，需自行對 1.21.11 校準（見 learnings + api-changes）。
- **攤平機制真相（修正 prompt §7）**〔查過〕：Stonecutter 不是「chisel dump 出乾淨碼」——它是**就地重寫** `src/`：設哪個 target 為 active，`src/` 的 `//?` 條件就攤平成那個（active 分支活碼、其餘 `/* */` 註解）。**無獨立輸出目錄**。upstream `src/` 當前已攤平成 `26.1.2-fabric`（`>=26.1` 是活碼，WorldShim L7-13 驗）。攤平操作很簡單，難的是「選起點切片 + 校準 1.21.11」。
- **render/NBT 適配位置（修正 prompt §2.4）**〔查過〕：`>=1.21.2`（12 處）只處理 registry `setId`/`useBlockDescriptionPrefix` + neoforge 封包，**無 render**。render-state 重構（submit/SubmitNodeCollector/RenderState）與 NBT（ValueInput/ValueOutput）全在 `>=26.1`；`<26.1` 是 1.21.1 老碼。
- **攤平起點＝待拍板的策略決策**：候選 (A) `<26.1` 切片（prompt 預設，render/NBT 是 1.21.1 老碼，離 1.21.11 遠）vs (B) `>=26.1` 切片（當前 active，26.1 新碼，**若 1.21.11 已是 1.21.6+ submit/ValueInput 範式則更近**）。**須 phase-0 動工建環境後 javap 1.21.11 才能定**，未驗前不選。〔(B) 更近是猜的，待 javap〕

## 唯讀 / 不可動資料夾（餵 plan 的 mutability 欄）
> 複製到各 phase plan 時轉成命令式護欄措辭：「動工/寫入前必須先對照本清單，標『唯讀』者禁止任何 Edit/Write 觸碰」。
- `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`（整個 upstream clone）: **唯讀**，僅供盤點 / 攤平 / javap 參考。
  - 例外：攤平借 stonecutter chisel 時的暫時改動只在 upstream clone 內、用完即丟（phase-0 plan 會明訂範圍）。

## 完整性母數（基準線，攤平後核對沒漏搬）
- source 總 **75** class；目標 fabric 切片 = **60** class（54 共用核心 + 6 fabric 專屬）；丟棄 **15** neoforge 專屬。
- 共用 package（54）: (root)3 / block2 / effect8 / entity1 / game6 / gui5 / item3 / mixin3 / network6 / platform7 / platform.multiver6 / render4
- fabric 專屬（6）: fabric2 / fabric.mixin1 / fabric.platform3
- 丟棄 neoforge（15）: neoforge2 / neoforge.mixin1 / neoforge.platform3 / neoforge.compat.signmeup1 / neoforge.compat.touhou_little_maid5 / .task3
- 資源母數（自己核對過；agent 計數有小錯故重數）:
  - blockstate 1 / block model 1 / item model(傳統 models/item/) 4 / **item definition(1.21.4+ assets/minopp/items/) 3** / texture PNG 8 / lang 6 / sounds.json 1 / sound OGG 2 / icon(minopp.png) 1 / recipe 1 / loot_table 1（1.21+ 單數命名）/ **無 tags**
  - mixin config: fabric 切片用 2（`minopp.mixins.json` common + `minopp.fabric.mixins.json`）；丟 neoforge
  - templates(modstitch property substitution): upstream modstitch 角度 2（`fabric.mod.json` + `pack.mcmeta`）；**純 Fabric 獨立專案實際只需 `fabric.mod.json`（1）** — `pack.mcmeta` 是 modstitch 多平台 template（NeoForge/Forge 需要 resource pack metadata），純 Fabric 的 assets+data 靠 fabric-loader 自動生成 pack metadata 載入、**不需手寫 pack.mcmeta**〔查過：官方 fabric-example-mod 1.21.11 的 src/main/resources 根不帶 pack.mcmeta（只有 assets/ + fabric.mod.json + mixins.json）+ 5a runClient 無 crash，雙實證〕。丟 `mods.toml`/`neoforge.mods.toml`。〔若 Phase 3 runClient 渲染真方塊證實需要再補：pack_format=**75**（MC 1.21.11 version.json `resource_major`），注意 1.21.9+ major.minor / cookbook 警告的 min_format-max_format schema〕
  - ⚠️ **item definition 3 個是 1.21.4+ 新 client item model 格式（cookbook 提的「raw translation key / 新 item 模型層」），porting 不可漏**

## 專屬路徑
- 乾淨專案: `H:\MC_Mods_Port\Minopp_Port\Minopp`（工作目錄、要 push）
- upstream 參考: `H:\MC_Mods_Port\Minopp_Port\Minopp-upstream`（唯讀）
- plan: `_notes/plans/phase-N.md`
- STATUS: `_notes/STATUS.md`
- learnings: `_notes/learnings.md`
- api-changes 帳本: `_notes/api-changes.md`
