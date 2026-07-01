# Porting Notes · 移植說明

> English first; 繁體中文在下半部。

---

## English

### Relationship to upstream

This repository is a fork of [Zbx1425/minopp](https://github.com/zbx1425/minopp)
("Mino++", play UNO in Minecraft), ported to **Fabric / Minecraft 1.21.11**.
It is distributed under the original MIT license (see `LICENSE`).

Upstream targets multiple loaders and Minecraft versions through a
[stonecutter](https://stonecutter.kikugie.dev/) multi-version setup. This fork is a
**single, flattened Fabric 1.21.11 target**: the stonecutter conditional-compilation
branches and the NeoForge-side code have been collapsed and removed, leaving only the
code that actually compiles and runs on 1.21.11.

The goal was a faithful port that is also a clean, maintainable single-version fork —
not a behavioral rewrite.

### Intentional deviations from upstream

A few things behave differently from upstream by deliberate decision, not by accident:

- **NBT serialization of `ActionMessage` and auto-player `CustomName`** was changed from
  "serialize a `Component` to a JSON string, then store that string in NBT" to the
  standard codec-based NBT (`ValueOutput.store` / `ValueInput.read`). This is cleaner and
  matches current Minecraft conventions, but it is **not backward-compatible with worlds
  saved by upstream** (or by pre-fork builds). Since this fork starts fresh, no migration
  path is provided.

- **Player feedback messages** (`PlayerShim.sendSystemMessage`) are shown on the
  **action bar** rather than the system chat. Minecraft 1.21.11 removed the
  `Player.sendSystemMessage(Component)` API that upstream relied on; the action bar was
  chosen as the replacement because it reads well for transient "action failed" feedback.
  (See the note in `PlayerShim` for details.)

### Known trade-offs kept as-is

The following were reviewed and **deliberately left unchanged**. They are recorded here so
it is clear they were considered, not overlooked:

- **`GuiShim.getMiencraftyFontDesc` — misspelling ("Miencrafty").** Inherited verbatim from
  upstream. Renaming it would create a purely cosmetic divergence from upstream and make
  future comparison / backporting noisier, for no functional gain. Kept.

- **`platform.multiver` package name and the `Shim` suffix.** These are vestigial from the
  original multi-version abstraction layer, which no longer exists after flattening.
  A rename would be the "most correct" change but touches many files and call sites for a
  cosmetic benefit; deferred to any future large refactor rather than done piecemeal.

- **Magic numbers in rendering code** (UV / texture-atlas coordinates, HUD offsets, voxel
  shapes). These are domain-conventional constants in Minecraft rendering; naming them
  would add noise, not clarity. Left in place.

- **`MinoClient` global mutable state** (`globalFovModifier`, `handCardOverlayActive`).
  A recognized design smell, but it is common in mod client code and is read across the
  render/mixin boundary. The refactor risk outweighs the benefit. Left as-is (WONTFIX).

### Build

Standard Fabric Loom project:

```
./gradlew build
```

The built jar is produced under `build/libs/`.

---

## 繁體中文

### 與 upstream 的關係

本專案是 [Zbx1425/minopp](https://github.com/zbx1425/minopp)（「Mino++」，在 Minecraft
裡玩 UNO）的 fork，移植到 **Fabric / Minecraft 1.21.11**。以原始 MIT 授權散布（見
`LICENSE`）。

upstream 透過 [stonecutter](https://stonecutter.kikugie.dev/) 多版本設定，同時支援多個
loader 與 Minecraft 版本。本 fork 則是**單一、攤平的 Fabric 1.21.11 目標**：stonecutter
的條件編譯分支與 NeoForge 端的程式碼都已收合並移除，只留下真正能在 1.21.11 編譯、執行的
程式碼。

目標是一個忠實的移植，同時也是乾淨、好維護的單版本 fork——而非行為上的重寫。

### 刻意偏離 upstream 之處

有幾處行為與 upstream 不同，是經過刻意決定、而非疏失：

- **`ActionMessage` 與自動玩家 `CustomName` 的 NBT 序列化**：從「把 `Component` 序列化成
  JSON 字串、再把字串存進 NBT」改為標準的 codec NBT（`ValueOutput.store` /
  `ValueInput.read`）。這樣更乾淨、也符合現行 Minecraft 慣例，但**與 upstream（或改造前的
  建置）存檔不相容**。由於本 fork 從頭開始，不提供遷移路徑。

- **玩家回饋訊息**（`PlayerShim.sendSystemMessage`）顯示在 **action bar**（動作列）而非
  系統聊天。Minecraft 1.21.11 移除了 upstream 依賴的 `Player.sendSystemMessage(Component)`
  API；選 action bar 作為替代，是因為它更適合呈現「操作失敗」這類短暫的即時回饋。（細節見
  `PlayerShim` 內的註解。）

### 已知取捨（刻意保留現狀）

以下項目都經過審視、**刻意保持不變**。在此記錄，是為了表明它們是被考量過、而非被忽略：

- **`GuiShim.getMiencraftyFontDesc`——拼字錯誤（「Miencrafty」）。** 原封不動繼承自
  upstream。改名只會與 upstream 產生純美觀的分歧、讓日後比對／backport 更雜，卻毫無功能上
  的好處。保留。

- **`platform.multiver` 套件名與 `Shim` 後綴。** 這些是原本多版本抽象層的殘留，攤平之後該層
  已不存在。改名雖然「最正確」，但會牽動大量檔案與呼叫點、僅換得美觀上的好處；留待未來大型
  重構時一併處理，而非零碎地改。

- **渲染程式碼中的魔法數字**（UV／材質圖集座標、HUD 偏移、voxel shape）。這些在 Minecraft
  渲染裡是領域慣例常數；為它們命名只會增加雜訊、而非提升清晰度。保留原樣。

- **`MinoClient` 的全域可變狀態**（`globalFovModifier`、`handCardOverlayActive`）。這是可
  辨識的設計異味，但在 mod client 程式碼中很常見，且會跨 render/mixin 邊界讀取。重構的風險
  大於好處。維持現狀（WONTFIX）。

### 建置

標準的 Fabric Loom 專案：

```
./gradlew build
```

建置產物位於 `build/libs/`。
