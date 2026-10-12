package dev.chunkindicators;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IndicatorScanner {
    private static final int VIOLET = 0xEE82EE;
    private static final int RED = 0xFF0000;

    private record CacheKey(ResourceKey<Level> dimension, long chunk) {}
    private record CacheEntry(long tick, boolean hasSkeletonSpawner) {}

    private static final Map<CacheKey, CacheEntry> SPAWNER_CACHE = new HashMap<>();

    private IndicatorScanner() {}

    public static void clearCache() {
        SPAWNER_CACHE.clear();
    }

    public static void tick(MinecraftServer server, IndicatorConfig cfg) {
        long time = server.overworld().getGameTime();
        if (time % Math.max(1, cfg.scanIntervalTicks) != 0) {
            return;
        }

        List<ServerPlayer> everyone = server.getPlayerList().getPlayers();
        for (ServerPlayer observer : everyone) {
            if (!cfg.observerTag.isEmpty() && !observer.getTags().contains(cfg.observerTag)) {
                continue;
            }
            scanFor(observer, everyone, cfg, time);
        }

        if (time % 6000 == 0) {
            SPAWNER_CACHE.values().removeIf(e -> time - e.tick() > cfg.spawnerCacheTicks * 4L);
        }
    }

    private static void scanFor(ServerPlayer observer, List<ServerPlayer> everyone, IndicatorConfig cfg, long time) {
        ServerLevel level = observer.level();
        ChunkPos center = observer.chunkPosition();
        int r = cfg.radiusChunks;

        Set<Long> playerChunks = new HashSet<>();
        for (ServerPlayer other : everyone) {
            if (other == observer || other.level() != level) {
                continue;
            }
            int y = other.getBlockY();
            if (y < cfg.minY || y > cfg.maxY) {
                continue;
            }
            ChunkPos cp = other.chunkPosition();
            if (Math.abs(cp.x() - center.x()) <= r && Math.abs(cp.z() - center.z()) <= r) {
                playerChunks.add(cp.pack());
            }
        }
        for (long packed : playerChunks) {
            drawIndicator(level, observer, new ChunkPos(packed), VIOLET, cfg);
        }

        for (int cx = center.x() - r; cx <= center.x() + r; cx++) {
            for (int cz = center.z() - r; cz <= center.z() + r; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                if (hasSkeletonSpawner(level, chunk, cfg, time)) {
                    drawIndicator(level, observer, chunk.getPos(), RED, cfg);
                }
            }
        }
    }

    private static boolean hasSkeletonSpawner(ServerLevel level, LevelChunk chunk, IndicatorConfig cfg, long time) {
        CacheKey key = new CacheKey(level.dimension(), chunk.getPos().pack());
        CacheEntry cached = SPAWNER_CACHE.get(key);
        if (cached != null && time - cached.tick() < cfg.spawnerCacheTicks) {
            return cached.hasSkeletonSpawner();
        }

        boolean found = false;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() < cfg.minY || pos.getY() > cfg.maxY) {
                continue;
            }
            if (entry.getValue() instanceof SpawnerBlockEntity spawner && isSkeleton(level, spawner)) {
                found = true;
                break;
            }
        }
        SPAWNER_CACHE.put(key, new CacheEntry(time, found));
        return found;
    }

    private static boolean isSkeleton(ServerLevel level, SpawnerBlockEntity spawner) {
        CompoundTag tag = spawner.saveWithoutMetadata(level.registryAccess());
        return tag.getCompound("SpawnData")
            .flatMap(spawnData -> spawnData.getCompound("entity"))
            .flatMap(entity -> entity.getString("id"))
            .map(id -> id.equals("minecraft:skeleton"))
            .orElse(false);
    }

    private static void drawIndicator(ServerLevel level, ServerPlayer observer, ChunkPos chunk, int rgb, IndicatorConfig cfg) {
        double x = chunk.getMinBlockX() + 8.0;
        double z = chunk.getMinBlockZ() + 8.0;
        double y = cfg.indicatorY + 0.5;
        level.sendParticles(observer, new DustParticleOptions(rgb, cfg.indicatorScale),
            true, true, x, y, z, 12, 0.35, 0.35, 0.35, 0.0);
    }
  }
