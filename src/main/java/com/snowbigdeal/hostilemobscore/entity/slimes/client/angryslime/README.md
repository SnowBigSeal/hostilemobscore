# AngrySlime

## Base Stats

| Attribute           | Value |
|---------------------|-------|
| Health              | 20    |
| Movement speed      | 0.25  |
| Attack damage       | 1.0   |
| Follow range        | 16    |
| Knockback resistance| 0.6   |
| Jump strength       | 0.42 (fallback — overridden contextually) |

---

## Behaviour

**Targets:** Players only  
**Aggro trigger:** Line of sight / proximity (within follow range)  
**De-aggro:** Target enters creative mode (invulnerable), target dies, target out of range

**Movement style:** Custom hop-based (`SlimeMoveControl`) — no pathfinder  
- Wanders with short hops (0.30f jump power) when idle  
- Leaps toward target (0.55f jump power) when aggroed  
- Evasive strafe (hops sideways) when within 3.5 blocks  
- Hysteresis band: enters strafe ≤ 3.5 blocks, exits ≥ 4.5 blocks  
- Leap force scales with distance: barely nudges at 4.5 blocks, full force at 14 blocks

---

## Attacks

### 1. Magic Proximity Hit
- **Trigger:** Slime enters ≤ 2.5 block radius of player  
- **Damage:** 1× `ATTACK_DAMAGE`  
- **Cooldown:** Re-arms when slime backs past 2.5 blocks  
- **Visual:** None (passive — fires as a side-effect of evasive movement)

### 2. Slam (Special)
- **Trigger:** Target in range, on ground, cooldown expired  
- **Cooldown:** 300–500 ticks (15–25 seconds); starts at 300 on spawn  
- **Windup:** 20 ticks — slime holds still, expanding red-orange ring appears at target position  
- **Launch:** Arc jump toward best target (position that hits the most players in radius)  
- **On landing:**
  - AoE radius: 3.5 blocks  
  - Damage: 1.5× `ATTACK_DAMAGE`  
  - Knockback: 1.5  
  - Recovery lock: 15 ticks frozen after landing  
- **Target selection:** Picks whichever player position maximises number of players hit in AoE radius

---

## Animations

| Controller | Trigger              | Animation        |
|------------|----------------------|------------------|
| `movement` | Airborne or moving   | `move.walk` (2s loop) |
| `movement` | Idle                 | `misc.idle` (2s loop) |
| `slam`     | Slam behaviour start | `slam` (play once)    |

Animation file: `assets/hostilemobscore/animations/angryslime.animation.json`

---

## Key Classes

| Class                   | Responsibility                                              |
|-------------------------|-------------------------------------------------------------|
| `AngrySlime.java`        | Entity definition, attributes, brain provider, animation controllers |
| `SlimeMoveControl.java`  | All hop/strafe/leap physics — runs after brain tick        |
| `SlimeMoveBehaviour.java`| SBL brain behaviour — dispatches movement state each tick  |
| `SlamAttackBehaviour.java`| SBL fight behaviour — windup, launch, AoE landing         |

---

## Notes

- `SlimeMoveControl.slamLock` suppresses all hop logic while `SlamAttackBehaviour` owns physics
- Magic hit and strafe are independent: strafe threshold is 3.5 blocks, damage threshold is 2.5 blocks
- `AnimatableMeleeAttack` is still registered as a fallback in `getFightTasks()` behind `SlamAttackBehaviour` via `FirstApplicableBehaviour`
