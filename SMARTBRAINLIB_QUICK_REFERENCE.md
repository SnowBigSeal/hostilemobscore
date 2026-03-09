# SmartBrainLib Return Home - Quick Summary

## MOST RELEVANT CLASSES FOR YOUR USE CASE

| Class | Package | Purpose | Pathfinding? | Key Use |
|-------|---------|---------|--------------|---------|
| **MoveToWalkTarget** | custom.move | Execute path to WALK_TARGET | YES | Core navigation behavior |
| **SetWalkTargetToBlock** | custom.path | Set target to nearby blocks | NO | Set destination before moving |
| **InvalidateAttackTarget** | custom.target | Clear ATTACK_TARGET | NO | Stop fighting |
| **FollowEntity** | custom.move | Follow another entity | YES | Follow entity with pathfinding |
| **SetRandomWalkTarget** | custom.path | Wander in radius | NO | Restrict to area around home |
| **NearestHomeSensor** | sensor.vanilla | Find home POI | N/A | Sensor populates NEAREST_BED |
| **BrainActivityGroup** | core | Activity container | N/A | Define return home activity |
| **SmartBrainOwner** | api | Entity interface | N/A | Override getAdditionalTasks() |

---

## IMPLEMENTATION CHECKLIST

- [ ] Store home position (custom memory or NBT)
- [ ] Create custom Activity for "RETURNING_HOME"
- [ ] Add behavior to clear ATTACK_TARGET (InvalidateAttackTarget)
- [ ] Add behavior to set WALK_TARGET to home (SetWalkTargetToBlock + custom)
- [ ] Add behavior to move to target (MoveToWalkTarget)
- [ ] Register in getAdditionalTasks() as BrainActivityGroup
- [ ] Override getActivityPriorities() with: [FIGHT, RETURNING_HOME, IDLE]
- [ ] Set onlyStartWithMemoryStatus() to activate when needed
- [ ] Detect arrival by checking distance to WALK_TARGET
- [ ] Clear memories when arriving home (via wipeMemoriesWhenFinished)

---

## COMBAT STOP: Two Approaches

### Approach 1: Clear Target Automatically (RECOMMENDED)
Use **InvalidateAttackTarget** behavior set to always invalidate:
\\\java
new InvalidateAttackTarget<>()
    .invalidateIf((entity, target) -> true) // Always clear
\\\

### Approach 2: Condition-Based Activity Selection
Override **getActivityPriorities()** and return RETURNING_HOME before FIGHT
- Brain will pick RETURNING_HOME if memory conditions met
- FIGHT won't activate if RETURNING_HOME is active

---

## TETHER/RESTRICT TO AREA: Two Options

### Option 1: Use SetRandomWalkTarget
Only wander with randomness:
\\\java
new SetRandomWalkTarget<>()
    .setRadius(16, 7) // Stay within 16 blocks horizontal, 7 vertical
    .walkTargetPredicate((entity, pos) -> 
        entity.blockPosition().distManhattan(pos) <= 16) // Enforce tether
\\\

### Option 2: Custom Behavior
Create custom behavior that:
1. Checks distance to home
2. If too far, sets walk target back to home
3. Otherwise allow normal behavior

---

## MEMORY MODULES TO USE

| Memory | Type | Purpose | Who Sets It |
|--------|------|---------|------------|
| WALK_TARGET | WalkTarget | Where to walk | SetWalkTargetToBlock (or custom) |
| ATTACK_TARGET | LivingEntity | Combat target | Sensors / Attack behaviors |
| PATH | Path | Current navigation path | MoveToWalkTarget auto-sets |
| NEAREST_BED | BlockPos | Home from POI | NearestHomeSensor |
| CANT_REACH_WALK_TARGET_SINCE | Long | Path failure time | MoveToWalkTarget auto-sets |
| LOOK_TARGET | EntityTracker/BlockPosTracker | Where to look | Various |

---

## MINIMAL EXAMPLE CODE

\\\java
public Map<Activity, BrainActivityGroup<YourEntity>> getAdditionalTasks() {
    Activity RETURNING_HOME = new Activity("returning_home");
    
    BrainActivityGroup<YourEntity> returnHome = 
        new BrainActivityGroup<>(RETURNING_HOME)
            .priority(8)
            .behaviours(
                new InvalidateAttackTarget<>()  // Stop fighting
                    .invalidateIf((e, t) -> true),
                new MoveToWalkTarget<>()        // Move to WALK_TARGET
            )
            .onlyStartWithMemoryStatus(
                YOUR_HOME_MEMORY, 
                MemoryStatus.VALUE_PRESENT
            );
    
    Map<Activity, BrainActivityGroup<YourEntity>> map = 
        new Object2ObjectOpenHashMap<>();
    map.put(RETURNING_HOME, returnHome);
    return map;
}

public List<Activity> getActivityPriorities() {
    return ObjectArrayList.of(
        Activity.FIGHT,
        RETURNING_HOME,  // Try this before IDLE
        Activity.IDLE
    );
}
\\\

---

## KEY FILES IN DECOMPILED SOURCE

Location: \C:\Users\steve\Documents\Git\hostilemobscore\build\decompiled-cache\smartbrainlib\net\tslat\smartbrainlib\api\core\

**Core:**
- SmartBrain.java - Brain tick and activity management
- BrainActivityGroup.java - Activity container
- SmartBrainOwner.java - Entity interface (override methods here!)

**Behaviors:**
- behaviour/custom/move/*.java - Movement behaviors
- behaviour/custom/path/*.java - Path setting behaviors  
- behaviour/custom/target/*.java - Target management

**Sensors:**
- sensor/vanilla/NearestHomeSensor.java - Home finding
- sensor/custom/*.java - Custom sensors

**Utilities:**
- util/BrainUtils.java - Memory access helpers

