# ⚡ ESN SpeedForce — 1.1.0

**ESN Studios' Paper/Purpur speedster gameplay plugin** with actual custom items, six speedster armor sets, matching summon rings, playable powers, a GUI, crafting recipes, and a generated Java resource pack.

Designed for **Paper/Purpur 1.21.4** with Java **21**. Later Minecraft versions may need a resource-pack format update and retesting. Not a Forge/Fabric mod.

## What is included

| Suit | Palette | Style |
| --- | --- | --- |
| Flash | Crimson/gold | Balanced, 0.44 sprint speed |
| Reverse Flash | Gold/red | Attack, 0.48 sprint speed |
| Zoom | Black/blue | Aggressive, 0.52 sprint speed |
| Godspeed | White/gold | Fast, 0.55 sprint speed |
| Savitar | Steel/blue | Damage-focused, 0.43 sprint speed |
| ESN Phantom | Violet/cyan | Fastest, 0.56 sprint speed |

Each suit contains **four uniquely identified and textured armor pieces** (helmet/chestplate/leggings/boots), a **matching summon ring**, dedicated wearable equipment textures, custom names and metadata that cannot be spoofed by renaming vanilla items.

Six active powers: **dash, lightning, whirlwind, heal, time (slows monsters), afterimage**. Full-set detection, adjustable energy, cooldowns, acceleration, lightning trails, water traversal assist, optional wall-running, PvP protections, and dampening mechanics.

Unique utility items: **Speed Force Core, Tachyon Enhancer, Velocity Serum, Lightning Shard, Speed Dampener, Meta Cuffs**. Cores and rings have crafting recipes; other items are currently admin-granted for controlled events and rewards.

## Download

1. Open [Actions](../../actions/workflows/build.yml).
2. Open the latest **green** “Build SpeedForce” run.
3. Download **ESN-SpeedForce-Install** from **Artifacts**. Extract the artifact ZIP.
4. Put `ESN-SpeedForce-1.1.0.jar` in the Minecraft server's `plugins/` folder.
5. Restart **Paper/Purpur**. Install the accompanying `ESN-SpeedForce-ResourcePack-1.1.0.zip` on the *Java* client via Options → Resource Packs, or host it as your server's resource pack.

**Do not extract the resource-pack ZIP** when installing it on the client. For Java players on a server, configure `resource-pack`, `resource-pack-sha1` and optionally `require-resource-pack` in server.properties using a public direct-download URL (not a GitHub Actions login-only artifact link).

**Bedrock/Geyser:** mechanics use normal Bukkit interaction controls and may work for connected Bedrock players, but the included Java resource pack **does not make the custom visuals appear on Bedrock**. Geyser requires a separate compatible Bedrock pack and appropriate item mappings. Full Bedrock visual parity is not claimed or tested.

## Commands

- `/speedforce` or `/speedster` or `/sf`: suit catalog, skills, custom item menu
- `/sf equip flash`: summon/retract a suit if you own its ring
- `/sf ability dash` (also `lightning`, `whirlwind`, `heal`, `time`, `afterimage`)
- `/sf status`: active suit, energy and disruption
- `/sf toggle`: enable/disable powers
- `/sf recipes`: ring crafting instructions
- Admin: `/sf list`; `/sf give <player> <item_id> [amount]`; `/sf kit <player> <suit>`; `/sf reload`

**Quick start for operator:** `/sf kit YOUR_USERNAME flash`, hold the Flash Ring in your **main hand** and right-click to summon your suit. Sprint to accelerate and run `/sf ability dash`. Right-click ring again to retract.

Item IDs: `ring_flash`, `ring_reverse`, `ring_zoom`, `ring_godspeed`, `ring_savitar`, `ring_esn`; e.g. `flash_helmet`; and `core`, `tachyon`, `serum`, `lightning_shard`, `dampener`, `meta_cuffs`.

## Crafting

Speed Force Core (shaped 3×3):

```
E R E
R N R
E R E
```

`E` = Echo Shard, `R` = Redstone Block, `N` = Nether Star.

Suit Ring (3×3):

```
  D
G C G
  G
```

`D` = dye matching the suit color, `G` = Gold Ingot, `C` = crafted Speed Force Core (the exact custom ingredient, not any similarly named item).

## Configuration and performance

Edit `plugins/ESNSpeedForce/config.yml` for energy, movement cap, trails, world blacklist, ability cooldowns, and experimental wall-running. `/sf reload` applies config changes. The effect loop runs every 2 ticks and leaves speed modifications restored when powers deactivate. **Movement anti-cheats may still require safe exceptions or lower speed limits.** Wall-running defaults to **OFF**.

### Current limitations

This **first playable foundation** is not a full Speed Force dimension, boss system, suit progression tree, animated 3D armor, or a Bedrock custom-item pack. Those systems need separate implementation and multiplayer testing before being advertised as supported. Builds must pass GitHub Actions compilation, and in-game testing is still required on your exact Paper/Purpur version and Geyser stack.

## Build locally

```sh
mvn clean package
python3 tools/make_pack.py
```

The Java JAR appears in `target/`. The custom item/equipment pack appears in `dist/`. The GitHub Actions workflow builds and checks both outputs automatically.

## v1.1 Permanent upgrades and Realm

Progression saves as player Persistent Data Container values and persists across reconnects/restarts.

- Sprinting in a complete suit earns 3 XP and 1 Velocity Shard per real 30 blocks traveled (2 shards in realm). Levels grant 5 bonus shards, cap level 100.
- `/sf upgrades` opens a GUI for permanent **Speed** (+0.015/tier, max 15), **Energy** (+25/tier, max 20), **Regeneration** (+0.09/tick per tier, max 12), and **Mastery** (cheaper skills/cooldowns, max 10). Cost starts at 12 shards and rises by 8/tier.
- `/sf upgrade speed` buys a rank; `/sf profile` shows stats.
- `/sf realm` enters a unique new superflat world with **generated lightning-inspired lobby, circuit, crystals, and arena**. It is created on first use, not on server startup; the system refuses to overwrite an existing unrelated world folder.
- `/sf trial` starts the 5-checkpoint, 90-second speedster circuit; grants XP, Shards and a custom Time Trial Medal. Three-minute trial cooldown.
- `/sf boss` near the east arena summons a 240-HP Temporal Wraith; a twelve-minute summon cooldown and boss bar apply. Nearby suited participants earn 125 XP, 40 Shards, Lightning Shard and Temporal Crystal.
- `/sf leave` returns to the last saved location in the original world.
- **Rift Compass** is craftable (Amethyst Shards top/bottom, Ender Pearls left/right, custom Speed Force Core center). Right-click to open the realm. **Temporal Crystal** redeems for 100 XP/25 shards; **Meta Cuffs** suppress hostile speedsters; the medal is a collectible. These 3 items have custom Java textures in the 1.1 resource pack.

**Upgrading:** Stop the server; remove the old `ESN-SpeedForce-1.0.0.jar` before adding the 1.1 JAR. Keep `plugins/ESNSpeedForce/config.yml` and your worlds. New config defaults may need to be added manually to old configs: `max-walk-speed: 0.85` if you want speed ranks above the old hard cap of 0.58. Add `realm: { world-name: esn_speedforce_realm }` or let the fallback name apply. Replace the old Java pack ZIP with the new one. World and player progression data are not erased by a JAR replacement. Test on a staging server and take a world backup.

**Compatibility:** This is a Paper plugin-created *separate world*, not a custom-dimension client mod. The included texture pack is Java-only; Geyser Bedrock users need their own Bedrock pack/mappings for custom suit appearances. Do not promise seamless Bedrock item visuals before testing.
