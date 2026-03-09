# SmartBrainLib Return Home - Class Hierarchy & Dependency Map

## CLASS HIERARCHY

\\\
ExtendedBehaviour<E extends LivingEntity>
├── MoveToWalkTarget                    [Executes pathfinding to WALK_TARGET]
│   └── WalkOrRunToWalkTarget          [+ Sprint animation]
│
├── Movement Behaviors (custom.move)
│   ├── FollowEntity                   [Follow target entity]
│   │   ├── FollowOwner                [Owner specific]
│   │   └── FollowParent               [Parent specific]
│   ├── FleeTarget                     [Run away from target]
│   ├── StayWithinDistanceOfAttackTarget [Strafe combat]
│   ├── AvoidEntity                    [Avoid specific entities]
│   ├── FloatToSurfaceOfFluid          [Water floating]
│   ├── EscapeSun                      [Flee sunlight]
│   ├── InteractWithDoor               [Open doors]
│   ├── FollowTemptation               [Follow items/mobs]
│   └── [More movement behaviors...]
│
├── Path Setting (custom.path)          [Set WALK_TARGET - no execution]
│   ├── SetWalkTargetToBlock           [Set to specific block]
│   ├── SetWalkTargetToAttackTarget    [Chase target]
│   ├── SetRandomWalkTarget            [Random nearby (TETHER!)]
│   ├── SeekRandomNearbyPosition       [Find valid positions]
│   ├── SetRandomFlyingTarget          [Flying random]
│   ├── SetRandomHoverTarget           [Hovering random]
│   ├── SetRandomSwimTarget            [Swimming random]
│   └── [More path behaviors...]
│
├── Target Management (custom.target)
│   ├── InvalidateAttackTarget         [Clear combat target]
│   ├── SetAttackTarget                [Set combat target]
│   ├── SetRetaliateTarget             [Set on damage]
│   ├── TargetOrRetaliate              [Dynamic targeting]
│   └── [More target behaviors...]
│
├── Misc (custom.misc)
│   ├── Idle                           [Do nothing]
│   ├── InvalidateMemory<M>            [Clear any memory]
│   ├── Panic                          [Flee everywhere]
│   ├── HoldItem                       [Hold item in hand]
│   ├── BreakBlock                     [Mine block]
│   ├── BreedWithPartner               [Breeding]
│   ├── BlockWithShield                [Shield defend]
│   ├── ReactToUnreachableTarget       [Unreachable response]
│   └── [More misc behaviors...]
│
└── Custom Behaviors
    └── Your custom behavior classes...

PredicateSensor<Owner, Subject>
└── NearestHomeSensor                  [Find bed/nest POI]

EntityFilteringSensor<Entity, Owner>
└── GenericAttackTargetSensor          [Find attackable entity]

[Other Sensors...]
└── NearbyBlocksSensor                 [Find nearby blocks]

BrainActivityGroup<E>
├── Wraps list of Behavior<E>
├── Contains Activity enum
├── Memory requirements
└── Memory wipe on finish

SmartBrainOwner<T extends LivingEntity & SmartBrainOwner<T>>
├── getCoreTasks()       → BrainActivityGroup
├── getIdleTasks()       → BrainActivityGroup
├── getFightTasks()      → BrainActivityGroup
├── getAdditionalTasks() → Map<Activity, BrainActivityGroup>
├── getActivityPriorities()        → List<Activity>
├── getScheduleIgnoringActivities() → Set<Activity>
└── tickBrain(T entity)  [CALL FROM customServerAiStep()]

SmartBrain<E>
├── tick(level, entity)
├── addActivity(group)
├── setActiveActivity(activity)
├── isActive(activity)
└── [Manages all behaviors]
\\\

---

## MEMORY FLOW FOR "RETURN HOME"

\\\
[Entity Combat]
    ↓
[Has ATTACK_TARGET] → YES → [FIGHT Activity] → StayWithinDistanceOfAttackTarget
    ↓ NO
[Has HOME_POSITION] → YES → [RETURNING_HOME Activity]
    ↓                          ├─ InvalidateAttackTarget (clear target)
    ↓                          ├─ Custom behavior sets WALK_TARGET = HOME
    ↓                          └─ MoveToWalkTarget (navigate home)
    ↓                              ↓
    ↓                          [Distance to home < 1?]
    ↓                              ↓ YES
    ↓                          [Arrival! Clear WALK_TARGET]
    ↓                              ↓
[IDLE Activity] ← [Default/Fallback]
    ↓
[SetRandomWalkTarget with radius constraint]
\\\

---

## MEMORY MODULES DEPENDENCY CHAIN

For "Return Home" to work, you need:

\\\
Step 1: Define Home Position
├─ Option A: Custom Memory Module
│  └─ Create: MemoryModuleType<BlockPos> HOME_POSITION
│  └─ Store via: BrainUtils.setMemory(entity, HOME_POSITION, pos)
│
└─ Option B: Entity Field
   └─ Store in entity NBT
   └─ Load on demand in behavior

Step 2: Activate Return Home Activity
└─ Condition: onlyStartWithMemoryStatus(HOME_POSITION, VALUE_PRESENT)
   └─ Triggers: BrainActivityGroup with behaviors

Step 3: Stop Combat
└─ Behavior: InvalidateAttackTarget
   └─ Clears: MemoryModuleType.ATTACK_TARGET
   └─ Also clears: LOOK_TARGET if it was entity tracker

Step 4: Set Navigation Target
└─ Custom Behavior or SetWalkTargetToBlock
   └─ Sets: MemoryModuleType.WALK_TARGET with BlockPos
   └─ Also sets: MemoryModuleType.LOOK_TARGET (optional)

Step 5: Execute Pathfinding
└─ Behavior: MoveToWalkTarget
   └─ Requires: WALK_TARGET present, PATH absent
   └─ Auto-sets: PATH memory with navigation path
   └─ Auto-sets: CANT_REACH_WALK_TARGET_SINCE if unreachable
   └─ Clears: WALK_TARGET when reached

Step 6: Detect Arrival
└─ Check: entity.blockPosition().distManhattan(homePos) <= 1
└─ Triggers: wipeMemoriesWhenFinished(HOME_POSITION)
└─ Result: IDLE activity runs next
\\\

---

## BEHAVIOR COMPOSITION PATTERN

All behaviors inherit from ExtendedBehaviour:

\\\
public abstract class ExtendedBehaviour<E extends LivingEntity> {
    // Lifecycle methods - override in subclass:
    protected abstract List<Pair<MemoryModuleType<?>, MemoryStatus>> 
        getMemoryRequirements();
    
    protected boolean checkExtraStartConditions(ServerLevel level, E entity)
        { return true; }
    
    protected boolean shouldKeepRunning(E entity) 
        { return true; }
    
    protected void start(E entity) {}
    protected void tick(E entity) {}
    protected void stop(E entity) {}
    
    // Fluent API - override + chain:
    public ExtendedBehaviour<E> runFor(Function<E, Integer> ticks)
    public ExtendedBehaviour<E> cooldownFor(Function<E, Integer> ticks)
    public ExtendedBehaviour<E> startCondition(Predicate<E> condition)
    public ExtendedBehaviour<E> stopCondition(Predicate<E> condition)
    public ExtendedBehaviour<E> whenStarting(Consumer<E> callback)
    public ExtendedBehaviour<E> whenStopping(Consumer<E> callback)
}
\\\

---

## ACTIVITY PRIORITY RESOLUTION

When brain.tick() is called:

\\\
1. forgetOutdatedMemories()
   └─ Clear expired memory values

2. tickSensors()
   └─ Run all sensors (populate memories)

3. checkForNewBehaviours()
   └─ Try starting each behavior for active activities

4. tickRunningBehaviours()
   └─ Tick all active behaviors

5. findAndSetActiveActivity()
   ├─ Check activities in getActivityPriorities() order
   ├─ Verify memory requirements via onlyStartWithMemoryStatus()
   ├─ Check schedule (if no priority match)
   └─ Use getDefaultActivity() as fallback (IDLE)

Priority check example:
┌──────────────────────────────┐
│ getActivityPriorities()      │
├──────────────────────────────┤
│ [FIGHT, RETURNING_HOME, IDLE]│
└──────────────────────────────┘
         ↓
    Check FIGHT
    Has ATTACK_TARGET? YES
    Memory requirements met? YES
    → SET ACTIVE = FIGHT
    
    OR
    
    Check FIGHT
    Has ATTACK_TARGET? NO
    → CHECK NEXT
         ↓
    Check RETURNING_HOME
    Has HOME_POSITION? YES
    Memory requirements met? YES
    → SET ACTIVE = RETURNING_HOME
    
    OR
    
    Check RETURNING_HOME
    Has HOME_POSITION? NO
    → CHECK NEXT
         ↓
    Check IDLE
    No requirements
    → SET ACTIVE = IDLE
\\\

---

## KEY CONFIGURATION POINTS

\\\java
// 1. BrainActivityGroup
new BrainActivityGroup<>(RETURNING_HOME)
    .priority(8)                    // Execution priority
    .behaviours(behavior1, behavior2)
    .onlyStartWithMemoryStatus(     // Activation condition
        HOME_MEMORY, 
        MemoryStatus.VALUE_PRESENT
    )
    .wipeMemoriesWhenFinished(      // Cleanup on exit
        HOME_MEMORY,
        WALK_TARGET
    );

// 2. MoveToWalkTarget defaults
new MoveToWalkTarget<>()
    // Automatically:
    // - Runs for 100-250 ticks
    // - Cooldown 0-40 ticks
    // - Checks if target moved
    // - Creates new paths as needed
    // - Clears WALK_TARGET when reached

// 3. SetWalkTargetToBlock predicate
new SetWalkTargetToBlock<>()
    .predicate((entity, blockPair) -> 
        // Filter which blocks to target
        true
    )
    .closeEnoughWhen((entity, blockPair) -> 
        // Distance to consider "arrived"
        1
    );

// 4. InvalidateAttackTarget conditions
new InvalidateAttackTarget<>()
    .stopTryingToPathAfter(200)     // Path timeout
    .invalidateIf((entity, target) -> 
        // Custom invalidation
        true
    );

// 5. SetRandomWalkTarget tether
new SetRandomWalkTarget<>()
    .setRadius(20, 8)               // Stay within 20 horizontal, 8 vertical
    .walkTargetPredicate((entity, pos) -> 
        // Additional position validation
        true
    );
\\\

