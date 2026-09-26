============================================================
ROMEO PAPER PLUGIN — COMPLETE SETTINGS GUI BLUEPRINT
============================================================

PROJECT:
RomeoPaperPlugin

PLATFORM:
Paper Minecraft server

PRIMARY GOAL:
Create a complete server-side Settings GUI system that acts as
a visual frontend for all existing plugin commands and features.

The system must be modular, expandable, and designed so that
new commands can easily be added to the correct GUI category.

============================================================
1. FUNDAMENTAL REQUIREMENTS
============================================================

This is a SERVER-SIDE plugin.

Players must NOT need:

- Fabric
- Forge
- NeoForge
- Lunar Client
- Badlion
- Feather
- Any client-side mod

The plugin must work using:

- Paper/Bukkit API
- Server-side inventory GUI
- Server resource pack
- Plugin configuration
- Existing command/backend systems

The resource pack is BUNDLED INSIDE THE PLUGIN JAR.

The server owner installs:

RomeoPaperPlugin.jar

ONLY.

They should NOT have to manually install a separate resource-pack
ZIP.

The plugin handles:

1. Loading the bundled resource pack.
2. Extracting it.
3. Building the final ZIP.
4. Calculating SHA-1.
5. Hosting/serving it through the configured URL or built-in host.
6. Sending it to players.
7. Handling acceptance/decline.
8. Tracking resource-pack status.

============================================================
2. IMPORTANT GUI LIMITATION
============================================================

This is NOT a client-side recreation of Minecraft's actual
OptionsScreen.

A server plugin cannot directly replace the client's native
OptionsScreen.

Instead:

Create a server-side inventory GUI that visually resembles
Minecraft's vanilla Options interface.

The resource pack is used to make the inventory GUI visually
similar to Minecraft.

The target visual language is:

Minecraft Java Edition Options menu.

Use:

- Minecraft pixel-style font
- Minecraft-style grey buttons
- dark borders
- subtle button shading
- Minecraft-style hover states
- gold/yellow headings
- simple rectangular controls
- clean spacing
- pixel-art icons
- dark background
- consistent UI elements

DO NOT create:

- modern web dashboard
- rounded cards
- glassmorphism
- neon UI
- futuristic interface
- excessive gradients
- random fantasy inventory design

============================================================
3. MAIN SETTINGS FLOW
============================================================

The entire GUI hierarchy should follow:

/settings
    |
    v
MAIN SETTINGS
    |
    +-- Server Settings
    |
    +-- Player Settings
    |
    +-- NPC Manager
    |
    +-- Visual Settings
    |
    +-- Commands
    |
    +-- Resource Pack
    |
    +-- Advanced
    |
    +-- Plugin Information

Every category can contain submenus.

Do NOT put every command onto the first screen.

The first screen should remain clean.

============================================================
4. MAIN SETTINGS SCREEN
============================================================

Title:

Options...

Use a two-column layout similar to Minecraft's Options screen.

Concept:

                 Options...

+------------------------+  +------------------------+
| Server Settings...     |  | Player Settings...     |
+------------------------+  +------------------------+

+------------------------+  +------------------------+
| NPC Manager...         |  | Visual Settings...     |
+------------------------+  +------------------------+

+------------------------+  +------------------------+
| Commands...            |  | Resource Pack...       |
+------------------------+  +------------------------+

+------------------------+  +------------------------+
| Advanced...            |  | Plugin Information...  |
+------------------------+  +------------------------+

                         [ Done ]

Each button has:

ICON
TITLE
SHORT DESCRIPTION

Example:

Server Settings...
Edit MOTD, player count and server icon

NPC Manager...
Manage all server NPCs

Resource Pack...
Manage the bundled resource pack

============================================================
5. COMMAND-TO-GUI ARCHITECTURE
============================================================

IMPORTANT:

The GUI is NOT supposed to replace the command system.

Commands remain fully functional.

The GUI is simply another frontend.

Architecture:

PLAYER
  |
  +--------------------+
  |                    |
CHAT COMMAND         /settings
  |                    |
  v                    v
RomeoCommand       GUI Controller
  |                    |
  +---------+----------+
            |
            v
       SHARED SERVICES
            |
            +-- MOTD Service
            +-- Player Count Service
            +-- Icon Service
            +-- NPC Manager
            +-- ResourcePack Manager
            +-- Configuration

Do NOT implement the same feature twice.

For example:

/editmotd
and
Settings GUI -> Edit MOTD

must use the SAME backend method.

============================================================
6. CURRENT COMMAND BLUEPRINT
============================================================

CURRENT COMMANDS:

/ping

/editmotd <text>

/editplayers <online|max> <count>

/editicon <imageUrl>

/npc <action> ...

/settings

============================================================
7. /PING
============================================================

Command:

/ping

Purpose:

Show player's latency.

GUI:

This does NOT need a complicated submenu.

Place it under:

Commands -> Utility

or

Plugin Information

Button:

Ping

Description:

Show your current latency.

Clicking it can simply send:

Your latency: XX ms

Do not create unnecessary settings for ping.

============================================================
8. SERVER SETTINGS
============================================================

Create:

Server Settings...

Inside:

Server Settings

+------------------------+
| MOTD...                |
+------------------------+

+------------------------+
| Player Count...        |
+------------------------+

+------------------------+
| Server Icon...         |
+------------------------+

+------------------------+
| Preview Server List    |
+------------------------+

[ Back ]

============================================================
9. MOTD SYSTEM
============================================================

Existing command:

/editmotd <text>

GUI location:

/settings
    -> Server Settings
        -> MOTD...

Screen:

Server MOTD

Display current value:

Current MOTD:
Welcome to Romeo Network!

Buttons:

[ Edit MOTD... ]

[ Preview MOTD ]

[ Reset MOTD ]

[ Back ]

Edit MOTD flow:

CLICK:

Edit MOTD...

GUI closes or transitions to an input screen.

Message:

Enter the new MOTD in chat.

Player types:

Welcome to Romeo Network!

The input manager captures the message.

The chat event is cancelled.

The backend updates the MOTD.

The GUI reopens.

Do NOT internally execute:

Bukkit.dispatchCommand(...)

just to reuse /editmotd.

Instead:

GUI
  ->
MOTD service
  ->
configuration
  ->
save

The command should also use the same service.

============================================================
10. PLAYER COUNT
============================================================

Existing command:

/editplayers <online|max> <count>

GUI:

Server Settings
    -> Player Count...

Screen:

Player Count

Current Online Display:
42

Current Max Display:
100

Buttons:

[ Edit Online Count... ]

[ Edit Maximum Count... ]

[ Reset Online Count ]

[ Reset Maximum Count ]

[ Back ]

Input:

Edit Online Count...

Player enters:

42

Backend:

setOnlineDisplayCount(42)

Edit Maximum Count...

Player enters:

100

Backend:

setMaxDisplayCount(100)

Do not duplicate logic.

============================================================
11. SERVER ICON
============================================================

Existing command:

/editicon <imageUrl>

GUI:

Server Settings
    -> Server Icon...

Screen:

Server Icon

Show:

Current Icon

Buttons:

[ Change Icon URL... ]

[ Reset Icon ]

[ Preview ]

[ Back ]

Change Icon URL:

Player enters URL.

Reuse the existing icon downloader/cache logic.

Do not create another icon downloader.

============================================================
12. NPC MANAGER
============================================================

Existing command:

/npc <action>

The NPC system contains:

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

DO NOT place all of these on one screen.

Use:

NPC Manager...

        |
        +-- NPC List
        |
        +-- Create NPC
        |
        +-- Remove NPC
        |
        +-- NPC Information
        |
        +-- Appearance...
        |
        +-- Actions...
        |
        +-- Behavior...
        |
        +-- Storage...

============================================================
13. NPC LIST
============================================================

NPC Manager
    -> NPC List

Display all existing NPCs.

Example:

NPC List

[ Bob ]
[ Shopkeeper ]
[ Guide ]
[ Tutorial ]

Clicking an NPC opens:

NPC Control

Example:

Bob

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

============================================================
14. NPC CREATE
============================================================

NPC Manager
    -> Create NPC...

Ask for:

NPC name

Then use existing:

/npc create <name>

backend.

After creation:

open NPC Control.

============================================================
15. NPC REMOVE
============================================================

NPC Manager
    -> Remove NPC...

Display NPC list.

Player selects NPC.

Show confirmation:

Delete NPC "Bob"?

[ Confirm ]
[ Cancel ]

Use existing NPC manager deletion logic.

============================================================
16. NPC APPEARANCE
============================================================

NPC Control
    -> Appearance...

Screen:

NPC Appearance

[ Skin... ]

[ Name... ]

[ Nametag... ]

[ Glow ]

[ Particles... ]

[ Back ]

============================================================
17. NPC SKIN
============================================================

Existing:

/npc <name> skin <skin>

GUI:

Skin

Current:
Steve

[ Change Skin... ]

Player enters skin name.

Use NPC manager.

============================================================
18. NPC NAME
============================================================

Existing:

/npc <name> name <value>

GUI:

Name

Current:
Bob

[ Change Name... ]

Player enters new name.

============================================================
19. NPC NAMETAG
============================================================

Existing:

/npc <name> nametag ...

GUI:

Nametag

[ Edit Nametag... ]

[ Enable ]

[ Disable ]

Reuse existing implementation.

============================================================
20. NPC GLOW
============================================================

Existing:

/npc <name> glow ...

GUI:

Glow

[ ON ]
[ OFF ]

Clicking changes the state.

============================================================
21. NPC PARTICLES
============================================================

Existing:

/npc <name> particle ...

GUI:

Particles...

Possible submenu:

[ Enable / Disable ]

[ Particle Type... ]

[ Amount... ]

[ Effect... ]

ONLY expose options that the existing NPC particle system actually supports.

Do not invent unsupported options.

============================================================
22. NPC ACTIONS
============================================================

NPC Control
    -> Actions...

Screen:

NPC Actions

[ Right Click... ]

[ Left Click... ]

[ Teleport ]

[ Move ]

[ Look ]

[ Back ]

============================================================
23. RIGHT CLICK
============================================================

Existing:

/npc <name> rightclick ...

GUI:

Right Click Action

Show the currently configured action.

[ Edit Action... ]

[ Clear Action ]

[ Test Action ]

Use existing NPC action backend.

============================================================
24. LEFT CLICK
============================================================

Same structure:

Left Click Action

[ Edit Action... ]

[ Clear Action ]

[ Test Action ]

============================================================
25. NPC BEHAVIOR
============================================================

NPC Control
    -> Behavior...

Possible existing functionality:

Look
Enable
Disable

GUI:

[ Look... ]

[ Enable ]

[ Disable ]

Do not invent behavior not implemented.

============================================================
26. NPC STORAGE
============================================================

NPC Manager
    -> Storage

Buttons:

[ Save NPCs ]

[ Reload NPCs ]

These directly use:

/npc save

/npc reload

backend.

============================================================
27. COMMANDS MENU
============================================================

Main Settings
    -> Commands...

This is NOT a command execution spam menu.

It should organize the plugin commands.

Commands

Utility:
[ /ping ]

Server:
[ /editmotd ]
[ /editplayers ]
[ /editicon ]

NPC:
[ /npc ]

Settings:
[ /settings ]

Clicking a command with editable arguments opens its GUI editor.

============================================================
28. RESOURCE PACK MENU
============================================================

Main Settings
    -> Resource Pack...

Screen:

Resource Pack

Status:
Loaded

Bundled:
YES

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
29. RESOURCE PACK IS BUNDLED
============================================================

The resource pack must be inside:

src/main/resources/resourcepack/

Example:

resourcepack/
├── pack.mcmeta
└── assets/
    └── romeopaper/
        ├── textures/
        │   └── gui/
        └── font/

Gradle/Maven build must include these files inside:

RomeoPaperPlugin.jar

The server owner installs ONE JAR.

============================================================
30. RESOURCE PACK EXTRACTION
============================================================

When plugin enables:

ResourcePackManager:

1. Finds resourcepack inside plugin JAR.
2. Extracts it to:

plugins/RomeoPaperPlugin/resourcepack/

3. Creates:

RomeoServerResourcePack.zip

4. Calculates SHA-1.

5. Stores metadata.

============================================================
31. RESOURCE PACK GUI DESIGN
============================================================

The resource pack must specifically support the GUI.

Create assets for:

GUI background
Button normal
Button hover
Button selected
Button disabled
Small button
Small button hover
Back
Done
Arrow
Input
Input selected
Confirmation
Panel
Divider

Icons:

Server
MOTD
Players
Icon
NPC
Skin
Name
Actions
Behavior
Commands
Resource Pack
Settings
Information
Save
Reload
Reset
Delete
Confirm
Cancel

ALL icons must have a consistent pixel-art style.

============================================================
32. RESOURCE PACK FONT
============================================================

Use Minecraft-compatible font assets.

Do NOT replace the entire game's font unnecessarily.

The visual style should be close to Minecraft's native UI.

Text hierarchy:

TITLE:
Gold/yellow

BUTTON:
Light grey/white

DESCRIPTION:
Grey

VALUE:
White or highlighted

WARNING:
Red

SUCCESS:
Green

============================================================
33. MAIN MENU VISUAL BLUEPRINT
============================================================

The final GUI should visually communicate:

Minecraft
+
Server Administration
+
Romeo branding

WITHOUT looking like:

Discord dashboard
Website
Mobile app
Modern SaaS panel

The player should feel like:

"I am inside Minecraft's settings."

============================================================
34. SUBMENU VISUAL BLUEPRINT
============================================================

Every submenu must have the same structure:

TITLE

Current values / description

Controls

Controls

Controls

Back / Done

Example:

Server MOTD

Current:
Welcome!

[ Edit MOTD... ]

[ Preview ]

[ Reset ]

[ Back ]

Do not change the visual language between menus.

============================================================
35. CONFIRMATION MENUS
============================================================

Dangerous operations should use confirmation.

Example:

Delete NPC?

Are you sure?

[ Confirm ]
[ Cancel ]

Do NOT immediately delete things accidentally.

============================================================
36. INPUT SYSTEM
============================================================

Create ONE reusable InputManager.

It should support:

String input
Integer input
URL input
NPC selection
Confirmation
Optional multi-step input

Example:

GUI
 ->
InputManager.beginStringInput(player, callback)

Player enters text.

Async chat event captures it.

Chat event is cancelled.

Switch back to main thread.

Callback updates backend.

GUI reopens.

Do NOT create separate AsyncPlayerChatEvent listeners for every GUI.

============================================================
37. THREAD SAFETY
============================================================

Paper/Bukkit API must run on the main thread unless explicitly
safe otherwise.

AsyncPlayerChatEvent is asynchronous.

NEVER do:

Bukkit.dispatchCommand(...)
player.openInventory(...)
player.closeInventory(...)
Bukkit API modifications

directly inside AsyncPlayerChatEvent.

Instead:

Bukkit.getScheduler().runTask(plugin, () -> {
    // Bukkit operations
});

Network operations such as downloading an icon can remain async,
but the final Bukkit state update must return to the main thread.

============================================================
38. MODULAR CLASS STRUCTURE
============================================================

Use:

com.romeo.paper.gui

SettingsGUI
ServerSettingsGUI
MotdGUI
PlayerCountGUI
IconGUI
NpcGUI
NpcListGUI
NpcControlGUI
NpcAppearanceGUI
NpcActionsGUI
CommandsGUI
ResourcePackGUI
AdvancedGUI

com.romeo.paper.resourcepack

ResourcePackManager
ResourcePackBuilder
ResourcePackHost
ResourcePackVersionResolver

com.romeo.paper.input

InputManager
InputSession

com.romeo.paper.services

MotdService
PlayerCountService
IconService
NpcService

The exact names can be adjusted to match the existing project.

============================================================
39. COMMAND ADDITION BLUEPRINT
============================================================

IMPORTANT FOR FUTURE DEVELOPMENT:

Whenever I give you a new command, DO NOT automatically put it
on the main /settings screen.

First determine:

1. What does the command do?
2. Is it informational?
3. Is it a setting?
4. Is it an editor?
5. Is it NPC-related?
6. Is it server-related?
7. Is it visual?
8. Is it destructive?
9. Does it need text input?
10. Does it need numeric input?
11. Does it need player selection?
12. Does it need an existing object selection?

Then place it into the appropriate category.

Example:

NEW COMMAND:

/editmotdgradient <color1> <color2>

Placement:

Server Settings
    -> MOTD
        -> Appearance
            -> Gradient

NOT:

Main Settings
    -> random button

============================================================
40. COMMAND REGISTRY
============================================================

Create a central command/feature registry.

Concept:

FeatureRegistry

Each feature contains:

ID
Display name
Description
Category
Permission
GUI handler
Command
Input type
Danger level

Example:

MOTD:

id:
server.motd

display:
Server MOTD

command:
/editmotd

category:
SERVER

permission:
plugin.admin

gui:
MotdGUI

This makes future expansion easy.

============================================================
41. PERMISSIONS
============================================================

Existing:

plugin.admin

All admin settings must respect:

plugin.admin

If a player doesn't have permission:

do not open admin GUI.

Show:

You do not have permission.

Do not rely only on hiding buttons.

Always check permission server-side when performing actions.

============================================================
42. CONFIGURATION
============================================================

Keep existing configuration structure.

Do not randomly rename existing configuration keys.

Add only necessary new sections.

Example:

gui:
  enabled: true

resource-pack:
  enabled: true
  required: true
  url: ""
  kick-on-decline: true
  version: "1.0.0"

============================================================
43. VERSION SUPPORT
============================================================

Do not claim one resource pack supports every Minecraft version.

Design the resource-pack system for version-specific packs.

Example:

resourcepacks/

1.20/
1.20.1/
1.21/
1.21.1/
1.21.x/

The plugin should select a compatible resource pack.

The GUI/backend should remain shared wherever possible.

============================================================
44. ONE-JAR DISTRIBUTION
============================================================

Final server owner experience:

Download:

RomeoPaperPlugin.jar

Put it into:

plugins/

Start server.

DONE.

Plugin automatically:

- loads configuration
- extracts bundled resource pack
- generates resource-pack ZIP
- calculates SHA-1
- starts resource-pack hosting if enabled
- sends pack to players
- registers commands
- registers GUI
- loads NPCs
- loads saved settings

============================================================
45. ERROR HANDLING
============================================================

If resource-pack generation fails:

DO NOT crash the entire plugin.

Log:

[ResourcePack] ERROR: ...

If GUI asset is missing:

log the exact missing asset.

If NPC data is invalid:

skip the invalid NPC and continue loading.

If player enters invalid number:

show:

Invalid number.

Try again.

If URL is invalid:

show:

Invalid URL.

If a required resource pack cannot be delivered:

follow configured fallback behavior.

============================================================
46. LOGGING
============================================================

Use clean console messages.

Example:

[RomeoPaperPlugin] Starting...
[RomeoPaperPlugin] Commands registered.
[RomeoPaperPlugin] NPC system loaded.
[RomeoPaperPlugin] Resource pack found inside plugin JAR.
[RomeoPaperPlugin] Resource pack generated.
[RomeoPaperPlugin] Resource pack SHA-1: XXXXX
[RomeoPaperPlugin] Settings GUI registered.
[RomeoPaperPlugin] Ready.

Do not spam the console.

============================================================
47. FINAL USER EXPERIENCE
============================================================

Player joins.

Resource pack loads.

Player types:

/settings

They see:

                 Options...

[ Server Settings... ]   [ Player Settings... ]

[ NPC Manager... ]       [ Visual Settings... ]

[ Commands... ]          [ Resource Pack... ]

[ Advanced... ]          [ Plugin Information... ]

                    [ Done ]

Player clicks:

Server Settings...

They see:

                 Server Settings

[ MOTD... ]              [ Player Count... ]

[ Server Icon... ]       [ Preview... ]

                         [ Back ]

Player clicks:

MOTD...

They see:

                 Server MOTD

Current:
Welcome to Romeo Network!

[ Edit MOTD... ]

[ Preview ]

[ Reset ]

[ Back ]

Player clicks:

Edit MOTD...

They enter the new value.

The existing backend changes.

GUI returns.

The new value is displayed.

This same pattern must work for every feature.

============================================================
48. DESIGN PRINCIPLE
============================================================

MAIN MENU:
Categories.

SUBMENU:
Features.

SUB-SUBMENU:
Detailed configuration.

INPUT:
Only when necessary.

CONFIRMATION:
Only for dangerous actions.

BACK:
Always available.

DONE:
Returns to previous/main screen where appropriate.

Never overload one GUI with 30+ buttons.

============================================================
49. WHEN I GIVE YOU MORE COMMANDS
============================================================

I will continue giving you commands/features.

For EVERY new command:

DO NOT just create a button.

Instead:

1. Analyze the command.
2. Identify its category.
3. Identify whether it belongs in an existing submenu.
4. If necessary create a new submenu.
5. Reuse existing backend logic.
6. Add GUI controls.
7. Add resource-pack assets only if needed.
8. Add input handling if required.
9. Add permissions.
10. Update the command/feature registry.
11. Keep the main Settings screen clean.

The GUI architecture must be expandable indefinitely.

============================================================
50. CURRENT COMMAND MAP
============================================================

CURRENT:

/ping
    -> Commands
    -> Utility

/editmotd <text>
    -> Server Settings
    -> MOTD

/editplayers <online|max> <count>
    -> Server Settings
    -> Player Count

/editicon <imageUrl>
    -> Server Settings
    -> Server Icon

/npc <action>
    -> NPC Manager
    -> NPC List
    -> NPC Control
    -> Appearance
    -> Actions
    -> Behavior
    -> Storage

/settings
    -> Main Settings GUI

============================================================
END GOAL
============================================================

Build this as a REAL expandable server administration system.

The important part is NOT simply making a pretty GUI.

The important architecture is:

COMMANDS
    |
    v
SHARED SERVICES
    |
    +----------------+
    |                |
    v                v
COMMAND SYSTEM    GUI SYSTEM
                     |
                     v
              RESOURCE PACK
                     |
                     v
             MINECRAFT-STYLE UI

Every future command I provide must plug into this architecture.

Do not rewrite the architecture every time a new command is added.

Do not duplicate backend logic.

Do not create fake settings.

Do not create meaningless GUI buttons.

Every GUI control must perform a real action or display a real value.
============================================================