# IslandCore

**Version:** 4.8  
**API:** Paper 1.21  
**Author:** YourName

IslandCore is a per-player skyblock island plugin. Each player owns a separate world that loads on demand. The plugin handles island visits, a skill tree progression system, a community rating system, a trading system, friends, staff tools, and server performance safeguards — all in one.

---

## Table of Contents
- [Setup](#setup)
- [How Islands Work](#how-islands-work)
- [Commands](#commands)
  - [Player Commands](#player-commands)
  - [Staff Commands](#staff-commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Data Files](#data-files)
- [Changelog](#changelog)

---

## Setup

1. Drop the compiled `.jar` into your `plugins/` folder.
2. Start the server once to generate `plugins/IslandCore/config.yml` and `plugins/IslandCore/skilltree-prices.yml`.
3. Edit `config.yml` to set your `world-prefix`, border size, and tuning values (see [Configuration](#configuration)).
4. Assign permissions via your permission plugin (LuckPerms, etc.).
5. Players who join for the first time will have an island world created for them automatically. The commands listed in `world-create-commands` in `config.yml` are run on the new world at creation time.

---

## How Islands Work

- Each player gets their own world. The world is named using the `world-prefix` setting followed by their UUID.
- Worlds **load automatically** when the owner joins, when someone uses `/visit`, or when a staff member uses `/stafftp`.
- Worlds **unload automatically** after the owner disconnects and a configurable grace period expires.
- A player's island has a square border (configurable via `island-border-size`). Players who leave the border are teleported to spawn.
- Visitors cannot break blocks, place blocks, or interact with containers on another player's island. Staff with `islandcore.bypass` are exempt.
- Island visits must be enabled by the owner via `/toggleislandvisits` before other players can use `/visit`.

---

## Commands

### Player Commands

---

#### `/visit <player>` — aliases: `/v`
**Permission:** `islandcore.visit`

Teleport to another player's island. The target player must have visits enabled via `/toggleislandvisits`. A confirmation GUI shows the island's rating and score before you arrive. There is a short teleport cooldown between visits.

---

#### `/home` — aliases: `/h`
**Permission:** `islandcore.home`

Teleport back to your own island spawn point. If your island world is not loaded it will be loaded first.

---

#### `/setspawn`
**Permission:** `islandcore.setspawn`

Set your island's spawn point to your current location. New visitors and yourself on `/home` will arrive here. Must be used on your own island.

---

#### `/myisland` — aliases: `/island`, `/mi`
**Permission:** `islandcore.myisland`

Shows a dashboard of your island's current status in one place:
- **Day** — what Minecraft day your island is on.
- **Visits** — whether visits are open or closed.
- **Unique visitors / total visits** — visit counts that feed your Island Score.
- **Community rating** — your average star rating and vote count.
- **Island Score** — your current 0–100 automatic score.

---

#### `/toggleislandvisits` — aliases: `/tiv`
**Permission:** `islandcore.toggleislandvisits`

Toggles whether other players can visit your island. Must be used while on your own island.

- **Enabling** visits: other players can now use `/visit <you>`.
- **Disabling** visits: any players currently on your island are immediately kicked back to their own islands.

---

#### `/toggle`
**Permission:** `islandcore.toggle`

Starts or stops the item looting minigame on your island. When active, random items from your unlocked skill tree categories are dropped at a configurable interval. A 10-second countdown appears when you first enable it.

---

#### `/skilltree` — aliases: `/st`, `/tree`
**Permission:** `islandcore.skilltree`

Opens the skill tree GUI. Spend Tree Tokens (dropped by the loot minigame) to unlock item categories. Unlocking more categories improves your Island Score and the quality of loot rewards.

---

#### `/rate <player> <1-5>`
**Permission:** `islandcore.rate`

Rate another player's island from 1 to 5 stars. You must have spent a minimum amount of time on their island during your current (or a previous) visit. You can re-rate after the revote cooldown (`ratings.revote-cooldown-hours` in config) has passed — your new vote replaces the old one.

---

#### `/topislands`
**Permission:** `islandcore.topislands`

View a leaderboard of the top-scoring islands on the server, ranked by automatic Island Score.

---

#### `/msg <player> <message>`
**Permission:** `islandcore.msg`

Send a private message to another online player.

---

#### `/reply <message>` — aliases: `/r`
**Permission:** `islandcore.reply`

Reply to the last player who sent you a private message.

---

#### `/friend <add|remove|accept|deny|list> [player]` — aliases: `/friends`, `/f`
**Permission:** `islandcore.friend` *(granted via `islandcore.*`)*

Manage your friends list.

| Subcommand | Description |
|---|---|
| `add <player>` | Send a friend request. |
| `remove <player>` | Remove someone from your friends list. |
| `accept <player>` | Accept a pending friend request. |
| `deny <player>` | Deny a pending friend request. |
| `list` | View your current friends. |

---

#### `/trade <player>` / `/trade <accept|deny|cancel>`
**Permission:** `islandcore.*` *(via wildcard)*

Trade items with another player who is on your island.

| Usage | Description |
|---|---|
| `/trade <player>` | Send a trade request. The target sees a clickable chat prompt. |
| `/trade accept` | Accept an incoming trade request. |
| `/trade deny` | Deny an incoming trade request. |
| `/trade cancel` | Cancel an ongoing trade session. |

Once both players accept, a two-sided inventory GUI opens. Place items in your side, then click **Confirm** when ready. Both players must confirm before the trade completes. Items are always safely returned if the trade is cancelled, either player disconnects, or the server restarts. Completed trades are logged to `plugins/IslandCore/tradelogs/`.

---

### Staff Commands

---

#### `/stafftp <player>`
**Permission:** `islandcore.stafftp`

Teleport directly to a player's island as a staff member, bypassing visit restrictions. The island loads if it is not already loaded.

---

#### `/invsee <player>`
**Permission:** `islandcore.invsee`

Open and view another player's inventory in real time.

---

#### `/enderchest <player>` — aliases: `/ecsee`
**Permission:** `islandcore.enderchest`

View another player's ender chest.

---

#### `/kick <player> [reason]`
**Permission:** `islandcore.kick`

Kick a player from the server with an optional reason.

---

#### `/ban <player> <reason> [duration]`
**Permission:** `islandcore.ban`

Ban a player. Duration is optional; omitting it results in a permanent ban. Duration format: `1d`, `2h`, `30m`, etc. (parsed by `DurationUtil`).

---

#### `/rank <set|view> <player> [rank]`
**Permission:** `islandcore.rank.manage`

Set or view a player's rank.

| Subcommand | Description |
|---|---|
| `set <player> <rank>` | Assign a rank to a player. |
| `view <player>` | View a player's current rank. |

Available ranks are defined in `config.yml`. Staff ranks (HELPER, ADMIN, OWNER) bypass island protection and cannot be kicked from islands.

---

#### `/report <player> <reason>`
**Permission:** `islandcore.report` *(via wildcard)*

Report a player. Reports are broadcast to online staff.

---

#### `/reportisland <player> <reason>`
**Permission:** `islandcore.reportisland` *(via wildcard)*

Report a player's island. Reports are broadcast to online staff.

---

#### `/spawntoken <amount>`
**Permission:** `islandcore.tokens.spawn`

Spawn a stack of Tree Tokens in your hand. Tree Tokens are the currency used to unlock skill tree nodes.

---

#### `/resetplayer <player>`
**Permission:** `islandcore.resetplayer`

Fully wipes a player's data: clears their inventory, deletes their island world, resets their skill tree, and kicks them from the server. Their rank is not affected. **This is irreversible.**

---

#### `/resetislandscore <player>`
**Permission:** `islandcore.resetislandscore`

Fully resets a player's Island Score back to 0. This clears all three components:
- **Visit/day data** — unique visitor count and island age contribution.
- **Community star ratings** — all 1–5 star votes from visitors.
- **Skill tree progress** — all unlocked nodes (which account for 40% of the score).

> ⚠ This also resets the player's skill tree, which affects their loot minigame categories. Use `/resetratings` if you only want to clear star votes without touching the skill tree.

---

#### `/resetratings <player>`
**Permission:** `islandcore.resetratings`

Clears only the community star ratings (1–5 votes) for a player's island. Visit history and skill tree progress are not affected. The Island Score's visit/day portion is also untouched.

---

#### `/ownerrate <player>`
**Permission:** `islandcore.ownerrate`

Awards the **Owner Star** badge to a player's island. This is a light-blue star displayed alongside their community stars — a manually-granted mark of quality from server staff. Shown in the tab list and on the scoreboard.

---

#### `/removeownerrate <player>`
**Permission:** `islandcore.ownerrate`

Removes the Owner Star badge from a player's island.

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `islandcore.visit` | Use `/visit` | false |
| `islandcore.home` | Use `/home` | false |
| `islandcore.setspawn` | Use `/setspawn` | false |
| `islandcore.day` | Use `/day` | false |
| `islandcore.toggleislandvisits` | Use `/toggleislandvisits` | false |
| `islandcore.toggle` | Use `/toggle` (loot minigame) | false |
| `islandcore.skilltree` | Use `/skilltree` | false |
| `islandcore.rate` | Use `/rate` | false |
| `islandcore.topislands` | Use `/topislands` | false |
| `islandcore.msg` | Use `/msg` | false |
| `islandcore.reply` | Use `/reply` | false |
| `islandcore.stafftp` | Use `/stafftp` | false |
| `islandcore.invsee` | Use `/invsee` | false |
| `islandcore.enderchest` | Use `/enderchest` | false |
| `islandcore.kick` | Use `/kick` | false |
| `islandcore.ban` | Use `/ban` | false |
| `islandcore.rank.manage` | Use `/rank` | false |
| `islandcore.tokens.spawn` | Use `/spawntoken` | false |
| `islandcore.resetplayer` | Use `/resetplayer` | false |
| `islandcore.resetislandscore` | Use `/resetislandscore` | false |
| `islandcore.resetratings` | Use `/resetratings` | false |
| `islandcore.ownerrate` | Use `/ownerrate` and `/removeownerrate` | false |
| `islandcore.bypass` | Bypass island protection everywhere | false |
| `islandcore.*` | Grants all of the above | false |

---

## Configuration

All values are in `plugins/IslandCore/config.yml`, generated on first run.

| Key | Default | Description |
|---|---|---|
| `world-prefix` | `worlds/` | Path prefix for island world folders. |
| `island-border-size` | `200.0` | Half-width of the square island border. |
| `legacy-bypass-name` | `ILiveOffCaffine` | Username that bypasses protection (legacy fallback). |
| `loot-interval-seconds` | `30` | How often the loot minigame drops items. |
| `item-cleanup.interval-seconds` | `60` | How often dropped items are scanned for cleanup. |
| `item-cleanup.max-age-seconds` | `300` | Age at which uncollected dropped items are removed. |
| `island-maintenance.interval-seconds` | `30` | How often idle/empty island worlds are checked for unloading. |
| `scoreboard.refresh-seconds` | `1` | How often the sidebar scoreboard updates. |
| `data-save-interval-seconds` | `300` | How often data is auto-saved to disk. |
| `ratings.min-visit-seconds` | `30` | Minimum seconds a visitor must spend on an island before rating. |
| `ratings.revote-cooldown-hours` | `24` | Hours before a player can re-rate the same island. |
| `ratings.auto-score.max-day` | `60` | Island day cap for the day component of the auto score. |
| `ratings.auto-score.max-unique-visitors` | `20` | Unique visitor cap for the popularity component of the auto score. |
| `world-create-commands` | `[]` | Commands run on a new island world when first created (e.g. setting gamerules). |

Skill tree upgrade costs are in `plugins/IslandCore/skilltree-prices.yml` and can be edited without recompiling.

---

## Data Files

All persistent data lives under `plugins/IslandCore/data/`:

| File | Contents |
|---|---|
| `data/playerdata.yml` | Per-player island flags: visitable state, looting state, loot pull count, world name, and ranks. |
| `data/ratings.yml` | Community star votes, visit counts, island age cache, and owner star badges. |
| `data/skilltree.yml` | Skill tree unlock progress and Tree Token balances per player. |
| `data/friends.yml` | Friends lists and pending friend requests. |
| `tradelogs/` | One JSON file per completed trade, named by timestamp. |

---

## Changelog

### v4.8
- Fixed void deaths sending the "You died" message twice.
- Fixed visitors losing fly ability after dying on someone else's island.
- Added `/trade` system: same-island trade requests, a two-sided confirmation GUI, and safe item return on cancel/disconnect/reload. Completed trades logged as JSON.
- Replaced `/settings` GUI with `/toggleislandvisits` — a dedicated command for toggling island visits.
- Fixed `/resetislandscore` to perform a **full** score reset (visit data, community ratings, and skill tree progress), not just the visit/day portion.
- Merged `/day` and `/myrating` into `/myisland` — a single dashboard showing island day, visit status, visitor counts, star rating, and Island Score.
- Replaced all "Random Blockz" branding with "Block Bound" in config.yml.
- Fixed world unloading: the maintenance task was resetting the unload timer on every pass, preventing worlds from ever unloading. Worlds now correctly unload after the configured delay (default 60 seconds) once all players have left.
