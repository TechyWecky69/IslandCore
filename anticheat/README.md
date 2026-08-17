# IslandCore AntiCheat

A lightweight anticheat + basic connection throttling plugin for Spigot 1.21.1.

## What it checks

| Check | Category | What it catches |
|---|---|---|
| Speed | Movement | Moving faster than sprint/potion-adjusted max speed |
| Fly | Movement | Staying airborne without falling (no elytra/vehicle/etc.) |
| NoFall | Movement | Landing with no block beneath you (spoofed ground packet) |
| Jesus | Movement | Walking on top of water |
| Rotation | Combat/Movement | Invalid pitch, or suspiciously identical yaw increments (aim-assist) |
| Reach | Combat | Hitting an entity from beyond legit melee range |
| KillAura | Combat | Hitting far off-crosshair, or hitting multiple entities almost simultaneously |
| AutoClicker | Combat | Sustained clicks-per-second above a human ceiling |
| FastBreak | World | Breaking blocks faster than is physically possible |
| DupeClick | Inventory | Replayed/macro'd clicks landing on the same slot+item faster than humanly possible |
| NestedContainer | Inventory | Placing a filled shulker box inside another open shulker box |

Each check raises a per-player, per-check **violation level (VL)** that decays
over time. Thresholds in `config.yml` control when staff get alerted and when
a kick/ban command actually fires.

## Important limitations (please read)

This is an **event-based** anticheat (built on standard Bukkit events like
`PlayerMoveEvent` and `EntityDamageByEntityEvent`). That makes it simple,
dependency-free, and easy for you to read/tune. It is **not** the same tier
as packet-level anticheats like Vulcan, Matrix, or GrimAC, which hook into
raw network packets via ProtocolLib and run full server-side physics
simulation to catch far more subtle cheats. Treat this as a solid first
layer of defense, not a silver bullet — expect to tune the thresholds in
`config.yml` against your real player base, since lag and terrain can trigger
false positives if the buffers are too tight.

## Building the plugin

Because Spigot's API jar is built from Mojang's mappings, it isn't legally
redistributable on public Maven repos — you build it locally with
BuildTools, once:

```bash
# 1. Download BuildTools
curl -O https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

# 2. Build and install spigot-api 1.21.1 into your local ~/.m2 repo
java -jar BuildTools.jar --rev 1.21.1

# 3. Build this plugin (from the islandcore-anticheat folder)
mvn clean package
```

The compiled plugin will be at `target/islandcore-anticheat-1.0.0.jar`.
Drop it into your server's `plugins/` folder and restart.

## Commands & permissions

- `/islandcore reload` — reload `config.yml` (`islandcore.admin`)
- `/islandcore alerts` — toggle violation alerts for yourself (`islandcore.admin`)
- `/islandcore vl <player>` — view a player's current violation levels
- `/islandcore reset <player>` — clear a player's violations
- `/islandcore check <name> <on|off>` — toggle an individual check
- `islandcore.alerts` — receive live violation alerts in chat (default: op)
- `islandcore.bypass` — exempt a player from all checks (default: false — only give this to staff/testers)

## About the "DDoS protection"

A Java plugin runs **after** a connection already reached your server
process — real DDoS traffic (SYN floods, UDP amplification, volumetric
floods) overwhelms your network/OS before Minecraft even sees a packet, so
no plugin can stop that. What `ConnectionListener` in this plugin *does* do
is throttle repeated login attempts per IP within a time window, which helps
against simple join-flood/fake-player bot tools hitting the application
layer.

For actual DDoS protection, in rough order of effectiveness:

1. **Put a proxy in front of your server** (Velocity or BungeeCord with IP
   forwarding) and route it through a service built for this — TCPShield,
   Cloudflare Spectrum, or your host's "Game" anti-DDoS tier (OVH, Hetzner,
   etc.). This is genuinely the main fix; everything else is secondary.
2. **Never expose your backend server's real IP** — only the proxy's address
   should be public. If the real IP leaks, attackers bypass the proxy
   entirely.
3. **OS-level firewall rate limiting** (iptables/ufw + fail2ban) for anything
   that does get through, and review the `connection-throttle` setting
   already built into `spigot.yml`.
4. Keep this plugin's connection throttle as a light extra layer on top —
   not a replacement for the above.

## Tuning false positives

If legitimate players get flagged:
- Raise `checks.speed.buffer` and `checks.speed.sample-size`.
- Raise `checks.fly.max-air-ticks` if your terrain has a lot of jump-heavy
  parkour or your TPS is unstable.
- Raise `checks.reach.max-distance` slightly if players report false Reach
  flags under lag (server-side lag compensation isn't modeled here).
- Loosen `checks.killaura.max-angle` if players with high mouse sensitivity
  or entities with unusual hitboxes get flagged.

Watch `/islandcore vl <player>` and your console logs while testing with a
few trusted staff before rolling it out server-wide.
