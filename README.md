# Hostile Mobs Core

> A **NeoForge 1.21.1** Minecraft mod that adds custom hostile entities with advanced SmartBrainLib AI, GeckoLib animations, and client-side visual effects.

---

## Features

### AngrySlime
A re-imagined slime with aggressive pursuit, evasive strafing, and a telegraphed slam attack.

#### Movement
- **Hopping locomotion** driven by a custom `SlimeMoveControl` — separates AI intent from physics so jump logic cannot be overridden by pathfinding
- **Aggressive leap** — scales horizontal launch force with distance so the slime lunges across gaps to close on its target
- **Evasive strafe** — at close range the slime circles the player with sideways hops, making it harder to hit
- **Contact damage** — deals melee damage when it lands within striking distance

#### Slam Attack
- **Target selection** — picks the position that would catch the most players in the AoE radius, falling back to the current attack target
- **Wind-up phase** — locks in place and faces the target while an expanding warning indicator is shown on the ground
- **Telegraphed launch** — leaps into the air toward the locked position; position is snapshotted at wind-up so the slam is fully dodgeable
- **Impact** — on landing, triggers the slam animation and deals AoE damage and knockback to all players within the radius
- **Cooldown** — enforced server-side cooldown with random variance to prevent predictable timing

#### Animations (GeckoLib)
| Controller | Trigger | Description |
|---|---|---|
| `movement` | — | Loops `idle` or `hop` based on movement state |
| `slam` | `slam_windup` | Plays on launch — crouching wind-up into leap |
| `slam` | `slam_impact` | Plays on landing — ground impact |

#### Visual Effects
- **Ground indicator** — flat textured disc rendered via `RenderLevelStageEvent`, visible as soon as wind-up begins
- **Expanding ring** — grows outward over the wind-up duration to show the exact AoE boundary
- Rendered client-side only via `CircleAoePacket` (server sends once, client manages the full lifetime)

---

## Architecture

```
entity/slimes/client/angryslime/
├── AngrySlime.java               — Entity definition, attributes, brain wiring, GeckoLib animations
├── AngrySlimeModel.java          — GeckoLib animated model
├── AngrySlimeRenderer.java       — GeoEntityRenderer
├── SlimeMoveControl.java         — Custom MoveControl; drives hopping, strafing, leap force
├── SlimeMoveBehaviour.java       — SmartBrainLib behaviour; sets MoveControl intent each tick
└── SlimeSlamAttackBehaviour.java — SmartBrainLib behaviour; full slam lifecycle

client/
└── SlamVfxManager.java           — Client-side ground decal renderer (RenderLevelStageEvent)

network/
└── CircleAoePacket.java          — Server to client packet carrying slam position, radius, lifetime
```

---

## Dependencies

| Library | Version |
|---|---|
| NeoForge | 21.1.219 |
| GeckoLib | 4.8.3 |
| SmartBrainLib | 1.16.11 |

Requires **Java 21** and **Minecraft 1.21.1**.

---

## Building

```bash
# Build JAR (output in build/libs/)
./gradlew build

# Run client with mod loaded
./gradlew runClient

# Run dedicated server with mod loaded
./gradlew runServer

# Run data generators (item models, loot tables, etc.)
./gradlew runData

# Refresh dependencies if IDE is missing libraries
./gradlew --refresh-dependencies
```

---

## Third-Party Notices

This mod depends on the following libraries. Their licenses apply to their respective code:

- **GeckoLib** — [MIT License](https://github.com/bernie-g/geckolib/blob/main/LICENSE)
- **SmartBrainLib** — [LGPL-2.1 License](https://github.com/Tslat/SmartBrainLib/blob/master/LICENSE)
- **NeoForge** — [LGPL-2.1 License](https://github.com/neoforged/NeoForge/blob/main/LICENSE.txt)
- **Minecraft source mappings** — subject to [Mojang's mapping license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md)

---

## License

This mod is licensed under the **Mozilla Public License 2.0**. See [LICENSE](LICENSE) for the full text.

In summary: you may use, modify, and distribute this source code, but modifications to MPL-licensed files must remain under the MPL 2.0. You may combine this code with code under other licenses in a larger work.
