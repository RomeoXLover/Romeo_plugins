# RomeoPaperPlugin — Settings GUI Blueprint (Work Plan)

Source of truth: `readit.md` (the full blueprint). This file tracks the goals and build order.

## Project

- **Project:** RomeoPaperPlugin — Paper Minecraft server plugin
- **Platform:** Paper/Bukkit (server-side only — no client mods required)
- **Build:** Gradle (`./gradlew build`, plugin jar via `:paper:shadowJar`)

## Primary goal

Create a complete, modular, expandable **server-side Settings GUI** that acts as a
visual frontend for all existing plugin commands and features. Commands remain fully
functional; the GUI is a second frontend over the same shared services.

## Core rules

- Resource pack is **bundled inside the plugin jar** — server owner installs one jar only.
- GUI = inventory menu styled like Minecraft's vanilla Options screen (grey buttons,
  dark borders, gold headings, pixel-art icons). No web-dashboard look.
- Never duplicate backend logic: `/editmotd` and GUI → Edit MOTD call the **same** service.
- Never reuse logic by running `Bukkit.dispatchCommand(...)` internally.
- One reusable chat-input system (`InputManager`) — no per-GUI chat listeners.
- Async chat events must hop to the main thread before touching Bukkit API.
- All admin actions check `plugin.admin` server-side.
- Confirmation menus for destructive actions (e.g. delete NPC).
- Keep the main `/settings` screen clean — categories only, no command spam.

## GUI hierarchy

```
/settings (Options...)
├── Server Settings
│   ├── MOTD...            (Edit / Preview / Reset)
│   ├── Player Count...    (Edit online / max, Reset)
│   ├── Server Icon...     (Change URL / Reset / Preview)
│   └── Preview Server List
├── Player Settings
├── NPC Manager
│   ├── NPC List           → NPC Control (per-NPC)
│   │   ├── Information / Appearance / Actions / Behavior
│   │   ├── Teleport / Move / Enable / Disable / Delete
│   │   └── Appearance → Skin, Name, Nametag, Glow, Particles
│   │   └── Actions → Right Click / Left Click (Edit / Clear / Test)
│   ├── Create NPC
│   ├── Remove NPC         (confirm)
│   └── Storage            (Save / Reload)
├── Visual Settings
├── Commands               (organized: Utility / Server / NPC / Settings)
├── Resource Pack          (status, Send Again, Regenerate, Info)
├── Advanced
└── Plugin Information
```

## Command map (current)

| Command | GUI placement |
|---|---|
| `/ping` | Commands → Utility (simple message, no submenu) |
| `/editmotd <text>` | Server Settings → MOTD |
| `/editplayers <online\|max> <count>` | Server Settings → Player Count |
| `/editicon <imageUrl>` | Server Settings → Server Icon |
| `/npc <action>` | NPC Manager → List / Control / Appearance / Actions / Behavior / Storage |
| `/settings` | Main Settings GUI |

## Architecture

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

Planned packages:

- `com.romeo.paper.gui` — SettingsGUI, ServerSettingsGUI, MotdGUI, PlayerCountGUI,
  IconGUI, NpcGUI, NpcListGUI, NpcControlGUI, NpcAppearanceGUI, NpcActionsGUI,
  CommandsGUI, ResourcePackGUI, AdvancedGUI
- `com.romeo.paper.resourcepack` — ResourcePackManager, ResourcePackBuilder,
  ResourcePackHost, ResourcePackVersionResolver
- `com.romeo.paper.input` — InputManager, InputSession
- `com.romeo.paper.services` — MotdService, PlayerCountService, IconService, NpcService
- `FeatureRegistry` — central registry (id, display, description, category, permission,
  GUI handler, command, input type, danger level)

## Resource pack system

1. Bundled under `paper/src/main/resources/resourcepack/` (per MC version folders, e.g. `resourcepacks/1.21/`).
2. On enable: extract from jar → `plugins/RomeoPaperPlugin/resourcepack/`
3. Build `RomeoServerResourcePack.zip` + calculate SHA-1 + store metadata.
4. Serve via configured URL or built-in host; send to players on join; track accepted/declined.
5. Config: `resource-pack.enabled`, `required`, `url`, `kick-on-decline`, `version`.
6. Pack supplies GUI textures (button normal/hover/selected/disabled, panel, divider,
   input, confirmation) and pixel-art icons (server, motd, players, npc, skin, actions, …).

## Workflow for every future command

1. Analyze what the command does (setting? editor? informational? destructive?).
2. Identify its category — place it in the correct submenu, never the main screen.
3. Reuse existing backend logic (add a service method if missing — shared with the command).
4. Add GUI controls, input handling (string / number / URL / selection / confirmation) as needed.
5. Register it in the FeatureRegistry.
6. Add resource-pack assets only if genuinely needed.

## Error handling & logging

- Resource-pack failure logs `[ResourcePack] ERROR: ...` — must not crash the plugin.
- Invalid NPC data is skipped at load; missing assets logged by exact path.
- Player input errors: "Invalid number. Try again." / "Invalid URL."
- Clean startup log sequence, no console spam:
  `Starting... → Commands registered. → NPC system loaded. → Resource pack found/generated
  → SHA-1 → Settings GUI registered. → Ready.`

## End goal

A real, expandable server administration system where every new command plugs into the
existing architecture — no rewrites, no duplicated logic, no fake settings.
