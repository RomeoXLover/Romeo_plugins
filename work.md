# ROMEO PAPER PLUGIN — MASTER IMPLEMENTATION SPECIFICATION

You are working on an existing Minecraft Paper plugin called:

RomeoPaperPlugin

This document is the MASTER BLUEPRINT.

Do not treat this as a conceptual example.
Implement the architecture and features in the existing project.

Before changing code:

1. Inspect the entire existing project.
2. Read readit.md.
3. Read work.md.
4. Inspect plugin.yml.
5. Inspect RomeoPaperPlugin.java.
6. Inspect RomeoCommand.java.
7. Inspect the existing NPC implementation.
8. Inspect existing SettingsGUI.java / OptionsScreen.java.
9. Inspect existing configuration files.
10. Inspect the Gradle build.
11. Preserve working functionality.
12. Then implement the architecture below.

============================================================
0. PROJECT FACTS
============================================================

Project:

RomeoPaperPlugin

Platform:

Paper/Bukkit

Primary target:

Paper 1.21

Server-side only.

NO client-side mods required.

Players do NOT need:

Fabric
Forge
NeoForge
Lunar
Badlion
Feather

Build:

./gradlew build

Main artifact:

paper/build/libs/paper-1.0.0.jar

Final distribution name:

RomeoPaperPlugin.jar

Permission:

plugin.admin

Default:

op

Existing configuration keys MUST NOT be renamed:

server-list.motd
server-list.max-players
server-list.online-players
server-list.cached-icon

Existing local testing:

python3 start_local_server.py

Java:

21+

============================================================
1. PRIMARY MISSION
============================================================

Turn the existing plugin into a complete expandable Minecraft-style
server administration system.

The system consists of:

1. Command system
2. Shared services
3. Settings GUI
4. NPC GUI
5. Resource-pack system
6. Resource-pack API integration
7. Feature registry
8. Reusable input system
9. Configuration system
10. Version-specific resource-pack system

The architecture must be:

                         COMMANDS
                            |
                            v
                    SHARED SERVICES
                            |
               +------------+------------+
               |                         |
               v                         v
          GUI SYSTEM                OTHER SYSTEMS
               |
               v
       RESOURCE PACK UI
               |
               v
        MINECRAFT-STYLE GUI


Commands and GUI MUST NEVER contain duplicated business logic.

============================================================
2. DISTRIBUTION PROMISE
============================================================

The server owner installs ONE file:

RomeoPaperPlugin.jar

They should NOT have to manually install:

resourcepack.zip
texture files
font files
GUI assets

The resource pack is bundled INSIDE the plugin JAR.

Example:

RomeoPaperPlugin.jar
│
├── plugin.yml
├── config.yml
├── Java classes
└── resourcepack/
    ├── pack.mcmeta
    └── assets/
        └── romeopaper/
            ├── textures/
            └── font/

When the plugin starts:

1. Find bundled resource pack.
2. Extract it.
3. Build ZIP.
4. Calculate SHA-1.
5. Determine resource-pack version.
6. Contact the Resource Pack API for distribution metadata if enabled.
7. Cache the result.
8. Send the pack to players when appropriate.

============================================================
3. IMPORTANT RESOURCE PACK LIMITATION
============================================================

A server plugin cannot literally replace Minecraft's native
client OptionsScreen.

Do NOT attempt to fake a client-side OptionsScreen through packets.

Instead:

Use a server-side inventory GUI.

Use the bundled resource pack to make that GUI visually resemble
Minecraft Java Edition's vanilla Options screen.

Target visual language:

Minecraft Java Options screen.

Use:

- Minecraft-style pixel typography
- grey rectangular buttons
- dark borders
- subtle shading
- hover states
- gold/yellow headings
- light-grey labels
- white values
- dark backgrounds
- pixel-art icons
- consistent spacing
- simple rectangular controls

DO NOT use:

- web dashboard styling
- rounded cards
- glassmorphism
- neon
- futuristic UI
- huge gradients
- random fantasy inventory styling
- modern mobile-app UI

The player should feel:

"I am inside Minecraft's settings."

============================================================
4. MAIN GUI BLUEPRINT
============================================================

Command:

/settings

opens:

                    Options...

+-----------------------------+  +-----------------------------+
| Server Settings...          |  | Player Settings...          |
| MOTD, player count, icon    |  | Player-related settings     |
+-----------------------------+  +-----------------------------+

+-----------------------------+  +-----------------------------+
| NPC Manager...              |  | Visual Settings...          |
| Manage server NPCs          |  | Visual plugin settings      |
+-----------------------------+  +-----------------------------+

+-----------------------------+  +-----------------------------+
| Commands...                 |  | Resource Pack...            |
| Plugin commands             |  | Pack status/configuration   |
+-----------------------------+  +-----------------------------+

+-----------------------------+  +-----------------------------+
| Advanced...                 |  | Plugin Information...       |
| Advanced administration     |  | Version/status/information  |
+-----------------------------+  +-----------------------------+

                         [ Done ]

IMPORTANT:

Do not show empty categories.

If a category contains no registered features, hide it.

The FeatureRegistry controls this.

============================================================
5. GUI NAVIGATION BLUEPRINT
============================================================

Main:

/settings
   |
   +-- Server Settings...
   |      |
   |      +-- MOTD...
   |      |
   |      +-- Player Count...
   |      |
   |      +-- Server Icon...
   |      |
   |      +-- Preview Server List
   |
   +-- Player Settings...
   |
   +-- NPC Manager...
   |      |
   |      +-- NPC List
   |      +-- Create NPC
   |      +-- Remove NPC
   |      +-- NPC Information
   |      +-- NPC Control
   |             |
   |             +-- Appearance...
   |             +-- Actions...
   |             +-- Behavior...
   |             +-- Teleport
   |             +-- Move
   |             +-- Enable
   |             +-- Disable
   |             +-- Delete
   |
   +-- Visual Settings...
   |
   +-- Commands...
   |
   +-- Resource Pack...
   |
   +-- Advanced...
   |
   +-- Plugin Information...

Every submenu:

TITLE
CURRENT VALUES
CONTROLS
BACK / DONE

============================================================
6. GUI CONTEXT SYSTEM
============================================================

Create:

GuiContext

Each player has a context.

Example:

GuiContext
{
    UUID player;

    Stack<Menu> history;

    UUID selectedNpc;

    InputSession inputSession;

    pendingConfirmation;

    currentCategory;
}

Example navigation:

SettingsGUI
    ↓
ServerSettingsGUI
    ↓
MotdGUI
    ↓
InputManager
    ↓
MotdService
    ↓
MotdGUI

When Back is clicked:

MotdGUI
    ↓
ServerSettingsGUI

Do NOT blindly reopen /settings every time.

Maintain proper history.

============================================================
7. CURRENT COMMANDS
============================================================

The existing commands are:

/ping

/editmotd <text>

/editplayers <online|max> <count>

/editicon <imageUrl>

/npc <action> ...

/settings

============================================================
8. COMMAND MAP
============================================================

/ping

Location:

Commands
    → Utility

Purpose:

Display player ping.

No complex GUI required.

Example:

Player clicks:

[ Ping ]

Chat:

Your latency: 12 ms

Use existing ping implementation.

------------------------------------------------------------

/editmotd <text>

Location:

Server Settings
    → MOTD

------------------------------------------------------------

/editplayers <online|max> <count>

Location:

Server Settings
    → Player Count

------------------------------------------------------------

/editicon <imageUrl>

Location:

Server Settings
    → Server Icon

------------------------------------------------------------

/npc <action>

Location:

NPC Manager

------------------------------------------------------------

/settings

Location:

Main Settings

============================================================
9. SERVER SETTINGS GUI
============================================================

Open:

Server Settings...

Show:

                    Server Settings

+-----------------------------+  +-----------------------------+
| MOTD...                     |  | Player Count...             |
| Edit server list MOTD       |  | Online/max display          |
+-----------------------------+  +-----------------------------+

+-----------------------------+  +-----------------------------+
| Server Icon...              |  | Preview Server List         |
| Change server icon          |  | Preview metadata            |
+-----------------------------+  +-----------------------------+

                         [ Back ]

============================================================
10. MOTD GUI
============================================================

Click:

MOTD...

Show:

                    Server MOTD

Current MOTD:

Welcome to Romeo Network!

Buttons:

[ Edit MOTD... ]

[ Preview MOTD ]

[ Reset MOTD ]

[ Back ]

Edit flow:

Player clicks:

Edit MOTD...

Plugin says:

Enter the new MOTD in chat:

Player:

&6Welcome &fto &bRomeo Network!

InputManager captures it.

Then:

InputManager
    ↓
main thread
    ↓
MotdService.set(...)
    ↓
Config update
    ↓
Config save
    ↓
GUI reopen

Do NOT use:

Bukkit.dispatchCommand(...)

to execute /editmotd internally.

============================================================
11. PLAYER COUNT GUI
============================================================

Click:

Player Count...

Show:

                    Player Count

Online display:
42

Maximum display:
100

Buttons:

[ Edit Online Count... ]

[ Edit Maximum Count... ]

[ Reset Online Count ]

[ Reset Maximum Count ]

[ Back ]

Example:

Edit Online Count...

Prompt:

Enter online player count:

Player:

42

Call:

PlayerCountService.setOnline(42)

Same backend used by:

/editplayers online 42

============================================================
12. SERVER ICON GUI
============================================================

Click:

Server Icon...

Show:

                    Server Icon

Current icon:

[ PREVIEW ]

Buttons:

[ Change Icon URL... ]

[ Reset Icon ]

[ Preview ]

[ Back ]

Change Icon URL:

Prompt:

Enter direct image URL:

Player enters URL.

Call:

IconService.downloadAsync(...)

Reuse the existing downloader.

Do NOT implement another icon downloader.

Network operation:

ASYNC

Final Bukkit update:

MAIN THREAD

============================================================
13. NPC SYSTEM
============================================================

Current NPC commands:

create
remove
list
move
info
skin
name
nametag
glow
look
tp
enable
disable
save
reload
rightclick
leftclick
particle

Do NOT put all of these on one menu.

Use nested menus.

============================================================
14. NPC MANAGER GUI
============================================================

NPC Manager...

Show:

[ NPC List... ]

[ Create NPC... ]

[ Remove NPC... ]

[ NPC Information... ]

[ Appearance... ]

[ Actions... ]

[ Behavior... ]

[ Storage... ]

[ Back ]

============================================================
15. NPC LIST GUI
============================================================

Show all existing NPCs.

Example:

                    NPC List

[ Bob ]

[ Shopkeeper ]

[ Guide ]

[ Tutorial ]

Clicking:

Bob

opens:

NPC Control

============================================================
16. NPC CONTROL GUI
============================================================

Example:

                    NPC: Bob

[ Information ]

[ Appearance... ]

[ Actions... ]

[ Behavior... ]

[ Teleport ]

[ Move ]

[ Enable ]

[ Disable ]

[ Delete ]

[ Back ]

Delete must require confirmation.

============================================================
17. NPC CREATE
============================================================

Click:

Create NPC...

Prompt:

Enter NPC name:

Player:

Bob

InputManager:

beginString(...)

Then:

NpcService.create(...)

Then:

NPC Control

Do NOT call:

/npc create Bob

internally.

Use shared service.

============================================================
18. NPC REMOVE
============================================================

Click:

Remove NPC...

Show NPC list.

Player clicks:

Bob

Show:

Delete NPC "Bob"?

[ Confirm ]

[ Cancel ]

If confirmed:

NpcService.remove(Bob)

Then return to NPC list.

============================================================
19. NPC APPEARANCE
============================================================

Appearance...

Show:

[ Skin... ]

[ Name... ]

[ Nametag... ]

[ Glow ]

[ Particles... ]

[ Back ]

Only show controls supported by the current NPC backend.

============================================================
20. NPC SKIN
============================================================

Skin...

Show:

Current skin:

Steve

[ Change Skin... ]

Input:

Enter skin name:

Call:

NpcService.setSkin(...)

============================================================
21. NPC NAME
============================================================

Name...

Current:

Bob

[ Change Name... ]

Input:

Enter NPC name:

Call:

NpcService.setName(...)

============================================================
22. NPC NAMETAG
============================================================

Nametag...

[ Edit Nametag... ]

[ Enable ]

[ Disable ]

Use existing NPC backend.

============================================================
23. NPC GLOW
============================================================

Glow:

[ ON ]

[ OFF ]

Clicking toggles existing glow state.

============================================================
24. NPC PARTICLES
============================================================

Particles...

Only expose functionality that actually exists.

Potential controls:

[ Enable / Disable ]

[ Particle Type... ]

[ Amount... ]

ONLY create these controls if the existing implementation supports them.

Never invent backend functionality merely to fill the GUI.

============================================================
25. NPC ACTIONS
============================================================

Actions...

[ Right Click... ]

[ Left Click... ]

[ Teleport ]

[ Move ]

[ Look ]

[ Back ]

============================================================
26. RIGHT CLICK ACTION
============================================================

Right Click...

Show current configured action.

[ Edit Action... ]

[ Clear Action ]

[ Test Action ]

Use existing NPC action implementation.

============================================================
27. LEFT CLICK ACTION
============================================================

Left Click...

[ Edit Action... ]

[ Clear Action ]

[ Test Action ]

Use existing implementation.

============================================================
28. NPC BEHAVIOR
============================================================

Behavior...

Only show actual backend functionality.

Example:

[ Look ]

[ Enable ]

[ Disable ]

============================================================
29. NPC STORAGE
============================================================

Storage...

[ Save NPCs ]

[ Reload NPCs ]

These call:

NpcService.save()

NpcService.reload()

NOT Bukkit.dispatchCommand.

============================================================
30. COMMANDS GUI
============================================================

Commands...

Organize commands into categories.

Example:

Commands

UTILITY

[ /ping ]

SERVER

[ /editmotd ]

[ /editplayers ]

[ /editicon ]

NPC

[ /npc ]

SYSTEM

[ /settings ]

The command menu is generated from FeatureRegistry.

Do not hardcode the list where possible.

============================================================
31. RESOURCE PACK GUI
============================================================

Resource Pack...

Show:

                    Resource Pack

Bundled:
YES

Status:
Loaded

Version:
1.0.0

SHA-1:
xxxxxxxxxxxxxxxx

Buttons:

[ Send Pack Again ]

[ Regenerate Pack ]

[ Pack Information ]

[ Back ]

============================================================
32. RESOURCE PACK CONTENT
============================================================

The resource pack must be specifically designed for RomeoPaperPlugin.

It is NOT a generic texture pack.

It should contain:

GUI background
button normal
button hover
button selected
button disabled
small button
small button hover
input
input selected
panel
divider
back
done
arrows
confirmation

Icons:

server
motd
players
icon
npc
skin
name
actions
behavior
commands
resourcepack
settings
information
save
reload
reset
delete
confirm
cancel

All icons must share the same pixel-art style.

============================================================
33. RESOURCE PACK STYLE
============================================================

Visual target:

Minecraft Java Edition vanilla Options.

Use:

- grey buttons
- dark edges
- pixelated shading
- subtle highlights
- Minecraft-style text
- gold title
- light grey labels
- white values
- red warnings
- green success
- dark background

Do NOT use:

rounded web cards
neon
glass
modern UI
3D futuristic icons
random item textures
overly saturated gradients

============================================================
34. RESOURCE PACK FONT
============================================================

Do not globally replace Minecraft's font unless necessary.

Use:

assets/romeopaper/font/

for plugin-specific font assets.

Keep it visually close to vanilla Minecraft.

============================================================
35. BUNDLED RESOURCE PACK
============================================================

Source:

paper/src/main/resources/resourcepack/

Example:

resourcepack/
├── pack.mcmeta
└── assets/
    └── romeopaper/
        ├── textures/
        │   ├── gui/
        │   └── misc/
        └── font/

Gradle must package this into:

RomeoPaperPlugin.jar

============================================================
36. RESOURCE PACK BUILD PIPELINE
============================================================

On startup:

ResourcePackManager.initialize()

Step 1:

Find bundled resource pack.

Step 2:

Extract to:

plugins/RomeoPaperPlugin/resourcepack/

Step 3:

Build:

RomeoServerResourcePack.zip

Step 4:

Calculate SHA-1.

Step 5:

Write:

pack-meta.yml

Example:

version: 1.0.0
sha1: XXXXX
size: 123456
built-at: ...

Step 6:

Resolve distribution URL.

Step 7:

Cache metadata.

Step 8:

Send to players when configured.

Never crash the plugin if generation fails.

============================================================
37. RESOURCE PACK API
============================================================

IMPORTANT:

DO NOT make the Minecraft plugin serve the production resource-pack
ZIP through the same website/API server.

The API should only provide METADATA.

Production architecture:

Minecraft Server
       |
       | HTTPS
       v
Romeo Resource Pack API
       |
       | returns metadata
       v
CDN / Object Storage
       |
       | actual ZIP
       v
Minecraft Client

Example request:

GET:

https://api.example.com/v1/resourcepack/latest

Example response:

{
  "success": true,
  "version": "1.0.0",
  "url": "https://cdn.example.com/romeopaper/1.0.0/pack.zip",
  "sha1": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "required": true
}

IMPORTANT:

The API must NOT proxy the ZIP.

Minecraft should download directly from:

https://cdn.example.com/romeopaper/1.0.0/pack.zip

This prevents the API/website server from becoming a bandwidth bottleneck.

============================================================
38. API CONFIGURATION
============================================================

Add only a new config section.

Example:

resource-pack:
  enabled: true

  required: false

  api-url: "https://api.example.com/v1/resourcepack/latest"

  kick-on-decline: false

  version: "1.0.0"

Do NOT rename existing config keys.

============================================================
39. RESOURCE PACK API CACHE
============================================================

The plugin must cache the last valid API response.

Example:

plugins/RomeoPaperPlugin/resourcepack-cache.json

{
  "version": "1.0.0",
  "url": "https://cdn.example.com/romeopaper/1.0.0/pack.zip",
  "sha1": "...",
  "required": true
}

If API fails:

Use cached response.

Do not crash.

Do not spam retries.

Use:

- timeout
- limited retry
- backoff

Never call API every tick.

API calls:

- plugin startup
- /resourcepack reload
- manual refresh
- optional configured interval

============================================================
40. RESOURCE PACK VERSIONING
============================================================

Do NOT claim one resource pack supports every Minecraft version.

Use version-specific packs.

Example:

resourcepacks/
├── 1.20/
├── 1.20.1/
├── 1.21/
├── 1.21.1/
└── ...

ResourcePackVersionResolver chooses the compatible pack.

The GUI/backend remains shared.

Only resource-pack assets and metadata are version-specific where required.

If no compatible pack exists:

log warning.

Do not send an incompatible pack.

============================================================
41. RESOURCE PACK STATUS
============================================================

Track:

UNKNOWN
DOWNLOADED
ACCEPTED
DECLINED
FAILED
DISCARDED

If required:

kick-on-decline determines whether player is kicked.

Example kick:

You must accept the Romeo Server resource pack to play.

Do not resend repeatedly every tick.

============================================================
42. RESOURCE PACK COMMANDS
============================================================

Add:

/resourcepack status

/resourcepack reload

/resourcepack generate

/resourcepack send [player]

Permission:

plugin.admin

These are useful for administration/debugging.

============================================================
43. INPUT MANAGER
============================================================

ONE input manager.

No per-GUI chat listeners.

Create:

InputManager
InputSession

API:

beginString(player, prompt, Consumer<String> callback)

beginInteger(player, prompt, Consumer<Integer> callback)

beginUrl(player, prompt, Consumer<String> callback)

beginConfirmation(player, prompt, Runnable yes, Runnable no)

beginNpcSelection(player, list, Consumer<NpcData> callback)

beginMultiStep(player, steps)

cancel(player)

============================================================
44. INPUT EXAMPLE
============================================================

Player:

/settings

→ Server Settings

→ MOTD

→ Edit MOTD

Plugin:

Enter new MOTD in chat:

Player:

Welcome!

InputManager:

1. captures message
2. cancels chat event
3. validates
4. switches to main thread
5. calls MotdService.set()
6. saves config
7. reopens MOTD GUI

============================================================
45. ASYNC CHAT SAFETY
============================================================

AsyncPlayerChatEvent is asynchronous.

NEVER perform Bukkit API operations directly inside it.

Incorrect:

@EventHandler
public void onChat(AsyncPlayerChatEvent event) {
    player.openInventory(...);
    Bukkit.dispatchCommand(...);
}

Correct concept:

@EventHandler
public void onChat(AsyncPlayerChatEvent event) {

    if (!inputManager.hasSession(event.getPlayer())) {
        return;
    }

    event.setCancelled(true);

    String input = event.getMessage();

    Bukkit.getScheduler().runTask(plugin, () -> {
        inputManager.handleInput(event.getPlayer(), input);
    });
}

Prefer services instead of command dispatch.

============================================================
46. SHARED SERVICES
============================================================

Create:

com.romeo.paper.services

MotdService
PlayerCountService
IconService
NpcService
ConfigService

Example:

MotdService:

get()
set(String)
reset()
preview()

PlayerCountService:

getOnline()
getMax()
setOnline(int)
setMax(int)
resetOnline()
resetMax()

IconService:

getCurrent()
downloadAsync(String)
reset()

NpcService:

create()
remove()
list()
move()
info()
skin()
name()
nametag()
glow()
look()
tp()
enable()
disable()
save()
reload()
rightClick()
leftClick()
particle()

GUI and command system both call these services.

============================================================
47. ROMEO COMMAND REFACTOR
============================================================

RomeoCommand must preserve all existing command syntax.

Example:

/editmotd Hello

Command:

MotdService.set("Hello")

NOT:

direct config modification in RomeoCommand.

Likewise:

/editplayers online 42

→ PlayerCountService.setOnline(42)

And:

/editicon URL

→ IconService.downloadAsync(URL)

And NPC commands:

→ NpcService.*

============================================================
48. FEATURE REGISTRY
============================================================

Create:

FeatureRegistry

FeatureEntry

Each feature contains:

id
display
description
category
permission
command
input type
danger level
GUI opener

Example:

FeatureEntry.builder()
    .id("server.motd")
    .display("Server MOTD")
    .description("Change the server list MOTD")
    .category(Category.SERVER)
    .permission("plugin.admin")
    .command("editmotd")
    .input(InputType.TEXT)
    .danger(DangerLevel.SAFE)
    .opener(MotdGUI::new)
    .register();

Categories:

SERVER
PLAYER
NPC
VISUAL
UTILITY
SYSTEM

Input types:

NONE
TEXT
INTEGER
URL
SELECTION
CONFIRM

Danger levels:

SAFE
RESET
DESTRUCTIVE

============================================================
49. FUTURE COMMAND EXPANSION
============================================================

This is extremely important.

When I give you a NEW command later:

DO NOT simply add another random button to the main menu.

Follow this exact process:

1. Read command.
2. Determine purpose.
3. Determine category.
4. Determine required input.
5. Determine danger level.
6. Determine appropriate submenu.
7. Check whether an existing service can handle it.
8. Add service method if necessary.
9. Add GUI control.
10. Add FeatureRegistry entry.
11. Add resource-pack icon ONLY if necessary.
12. Update COMMANDS_GUIDE.md.

Example:

New command:

/editmotdgradient <color1> <color2>

Correct placement:

Server Settings
    ↓
MOTD
    ↓
Appearance
    ↓
Gradient

NOT:

Main Settings
    ↓
Random "Gradient" button

============================================================
50. GUI SECURITY
============================================================

Never trust GUI slots alone.

Every click must validate:

player
permission
current menu
slot
action
selected NPC
input state

Use a custom InventoryHolder/menu context.

Do NOT identify menus only by title.

Titles can be duplicated.

Use holder/context identifiers.

============================================================
51. PERMISSIONS
============================================================

Every administrative operation must verify:

plugin.admin

server-side.

Do not only hide buttons.

Example:

if (!player.hasPermission("plugin.admin")) {
    player.sendMessage(...);
    return;
}

============================================================
52. DESTRUCTIVE ACTIONS
============================================================

Require confirmation for:

NPC delete
reset settings
destructive future features

Example:

Delete NPC "Bob"?

[ Confirm ]

[ Cancel ]

============================================================
53. ADVANCED GUI
============================================================

Advanced...

Show:

[ Reload Configuration ]

[ Force Save ]

[ Resource Pack Diagnostics ]

[ Plugin Diagnostics ]

[ Back ]

Only implement actions that actually exist.

============================================================
54. PLUGIN INFORMATION GUI
============================================================

Plugin Information...

Show:

RomeoPaperPlugin

Version:
1.0.0

Author:
Romeo

Platform:
Paper

Permission:
plugin.admin

Resource Pack:
Loaded

Resource Pack Version:
1.0.0

Commands:
CURRENT_COUNT

[ Back ]

============================================================
55. EMPTY CATEGORY RULE
============================================================

If:

FeatureRegistry

contains zero features in a category:

DO NOT display that category on the main screen.

Example:

If PLAYER currently has no real settings:

do not show:

Player Settings...

Later, if a command is added:

/somethingplayerrelated

and registered under PLAYER:

Player Settings...

automatically appears.

============================================================
56. RESOURCE PACK FALLBACK
============================================================

The GUI must remain functional if the resource pack has not loaded.

Use fallback vanilla Materials/textures.

Flow:

Custom icon available?
    YES → use custom asset
    NO  → vanilla Material fallback

Never make GUI functionality depend on the pack successfully loading.

============================================================
57. THREADING ARCHITECTURE
============================================================

MAIN THREAD:

GUI
Bukkit API
World
NPC operations
Config state changes
Player operations

ASYNC:

HTTP
API calls
Image downloads
Resource-pack generation where safe

When an async task needs Bukkit:

ASYNC
  ↓
Bukkit scheduler
  ↓
MAIN THREAD

============================================================
58. RESOURCE PACK API FAILURE
============================================================

If API is unavailable:

Do NOT crash.

Try cached metadata.

If cached metadata exists:

use it.

If no metadata exists:

log:

[ResourcePack] API unavailable and no cached pack metadata exists.

Continue plugin startup.

============================================================
59. ERROR HANDLING
============================================================

Pack generation failure:

Plugin continues.

GUI shows:

Resource Pack: Disabled / Error

Invalid number:

Invalid number. Try again.

Keep session open.

Invalid URL:

Invalid URL.

Keep session open.

Bad NPC data:

Skip bad NPC.

Load remaining NPCs.

Missing world:

Skip NPC spawn.

Warn once.

Missing GUI asset:

Log exact path.

Do not crash.

Compatibility method missing:

Use existing MethodBridge pattern.

============================================================
60. LOGGING
============================================================

Clean startup:

[RomeoPaperPlugin] Starting...
[RomeoPaperPlugin] Services initialized.
[RomeoPaperPlugin] Commands registered.
[RomeoPaperPlugin] NPC system loaded.
[RomeoPaperPlugin] Resource pack found in plugin JAR.
[RomeoPaperPlugin] Resource pack generated.
[RomeoPaperPlugin] Resource pack SHA-1: ...
[RomeoPaperPlugin] Resource pack API metadata loaded.
[RomeoPaperPlugin] Settings GUI registered.
[RomeoPaperPlugin] Ready.

Do not spam console.

============================================================
61. FILE STRUCTURE
============================================================

Expected structure:

paper/src/main/java/com/romeo/paper/

├── RomeoPaperPlugin.java
├── RomeoCommand.java
│
├── services/
│   ├── MotdService.java
│   ├── PlayerCountService.java
│   ├── IconService.java
│   ├── NpcService.java
│   └── ConfigService.java
│
├── input/
│   ├── InputManager.java
│   └── InputSession.java
│
├── gui/
│   ├── GuiManager.java
│   ├── GuiContext.java
│   ├── SettingsGUI.java
│   ├── ServerSettingsGUI.java
│   ├── MotdGUI.java
│   ├── PlayerCountGUI.java
│   ├── IconGUI.java
│   ├── NpcGUI.java
│   ├── NpcListGUI.java
│   ├── NpcControlGUI.java
│   ├── NpcAppearanceGUI.java
│   ├── NpcActionsGUI.java
│   ├── CommandsGUI.java
│   ├── ResourcePackGUI.java
│   ├── AdvancedGUI.java
│   ├── PluginInfoGUI.java
│   ├── ConfirmationGUI.java
│   └── MenuIcons.java
│
├── resourcepack/
│   ├── ResourcePackManager.java
│   ├── ResourcePackBuilder.java
│   ├── ResourcePackVersionResolver.java
│   ├── ResourcePackApiClient.java
│   └── ResourcePackStatusBridge.java
│
└── registry/
    ├── FeatureRegistry.java
    └── FeatureEntry.java

Resources:

paper/src/main/resources/

├── plugin.yml
├── config.yml
│
└── resourcepack/
    ├── pack.mcmeta
    └── assets/
        └── romeopaper/
            ├── textures/
            │   ├── gui/
            │   └── misc/
            └── font/

============================================================
62. EXISTING FILE REFACTOR
============================================================

RomeoCommand.java:

Keep command syntax.

Move business logic to services.

RomeoPaperPlugin.java:

Initialize:

ConfigService
MotdService
PlayerCountService
IconService
NpcService
InputManager
GuiManager
FeatureRegistry
ResourcePackManager

SettingsGUI.java legacy:

Replace with new GUI framework.

OptionsScreen.java legacy:

Remove/merge into new GUI framework.

NpcManager:

Keep.

NpcData:

Keep.

ServerFeatureListener:

Keep.

LegacyText:

Keep if still needed.

============================================================
63. BUILD REQUIREMENTS
============================================================

Run:

./gradlew build

Expected:

BUILD SUCCESSFUL

Final:

paper/build/libs/paper-1.0.0.jar

Rename:

RomeoPaperPlugin.jar

Verify:

jar tf RomeoPaperPlugin.jar

must show:

resourcepack/pack.mcmeta

and:

resourcepack/assets/romeopaper/...

============================================================
64. TEST PLAN
============================================================

Test:

/settings

/editmotd Test MOTD

/editplayers online 42

/editplayers max 100

/editicon https://example.com/icon.png

/ping

/npc create Bob

/npc list

/npc Bob info

/resourcepack status

/resourcepack generate

/resourcepack reload

/resourcepack send

Then restart.

Verify:

1. Commands still work.
2. GUI opens.
3. Navigation works.
4. Back works.
5. Values display correctly.
6. MOTD persists.
7. Player count persists.
8. Icon persists.
9. NPCs persist.
10. Resource pack is bundled in JAR.
11. Resource pack is generated.
12. SHA-1 is calculated.
13. API metadata is retrieved.
14. Cached API metadata works when API is unavailable.
15. Minecraft receives resource pack.
16. Required/optional behavior works.
17. No Inventory#getTitle() errors.
18. No asynchronous Bukkit API errors.
19. No duplicate chat listeners.
20. No duplicated backend logic.

============================================================
65. DEFINITION OF DONE
============================================================

ALL of the following must be true:

[ ] Existing commands work unchanged.

[ ] /settings works.

[ ] GUI has proper navigation.

[ ] Main menu only contains categories.

[ ] Empty categories are hidden.

[ ] GUI values are real.

[ ] GUI actions are real.

[ ] Commands and GUI share services.

[ ] GUI does not use Bukkit.dispatchCommand().

[ ] One InputManager exists.

[ ] Async chat input is thread-safe.

[ ] Permissions are checked on every action.

[ ] Destructive operations require confirmation.

[ ] NPC GUI works.

[ ] NPC persistence works.

[ ] Resource pack is bundled inside the JAR.

[ ] Resource pack is automatically extracted.

[ ] Resource pack ZIP is automatically generated.

[ ] SHA-1 is automatically generated.

[ ] Resource-pack API integration works.

[ ] API returns metadata rather than proxying ZIP traffic.

[ ] CDN/download URL is used directly by Minecraft.

[ ] API response is cached.

[ ] API failures do not crash plugin.

[ ] Version-specific packs are supported.

[ ] GUI has fallback icons if resource pack isn't loaded.

[ ] ./gradlew build succeeds.

[ ] Local Paper server starts.

[ ] No major console errors.

============================================================
66. FUTURE DEVELOPMENT CONTRACT
============================================================

From this point forward, when I give you new commands:

DO NOT rewrite the architecture.

DO NOT add random buttons.

DO NOT duplicate backend logic.

DO NOT create another chat listener.

DO NOT create a separate GUI framework.

DO NOT create a separate resource-pack system.

Instead:

NEW COMMAND
    ↓
Analyze feature
    ↓
Choose category
    ↓
Shared Service
    ↓
FeatureRegistry
    ↓
Existing GUI submenu OR new submenu
    ↓
Resource-pack asset only if necessary
    ↓
COMMANDS_GUIDE.md update

Example:

New:

/maintenance on

Correct:

Advanced
    ↓
Maintenance
        ↓
ON/OFF

Not:

Main Settings
    ↓
Maintenance button

============================================================
67. FINAL USER EXPERIENCE
============================================================

Server owner:

1. Downloads RomeoPaperPlugin.jar.
2. Places it in plugins/.
3. Starts server.

Plugin:

1. Loads.
2. Finds bundled resource pack.
3. Generates required resource-pack files.
4. Gets distribution metadata from API.
5. Registers commands.
6. Loads NPCs.
7. Initializes GUI.
8. Ready.

Admin:

/settings

Sees:

                         Options...

[ Server Settings... ]     [ NPC Manager... ]

[ Commands... ]            [ Resource Pack... ]

[ Advanced... ]            [ Plugin Information... ]

                        [ Done ]

Clicks:

Server Settings...

Sees:

                     Server Settings

[ MOTD... ]                [ Player Count... ]

[ Server Icon... ]         [ Preview Server List ]

                         [ Back ]

Clicks:

MOTD...

Sees:

                       Server MOTD

Current MOTD:
Welcome to Romeo!

[ Edit MOTD... ]

[ Preview MOTD ]

[ Reset MOTD ]

[ Back ]

Clicks Edit.

Enters:

Welcome to Romeo Network!

The value changes.

The GUI refreshes.

No command dispatch.

No duplicated logic.

No server restart required unless the existing feature requires it.

============================================================
FINAL INSTRUCTION
============================================================

Implement this specification inside the existing project.

First inspect the existing implementation.

Do not blindly overwrite working code.

Preserve existing functionality.

Where existing code already provides a working implementation,
wrap/refactor it into the shared service architecture rather than
rewriting it unnecessarily.

After implementation:

1. Build.
2. Fix compilation errors.
3. Start local Paper server.
4. Test commands.
5. Test GUI.
6. Test NPC system.
7. Test resource-pack generation.
8. Test resource-pack API.
9. Test failure/fallback behavior.
10. Verify final JAR contains the resource-pack assets.
11. Update COMMANDS_GUIDE.md.
12. Update README.md.
13. Report exactly what was implemented and any remaining limitations.

Do not claim a feature works until it has been tested.