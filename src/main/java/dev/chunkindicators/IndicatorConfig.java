package dev.chunkindicators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IndicatorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chunk_indicators.json");

    public boolean enabled = true;
    public int radiusChunks = 32;
    public String observerName = "";
    public int scanIntervalTicks = 20;
    public int spawnerCacheTicks = 600;
    public float indicatorScale = 3.0f;
    public int minY = -66;
    public int maxY = -1;
    public int indicatorY = 67;

    public static IndicatorConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                IndicatorConfig loaded = GSON.fromJson(reader, IndicatorConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                ChunkIndicatorsMod.LOGGER.error("Could not read {}, using defaults", PATH, e);
            }
        }
        IndicatorConfig fresh = new IndicatorConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ChunkIndicatorsMod.LOGGER.error("Could not write {}", PATH, e);
        }
    }
        }
