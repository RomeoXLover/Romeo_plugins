# Commands Guide

This file explains how the Paper plugin commands are defined, registered, and executed in a live Minecraft server.

## 1. Where commands are defined

The server-side commands are declared in the plugin configuration file:

- [paper/src/main/resources/plugin.yml](paper/src/main/resources/plugin.yml)

That file contains all command names, descriptions, and permission rules.

Example structure:

```yaml
commands:
  ping:
    description: Shows your latency.
  editmotd:
    description: Changes the server list MOTD.
    permission: plugin.admin
```

Each command entry tells Bukkit which command names exist and what permission is required.

---

## 2. Where commands are registered

The plugin registers each command in the main plugin class:

- [paper/src/main/java/com/romeo/paper/RomeoPaperPlugin.java](paper/src/main/java/com/romeo/paper/RomeoPaperPlugin.java)

During startup, the plugin runs `onEnable()`, and this is where the command executors are attached.

Example:

```java
RomeoCommand command = new RomeoCommand(this, npcManager);
getCommand("ping").setExecutor(command);
getCommand("editmotd").setExecutor(command);
getCommand("npc").setExecutor(command);
```

This means when a player types a command like `/ping`, the server routes that command to the `RomeoCommand` class.

---

## 3. Where command logic is handled

The actual command behavior is implemented here:

- [paper/src/main/java/com/romeo/paper/RomeoCommand.java](paper/src/main/java/com/romeo/paper/RomeoCommand.java)

The important method is:

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
```

Inside this method, the code checks the command name and then calls the correct logic method.

Example:

```java
String name = command.getName().toLowerCase(Locale.ENGLISH);
if (name.equals("ping")) {
    return ping(sender);
}
if (name.equals("editmotd")) {
    return editMotd(sender, args);
}
```

This is the full command flow:

1. Player types a command in Minecraft
2. Bukkit looks up that command in `plugin.yml`
3. The server calls the registered executor
4. `onCommand(...)` decides which action to run
5. The command method checks permissions and arguments
6. It updates config, saves data, or sends a message back to the player

---

## 4. Commands available in this plugin

### 1) `/ping`

Purpose:
- Shows the player latency in ms.

Where handled:
- `RomeoCommand.ping(...)`

Behavior:
- Only works for players.
- If the sender is not a player, it sends a message telling them only players can use it.
- If the player is online, it retrieves their ping and sends it back.

Example:

```text
/ping
```

Expected result:

```text
Your latency: 12 ms
```

---

### 2) `/editmotd <text>`

Purpose:
- Changes the server list MOTD.

Permissions:
- `plugin.admin`

Behavior:
- Requires a text argument.
- Saves the value into the plugin config under `server-list.motd`.
- Sends a success message.

Example:

```text
/editmotd Welcome to our server!
```

Expected result:
- The MOTD used in the server list updates.

---

### 3) `/editplayers <online|max> <count>`

Purpose:
- Changes the displayed online or max player count.

Permissions:
- `plugin.admin`

Behavior:
- The first argument must be `online` or `max`.
- The second argument must be a valid non-negative number.
- It stores the value in the config and saves it.

Examples:

```text
/editplayers online 42
/editplayers max 100
```

Expected result:
- The fake server player count in the server list changes.

---

### 4) `/editicon <imageUrl>`

Purpose:
- Downloads and caches a server icon.

Permissions:
- `plugin.admin`

Behavior:
- Takes a direct image URL.
- Downloads it asynchronously.
- Saves it as `server-icon.png` inside the plugin data folder.
- Updates the config with the cached file path.

Example:

```text
/editicon https://example.com/server-icon.png
```

Expected result:
- A server icon is downloaded and used for the server list.

---

### 5) `/npc <action> ...`

Purpose:
- Manages custom NPCs.

Permissions:
- `plugin.admin`

This command is more advanced and supports many sub-actions such as:

- `create`
- `remove`
- `list`
- `move`
- `info`
- `skin`
- `name`
- `nametag`
- `glow`
- `look`
- `tp`
- `enable`
- `disable`
- `save`
- `reload`
- `rightclick`
- `leftclick`
- `particle`

Examples:

```text
/npc create Bob
/npc list
/npc Bob info
/npc Bob move
/npc Bob skin Steve
```

Expected result:
- NPCs appear or update based on the requested action.
- The NPC data is saved when the command finishes.

---

### 6) `/settings`

Purpose:
- Opens the server settings GUI.

Permissions:
- `plugin.admin`

Behavior:
- Only the player who runs it can open the panel.
- It opens a GUI handled by `SettingsGUI`.

Example:

```text
/settings
```

Expected result:
- A menu opens for editing server settings.

---

## 5. How permissions work

The permission is declared in `plugin.yml`:

```yaml
permissions:
  plugin.admin:
    description: Allows server and NPC administration.
    default: op
```

This means:
- OP players can use the admin commands by default.
- Non-OP players are denied access.

The code checks this in `RomeoCommand`:

```java
if (!sender.hasPermission("plugin.admin")) {
    sender.sendMessage(LegacyText.colorize("&cYou do not have permission."));
    return true;
}
```

---

## 6. How commands should work in practice on a server

To use this plugin on a Paper server:

1. Put the built plugin jar into the server's `plugins` folder.
2. Start the server.
3. Log in as an OP.
4. Type the commands in the Minecraft chat.
5. If the command is admin-only, the OP must have permission.

Example full flow:

```text
/settings
/editmotd Welcome to the survival server!
/editplayers online 20
/editicon https://example.com/server-icon.png
/npc create Bob
```

After those actions, the plugin should:
- update server metadata
- save config values
- create and track NPCs
- open the settings GUI for admin editing

---

## 7. How to add a new command

If you want to add another command, the usual pattern is:

1. Add it to [paper/src/main/resources/plugin.yml](paper/src/main/resources/plugin.yml)
2. Register it in [paper/src/main/java/com/romeo/paper/RomeoPaperPlugin.java](paper/src/main/java/com/romeo/paper/RomeoPaperPlugin.java)
3. Add a matching branch in [paper/src/main/java/com/romeo/paper/RomeoCommand.java](paper/src/main/java/com/romeo/paper/RomeoCommand.java)
4. Check sender permissions if it is admin-only
5. Save config or update state when needed

Example:

```yaml
commands:
  mycommand:
    description: My custom command.
    permission: plugin.admin
```

Then in Java:

```java
getCommand("mycommand").setExecutor(command);
```

And in `onCommand(...)`:

```java
if (name.equals("mycommand")) {
    return mycommand(sender, args);
}
```

---

## 8. Final notes

- Commands are handled by Bukkit and Paper using the `CommandExecutor` system.
- The command list is not hardcoded only in Java; it is also declared in `plugin.yml` so the server knows the command exists.
- If the command is missing from `plugin.yml`, the server will usually not recognize it.
- If the command is not registered in `RomeoPaperPlugin`, it will not be assigned a handler.

This is the expected command architecture for this project.
