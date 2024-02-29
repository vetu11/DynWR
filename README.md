# DynWR - Dynamic Worldrespawn

This is a simple server-side Minecraft mod that adjust the world respawn point
based on player activity (sleeping and respawning).

It is intended for survival servers where players travel away from the original
spawn point but want to bring new players with them without using /tp or other
admin tools.

## Important notes

- Consider disabling the spawn protection to avoid griefing non-OP players, as
  is pretty normal for the spawn point to end up in player bases. You can do this
  by either setting spawn-protection=0 in the server.properties or having no OP 
  players. https://minecraft.wiki/w/Spawn_protection
- This mod adjusts the 'spawnRadious' gamerule.

## How does it work?

In a nutshell, it averages the positions where players sleep and respawn. It
changes pretty quickly the first days after the mod was added but slows down
depending on the number of active players.

