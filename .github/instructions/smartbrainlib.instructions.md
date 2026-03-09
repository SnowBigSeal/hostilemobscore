---
applyTo: "src/main/java/com/snowbigdeal/hostilemobscore/entity/**"
---

# SmartBrainLib AI System

SmartBrainLib (SBL) replaces vanilla goal selectors with a declarative brain system.
All entities extend `HostileMob<T>` which implements `SmartBrainOwner<T>`.

**Sources:** Extracted to `build/decompiled-cache/smartbrainlib/` (119 `.java` files).

---

## How the Brain System Works

```
Sensors (periodic, ~1/s)
  → write data into Memories
    → Behaviours read Memories and act
      → grouped into Activities (CORE / IDLE / FIGHT / custom)
        → Brain picks the highest-priority active Activity each tick
```

**Key principle:** Behaviours must never hold entity state in instance fields.
All shared state goes into `MemoryModuleType` entries so every behaviour can read it.

---

## SmartBrainOwner — Methods to Override

| Method | Activity | Notes |
|---|---|---|
| `getSensors()` | — | **Required.** Return list of `ExtendedSensor` instances. |
| `getCoreTasks()` | `CORE` | Always runs. Use for movement, targeting helpers, timers. |
| `getIdleTasks()` | `IDLE` | Runs when no higher-priority activity is active. |
| `getFightTasks()` | `FIGHT` | Auto-requires `ATTACK_TARGET` memory to be present. |
| `getAdditionalTasks()` | custom | Returns `Map<Activity, BrainActivityGroup<T>>` for non-standard activities. |
| `getActivityPriorities()` | — | `List<Activity>` — earlier = higher priority. Default: `[FIGHT, IDLE]`. |

`tickBrain(typedSelf())` **must** be called from `customServerAiStep()`. Nothing else
should go there — move any logic that reads/writes entity state into behaviours or sensors.

---

## Registering Custom Memories and Activities

```java
// ModMemoryTypes.java
private static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES =
        DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, MODID);

private static final DeferredRegister<Activity> ACTIVITIES =
        DeferredRegister.create(BuiltInRegistries.ACTIVITY, MODID);

public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MY_FLAG =
        MEMORY_TYPES.register("my_flag", () -> new MemoryModuleType<>(Optional.empty()));

public static final DeferredHolder<Activity, Activity> MY_ACTIVITY =
        ACTIVITIES.register("my_activity", () -> new Activity("my_activity"));

public static void register(IEventBus bus) {
    MEMORY_TYPES.register(bus);
    ACTIVITIES.register(bus);
}
```

Call `ModMemoryTypes.register(modEventBus)` in the mod constructor.

---

## BrainActivityGroup — Building Activity Groups

```java
// Standard groups
BrainActivityGroup.coreTasks(behaviour1, behaviour2)   // CORE, priority 0
BrainActivityGroup.idleTasks(behaviour1, behaviour2)   // IDLE, priority 10
BrainActivityGroup.fightTasks(behaviour1, behaviour2)  // FIGHT, priority 10 + auto ATTACK_TARGET requirement

// Custom activity — chain builder methods
new BrainActivityGroup<T>(MY_ACTIVITY)
    .onlyStartWithMemoryStatus(MY_FLAG, MemoryStatus.VALUE_PRESENT)
    .behaviours(new MyBehaviour<>())
```

Add custom activities to `getAdditionalTasks()` and list the activity in `getActivityPriorities()`.

---

## Writing a New Behaviour

```java
public class MyBehaviour<T extends HostileMob<T>> extends ExtendedBehaviour<T> {

    // Static list = allocated once, not per call (per wiki recommendation)
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS =
            List.of(Pair.of(ModMemoryTypes.MY_FLAG.get(), MemoryStatus.VALUE_PRESENT));

    // Configurable options via builder methods, not hardcoded constants
    private int myOption = 20;

    public MyBehaviour<T> myOption(int value) {
        this.myOption = value;
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    // Optional: extra start gate beyond memory requirements
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        return /* additional condition */;
    }

    @Override
    protected void start(T entity) {
        // Called once when behaviour activates. Memory requirements are guaranteed here.
    }

    @Override
    protected void tick(T entity) {
        // Called every tick while running. Memory is NOT guaranteed — check if needed.
    }

    @Override
    protected boolean shouldKeepRunning(T entity) {
        // Return true to keep ticking. Return false to stop.
        return BrainUtils.hasMemory(entity, ModMemoryTypes.MY_FLAG.get());
    }

    @Override
    protected void stop(T entity) {
        // Optional cleanup when behaviour stops.
    }
}
```

**Design rules (from wiki):**
1. One function per behaviour — don't combine targeting + movement + attacking.
2. Read/write `MemoryModuleType` entries, not raw entity fields, for any shared state.
3. Expose configurable options as builder methods (`.hitTimeout(600)`) not magic numbers.
4. If gathering in-world data (nearby entities, blocks) use a **sensor** instead.

### No-timeout behaviours (e.g. ongoing timers)

Call `noTimeout()` in the constructor to make the behaviour run indefinitely until
`shouldKeepRunning()` returns false:

```java
public MyBehaviour() {
    noTimeout();
}
```

---

## BrainUtils — Common API

```java
// Reading memories
BrainUtils.hasMemory(entity, MemoryModuleType.ATTACK_TARGET)          // boolean
BrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET)          // @Nullable T
BrainUtils.memoryOrDefault(entity, MY_MEMORY, () -> defaultValue)     // T

// Writing memories
BrainUtils.setMemory(entity, MY_MEMORY, value)
BrainUtils.setForgettableMemory(entity, MY_MEMORY, value, expiryTicks) // auto-clears
BrainUtils.clearMemory(entity, MY_MEMORY)
BrainUtils.clearMemories(entity, MEMORY_A, MEMORY_B)

// Target helpers
BrainUtils.getTargetOfEntity(entity)                  // @Nullable LivingEntity
BrainUtils.setTargetOfEntity(entity, target)          // also sets non-brain target
BrainUtils.canSee(entity, target)                     // uses memory or raytrace

// Special attack cooldown
BrainUtils.setSpecialCooldown(entity, ticks)
BrainUtils.isOnSpecialCooldown(entity)
```

---

## SBL Built-in Memories (SBLMemoryTypes)

| Memory | Type | Description |
|---|---|---|
| `INCOMING_PROJECTILES` | `List<Projectile>` | Projectiles about to hit the entity |
| `TARGET_UNREACHABLE` | `Boolean` | Current target cannot be pathfound to |
| `SPECIAL_ATTACK_COOLDOWN` | `Boolean` | General-purpose special attack gate |
| `NEARBY_BLOCKS` | `List<Pair<BlockPos,BlockState>>` | Nearby block scan results |
| `NEARBY_ITEMS` | `List<ItemEntity>` | Nearby item entities |

Access via `SBLMemoryTypes.INCOMING_PROJECTILES.get()` etc.

---

## Mod Patterns: HostileMob Conventions

- `getCoreTasks()` in `BaseSlime`: `LookAtAttackTarget`, `HoppingCombatBehaviour`, `DeaggroBehaviour`
- `getIdleTasks()` in `BaseSlime`: `TetheredTargetBehaviour` → `SetPlayerLookTarget` → `SetRandomLookTarget`, `Idle`
- `getFightTasks()` in `BaseSlime`: `InvalidateAttackTarget` (drops invulnerable/out-of-range targets), then subclass attack behaviours
- Custom `ACTIVITY_RETURNING_HOME` wired in `getAdditionalTasks()` / `getActivityPriorities()`
- `setReturningHome(true)` writes `RETURNING_HOME` memory → brain auto-switches to `ACTIVITY_RETURNING_HOME`
- `customServerAiStep()` on `HostileMob` contains only `tickBrain(typedSelf())`

---

## Available Built-in SBL Behaviours

Key classes in `build/decompiled-cache/smartbrainlib/`:

| Behaviour | Purpose |
|---|---|
| `SetWalkTargetToAttackTarget` | Walk toward current attack target |
| `WalkOrRunToWalkTarget` | Execute the walk target set by sensors/behaviours |
| `MoveToWalkTarget` | Lower-level movement to walk target |
| `InvalidateAttackTarget` | Drop attack target on conditions |
| `TargetOrRetaliate` | Acquire attack target; retaliate against attacker |
| `SetAttackTarget` / `SetRetaliateTarget` | Set specific targets |
| `AnimatableMeleeAttack` | GeckoLib melee attack with cooldown |
| `AnimatableRangedAttack` | GeckoLib ranged attack |
| `LookAtAttackTarget` | Face the current attack target |
| `LookAtTarget` | Face a walk target |
| `Idle` | Stand still for N ticks |
| `FleeTarget` | Move away from a target |
| `StrafeTarget` | Strafe around a target |
| `FirstApplicableBehaviour` | Try each child; run the first that starts |
| `AllApplicableBehaviours` | Run all children that can start |
| `OneRandomBehaviour` | Pick one random eligible child |
| `SequentialBehaviour` | Run children in sequence |

---

## Common Pitfalls

- **Don't put logic in `customServerAiStep()`** — only `tickBrain(typedSelf())` belongs there.
- **Don't store entity state in behaviour instance fields** — use memories. (Exception: per-run
  counters like a timeout that reset in `start()` are safe because `start()` always runs first.)
- **`getMemoryRequirements()` is called every tick** — use a static final list for performance.
- **Memory requirements gate `start()`, not `tick()`** — check memories you need inside `tick()`.
- **`fightTasks()` automatically requires `ATTACK_TARGET`** — don't add it manually.
- **Custom activities must be in `getActivityPriorities()`** — otherwise the brain never switches to them.
