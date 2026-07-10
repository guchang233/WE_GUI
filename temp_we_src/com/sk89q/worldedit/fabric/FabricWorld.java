/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.fabric;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.fabric.internal.ExtendedMinecraftServer;
import com.sk89q.worldedit.fabric.internal.FabricEntity;
import com.sk89q.worldedit.fabric.internal.FabricLoggingProblemReporter;
import com.sk89q.worldedit.fabric.internal.FabricServerLevelDelegateProxy;
import com.sk89q.worldedit.fabric.internal.FabricWorldNativeAccess;
import com.sk89q.worldedit.fabric.internal.NBTConverter;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.generation.ConfiguredFeatureType;
import com.sk89q.worldedit.world.generation.StructureType;
import com.sk89q.worldedit.world.generation.TreeType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import net.minecraft.class_11362;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1299;
import net.minecraft.class_1542;
import net.minecraft.class_156;
import net.minecraft.class_1799;
import net.minecraft.class_1838;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_238;
import net.minecraft.class_2404;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_27;
import net.minecraft.class_2791;
import net.minecraft.class_2802;
import net.minecraft.class_2806;
import net.minecraft.class_2818;
import net.minecraft.class_2841;
import net.minecraft.class_2960;
import net.minecraft.class_2975;
import net.minecraft.class_31;
import net.minecraft.class_3195;
import net.minecraft.class_32;
import net.minecraft.class_3215;
import net.minecraft.class_3218;
import net.minecraft.class_3341;
import net.minecraft.class_3449;
import net.minecraft.class_3730;
import net.minecraft.class_3829;
import net.minecraft.class_3965;
import net.minecraft.class_4076;
import net.minecraft.class_5217;
import net.minecraft.class_5268;
import net.minecraft.class_5285;
import net.minecraft.class_5321;
import net.minecraft.class_5363;
import net.minecraft.class_5819;
import net.minecraft.class_6796;
import net.minecraft.class_6813;
import net.minecraft.class_6818;
import net.minecraft.class_6880;
import net.minecraft.class_7924;
import net.minecraft.server.MinecraftServer;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An adapter to Minecraft worlds for WorldEdit.
 */
public class FabricWorld extends AbstractWorld {

    private static final class_5819 random = class_5819.method_43047();

    private static class_2960 getDimensionRegistryKey(class_1937 world) {
        return Objects.requireNonNull(world.method_8503(), "server cannot be null")
            .method_30611()
            .method_30530(class_7924.field_41241)
            .method_10221(world.method_8597());
    }

    private final WeakReference<class_1937> worldRef;
    private final FabricWorldNativeAccess worldNativeAccess;

    /**
     * Construct a new world.
     *
     * @param world the world
     */
    FabricWorld(class_1937 world) {
        checkNotNull(world);
        this.worldRef = new WeakReference<>(world);
        this.worldNativeAccess = new FabricWorldNativeAccess(worldRef);
    }

    /**
     * Get the underlying handle to the world.
     *
     * @return the world
     * @throws RuntimeException thrown if a reference to the world was lost (i.e. world was unloaded)
     */
    public class_1937 getWorld() {
        class_1937 world = worldRef.get();
        if (world != null) {
            return world;
        } else {
            throw new RuntimeException("The reference to the world was lost (i.e. the world may have been unloaded)");
        }
    }

    @Override
    public String getName() {
        class_5217 levelProperties = getWorld().method_8401();
        return ((class_5268) levelProperties).method_150();
    }

    @Override
    public String id() {
        return getName() + "_" + getDimensionRegistryKey(getWorld());
    }

    @Override
    public Path getStoragePath() {
        final class_1937 world = getWorld();
        MinecraftServer server = world.method_8503();
        checkState(server instanceof ExtendedMinecraftServer, "Need a server world");
        return ((ExtendedMinecraftServer) server).getStoragePath(world);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) throws WorldEditException {
        clearContainerBlockContents(position);
        return worldNativeAccess.setBlock(position, block, sideEffects);
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) {
        worldNativeAccess.applySideEffects(position, previousType, sideEffectSet);
        return Sets.intersection(FabricWorldEdit.inst.getPlatform().getSupportedSideEffects(), sideEffectSet.getSideEffectsToApply());
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        checkNotNull(position);
        return getWorld().method_22339(FabricAdapter.toBlockPos(position));
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        checkNotNull(position);

        class_2586 tile = getWorld().method_8321(FabricAdapter.toBlockPos(position));
        if (tile instanceof class_3829 clearable) {
            clearable.method_5448();
            return true;
        }
        return false;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        checkNotNull(position);
        class_2791 chunk = getWorld().method_8497(position.x() >> 4, position.z() >> 4);
        return getBiomeInChunk(position, chunk);
    }

    private BiomeType getBiomeInChunk(BlockVector3 position, class_2791 chunk) {
        return FabricAdapter.adapt(
            chunk.method_16359(position.x() >> 2, position.y() >> 2, position.z() >> 2).comp_349()
        );
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        checkNotNull(position);
        checkNotNull(biome);

        class_2791 chunk = getWorld().method_8497(position.x() >> 4, position.z() >> 4);
        // Screw it, we know it's really mutable...
        var biomeArray = (class_2841<class_6880<class_1959>>) chunk.method_38259(chunk.method_31602(position.y())).method_38294();
        biomeArray.method_16678(
            (position.x() >> 2) & 3, (position.y() >> 2) & 3, (position.z() >> 2) & 3,
            getWorld().method_30349().method_46759(class_7924.field_41236)
                .orElseThrow()
                .method_46747(class_5321.method_29179(class_7924.field_41236, class_2960.method_60654(biome.id())))
        );
        chunk.method_65063();
        return true;
    }

    private static final LoadingCache<class_3218, FabricFakePlayer> fakePlayers
            = CacheBuilder.newBuilder().weakKeys().softValues().build(CacheLoader.from(FabricFakePlayer::new));

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        class_1799 stack = FabricAdapter.adapt(new BaseItemStack(item.getType(), item.getNbtReference(), 1));
        class_3218 world = (class_3218) getWorld();
        final FabricFakePlayer fakePlayer;
        try {
            fakePlayer = fakePlayers.get(world);
        } catch (ExecutionException ignored) {
            return false;
        }
        fakePlayer.method_6122(class_1268.field_5808, stack);
        fakePlayer.method_5641(position.x(), position.y(), position.z(),
                (float) face.toVector().toYaw(), (float) face.toVector().toPitch());
        final class_2338 blockPos = FabricAdapter.toBlockPos(position);
        final class_3965 rayTraceResult = new class_3965(FabricAdapter.toVec3(position),
                FabricAdapter.adapt(face), blockPos, false);
        class_1838 itemUseContext = new class_1838(fakePlayer, class_1268.field_5808, rayTraceResult);
        class_1269 used = stack.method_7981(itemUseContext);
        if (used != class_1269.field_5812) {
            // try activating the block
            used = getWorld().method_8320(blockPos).method_55780(stack, world, fakePlayer, class_1268.field_5808, rayTraceResult);
        }
        if (used != class_1269.field_5812) {
            used = stack.method_7913(world, fakePlayer, class_1268.field_5808);
        }
        return used == class_1269.field_5812;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        checkNotNull(position);
        checkNotNull(item);

        if (item.getType() == ItemTypes.AIR) {
            return;
        }

        class_1542 entity = new class_1542(getWorld(), position.x(), position.y(), position.z(), FabricAdapter.adapt(item));
        entity.method_6982(10);
        getWorld().method_8649(entity);
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        class_2338 pos = FabricAdapter.toBlockPos(position);
        getWorld().method_22352(pos, true);
    }

    @Override
    public boolean canPlaceAt(BlockVector3 position, BlockState blockState) {
        return FabricAdapter.adapt(blockState).method_26184(getWorld(), FabricAdapter.toBlockPos(position));
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        // Don't even try to regen if it's going to fail.
        class_2802 provider = getWorld().method_8398();
        if (!(provider instanceof class_3215)) {
            return false;
        }

        try {
            doRegen(region, extent, options);
        } catch (Exception e) {
            throw new IllegalStateException("Regen failed", e);
        }

        return true;
    }

    private void doRegen(Region region, Extent extent, RegenOptions options) throws Exception {
        Path tempDir = Files.createTempDirectory("WorldEditWorldGen");
        class_32 levelStorage = class_32.method_26999(tempDir);
        try (class_32.class_5143 session = levelStorage.method_27002("WorldEditTempGen")) {
            class_3218 originalWorld = (class_3218) getWorld();
            class_31 levelProperties = getPrimaryLevelData(originalWorld.method_8401());
            class_5285 originalOpts = levelProperties.method_28057();

            long seed = options.getSeed().orElse(originalWorld.method_8412());
            levelProperties.field_25425 = options.getSeed().isPresent()
                ? originalOpts.method_28024(OptionalLong.of(seed))
                : originalOpts;

            class_5321<class_1937> worldRegKey = originalWorld.method_27983();
            try (class_3218 serverWorld = new class_3218(
                originalWorld.method_8503(), class_156.method_18349(), session,
                ((class_5268) originalWorld.method_8401()),
                worldRegKey,
                new class_5363(
                    originalWorld.method_40134(),
                    originalWorld.method_14178().method_12129()
                ),
                originalWorld.method_27982(),
                seed,
                // No spawners are needed for this world.
                ImmutableList.of(),
                // This controls ticking, we don't need it so set it to false.
                false,
                originalWorld.method_52168()
            )) {
                regenForWorld(region, extent, serverWorld, options);

                // drive the server executor until all tasks are popped off
                originalWorld.method_8503().method_18857(() -> originalWorld.method_8503().method_21684() == 0);
            } finally {
                levelProperties.field_25425 = originalOpts;
            }
        } finally {
            SafeFiles.tryHardToDeleteDir(tempDir);
        }
    }

    private static class_31 getPrimaryLevelData(class_5217 levelData) {
        if (levelData instanceof class_27 derivedLevelData) {
            return getPrimaryLevelData(derivedLevelData.field_139);
        } else if (levelData instanceof class_31 primaryLevelData) {
            return primaryLevelData;
        } else {
            throw new IllegalStateException("Unknown level data type: " + levelData.getClass());
        }
    }

    private void regenForWorld(Region region, Extent extent, class_3218 serverWorld,
                               RegenOptions options) throws WorldEditException {
        List<CompletableFuture<class_2791>> chunkLoadings = submitChunkLoadTasks(region, serverWorld);

        // drive executor until loading finishes
        serverWorld.method_14178().field_18809
            .method_18857(() -> {
                // bail out early if a future fails
                if (chunkLoadings.stream().anyMatch(ftr ->
                    ftr.isDone() && Futures.getUnchecked(ftr) == null
                )) {
                    return false;
                }
                return chunkLoadings.stream().allMatch(CompletableFuture::isDone);
            });

        Map<class_1923, class_2791> chunks = new HashMap<>();
        for (CompletableFuture<class_2791> future : chunkLoadings) {
            @Nullable
            class_2791 chunk = future.getNow(null);
            checkState(chunk != null, "Failed to generate a chunk, regen failed.");
            chunks.put(chunk.method_12004(), chunk);
        }

        for (BlockVector3 vec : region) {
            class_2338 pos = FabricAdapter.toBlockPos(vec);
            class_2791 chunk = chunks.get(new class_1923(pos));
            BlockStateHolder<?> state = FabricAdapter.adapt(chunk.method_8320(pos));
            class_2586 blockEntity = chunk.method_8321(pos);
            if (blockEntity != null) {
                class_2487 tag = FabricLoggingProblemReporter.with(
                    () -> "serializing block entity at " + pos,
                    reporter -> {
                        var tagValueOutput = class_11362.method_71459(reporter, getWorld().method_30349());
                        blockEntity.method_38243(tagValueOutput);
                        return tagValueOutput.method_71475();
                    }
                );
                state = state.toBaseBlock(LazyReference.from(() -> NBTConverter.fromNative(tag)));
            }
            extent.setBlock(vec, state.toBaseBlock());

            if (options.shouldRegenBiomes()) {
                BiomeType biome = getBiomeInChunk(vec, chunk);
                extent.setBiome(vec, biome);
            }
        }
    }

    private List<CompletableFuture<class_2791>> submitChunkLoadTasks(Region region, class_3218 world) {
        List<CompletableFuture<class_2791>> chunkLoadings = new ArrayList<>();
        // Pre-gen all the chunks
        for (BlockVector2 chunk : region.getChunks()) {
            chunkLoadings.add(
                world.method_14178().method_17299(chunk.x(), chunk.z(), class_2806.field_12795, true)
                    .thenApply(either -> either.method_57130(null))
            );
        }
        return chunkLoadings;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static class_5321<class_6796> createTreeFeatureGenerator(com.sk89q.worldedit.util.TreeGenerator.TreeType type) {
        return switch (type) {
            // Based off of the SaplingGenerator class, as well as uses of DefaultBiomeFeatures fields
            case TREE -> class_6818.field_36090;
            case BIG_TREE -> class_6818.field_36101;
            case REDWOOD -> class_6818.field_36094;
            case TALL_REDWOOD -> class_6818.field_36103;
            case MEGA_REDWOOD -> class_6818.field_36104;
            case BIRCH -> class_6818.field_36092;
            case JUNGLE -> class_6818.field_36102;
            case SMALL_JUNGLE -> class_6818.field_36100;
            case SHORT_JUNGLE -> class_6818.field_36100;
            case JUNGLE_BUSH -> class_6818.field_36105;
            case SWAMP -> class_6818.field_36090;
            case ACACIA -> class_6818.field_36093;
            case DARK_OAK -> class_6818.field_36091;
            case TALL_BIRCH -> class_6818.field_36106;
            case WARPED_FUNGUS -> class_6818.field_36089;
            case CRIMSON_FUNGUS -> class_6818.field_36088;
            case CHORUS_PLANT -> class_6813.field_35999;
            case MANGROVE -> class_6818.field_38814;
            case TALL_MANGROVE -> class_6818.field_38815;
            case CHERRY -> class_6818.field_42963;
            case PALE_OAK -> class_6818.field_54887;
            case PALE_OAK_CREAKING -> class_6818.field_54888;
            case RANDOM -> {
                // We're intentionally using index here to get a random tree type
                @SuppressWarnings("EnumOrdinal")
                com.sk89q.worldedit.util.TreeGenerator.TreeType randomTreeType = com.sk89q.worldedit.util.TreeGenerator.TreeType.values()[ThreadLocalRandom.current().nextInt(com.sk89q.worldedit.util.TreeGenerator.TreeType.values().length)];
                yield createTreeFeatureGenerator(randomTreeType);
            }
            default -> null;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean generateTree(com.sk89q.worldedit.util.TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        class_3218 world = (class_3218) getWorld();
        class_6796 generator = Optional.ofNullable(createTreeFeatureGenerator(type))
            .map(k -> world.method_30349().method_30530(class_7924.field_41245).method_29107(k))
            .orElse(null);
        class_3215 chunkManager = world.method_14178();
        if (type == com.sk89q.worldedit.util.TreeGenerator.TreeType.CHORUS_PLANT) {
            position = position.add(0, 1, 0);
        }
        try (FabricServerLevelDelegateProxy.LevelAndProxy proxyLevel = FabricServerLevelDelegateProxy.newInstance(editSession, world)) {
            return generator != null && generator.method_39644(
                proxyLevel.level(), chunkManager.method_12129(), random,
                FabricAdapter.toBlockPos(position)
            );
        }
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        class_3218 world = (class_3218) getWorld();
        class_6796 generator = world.method_30349().method_30530(class_7924.field_41245).method_63535(class_2960.method_12829(type.id()));
        class_3215 chunkManager = world.method_14178();
        try (FabricServerLevelDelegateProxy.LevelAndProxy proxyLevel = FabricServerLevelDelegateProxy.newInstance(editSession, world)) {
            return generator != null && generator.method_39644(
                    proxyLevel.level(), chunkManager.method_12129(), random,
                    FabricAdapter.toBlockPos(position)
            );
        }
    }

    @Override
    public boolean generateFeature(ConfiguredFeatureType type, EditSession editSession, BlockVector3 position) {
        class_3218 world = (class_3218) getWorld();
        class_2975<?, ?> feature = world.method_30349().method_30530(class_7924.field_41239).method_63535(class_2960.method_12829(type.id()));
        class_3215 chunkManager = world.method_14178();
        try (FabricServerLevelDelegateProxy.LevelAndProxy proxyLevel = FabricServerLevelDelegateProxy.newInstance(editSession, world)) {
            return feature != null && feature.method_12862(
                proxyLevel.level(), chunkManager.method_12129(), random,
                FabricAdapter.toBlockPos(position)
            );
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean generateStructure(StructureType type, EditSession editSession, BlockVector3 position) {
        class_3218 world = (class_3218) getWorld();
        class_2378<class_3195> structureRegistry = world.method_30349().method_30530(class_7924.field_41246);
        class_3195 structure = structureRegistry.method_63535(class_2960.method_12829(type.id()));
        if (structure == null) {
            return false;
        }

        class_3215 chunkManager = world.method_14178();
        try (FabricServerLevelDelegateProxy.LevelAndProxy proxyLevel = FabricServerLevelDelegateProxy.newInstance(editSession, world)) {
            class_1923 chunkPos = new class_1923(new class_2338(position.x(), position.y(), position.z()));
            class_3449 structureStart = structure.method_41614(
                structureRegistry.method_47983(structure), world.method_27983(), world.method_30349(),
                chunkManager.method_12129(), chunkManager.method_12129().method_12098(), chunkManager.method_41248(),
                world.method_14183(), world.method_8412(), chunkPos, 0, proxyLevel.level(),
                biome -> true
            );

            if (!structureStart.method_16657()) {
                return false;
            } else {
                class_3341 boundingBox = structureStart.method_14969();
                class_1923 min = new class_1923(class_4076.method_18675(boundingBox.method_35415()), class_4076.method_18675(boundingBox.method_35417()));
                class_1923 max = new class_1923(class_4076.method_18675(boundingBox.method_35418()), class_4076.method_18675(boundingBox.method_35420()));
                class_1923.method_19281(min, max).forEach((chunkPosx) ->
                    structureStart.method_14974(
                        proxyLevel.level(), world.method_27056(), chunkManager.method_12129(), world.method_8409(),
                        new class_3341(chunkPosx.method_8326(), world.method_31607(), chunkPosx.method_8328(),
                            chunkPosx.method_8327(), world.method_31600(), chunkPosx.method_8329()),
                        chunkPosx
                    )
                );
                return true;
            }
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        getWorld().method_22350(FabricAdapter.toBlockPos(pt));
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
        fixLighting(chunks);
    }

    @Override
    public void sendBiomeUpdates(Iterable<BlockVector2> chunks) {
        List<class_2791> nativeChunks = chunks instanceof Collection<BlockVector2> chunkCollection ? Lists.newArrayListWithCapacity(chunkCollection.size()) : Lists.newArrayList();
        for (BlockVector2 chunk : chunks) {
            nativeChunks.add(getWorld().method_8402(chunk.x(), chunk.z(), class_2806.field_12794, false));
        }
        ((class_3218) getWorld()).method_14178().field_17254.method_49421(nativeChunks);
    }

    @Override
    public void fixLighting(Iterable<BlockVector2> chunks) {
        class_1937 world = getWorld();
        for (BlockVector2 chunk : chunks) {
            // Fetch the chunk after light initialization at least
            // We'll be doing a full relight anyways, so we don't need to be LIGHT yet
            ((class_3218) world).method_14178().method_17293().method_17310(world.method_22342(
                chunk.x(), chunk.z(), class_2806.field_44633
            ), false).exceptionally(t -> {
                WorldEdit.logger.warn("Failed to relight chunk at " + chunk, t);
                return null;
            });
        }
    }

    @Override
    public WeatherType getWeather() {
        class_5217 info = getWorld().method_8401();
        if (info.method_203()) {
            return WeatherTypes.THUNDER_STORM;
        }
        if (info.method_156()) {
            return WeatherTypes.RAIN;
        }
        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        class_5268 info = (class_5268) getWorld().method_8401();
        if (info.method_203()) {
            return info.method_145();
        }
        if (info.method_156()) {
            return info.method_190();
        }
        return info.method_155();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        setWeather(weatherType, 0);
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        class_5268 info = (class_5268) getWorld().method_8401();
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            info.method_167(0);
            info.method_147(true);
            info.method_173((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            info.method_167(0);
            info.method_157(true);
            info.method_164((int) duration);
        } else if (weatherType == WeatherTypes.CLEAR) {
            info.method_157(false);
            info.method_147(false);
            info.method_167((int) duration);
        }
    }

    @Override
    public int getMinY() {
        return getWorld().method_31607();
    }

    @Override
    public int getMaxY() {
        return getWorld().method_31600();
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return FabricAdapter.adapt(getWorld().method_8401().method_74893().method_74897());
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        net.minecraft.class_2680 mcState = getWorld()
                .method_8497(position.x() >> 4, position.z() >> 4)
                .method_8320(FabricAdapter.toBlockPos(position));

        return FabricAdapter.adapt(mcState);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        class_2338 pos = new class_2338(position.x(), position.y(), position.z());
        // Avoid creation by using the CHECK mode -- if it's needed, it'll be re-created anyways
        class_2586 tile = ((class_2818) getWorld().method_22350(pos)).method_12201(pos, class_2818.class_2819.field_12859);

        if (tile != null) {
            class_2487 tag = FabricLoggingProblemReporter.with(
                () -> "serializing block entity at " + pos,
                reporter -> {
                    var tagValueOutput = class_11362.method_71459(reporter, getWorld().method_30349());
                    tile.method_38243(tagValueOutput);
                    return tagValueOutput.method_71475();
                }
            );
            return getBlock(position).toBaseBlock(LazyReference.from(() -> NBTConverter.fromNative(tag)));
        } else {
            return getBlock(position).toBaseBlock();
        }
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return switch (o) {
            case FabricWorld other -> {
                class_1937 otherWorld = other.worldRef.get();
                class_1937 thisWorld = worldRef.get();
                yield otherWorld != null && otherWorld.equals(thisWorld);
            }
            case World world -> world.getName().equals(getName());
            case null, default -> false;
        };
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        final class_1937 world = getWorld();
        class_238 box = new class_238(
            FabricAdapter.toVec3(region.getMinimumPoint()),
            FabricAdapter.toVec3(region.getMaximumPoint().add(BlockVector3.ONE))
        );
        List<net.minecraft.class_1297> nmsEntities = world.method_8333(
            (net.minecraft.class_1297) null,
            box,
            e -> region.contains(FabricAdapter.adapt(e.method_24515()))
        );
        return nmsEntities.stream()
            .map(FabricEntity::new)
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<? extends Entity> getEntities() {
        final class_1937 world = getWorld();
        if (!(world instanceof class_3218 serverLevel)) {
            return Collections.emptyList();
        }
        return Streams.stream(serverLevel.method_27909())
            .map(FabricEntity::new)
            .collect(ImmutableList.toImmutableList());
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        class_3218 world = (class_3218) getWorld();
        String entityId = entity.getType().id();
        final Optional<class_1299<?>> entityType = class_1299.method_5898(entityId);
        if (entityType.isEmpty()) {
            return null;
        }
        LinCompoundTag linTag = entity.getNbt();
        class_2487 tag;
        if (linTag != null) {
            tag = NBTConverter.toNative(linTag);
            removeUnwantedEntityTagsRecursively(tag);
        } else {
            tag = new class_2487();
        }
        tag.method_10582("id", entityId);

        net.minecraft.class_1297 createdEntity = class_1299.method_71371(tag, world, class_3730.field_16462, (loadedEntity) -> {
            loadedEntity.method_5641(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            return loadedEntity;
        });
        if (createdEntity != null) {
            world.method_30771(createdEntity);
            return new FabricEntity(createdEntity);
        }
        return null;
    }

    private void removeUnwantedEntityTagsRecursively(class_2487 tag) {
        for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
            tag.method_10551(name);
        }

        // Adapted from net.minecraft.world.entity.EntityType#loadEntityRecursive
        tag.method_10554("Passengers").ifPresent(nbttaglist -> {
            for (int i = 0; i < nbttaglist.size(); ++i) {
                removeUnwantedEntityTagsRecursively(nbttaglist.method_68582(i));
            }
        });
    }

    @Override
    public Mask createLiquidMask() {
        return new AbstractExtentMask(this) {
            @Override
            public boolean test(BlockVector3 vector) {
                return FabricAdapter.adapt(getExtent().getBlock(vector)).method_26204() instanceof class_2404;
            }
        };
    }

    @Override
    public boolean isValid() {
        return worldRef.get() != null;
    }
}
