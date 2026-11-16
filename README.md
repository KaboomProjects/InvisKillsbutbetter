# KillCorrupt

Paper plugin (1.21.x, Java 21) that corrupts the kill messages of selected players, like "invisible kills" -
but **without potion effects and without invisibility**. Nothing changes for the player, only the chat message.

## Commands
| Command | Description |
|---|---|
| `/killcorrupt add <player>` | Enable corrupted kill messages |
| `/killcorrupt remove <player>` | Disable |
| `/killcorrupt toggle <player>` | Switch on/off |
| `/killcorrupt list` | Show all corrupted players |
| `/killcorrupt reload` | Reload config and data |

Alias: `/kc` - Permission: `killcorrupt.use` (default: op)

## Modes (`config.yml`)
- `HIDDEN` - killer name becomes `hidden-name`
- `OBFUSCATED` - killer name shown with `&k`
- `GLITCH` - random characters on every kill
- `CUSTOM` - whole message from `custom-format` (`%killer%`, `%victim%`)

## Build
```
mvn clean package
```
The jar is created at `target/KillCorrupt.jar`. Every push builds it via GitHub Actions
(Actions tab -> Artifacts). Push a tag like `v1.0.0` to get an automatic GitHub Release with the jar.
