# Copilot Instructions for Hostile Mobs Core

## Project Overview

Hostile Mobs Core is a **Minecraft Mod** for **NeoForge 1.21.1** that adds custom hostile entities and related content. The mod uses **Gradle** for building and is structured as a standard NeoForge mod project with dependencies on GeckoLib (for animations) and SmartBrainLib (for AI behavior).

## Build, Test, and Lint

### Build Commands

```bash
# Build the entire mod (produces JAR in build/libs/)
./gradlew build

# Clean build (removes build artifacts)
./gradlew clean

# Run refresh dependencies
./gradlew --refresh-dependencies

# Build with configuration cache
./gradlew build --configuration-cache
```

### Run Configurations

The project has multiple run configurations defined in `build.gradle`:

```bash
# Run Minecraft client with mod loaded
./gradlew runClient

# Run Minecraft server with mod loaded
./gradlew runServer

# Run game tests
./gradlew runGameTestServer

# Run data generators (creates assets, models, translations, etc.)
./gradlew runData
```

### Notes on Testing

- This is a Minecraft mod, so testing is primarily done through game testing via run configurations
- GeckoLib and SmartBrainLib are required for entity rendering and AI behavior
- Data generators are essential for creating JSON files; run `./gradlew runData` after adding new content

## High-Level Architecture

### Package Structure

- **`com.snowbigdeal.hostilemobscore`** - Main mod class and core functionality
  - `HostileMobsCore.java` - Entry point, handles mod initialization and event registration
  - `HostileMobsCoreClient.java` - Client-side setup
  - `Config.java` - Mod configuration (NeoForge ModConfigSpec)

- **`entity/`** - Custom entity definitions
  - `ModEntities.java` - Entity registry
  - `HostileMob.java` - Abstract base for all mod entities (brain, tether, return-home)
  - `ModMemoryTypes.java` - Custom memory modules and activities
  - `slimes/` - Slime-type mobs (AngrySlime, SleepySlime)
  - `behaviour/` - Shared SmartBrainLib behaviours (DeaggroBehaviour, ReturnHomeBehaviour, etc.)

- **`items/`** - Item definitions
  - `ModItems.java` - Item registry (includes spawn eggs)

- **`datagen/`** - Data generation (creates JSON assets at runtime)
  - `DataGenerators.java` - Data gen driver
  - `ModItemModelProvider.java` - Item model generation

- **`events/`** - Event handlers
  - `MobEventBusEvents.java` - Lifecycle and game events

### Key Concepts

- **Event Bus Registration**: Mod uses NeoForge's event bus for lifecycle events and game events
- **Registry Pattern**: All entities and items are registered via DeferredRegister in their respective registry classes (`ModEntities`, `ModItems`)
- **Distributed Setup**: Client-only code is marked with `@EventBusSubscriber(Dist.CLIENT)`
- **Data Generation**: JSON files are generated during data generation, not committed to source

## Key Conventions

### Naming & Structure

- Mod ID: `hostilemobscore` (lowercase, must match `@Mod` annotation)
- Package structure mirrors feature organization (`entity.*`, `items.*`, `events.*`)
- Entity classes follow pattern: `EntityName.java`, `EntityNameModel.java`, `EntityNameRenderer.java`

### Event Handling

- Use `@SubscribeEvent` for instance methods (register class with `NeoForge.EVENT_BUS`)
- Use `@EventBusSubscriber` + `@SubscribeEvent` for static methods
- Client-only subscribers must specify `value = Dist.CLIENT`

### Model & Rendering

- Models use **GeckoLib** for animations (models extend `AnimatedGeoModel`)
- Renderers extend `GeoEntityRenderer<T>`
- Register client-side renderers in `ClientModEvents.onClientSetup()`

### Sounds

- Register sound events via `DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID)` using `SoundEvent.createVariableRangeEvent()`
- `sounds.json` sound file paths **must** include the mod namespace prefix (e.g. `"hostilemobscore:entity/foo/bar"`). Paths without a namespace default to `minecraft:` and will silently fail.
- **OGG files must be mono.** Stereo files bypass OpenAL distance attenuation and always play at full volume regardless of the entity's position.
- `Entity.playSound()` plays at the entity's location and is automatically silent on the client side — no side-check needed.

### AI Behavior

- Use **SmartBrainLib** for goal-based AI (replaces vanilla goal selectors)
- See `.github/instructions/smartbrainlib.instructions.md` for full patterns and API reference
- Entity AI is defined in `entity/behaviour/` (shared behaviours) and per-entity classes
- `customServerAiStep()` must contain only `tickBrain(typedSelf())` — all logic goes in behaviours

### Configuration

- Mod config uses `ModConfigSpec` builder pattern
- Config file is auto-created by NeoForge at `config/hostilemobscore-common.toml`
- Register config in mod constructor with `modContainer.registerConfig()`

### Dependencies & Versions

- **Java 21** (required by Minecraft 1.21.1)
- **NeoForge**: 21.1.219
- **Parchment Mappings**: 2024.11.17 (provides better method/field names)
- **GeckoLib**: 4.8.3
- **SmartBrainLib**: 1.16.11

### Code Style

- If an if-statement chain exceeds 2 branches, extract it into a named method.

- Multi-threaded parallel builds enabled
- Gradle daemon enabled for faster iterations
- Configuration cache enabled (use `--configuration-cache` flag)
- Max heap: 1GB (set in `gradle.properties`)

## Copilot CLI Workflow Guidelines

### Planning
- Use plan mode (Shift+Tab or `/plan`) for complex multi-file changes, new features, and refactoring.
- Always ask clarifying questions before implementing to align on scope and approach.
- Follow the **explore → plan → code → verify → commit** cycle for non-trivial work.

### Sessions
- Use `/new` or `/clear` between unrelated tasks to keep context focused.
- Use `/fleet` to parallelise large tasks into subagents when independent subtasks can run concurrently.
- Use `/delegate` for tangential work (docs, unrelated modules) so main session stays focused.

### Code changes
- Make the **smallest possible change** that correctly addresses the request.
- Run `.\gradlew compileJava` after every change to catch errors before moving on.
- Run `.\gradlew build` before committing to ensure the full build passes.
- Never commit secrets or generated JSON (data-gen output).

### Instructions
- Keep `.github/copilot-instructions.md` concise and actionable — lengthy instructions dilute effectiveness.
- Use `.github/instructions/*.instructions.md` for modular, feature-specific guidance.

## Copilot Skills: Library Source Code Access

**Copilot can extract and examine NeoForge/Minecraft source code** from the build artifacts in `build/moddev/artifacts/`:

- **`neoforge-21.1.219-sources.jar`** - Complete Java source code for all NeoForge and Minecraft classes
- **`neoforge-21.1.219-merged.jar`** - Compiled bytecode (30.7 MB) with all classes
- **`neoforge-21.1.219.jar`** - NeoForge-only artifact

### Decompile Cache

When Copilot needs to inspect source from any JAR, it must extract it to a persistent cache directory **once** and reuse it on subsequent sessions:

```
build/decompiled-cache/<artifact-name>/
```

**Workflow:**
1. Check if `build/decompiled-cache/<artifact-name>/` already exists.
2. If not, extract the JAR there: `Expand-Archive` (for zip/jar) or `jar xf` into that directory.
3. Search the cached `.java` files with grep/glob as normal source code.
4. Never re-extract if the cache directory is already present.

**Known artifacts to cache on first use:**
- `build/moddev/artifacts/neoforge-21.1.219-sources.jar` → `build/decompiled-cache/neoforge-21.1.219-sources/`

### What Copilot Can Do

When working on features, ask Copilot to look up:
- Specific API signatures and method implementations
- Class hierarchies and inheritance patterns
- Real examples of how NeoForge implements specific systems
- SmartBrainLib or GeckoLib API details from their source

### Example Queries

- "How does Minecraft's Entity class handle collision?"
- "What methods does LivingEntity provide for AI?"
- "Show me how GeckoLib AnimatedGeoModel works"
- "What's the exact structure of ModConfig.Type?"

This accelerates development by providing access to actual source code instead of relying on documentation alone.
