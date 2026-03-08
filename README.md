# DiscordBridgePlugin for Hytale
**Version:** 1.0.0 | **Author:** HowlStudio

Sync your Hytale server with a Discord channel. Chat messages, player joins/leaves, and server status — all posted to Discord automatically.

**First Discord integration mod on hymods.io** — every multiplayer server needs this.

---

## Features

- **Chat sync** — in-game messages → Discord channel in real time
- **Join/leave announcements** — player activity posted to Discord
- **Server status** — start/stop notifications
- **Dungeon events** — boss kills, dungeon completions (hooks into DungeonPlugin)
- **Async delivery** — never lags the game thread
- **Anti-mention protection** — blocks @everyone / @here injection
- **Fully configurable** — JSON config, no restart needed for URL updates

---

## Installation

### 1. Build the JAR (requires JDK 21)

```bash
brew install openjdk@21    # macOS
./gradlew jar
```

Output: `build/libs/DiscordBridgePlugin-1.0.0.jar`

### 2. Install

Copy to your world's mods folder:

```
~/Library/Application Support/Hytale/UserData/Saves/<Your World>/mods/com.howlstudio_DiscordBridge/
```

### 3. Get a Discord Webhook URL

1. Open your Discord server settings
2. Go to Integrations → Webhooks → New Webhook
3. Set the channel you want Hytale chat to appear in
4. Copy the webhook URL

### 4. Configure

Edit the auto-generated `bridge-config.json` in the plugin folder:

```json
{
  "enabled": true,
  "webhook_url": "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL",
  "chat_username": "Hytale",
  "chat_avatar_url": "",
  "system_username": "Hytale Server",
  "system_avatar_url": "",
  "announce_joins": true,
  "announce_leaves": true,
  "announce_server_status": true,
  "announce_dungeon_events": true,
  "max_message_length": 500
}
```

### 5. Restart Hytale

Chat will now appear in your Discord channel.

---

## Configuration Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | bool | `true` | Master switch |
| `webhook_url` | string | `""` | **Required** — Discord webhook URL |
| `chat_username` | string | `"Hytale"` | Bot name for chat messages |
| `chat_avatar_url` | string | `""` | Bot avatar URL (optional) |
| `system_username` | string | `"Hytale Server"` | Bot name for system messages |
| `system_avatar_url` | string | `""` | System bot avatar (optional) |
| `announce_joins` | bool | `true` | Post player join messages |
| `announce_leaves` | bool | `true` | Post player leave messages |
| `announce_server_status` | bool | `true` | Post server start/stop |
| `announce_dungeon_events` | bool | `true` | Post dungeon boss kills etc. |
| `max_message_length` | int | `500` | Truncate long chat messages |

---

## Dungeon Integration

If you also have **DungeonPlugin** installed, you can call:

```java
// From your DungeonPlugin event handler:
DiscordWebhook webhook = ...; // get via dependency
webhook.sendDungeonEvent("Party defeated the Frost Colossus in Frozen Cavern! 🎉");
```

---

## Building for Distribution

```bash
./gradlew jar
# Output: build/libs/DiscordBridgePlugin-1.0.0.jar
```

The JAR includes gson. Copy it to the mods directory with no other dependencies.

---

## Compile Requirements

- Java 21+ (`brew install openjdk@21`)
- Gradle (included via wrapper)
- `libs/HytaleServer.jar` (included in this repo)
