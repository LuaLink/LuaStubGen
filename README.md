# Lua Stub Generator

A Kotlin-based tool to generate Lua stub files with LuaLS annotations (Teal type defintions planned) from Java source or compiled JARs. Designed for use with [LuaLink](https://github.com/LuaLink/LuaLink).

## Features

- Parses both source and compiled JARs
- Supports LuaLS Teal-style annotations
- Extracts method signatures, field types, and class documentation
- Processes Javadoc comments
- Outputs structured Lua files for use with IDEs and Lua tooling

## Usage

```bash
java -jar lua-stubgen.jar path/to/jar/paper.jar path/to/jar/adventure.jar...
```

# Example output of Bukkit `Player` from Paper sources JAR
```lua
--- Represents a player, connected or not
-- org.bukkit.entity.Player
---@class Player 
local Player = {}

---@return Identity 
--- Paper start
function Player:identity() end

---@return BossBar> 
--- Gets an unmodifiable view of all known currently active bossbars. This currently only returns bossbars shown to the player via #showBossBar(net.kyori.adventure.bossbar.BossBar) and does not contain bukkit org.bukkit.boss.BossBar instances shown to the player.
function Player:activeBossBars() end

---@return Component 
--- Gets the "friendly" name to display of this player.
function Player:displayName() end

---@param displayName? Component 
---@return nil 
--- Sets the "friendly" name to display of this player.
function Player:displayName(displayName) end

---@return string 
function Player:getName() end

---@deprecated
---@return string 
--- Gets the "friendly" name to display of this player. This may include color. Note that this name will not be displayed in game, only in chat and places defined by plugins.
function Player:getDisplayName() end

---@deprecated
---@param name string 
---@return nil 
--- Sets the "friendly" name to display of this player. This may include color. Note that this name will not be displayed in game, only in chat and places defined by plugins.
function Player:setDisplayName(name) end

---@param name? Component 
---@return nil 
--- Sets the name that is shown on the in-game player list. If the value is null, the name will be identical to #getName().
function Player:playerListName(name) end
```
