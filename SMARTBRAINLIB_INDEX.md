# SmartBrainLib Return Home System - Documentation Index

## Generated Documentation Files

This analysis contains 4 comprehensive guides for implementing a "return home / tether" system using SmartBrainLib.

### 1. SMARTBRAINLIB_QUICK_REFERENCE.md (5 KB)
Start here! Overview of the most relevant classes and quick checklist.
- 8-class reference table with pathfinding info
- Implementation checklist (10 items)
- Combat stop approaches
- Tether/restrict options
- Minimal example code

### 2. SMARTBRAINLIB_RETURN_HOME_ANALYSIS.md (24 KB)
Comprehensive deep dive into all relevant classes.
- 23 detailed behavior classes with constructor parameters
- 2 relevant sensor classes with memory info
- Activity management system explanation
- Recommended implementation strategy (5 steps)
- Custom behavior creation template
- Key takeaways and best practices

### 3. SMARTBRAINLIB_ARCHITECTURE.md (10 KB)
Visual guides for understanding the system flow.
- Class hierarchy tree (full inheritance)
- Memory flow diagram for return home
- Memory module dependency chain (6 steps)
- Behavior composition pattern
- Activity priority resolution flowchart
- Configuration point reference

### 4. THIS FILE (index)
Navigation and usage instructions.

---

## Quick Start Summary

If you just want core classes:

1. MoveToWalkTarget - Primary pathfinding behavior
2. SetWalkTargetToBlock - Set destination before moving
3. InvalidateAttackTarget - Stop fighting
4. BrainActivityGroup - Container for behaviors
5. SmartBrainOwner - Override getAdditionalTasks()

---

## Key Classes Reference

| Class | Purpose | Pathfinding? |
|-------|---------|--------------|
| MoveToWalkTarget | Execute path to WALK_TARGET | YES |
| SetWalkTargetToBlock | Set target to block | NO |
| InvalidateAttackTarget | Clear ATTACK_TARGET | NO |
| FollowEntity | Follow entity with pathfinding | YES |
| SetRandomWalkTarget | Wander within radius | NO |
| NearestHomeSensor | Find home POI | N/A |
| BrainActivityGroup | Activity container | N/A |
| SmartBrainOwner | Entity interface | N/A |

---

## Memory Modules for Return Home

| Memory | Set By | Used By | Purpose |
|--------|--------|---------|---------|
| WALK_TARGET | SetWalkTargetToBlock | MoveToWalkTarget | Where to go |
| PATH | MoveToWalkTarget | MoveToWalkTarget | Current path |
| ATTACK_TARGET | Sensors | Fight tasks | Combat target |
| CANT_REACH_WALK_TARGET_SINCE | MoveToWalkTarget | InvalidateAttackTarget | Path failure tracking |

---

## Implementation Overview

1. Store home position (custom memory or NBT)
2. Create custom Activity for RETURNING_HOME
3. Add behavior to clear ATTACK_TARGET
4. Add behavior to set WALK_TARGET to home
5. Add behavior to move (MoveToWalkTarget)
6. Register in getAdditionalTasks()
7. Override getActivityPriorities() with ordering
8. Set onlyStartWithMemoryStatus() condition
9. Detect arrival by checking distance
10. Clear memories when home

---

## File Locations

All classes in: C:\Users\steve\Documents\Git\hostilemobscore\build\decompiled-cache\smartbrainlib\net\tslat\smartbrainlib\api\core\

- Behaviors: behaviour/custom/{move,path,target,misc}/
- Sensors: sensor/{vanilla,custom}/
- Core: BrainActivityGroup.java, SmartBrain.java
- Utilities: util/BrainUtils.java
- Interface: api/SmartBrainOwner.java

---

## Which Document to Read?

Quick overview: 5 min -> QUICK_REFERENCE.md
Need code examples: 5 min -> QUICK_REFERENCE.md
Understand all options: 15 min -> RETURN_HOME_ANALYSIS.md
See memory flow: 10 min -> ARCHITECTURE.md
Deep technical reference: 25 min -> RETURN_HOME_ANALYSIS.md

---

## Key Takeaways

Use MoveToWalkTarget for pathfinding execution
Use SetWalkTargetToBlock to set destination first
Use InvalidateAttackTarget to stop combat
Use separate Activity for return home priority
Store home in custom memory or entity NBT
Check arrival via distance to WALK_TARGET
Use getActivityPriorities() to control order
Override getAdditionalTasks() for custom activities
Use BrainActivityGroup to bundle behaviors
Use onlyStartWithMemoryStatus() for conditions

---

Last Updated: 2024
Source: SmartBrainLib Decompiled Source
