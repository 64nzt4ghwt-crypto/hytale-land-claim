# HyMods.io Listing — DiscordBridgePlugin

> **First Discord integration mod on hymods.io** — every server operator needs this
> Suggested price: Free (drives DungeonPlugin installs) OR $2.99 fixed

---

## Listing Title
**DiscordBridge — Sync Your Hytale Server to Discord (Chat, Joins, Server Status)**

## Short Description
Connect your Hytale server to Discord in 60 seconds. Chat messages, player joins/leaves, and server status post to your Discord channel automatically. Drop-in, zero config required after pasting your webhook URL.

## Long Description

Every multiplayer server community lives in Discord. Now your Hytale server does too.

DiscordBridge forwards your in-game chat to a Discord channel in real time — and sends player join/leave announcements and server status updates automatically. Set it up in two steps: paste your Discord webhook URL into the config file, restart Hytale.

**What gets posted to Discord:**
- 💬 In-game chat messages (with player names)
- 🟢 Player join announcements
- 🔴 Player leave announcements
- ✅ Server start / 🛑 Server stop notifications
- ⚔️ Dungeon events (boss kills, run completions — if DungeonPlugin is installed)

**Built for server operators:**
- Fully configurable — toggle each announcement type independently
- Anti-mention protection — no @everyone injection from players
- Async delivery — webhook calls never lag the game thread
- Custom bot name and avatar per announcement type
- Max message length limit (prevents chat spam from Discord)

**DungeonPlugin integration:**
If you have **DungeonPlugin** installed, dungeon events (boss kills, dungeon completions, player deaths) can also be forwarded to Discord. Great for building community hype around dungeon runs.

**Setup:**
1. Get a Discord webhook URL (Discord server settings → Integrations → Webhooks)
2. Paste it into `bridge-config.json`
3. Restart Hytale

That's it.

---

## Tags
`utility` `discord` `chat` `server-management` `community` `integration` `webhook`

## Category
Utility / Server Management

## Price
- **Free** (recommended — drives adoption, pairs with DungeonPlugin sales)
- OR **$1.99** (still underpriced vs the value)

## Files
- `DiscordBridgePlugin-1.0.0.jar` → place in `mods/com.howlstudio_DiscordBridge/`
- `bridge-config.json` (auto-generated on first run)

## Screenshots to Take
1. Discord channel showing in-game chat forwarded in real time
2. Player join/leave announcements in Discord
3. Server start notification in Discord
4. Config file (shows how simple it is)
