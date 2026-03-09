>> // Store in entity NBT and load on demand
>> ```
>>
>> ### Step 2: Check if Should Return Home
>> Create condition predicate:
>> ```java
>> BiPredicate<E, BlockP>> BiPredicate<E, BlockPos> shou>> BiPredicate<E, BlockPos> shouldReturnHome = (entity, home) -> {
>>     // Return true if:
>>     // - No ATTACK_TARGET memory
>>     // - OR ATTACK_TARGET is n>>     // - OR ATTACK_TARGET is null
>>>>     // - OR distance to home > threshold
>>     return !BrainUtils.hasMemory(entity, MemoryModuleType.ATTACK_TARGET);
>> };
>> ```
>>
>> #>> ### Step>> ### Step 3: Create Return Home Activity
>> ```java
>> Activity RETURNING_HOME = new Activity("returning_home");
>>
>> BrainActivityGroup<Yo>> BrainActivityGroup<YourEntity>> BrainActivityGroup<YourEntity> returnHomeGroup = 
>>     new BrainActivityGroup<>(RETURNING_HOME)
>>.priority(8) // Higher than idle (10), lower tha>>.priority(8) // Higher than idle (10), lower than figh>>.priority(8) // Higher than idle (10), lower than fight (10 might need adjustment)
>>.behaviours(
>>// 1. Clear attack target
>>new InvalidateAttackTar>>new InvalidateAttackTarget<>>>new InvalidateAttackTarget<>(>>new InvalidateAttackTarget<>().invalidateIf((entity, target) -> true),
>>
>>// 2. Set walk target to home
>>new SetWalkTargetT>>new SetWalkTargetToBlockPos<>()  // Custom behavior OR:
>>.whenStarting(entity -> {
>>BrainUtils.setMemory>>BrainUtils.setMemory(entity,>>BrainUtils.setMemory(entity, 
>>MemoryModuleType.WALK_TARGET,
>>new WalkTarget(
>>>>home>>homePos,
>>1.0f, // speed
>>1     // closeEnough distance
>>>>>>)
>>);
>>}),
>>
>>// 3. Actually move to target
>>new MoveT>>new MoveToWalk>>new MoveToWalkTarget<>(),
>>
>>// 4. Optional: Clear other combat memories
>>new InvalidateMemory<>(Memory>>new InvalidateMemory<>(MemoryModule>>new InvalidateMemory<>(MemoryModuleType.LOOK_TARGET)
>>.invalidateIf((entity, target) -> true)
>>)
>>.onlyStartWithMemoryStatus(HO>>.onlyStartWithMemoryStatus(HOME_MEMO>>.onlyStartWithMemoryStatus(HOME_MEMORY, MemoryStatus.VALUE_PRESENT);
>> ```
>>
>> ### Step 4: Register Activity in SmartBrainOwner
>> ```java
>> @Override
>> default Map<Act>> default Map<Activity, B>> default Map<Activity, Br>> default Map<Activity, BrainActivityGroup<? extends T>> getAdditionalTasks() {
>>     Map<Activity, BrainActivityGroup<T>> tasks = new Object2ObjectOpenHas>>     Map<Activity, BrainActivityGroup<T>> tasks = new Object2ObjectOpenHashMap<>();
>>     tasks.put(RETURNING_HOME, returnHomeGroup);
>>     return tasks;
>> }
>>
>> @Override
>> default List<Activity> getActi>> default List<Activity> getActivityPrior>> default List<Activity> getActivityPriorities() {
>>     return ObjectArrayList.of(
>>Activity.FIGHT,
>>RETURNING_HOME,  // New!
>>Activity.IDL>>Activity.IDLE
>>     );
>> }
>> ```
>>
>> ### Step 5: Detect Arrival
>> ```java
>> // In a behavior or tick method:
>> if (BrainUtils.hasMemory(entity, WALK_TARGET))>> if (BrainUtils.hasMemory(entity, WALK_TARGET)) {
>>     W>>     WalkTarget target = BrainUtils.getMemory(entity, WALK_TARGET);
>>     if (entity.blockPosition().distManhattan(
>>targ>>target.getTa>>target.getTarget().currentBlockPosition()) <= target.getCloseEnoughDist()) {
>>// Arrived home
>>BrainUtils.clearMemory(e>>BrainUtils.clearMemory(entity, >>BrainUtils.clearMemory(entity, W>>BrainUtils.clearMemory(entity, WALK_TARGET);
>>// Optional: trigger action (despawn, reset, rest, etc)
>>     }
>> }
>> ```
>>
>> ---
>>
>> ## CREATING CUSTOM BEHAVIORS
>>
>> Ex>> Extend `ExtendedBehaviour<E extends LivingEntity>`:
>>
>> ```java
>> public class SetWalkTargetToBlockPos<E extends PathfinderMob>
>>     ex>>     extends ExtendedBehaviour<E> {
>>
>>     protected BlockPos targetPos;
>>
>>     @Override
>>     protected List<Pair<MemoryModu>>     protected List<Pair<MemoryModuleType<>>     protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
>>return List.of(); // No specific requirements
>>     }
>>
>>     @Overri>>     @Override
>>>>     protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
>>return this.targetPos != null;
>>     }
>>>>     
>>>>     @Override
>>     protected void start(E entity) {
>>BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET,
>>>>n>>new WalkTarget(this.targetPos, 1f, 1));
>>     }
>>
>>     public SetWalkTargetToBlockPos<E> target(BlockPos pos) {
>>t>>this.targ>>this.targetPos = pos;
>>return this;
>>     }
>> }
>> ```
>>
>> ---
>>
>> ## KEY TAKEAWAYS
>>
>> ✅ **Use MoveToWalkTarget** to execute pathfinding (handles>> ✅ **Use MoveToWalkTarget** to execute pathfinding (handles path creation + navigation)
>> ✅ **Use SetWalkTargetToBlock/Custom** to set WALK_TARGET before moving
>> ✅ **Use InvalidateAtt>> ✅ **Use InvalidateAttackTarg>> ✅ **Use InvalidateAttackTarget** to stop combat automatically
>> ✅ **Use separate Activity** for return home to manage priority
>> ✅ **Store home position*>> ✅ **Store home position** in cu>> ✅ **Store home position** in custom memory or entity NBT
>> ✅ **Check arrival** via WALK_TARGET memory and distance checks
>> ✅ **Use BrainActivityGroup.prior>> ✅ **Use BrainActivityGroup.priority()**>> ✅ **Use BrainActivityGroup.priority()** to control which activity activates
>> ✅ **Override getActivityPriorities()** to order activities
>> ✅ **Use NearestHomeSensor>> ✅ **Use NearestHomeSensor** if y>> ✅ **Use NearestHomeSensor** if you have POI system set up
>>
>> "@ | Out-String
>>
>> }; echo ___BEGIN___COMMAND_DONE_MARKER___$LASTEX>> }; echo ___BEGIN___COMMAND_DONE_MARKER___$LASTEXITCODE
___BEGIN___COMMAND_OUTPUT_MARKER___
# SmartBrainLib - Return Home / Tether System Analysis

## QUICK REFERENCE FOR "RETURN HOME" IMPLEMENTATION

### Recommended Approach:
1. Create a custom Activity (e.g., Activity.REST or custom enum)
2. Use SetWalkTargetToBlockPos or custom behavior to set walk target to home position
3. Clear ATTACK_TARGET to stop fighting
4. Use MoveToWalkTarget to navigate home
5. Store home position in a custom memory module or SBLMemoryTypes

---

## BUILT-IN BEHAVIORS FOR MOVEMENT & PATHFINDING

### 1. MoveToWalkTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Moves entity to a target stored in MemoryModuleType.WALK_TARGET memory
**Uses Pathfinding:** YES - uses entity.getNavigation().createPath()
**Key Methods:**
- Requires: WALK_TARGET memory present, PATH memory absent
- Pathfinding recheck on target movement
- Handles unreachable targets via CANT_REACH_WALK_TARGET_SINCE memory
- Returns true when within closeEnoughDist blocks of target

**Constructor Parameters:** None (uses defaults)
**Configuration:**
- Default run time: 100-250 ticks random
- Default cooldown: 0-40 ticks random

**Key Constructor Details:**
- runFor(100 + random 150 ticks)
- cooldownFor(random 40 ticks)

---

### 2. WalkOrRunToWalkTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Extension of MoveToWalkTarget that also sets sprint animation flag
**Uses Pathfinding:** YES (inherits from MoveToWalkTarget)
**Benefits:** Applies entity.setSharedFlag(3, speedModifier > 1) for client animation

---

### 3. FollowEntity<E extends PathfinderMob, T extends Entity>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Follow a specific entity (can be used for parent/owner following)
**Uses Pathfinding:** YES - uses entity.getNavigation().moveTo(target)
**Special Features:**
- Can teleport if target gets too far away
- Customizable teleport predicate
- Tracks both water and lava pathfinding penalties

**Configuration Methods:**
- following(Function<E, T>) - provide the entity to follow
- stopFollowingWithin(double distance) - stop when this close
- teleportToTargetAfter(double distance) - teleport if further than this
- speedMod(BiFunction<E, T, Float>) - movement speed modifier

---

### 4. FollowOwner<E extends TamableAnimal>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Follow the owner of a tamed animal
**Uses Pathfinding:** YES (extends FollowEntity)
**Defaults:**
- Teleport after 12 blocks away
- Stop following within 2 blocks

---

### 5. FollowParent<E extends AgeableMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Baby animals follow parent
**Uses Pathfinding:** YES (extends FollowEntity)
**Configuration:**
- parentPredicate(BiPredicate<E, AgeableMob>) - filter parent candidates
- Default: same class as entity and age >= 0

---

### 6. FleeTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Run away from attack target
**Uses Pathfinding:** YES - creates path away from target
**Useful For:** Disengaging during "return home" by clearing attack target first
**Configuration:**
- fleeDistance(int blocks) - how far to flee (default: 20)
- speedModifier(float mod) - movement speed (default: 1.0)
**Memory Requirements:** ATTACK_TARGET must be present
**Behavior:** Clears ATTACK_TARGET after starting flee

---

### 7. StayWithinDistanceOfAttackTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Strafe around target, maintaining distance
**Uses Pathfinding:** YES (uses navigation for repositioning)
**Useful For:** Understanding distance management during combat
**Configuration:**
- minDistance(float/BiFunction) - get closer if too far (default: 5)
- maxDistance(float/BiFunction) - run away if too close (default: 20)
- speedMod(float) - strafe speed (default: 1.0)
- repositionSpeedMod(float) - reposition speed (default: 1.3)

---

### 8. AvoidEntity<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Move away from specific entities
**Uses Pathfinding:** YES
**Useful For:** Additional evasion during return home
**Memory Requirements:** NEAREST_VISIBLE_LIVING_ENTITIES
**Configuration:**
- avoiding(Predicate<LivingEntity>) - filter which entities to avoid
- noCloserThan(float blocks) - threshold to start avoiding (default: 3)
- stopCaringAfter(float blocks) - max avoidance distance (default: 7)
- speedModifier(float mod) - evasion speed (default: 1.0)

---

## PATH-SETTING BEHAVIORS (Set walk target to destination)

### 9. SetWalkTargetToBlock<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Sets walk target to nearby blocks from NEARBY_BLOCKS memory
**Uses Pathfinding:** NO - sets WALK_TARGET; pathfinding handled by MoveToWalkTarget
**Memory Requirements:** SBLMemoryTypes.NEARBY_BLOCKS must be present
**Use Case:** For navigating to specific block types
**Configuration:**
- predicate(BiPredicate<E, Pair<BlockPos, BlockState>>) - filter blocks
- speedMod(BiFunction) - movement speed per target
- closeEnoughWhen(BiFunction) - distance to consider "arrived"

**NOTE:** Requires NearbyBlocksSensor to populate NEARBY_BLOCKS memory

---

### 10. SetWalkTargetToAttackTarget<E extends Mob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Set walk target to current attack target
**Uses Pathfinding:** NO - sets WALK_TARGET memory
**Memory Requirements:** ATTACK_TARGET present
**Configuration:**
- speedMod(BiFunction<E, LivingEntity, Float>) - attack approach speed
- closeEnoughDist(ToIntBiFunction) - how close to get before melee
- targetEyePosition(ToBooleanBiFunction) - use eye height for flying entities

---

### 11. SetRandomWalkTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Set a random walk target nearby
**Uses Pathfinding:** NO - sets WALK_TARGET memory
**Useful For:** Wandering around home area (tether)
**Configuration:**
- setRadius(double xz, double y) - search radius (default: 10x10x7)
- speedModifier(BiFunction) - movement speed
- dontAvoidWater() - allow water in targets
- walkTargetPredicate(BiPredicate) - custom position validation

---

### 12. SeekRandomNearbyPosition<E extends LivingEntity>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Find random nearby position matching predicate
**Uses Pathfinding:** NO - sets WALK_TARGET memory
**Configuration:**
- setRadius(double xz, double y) - search area (default: 10x7)
- validPositions(BiPredicate<E, BlockState>) - position filter
- attempts(int) - search attempts (default: 10)
- speedModifier(BiFunction) - movement speed

---

### 13. SetRandomFlyingTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Flying entity random target
**Uses Pathfinding:** NO - sets WALK_TARGET
**Configuration:**
- verticalWeight(ToIntFunction) - bias toward up/down (default: -2 = slightly up)
- setRadius(double xz, double y) - search area (default: 10x10)

---

### 14. SetRandomHoverTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Hover/levitating entity random target
**Uses Pathfinding:** NO - sets WALK_TARGET

---

### 15. SetRandomSwimTarget<E extends PathfinderMob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.path
**Purpose:** Swimming entity random target
**Uses Pathfinding:** NO - sets WALK_TARGET

---

## TARGET/COMBAT CONTROL BEHAVIORS

### 16. InvalidateAttackTarget<E extends LivingEntity>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.target
**Purpose:** Clear attack target under conditions
**Useful For:** Stopping combat during return home
**Memory Requirements:** ATTACK_TARGET present
**Default Conditions Clear Target If:**
- Target is in creative/spectator mode
- Target is too far (beyond FOLLOW_RANGE)
- Unable to path to target for 200+ ticks
- Custom predicate returns true

**Configuration:**
- stopTryingToPathAfter(long ticks) - pathfinding timeout (default: 200)
- ignoreFailedPathfinding() - don't invalidate on path failure
- invalidateIf(BiPredicate) - custom invalidation condition

**Key Feature:** Automatically stops walking and clears LOOK_TARGET if set

---

### 17. InvalidateMemory<E extends LivingEntity, M>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.misc
**Purpose:** Clear any memory conditionally
**Use Case:** Generic memory clearing behavior
**Constructor:** InvalidateMemory(MemoryModuleType<M> memory)
**Configuration:**
- invalidateIf(BiPredicate<E, M>) - condition to clear

---

### 18. SetRetaliateTarget<E extends Mob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.target
**Purpose:** Set attack target if mob is hurt
**Use Case:** Encourage combat vs. returning home (lower priority)

---

## MISC BEHAVIORS

### 19. Idle<E extends LivingEntity>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.misc
**Purpose:** Do absolutely nothing
**Use Case:** Fallback behavior or waiting state

---

### 20. FloatToSurfaceOfFluid<E extends Mob>
**Package:** net.tslat.smartbrainlib.api.core.behaviour.custom.move
**Purpose:** Float/swim to water surface
**Configuration:**
- riseChance(float chance) - jump to surface chance per tick (default: 0.8)
**Useful For:** Aquatic mobs returning home through water

---

## SENSORS FOR DETECTING HOME/POSITION

### 21. NearestHomeSensor<E extends Mob>
**Package:** net.tslat.smartbrainlib.api.core.sensor.vanilla
**Purpose:** Finds nearest HOME POI (bed/nest point)
**Memory Populated:** MemoryModuleType.NEAREST_BED
**Default Conditions:** Only runs if entity is baby
**Useful For:** Implementing natural "go home" for babies
**Configuration:**
- setRadius(int radius) - search radius (default: 48 blocks)
- predicate() - override to always run: new NearestHomeSensor<>((brainOwner, entity) -> true)

**How It Works:**
- Scans for POI type PoiTypes.HOME
- Uses path-based pathfinding to nearest reachable home
- Caches homes with expiry to avoid recalculation

---

### 22. GenericAttackTargetSensor<E extends LivingEntity>
**Package:** net.tslat.smartbrainlib.api.core.sensor.custom
**Purpose:** Find nearest attackable entity
**Memory Populated:** MemoryModuleType.NEAREST_ATTACKABLE

---

### 23. NearbyBlocksSensor
**Package:** net.tslat.smartbrainlib.api.core.sensor
**Purpose:** Find nearby blocks of interest
**Memory Populated:** SBLMemoryTypes.NEARBY_BLOCKS

---

## MEMORY MODULES RELEVANT FOR RETURN HOME

### Vanilla Memory Modules:
- **ATTACK_TARGET** - Current combat target (clear to stop fighting)
- **WALK_TARGET** - Where to walk (set by path behaviors)
- **PATH** - Current navigation path
- **CANT_REACH_WALK_TARGET_SINCE** - When we started failing to path
- **NEAREST_BED** - Nearest bed/home POI (from NearestHomeSensor)
- **LOOK_TARGET** - Where to look
- **NEAREST_VISIBLE_LIVING_ENTITIES** - Nearby entities
- **FOLLOW_RANGE** - Combat range attribute

### Custom Memory Modules (SBLMemoryTypes):
- **NEARBY_BLOCKS** - List<Pair<BlockPos, BlockState>> blocks of interest
- **NEARBY_ITEMS** - List<ItemEntity> items nearby
- **INCOMING_PROJECTILES** - List<Projectile> incoming attacks
- **TARGET_UNREACHABLE** - Boolean flag if target can't be reached
- **SPECIAL_ATTACK_COOLDOWN** - Boolean for attack timing

---

## ACTIVITY MANAGEMENT & PRIORITIES

### SmartBrainOwner Activity Methods:
`java
// Override in your entity class implementing SmartBrainOwner

default BrainActivityGroup<? extends T> getCoreTasks() {
    // Tasks always running (priority: 0)
    // Example: floating, preventing suffocation
}

default BrainActivityGroup<? extends T> getIdleTasks() {
    // Tasks when idle (priority: 10)
    // Example: random wandering
}

default BrainActivityGroup<? extends T> getFightTasks() {
    // Tasks during combat (priority: 10)
    // Requires ATTACK_TARGET memory to activate
}

default Map<Activity, BrainActivityGroup<? extends T>> getAdditionalTasks() {
    // Custom activities like RETURNING_HOME
    Map<Activity, BrainActivityGroup<T>> additional = new Object2ObjectOpenHashMap<>();
    additional.put(YOUR_RETURN_HOME_ACTIVITY,
        new BrainActivityGroup<>(YOUR_RETURN_HOME_ACTIVITY)
            .priority(5) // Higher priority than FIGHT (10) and IDLE (10)
            .behaviours(clearAttackTargetBehavior, moveToHomeBehavior)
            .onlyStartWithMemoryStatus(YOUR_HOME_MEMORY, MemoryStatus.VALUE_PRESENT)
    );
    return additional;
}

default List<Activity> getActivityPriorities() {
    // Order: check FIGHT first, then RETURN_HOME, then IDLE
    return ObjectArrayList.of(
        Activity.FIGHT,
        YOUR_RETURN_HOME_ACTIVITY,  // New custom activity
        Activity.IDLE
    );
}
`

### BrainActivityGroup Features:
- **priority(int)** - Task scheduling priority (lower = runs first)
- **behaviours(Behavior...)** - Add tasks
- **onlyStartWithMemoryStatus(memory, status)** - Condition to activate
- **wipeMemoriesWhenFinished(memory...)** - Clear on completion
- **requireAndWipeMemoriesOnUse(memory...)** - Require present + clear after

---

## RECOMMENDED "RETURN HOME" IMPLEMENTATION STRATEGY

### Step 1: Store Home Position
**Option A - Custom Memory Module:**
`java
// In registry or your mod
public static final Supplier<MemoryModuleType<BlockPos>> HOME_POSITION =
    register("home_position");
`

**Option B - Use Entity Field:**
`java
// Store in entity NBT and load on demand
`

### Step 2: Check if Should Return Home
Create condition predicate:
`java
BiPredicate<E, BlockPos> shouldReturnHome = (entity, home) -> {
    // Return true if:
    // - No ATTACK_TARGET memory
    // - OR ATTACK_TARGET is null
    // - OR distance to home > threshold
    return !BrainUtils.hasMemory(entity, MemoryModuleType.ATTACK_TARGET);
};
`

### Step 3: Create Return Home Activity
`java
Activity RETURNING_HOME = new Activity("returning_home");

BrainActivityGroup<YourEntity> returnHomeGroup =
    new BrainActivityGroup<>(RETURNING_HOME)
        .priority(8) // Higher than idle (10), lower than fight (10 might need adjustment)
        .behaviours(
            // 1. Clear attack target
            new InvalidateAttackTarget<>().invalidateIf((entity, target) -> true),

            // 2. Set walk target to home
            new SetWalkTargetToBlockPos<>()  // Custom behavior OR:
                .whenStarting(entity -> {
                    BrainUtils.setMemory(entity,
                        MemoryModuleType.WALK_TARGET,
                        new WalkTarget(
                            homePos,
                            1.0f, // speed
                            1     // closeEnough distance
                        )
                    );
                }),

            // 3. Actually move to target
            new MoveToWalkTarget<>(),

            // 4. Optional: Clear other combat memories
            new InvalidateMemory<>(MemoryModuleType.LOOK_TARGET)
                .invalidateIf((entity, target) -> true)
        )
        .onlyStartWithMemoryStatus(HOME_MEMORY, MemoryStatus.VALUE_PRESENT);
`

### Step 4: Register Activity in SmartBrainOwner
`java
@Override
default Map<Activity, BrainActivityGroup<? extends T>> getAdditionalTasks() {
    Map<Activity, BrainActivityGroup<T>> tasks = new Object2ObjectOpenHashMap<>();
    tasks.put(RETURNING_HOME, returnHomeGroup);
    return tasks;
}

@Override
default List<Activity> getActivityPriorities() {
    return ObjectArrayList.of(
        Activity.FIGHT,
        RETURNING_HOME,  // New!
        Activity.IDLE
    );
}
`

### Step 5: Detect Arrival
`java
// In a behavior or tick method:
if (BrainUtils.hasMemory(entity, WALK_TARGET)) {
    WalkTarget target = BrainUtils.getMemory(entity, WALK_TARGET);
    if (entity.blockPosition().distManhattan(
        target.getTarget().currentBlockPosition()) <= target.getCloseEnoughDist()) {
        // Arrived home
        BrainUtils.clearMemory(entity, WALK_TARGET);
        // Optional: trigger action (despawn, reset, rest, etc)
    }
}
`

---

## CREATING CUSTOM BEHAVIORS

Extend ExtendedBehaviour<E extends LivingEntity>:

`java
public class SetWalkTargetToBlockPos<E extends PathfinderMob>
    extends ExtendedBehaviour<E> {

    protected BlockPos targetPos;

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(); // No specific requirements
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return this.targetPos != null;
    }

    @Override
    protected void start(E entity) {
        BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET,
            new WalkTarget(this.targetPos, 1f, 1));
    }

    public SetWalkTargetToBlockPos<E> target(BlockPos pos) {
        this.targetPos = pos;
        return this;
    }
}
`

---

## KEY TAKEAWAYS

✅ **Use MoveToWalkTarget** to execute pathfinding (handles path creation + navigation)
✅ **Use SetWalkTargetToBlock/Custom** to set WALK_TARGET before moving
✅ **Use InvalidateAttackTarget** to stop combat automatically
✅ **Use separate Activity** for return home to manage priority
✅ **Store home position** in custom memory or entity NBT
✅ **Check arrival** via WALK_TARGET memory and distance checks
✅ **Use BrainActivityGroup.priority()** to control which activity activates
✅ **Override getActivityPriorities()** to order activities
✅ **Use NearestHomeSensor** if you have POI system set up


___BEGIN___COMMAND_DONE_MARKER___0
PS C:\Users\steve\Documents\Git\hostilemobscore>
