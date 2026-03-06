package com.snowbigdeal.hostilemobscore.datapack;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.*;

/**
 * Loads all {@code data/<namespace>/boss_sequences/*.json} files into
 * {@link BossSequenceLoader#ALL} on every datapack reload.
 *
 * <p>Register via {@code AddReloadListenerEvent}:
 * <pre>{@code event.addListener(new BossSequenceLoader()); }</pre>
 */
public class BossSequenceLoader extends SimplePreparableReloadListener<Map<ResourceLocation, BossSequence>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BossSequenceLoader.class);
    private static final String FOLDER  = "boss_sequences";
    private static final Gson   GSON    = new GsonBuilder().create();

    /** All loaded sequences, keyed by their datapack resource location. */
    public static final Map<ResourceLocation, BossSequence> ALL = new HashMap<>();

    // -------------------------------------------------------------------------
    // Reload lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected Map<ResourceLocation, BossSequence> prepare(ResourceManager manager,
                                                           ProfilerFiller profiler) {
        Map<ResourceLocation, BossSequence> result = new HashMap<>();

        manager.listResources(FOLDER, path -> path.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        BossSequence sequence = parse(location, json);
                        result.put(location, sequence);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to load boss sequence '{}': {}", location, ex.getMessage());
                    }
                });

        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, BossSequence> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        ALL.clear();
        ALL.putAll(prepared);
        LOGGER.info("Loaded {} boss sequence(s)", ALL.size());
    }

    // -------------------------------------------------------------------------
    // JSON parsing
    // -------------------------------------------------------------------------

    private static BossSequence parse(ResourceLocation fileLocation, JsonObject json) {
        boolean loop = json.has("loop") && json.get("loop").getAsBoolean();
        List<BossAttackStep> steps = new ArrayList<>();

        JsonArray array = json.getAsJsonArray("sequence");
        for (JsonElement element : array) {
            JsonObject stepJson  = element.getAsJsonObject();
            ResourceLocation id  = ResourceLocation.parse(stepJson.get("attack").getAsString());
            int delay            = stepJson.has("delay_ticks")
                    ? stepJson.get("delay_ticks").getAsInt() : 0;
            steps.add(new BossAttackStep(id, delay));
        }

        // Derive the sequence's own ID from the file location
        // e.g. "hostilemobscore:boss_sequences/slime_king.json" → "hostilemobscore:slime_king"
        String path = fileLocation.getPath()
                .replace(FOLDER + "/", "")
                .replace(".json", "");
        ResourceLocation sequenceId = ResourceLocation.fromNamespaceAndPath(
                fileLocation.getNamespace(), path);

        return new BossSequence(sequenceId, Collections.unmodifiableList(steps), loop);
    }
}
