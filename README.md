# Mino++ — Fabric 1.21.11 Port

> Play UNO in Minecraft.

A single-version **Fabric / Minecraft 1.21.11** port of
[Zbx1425/minopp](https://github.com/zbx1425/minopp) ("Mino++"), distributed under the
original MIT license.

> English first; 繁體中文在下半部。

---

## English

### About

This is a fork of the original [Mino++](https://github.com/zbx1425/minopp) by
**zbx1425**. Upstream targets multiple loaders and Minecraft versions through a
[stonecutter](https://stonecutter.kikugie.dev/) multi-version setup; this fork is a
**flattened, single-target Fabric 1.21.11 build** — a faithful port that is also a clean,
maintainable single-version fork, not a behavioral rewrite.

For the relationship to upstream, intentional deviations, and known trade-offs, see
[`PORTING.md`](PORTING.md).

### Features

- Play UNO with other players — or with automated (bot) players — at an in-world table block.
- A config screen for the bot players (powered by YACL).
- In-game hand HUD and card interactions.

### How to play

- Sit at a table block and play against other players or bots. Each player starts with a
  hand of **7 cards**.
- **Right-click a draw pile** to draw or pass; **right-click elsewhere** to play a card.
- You can play a card identical to the last one to steal the turn, and `+2` / `+4` cards
  can be stacked.
- When you are down to one card, remember to call out — hold `Ctrl` while playing the card,
  or type the callout in chat. Miss it and you draw a **2-card penalty**.
- Physically bumping into a player challenges them if they forgot their callout.

### Requirements

| | |
|---|---|
| Minecraft | `1.21.11` |
| Fabric Loader | `>= 0.19.3` |
| Java | `21` |
| [Fabric API](https://modrinth.com/mod/fabric-api) | required |
| [YACL](https://modrinth.com/mod/yacl) (Yet Another Config Lib) | required — powers the config GUI |

The mod runs on both client and server. For multiplayer, install it (and its
dependencies) on the server as well as every client.

### Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11.
2. Drop the following into your `mods/` folder:
   - this mod's jar (from [Releases](../../releases) or built from source)
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [YACL](https://modrinth.com/mod/yacl)
3. Launch Minecraft.

### Building from source

Standard Fabric Loom project:

```
./gradlew build
```

The built jar is produced under `build/libs/`.

### Credits & License

- **Original mod:** [Mino++](https://github.com/zbx1425/minopp) by zbx1425 — MIT
- **This fork:** Fabric 1.21.11 port — MIT (see [`LICENSE`](LICENSE))
- **Porting notes:** [`PORTING.md`](PORTING.md)

---

## 繁體中文

### 關於

本專案是 zbx1425 所作 [Mino++](https://github.com/zbx1425/minopp) 的 fork。upstream 透過
[stonecutter](https://stonecutter.kikugie.dev/) 多版本設定同時支援多個 loader 與 Minecraft
版本；本 fork 則是**攤平的單一 Fabric 1.21.11 目標**——一個忠實的移植，同時也是乾淨、好維護的
單版本 fork，而非行為上的重寫。

與 upstream 的關係、刻意偏離之處、已知取捨，詳見 [`PORTING.md`](PORTING.md)。

### 功能

- 在世界中的桌子方塊上，與其他玩家——或自動（bot）玩家——一起玩 UNO。
- bot 玩家的設定畫面（由 YACL 提供）。
- 遊戲內手牌 HUD 與卡牌互動。

### 怎麼玩

- 坐到桌子方塊旁，與其他玩家或 bot 對戰。每位玩家初始手牌為 **7 張**。
- **右鍵牌堆**抽牌或 pass；**右鍵其他地方**出牌。
- 可以出一張與前一張完全相同的牌來搶過回合，`+2` / `+4` 可以疊加。
- 當你只剩一張牌時，記得喊牌——出牌時按住 `Ctrl`，或在聊天欄打出喊牌。漏喊會**罰抽 2 張**。
- 用身體撞向漏喊的玩家即可對他發起質疑（challenge）。

### 需求

| | |
|---|---|
| Minecraft | `1.21.11` |
| Fabric Loader | `>= 0.19.3` |
| Java | `21` |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 必需 |
| [YACL](https://modrinth.com/mod/yacl)（Yet Another Config Lib） | 必需——設定 GUI 依賴它 |

本 mod 在 client 與 server 端都會運行。多人遊玩時，server 與每個 client 都要安裝本 mod（及其
依賴）。

### 安裝

1. 為 Minecraft 1.21.11 安裝 [Fabric Loader](https://fabricmc.net/use/installer/)。
2. 把以下檔案放進你的 `mods/` 資料夾：
   - 本 mod 的 jar（從 [Releases](../../releases) 下載、或自行 build）
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [YACL](https://modrinth.com/mod/yacl)
3. 啟動 Minecraft。

### 從原始碼 build

標準的 Fabric Loom 專案：

```
./gradlew build
```

建置產物位於 `build/libs/`。

### 致謝與授權

- **原作 mod：** [Mino++](https://github.com/zbx1425/minopp) by zbx1425 — MIT
- **本 fork：** Fabric 1.21.11 移植 — MIT（見 [`LICENSE`](LICENSE)）
- **移植說明：** [`PORTING.md`](PORTING.md)
