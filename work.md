# RomeoPaperPlugin — Master Work Plan
**Complete, expandable, server-side Settings GUI + bundled resource pack system.**
Source of truth: `readit.md`. Status tracker + full specification below.

---

## 0. QUICK FACTS

| Item | Value |
|---|---|
| Project | RomeoPaperPlugin |
| Platform | Paper/Bukkit (server-side only, zero client mods) |
| Build | Gradle — `./gradlew build`, plugin jar = `:paper:shadowJar` |
| Artifact | `paper/build/libs/paper-1.0.0.jar` → rename `RomeoPaperPlugin.jar` |
| Permission | `plugin.admin` (default: op) |
| Config keys (existing, do NOT rename) | `server-list.motd`, `server-list.max-players`, `server-list.online-players`, `server-list.cached-icon` |
| Local test | `python3 start_local_server.py` (needs Java 21+) |
| Current modules | `common` (shared), `paper` (plugin), `fabric` (client test mod, not required for users) |

**Distribution promise:** server owner installs ONE jar. Resource pack is bundled inside it.
No separate ZIP. No Fabric/Forge/NeoForge/Lunar/Badlion/Feather required.

---

## 1. MISSION

Build a **real expandable server administration system**:

- A complete server-side **Settings GUI** (`/settings`) that visually fronts **every** plugin command and feature.
- Styled like **Minecraft's vanilla Options screen** (not a web dashboard).
- A **bundled, self-hosting resource pack pipeline** that powers the GUI visuals.
- A **shared services layer** so commands and GUI never duplicate logic.
- Architecture that accepts every future command without rewrites.

The important part is NOT a pretty GUI — it is the architecture:

```
COMMANDS                GUI SYSTEM
     \                  /
      → SHARED SERVICES ←
      (Motd, PlayerCount, Icon, NPC, ResourcePack, Config)
                  |
          RESOURCE PACK (bundled in jar)
                  |
        MINECRAFT-STYLE UI
```

---

## 2. HARD RULES (NON-NEGOTIABLE)

1. **No duplicated logic.** `/editmotd` and GUI → Edit MOTD call the same service method.
2. **No internal `Bukkit.dispatchCommand(...)`** to "reuse" a command. GUI → service → config → save.
3. **One reusable InputManager.** Never register per-GUI chat listeners.
4. **Thread safety:** `AsyncPlayerChatEvent` is async — hop to main thread before ANY Bukkit API call.
5. **Permissions checked server-side on every action**, not just by hiding buttons.
6. **Confirmations for destructive actions** (delete NPC, reset config).
7. **Main screen stays clean:** categories only. Never 30+ buttons on one screen.
8. **Every GUI control performs a real action or shows a real value.** No fake settings.
9. **Never crash the plugin:** resource-pack failure, bad NPC data, bad input → log + degrade gracefully.
10. **Keep existing config keys.** Only ADD new sections.
11. **No client-side recreation of OptionsScreen.** It's an inventory GUI *resembling* vanilla.
12. **Version-specific resource packs** — never claim one pack fits all MC versions.

---

## 3. GUI HIERARCHY (TARGET)

```
/settings → "Options..."
├── Server Settings... ── MOTD...            (Edit/Preview/Reset)
│                      ├─ Player Count...    (Edit online/max, Reset both)
│                      ├─ Server Icon...     (Change URL/Reset/Preview)
│                      └─ Preview Server List
├── Player Settings...
├── NPC Manager... ────── NPC List ── NPC Control (per NPC)
│                      ├─ Create NPC (name input)
│                      ├─ Remove NPC (list → confirm)
│                      ├─ NPC Information
│                      ├─ Appearance... → Skin/Name/Nametag/Glow/Particles
│                      ├─ Actions... → Right-Click / Left-Click (Edit/Clear/Test), TP, Move, Look
│                      ├─ Behavior... → Look / Enable / Disable (only what exists!)
│                      └─ Storage... → Save NPCs / Reload NPCs
├── Visual Settings...
├── Commands... ───────── Utility: /ping
│                      ├─ Server: /editmotd /editplayers /editicon
│                      ├─ NPC: /npc
│                      └─ Settings: /settings
├── Resource Pack... ──── Status/Bundled/Version/SHA-1
│                      ├─ Send Pack Again
│                      ├─ Regenerate Pack
│                      └─ Pack Information
├── Advanced...
└── Plugin Information...
```

Every submenu follows the SAME template: **TITLE → current values → controls → Back/Done.**
Visual language never changes between menus.

---

## 4. ARCHITECTURE & FILE MANIFEST

### `com.romeo.paper.services` — shared backend (commands + GUI both call these)
| File | Responsibility |
|---|---|
| `MotdService.java` | get/set/reset/preview MOTD → config `server-list.motd` |
| `PlayerCountService.java` | get/set/reset online & max display counts |
| `IconService.java` | cached icon state + async download/convert/apply (reuse existing downloader logic) |
| `NpcService.java` | wraps `NpcManager` for GUI-safe operations |
| `ConfigService.java` | typed config access, save, reload, reset |

### `com.romeo.paper.input` — chat input system (ONE listener for everything)
| File | Responsibility |
|---|---|
| `InputManager.java` | begins/cancels sessions; single `AsyncPlayerChatEvent` handler; cancels event; hops to main thread; supports string/int/URL/npc-selection/confirmation/multi-step |
| `InputSession.java` | type, prompt, callback, validator, reopen-target, expiry |

### `com.romeo.paper.gui` — menu screens (one file per screen)
| File | Screen |
|---|---|
| `GuiManager.java` | click routing (slot→action maps, holder pattern), open/close tracking, permission gate, session state |
| `GuiContext.java` | per-player context: history stack, selected NPC, pending confirmations |
| `SettingsGUI.java` | MAIN "Options..." two-column category grid + Done |
| `ServerSettingsGUI.java` | MOTD / Player Count / Icon / Preview + Back |
| `MotdGUI.java` | current MOTD + Edit/Preview/Reset |
| `PlayerCountGUI.java` | current values + Edit online/max, Reset both |
| `IconGUI.java` | current icon + Change URL/Reset/Preview |
| `NpcGUI.java` | hub: List/Create/Remove/Info/Appearance/Actions/Behavior/Storage |
| `NpcListGUI.java` | all NPCs → click opens control |
| `NpcControlGUI.java` | per-NPC: Info/Appearance/Actions/Behavior/TP/Move/Enable/Disable/Delete |
| `NpcAppearanceGUI.java` | Skin/Name/Nametag/Glow/Particles |
| `NpcActionsGUI.java` | Right/Left click actions (Edit/Clear/Test), TP, Move, Look |
| `CommandsGUI.java` | grouped command list (Utility/Server/NPC/Settings) |
| `ResourcePackGUI.java` | status/bundled/version/SHA-1 + Send/Regenerate/Info |
| `AdvancedGUI.java` | reload config, force save, diagnostics |
| `PluginInfoGUI.java` | version, author, commands, pack info |
| `ConfirmationGUI.java` | generic Confirm/Cancel with callback |
| `MenuIcons.java` | material + icon map (custom model data when pack active) |

### `com.romeo.paper.resourcepack`
| File | Responsibility |
|---|---|
| `ResourcePackManager.java` | lifecycle: find in jar → extract → build → hash → host → send → track status; NEVER crashes plugin |
| `ResourcePackBuilder.java` | extract `resourcepack/` from jar, zip → `RomeoServerResourcePack.zip`, SHA-1, metadata file |
| `ResourcePackHost.java` | built-in HTTP host (configurable port) or external URL passthrough |
| `ResourcePackVersionResolver.java` | picks version-specific pack folder for the player's protocol version |
| `ResourcePackStatusBridge.java` | per-player status tracking (accepted/declined/failed), kick-on-decline fallback |

### `com.romeo.paper.registry`
| File | Responsibility |
|---|---|
| `FeatureRegistry.java` | central registry of every feature (see §7) |
| `FeatureEntry.java` | id, display, description, category, permission, command, input type, danger level, GUI opener |

### Refactors of existing files
| File | Change |
|---|---|
| `RomeoCommand.java` | delegate all logic to services; keep command syntax identical |
| `RomeoPaperPlugin.java` | wire services → registry → GUI manager → pack manager; clean boot log |
| `SettingsGUI.java` (legacy) | DELETE — replaced by `gui` package |
| `OptionsScreen.java` (legacy) | DELETE — merged into new GUI system |
| `NpcManager/NpcData/ServerFeatureListener/LegacyText` | keep; expose what services need |

---

## 5. RESOURCE PACK SYSTEM (BUNDLED)

### Pipeline (all automatic on server start)
1. Find `resourcepack/` inside the plugin jar.
2. Extract → `plugins/RomeoPaperPlugin/resourcepack/`.
3. Build `RomeoServerResourcePack.zip` (+ per-version variants).
4. Calculate SHA-1, store metadata (`pack-meta.yml`: sha1, version, size, built-at).
5. Serve via configured `url` or built-in HTTP host.
6. Send to players on join + via GUI; track accepted/declined/failed.
7. Fallback per config if delivery fails (`required` + `kick-on-decline`).

### Bundled layout
```
paper/src/main/resources/resourcepack/
├── pack.mcmeta
└── assets/romeopaper/
    ├── textures/gui/   (ui textures below)
    ├── textures/misc/  (icons below)
    └── font/           (optional, don't replace vanilla font wholesale)
```

### Required GUI textures
`background, button_normal, button_hover, button_selected, button_disabled,
small_button, small_button_hover, back, done, arrow, input, input_selected,
confirmation, panel, divider`

### Required pixel-art icons (consistent style!)
`server, motd, players, icon, npc, skin, name, actions, behavior, commands,
resourcepack, settings, information, save, reload, reset, delete, confirm, cancel`

### Text hierarchy
| Element | Color |
|---|---|
| Title | gold/yellow |
| Button | light grey/white |
| Description | grey |
| Value | white / highlighted |
| Warning | red |
| Success | green |

### Config (NEW section only)
```yaml
gui:
  enabled: true
resource-pack:
  enabled: true
  required: false
  url: ""
  host:
    enabled: true
    port: 8765
  kick-on-decline: false
  version: "1.0.0"
```

---

## 6. INPUT & THREADING CONTRACTS

### InputManager API
```java
beginString(player, prompt, Consumer<String> onMain)
beginInteger(player, prompt, Consumer<Integer> onMain)   // invalid → "Invalid number. Try again."
beginUrl(player, prompt, Consumer<String> onMain)        // invalid → "Invalid URL."
beginConfirmation(player, prompt, Runnable yes, Runnable no)
beginNpcSelection(player, list, Consumer<NpcData> onMain)
beginMultiStep(player, steps)                            // e.g. create NPC → then configure
cancel(player)
```
- Any input of `cancel` aborts. GUI reopens after every finished session.
- Chat event is ALWAYS cancelled during an active session.

### Threading rules
- Chat handler: cancel event → `Bukkit.getScheduler().runTask(plugin, ...)` → callback on main thread.
- Icon/skin downloads: async OK; applying results: main thread only.
- Pack hosting HTTP: own thread; never touch Bukkit from it directly.

---

## 7. FEATURE REGISTRY (EXPANSION CONTRACT)

Every feature registers once; commands AND GUI consume the same entry:
```java
FeatureEntry.builder()
    .id("server.motd")
    .display("Server MOTD")
    .description("Change the server list MOTD")
    .category(Category.SERVER)          // SERVER|PLAYER|NPC|VISUAL|UTILITY|SYSTEM
    .permission("plugin.admin")
    .command("editmotd")
    .input(InputType.TEXT)              // NONE|TEXT|INTEGER|URL|SELECTION|CONFIRM
    .danger(DangerLevel.SAFE)           // SAFE|RESET|DESTRUCTIVE
    .opener(MotdGUI::new)
    .register();
```
Registry drives: Commands menu listing, permission checks, input routing, danger confirmations.

### Workflow for EVERY future command (never skip)
1. Analyze: informational? setting? editor? destructive? NPC-related? input needed?
2. Pick category → correct submenu (NOT the main screen).
3. Reuse/add a service method (shared with the command).
4. Add GUI controls + input flow.
5. Add pack assets only if truly needed.
6. Register in FeatureRegistry.

Example: `/editmotdgradient <c1> <c2>` → Server Settings → MOTD → Appearance → Gradient.

---

## 8. COMMAND MAP (CURRENT)

| Command | Service call | GUI placement |
|---|---|---|
| `/ping` | `ping(player)` | Commands → Utility (click = "Your latency: X ms") |
| `/editmotd <text>` | `MotdService.set` | Server Settings → MOTD |
| `/editplayers online\|max <n>` | `PlayerCountService.set` | Server Settings → Player Count |
| `/editicon <url>` | `IconService.downloadAsync` | Server Settings → Server Icon |
| `/npc create\|remove\|list\|move\|info\|skin\|name\|nametag\|glow\|look\|tp\|enable\|disable\|save\|reload\|rightclick\|leftclick\|particle` | `NpcService.*` | NPC Manager tree (§3) |
| `/settings` | `GuiManager.openMain` | Main GUI |

NPC GUI exposes ONLY what the backend actually supports — no invented options.

---

## 9. BUILD PHASES

- [x] **P0 — Plan committed** (`work.md`, pushed `e15d05f`)
- [ ] **P1 — Services layer** (Motd/PlayerCount/Icon/Config/NpcService) + command refactor off direct config
- [ ] **P2 — InputManager/InputSession** (single listener, thread-safe, validators)
- [ ] **P3 — GUI framework** (GuiManager slot-routing, GuiContext history, ConfirmationGUI, MenuIcons)
- [ ] **P4 — Screens** (Settings→Server→Motd/PlayerCount/Icon, Commands, PluginInfo, Advanced)
- [ ] **P5 — NPC suite** (Hub/List/Control/Appearance/Actions/Behavior/Storage + create/remove flows)
- [ ] **P6 — Resource pack** (builder/host/resolver/status bridge + bundled assets + GUI screen)
- [ ] **P7 — FeatureRegistry** + wire Commands menu from registry
- [ ] **P8 — Config additions** (`gui:`, `resource-pack:` sections only)
- [ ] **P9 — Logging polish** (`Starting… → Commands registered. → NPC system loaded. → Resource pack found/generated → SHA-1 → Settings GUI registered. → Ready.`)
- [ ] **P10 — Build green** (`./gradlew build`), update `COMMANDS_GUIDE.md` + `README.md`
- [ ] **P11 — Live test** on local Paper server (`start_local_server.py`)

---

## 10. ERROR HANDLING MATRIX

| Failure | Behavior |
|---|---|
| Pack generation fails | `[ResourcePack] ERROR: ...` — plugin continues, GUI shows pack disabled |
| GUI asset missing | log exact missing asset path |
| Invalid NPC data in npcs.yml | skip that NPC, load the rest |
| Invalid number input | `Invalid number. Try again.` (session stays open) |
| Invalid URL input | `Invalid URL.` (session stays open) |
| Pack delivery fails | configured fallback; `required`+`kick-on-decide` honored |
| World missing for NPC | skip spawn, warn once |
| Reflection/compat miss (e.g. `setGlowing`) | silent no-op via existing MethodBridge pattern |

---

## 11. VISUAL TARGET (VANILLA OPTIONS LOOK)

- Inventory GUI resembling Minecraft Java Options: grey buttons, dark borders, subtle shading, hover states.
- Gold headings, light-grey button labels, grey descriptions, white values.
- Pixel-art icons, clean spacing, dark background. Rectangular, simple controls.
- FORBIDDEN: web dashboard, rounded cards, glassmorphism, neon, futuristic UI, gradients spam, fantasy inventories.
- Player reaction test: *"I am inside Minecraft's settings."*

---

## 12. DEFINITION OF DONE

- [ ] All commands work unchanged in chat (same syntax, same results).
- [ ] Every GUI control performs a real action or displays a real value.
- [ ] GUI and commands share 100% of business logic via services.
- [ ] Zero `dispatchCommand` reuse inside GUI code.
- [ ] One chat listener total for input.
- [ ] Resource pack fully bundled; single-jar install; auto-extract/host/send/track works.
- [ ] `./gradlew build` green; boot log clean sequence; no console spam.
- [ ] NPC flows: create→control→edit→delete all survive server restart (npcs.yml intact).
- [ ] Adding a new command = 1 service method + 1 screen hookup + 1 registry entry (documented in COMMANDS_GUIDE.md).
