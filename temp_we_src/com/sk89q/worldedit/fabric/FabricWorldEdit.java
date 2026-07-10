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

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.internal.anvil.ChunkDeleter;
import com.sk89q.worldedit.internal.event.InteractionDebouncer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.lifecycle.Lifecycled;
import com.sk89q.worldedit.util.lifecycle.SimpleLifecycled;
import com.sk89q.worldedit.world.biome.BiomeCategory;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.generation.ConfiguredFeatureType;
import com.sk89q.worldedit.world.generation.StructureType;
import com.sk89q.worldedit.world.generation.TreeType;
import com.sk89q.worldedit.world.item.ItemCategory;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.class_10853;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2378;
import net.minecraft.class_2944;
import net.minecraft.class_2960;
import net.minecraft.class_2979;
import net.minecraft.class_3222;
import net.minecraft.class_3244;
import net.minecraft.class_3965;
import net.minecraft.class_5321;
import net.minecraft.class_5455;
import net.minecraft.class_6796;
import net.minecraft.class_6880;
import net.minecraft.class_6885;
import net.minecraft.class_7157;
import net.minecraft.class_7924;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.enginehub.piston.Command;
import org.enginehub.worldeditcui.protocol.CUIPacket;
import org.enginehub.worldeditcui.protocol.CUIPacketHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static com.sk89q.worldedit.internal.anvil.ChunkDeleter.DELCHUNKS_FILE_NAME;

/**
 * The Fabric implementation of WorldEdit.
 */
public class FabricWorldEdit implements ModInitializer {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    public static final String MOD_ID = "worldedit";

    public static final Lifecycled<MinecraftServer> LIFECYCLED_SERVER;

    static {
        SimpleLifecycled<MinecraftServer> lifecycledServer = SimpleLifecycled.invalid();
        ServerLifecycleEvents.SERVER_STARTED.register(lifecycledServer::newValue);
        ServerLifecycleEvents.SERVER_STOPPING.register(__ -> lifecycledServer.invalidate());
        LIFECYCLED_SERVER = lifecycledServer;
    }

    /**
     * {@return current server's registry access} Not for long-term storage.
     */
    public static class_5455 registryAccess() {
        return LIFECYCLED_SERVER.valueOrThrow().method_30611();
    }

    /**
     * {@return current server's registry} Not for long-term storage.
     *
     * @param key the registry key
     */
    public static <T> class_2378<T> getRegistry(class_5321<class_2378<T>> key) {
        return LIFECYCLED_SERVER.valueOrThrow().method_30611().method_30530(key);
    }

    private FabricPermissionsProvider provider;

    public static FabricWorldEdit inst;

    private InteractionDebouncer debouncer;
    private FabricPlatform platform;
    private FabricConfiguration config;
    private Path workingDir;

    private ModContainer container;

    public FabricWorldEdit() {
        inst = this;
    }

    @Override
    public void onInitialize() {
        this.container = FabricLoader.getInstance().getModContainer("worldedit").orElseThrow(
            () -> new IllegalStateException("WorldEdit mod missing in Fabric")
        );

        // Setup working directory
        workingDir = FabricLoader.getInstance().getConfigDir().resolve("worldedit");
        if (!Files.exists(workingDir)) {
            try {
                Files.createDirectory(workingDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        this.platform = new FabricPlatform(this);
        debouncer = new InteractionDebouncer(platform);

        WorldEdit.getInstance().getPlatformManager().register(platform);

        config = new FabricConfiguration(this);
        this.provider = getInitialPermissionsProvider();

        CUIPacketHandler.instance().registerServerboundHandler(this::onCuiPacket);

        ServerTickEvents.END_SERVER_TICK.register(ThreadSafeCache.getInstance());
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onStartingServer);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onStopServer);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerDisconnect);
        AttackBlockCallback.EVENT.register(this::onLeftClickBlock);
        UseBlockCallback.EVENT.register(this::onRightClickBlock);
        UseItemCallback.EVENT.register(this::onRightClickItem);
        LOGGER.info("WorldEdit for Fabric (version " + getInternalVersion() + ") is loaded");
    }

    private void registerCommands(CommandDispatcher<class_2168> dispatcher, class_7157 registryAccess, class_2170.class_5364 environment) {
        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());
        PlatformManager manager = WorldEdit.getInstance().getPlatformManager();
        Platform commandsPlatform = manager.queryCapability(Capability.USER_COMMANDS);
        if (commandsPlatform != platform || !platform.isHookingEvents()) {
            // We're not in control of commands/events -- do not register.
            return;
        }

        List<Command> commands = manager.getPlatformCommandManager().getCommandManager()
            .getAllCommands().toList();
        for (Command command : commands) {
            CommandWrapper.register(dispatcher, command);
            Set<String> perms = command.getCondition().as(PermissionCondition.class)
                .map(PermissionCondition::getPermissions)
                .orElseGet(Collections::emptySet);
            if (!perms.isEmpty()) {
                perms.forEach(getPermissionsProvider()::registerPermission);
            }
        }
    }

    private FabricPermissionsProvider getInitialPermissionsProvider() {
        try {
            Class.forName("me.lucko.fabric.api.permissions.v0.Permissions", false, getClass().getClassLoader());
            Optional<Version> version = FabricLoader.getInstance().getModContainer("fabric-permissions-api-v0")
                    .map(ModContainer::getMetadata)
                    .map(ModMetadata::getVersion);

            if (version.isPresent() && !VersionPredicate.parse(">=0.6.0").test(version.get())) {
                throw new RuntimeException("Fabric permissions version " + version.get() + " is not supported. Please update Fabric Permissions API");
            }

            return new FabricPermissionsProvider.LuckoFabricPermissionsProvider(platform);
        } catch (ClassNotFoundException ignored) {
            // fallback to vanilla
        } catch (Exception e) {
            // catch any exception to prevent crashing the server, but still print a warning
            LOGGER.warn("Failed to load Fabric permissions provider. Falling back to Minecraft", e);
        }

        return new FabricPermissionsProvider.VanillaPermissionsProvider(platform);
    }

    private void setupRegistries(MinecraftServer server) {
        // Blocks
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41254).method_10235()) {
            String key = name.toString();
            if (BlockType.REGISTRY.get(key) == null) {
                BlockType.REGISTRY.register(key, new BlockType(key,
                    input -> FabricAdapter.adapt(FabricAdapter.adapt(input.getBlockType()).method_9564())));
            }
        }
        // Items
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41197).method_10235()) {
            String key = name.toString();
            if (ItemType.REGISTRY.get(key) == null) {
                ItemType.REGISTRY.register(key, new ItemType(key));
            }
        }
        // Entities
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41266).method_10235()) {
            String key = name.toString();
            if (EntityType.REGISTRY.get(key) == null) {
                EntityType.REGISTRY.register(key, new EntityType(key));
            }
        }
        // Biomes
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41236).method_10235()) {
            String key = name.toString();
            if (BiomeType.REGISTRY.get(key) == null) {
                BiomeType.REGISTRY.register(key, new BiomeType(key));
            }
        }
        // Tags
        server.method_30611().method_30530(class_7924.field_41254).method_40272().map(t -> t.method_40251().comp_327()).forEach(name -> {
            String key = name.toString();
            if (BlockCategory.REGISTRY.get(key) == null) {
                BlockCategory.REGISTRY.register(key, new BlockCategory(key));
            }
        });
        server.method_30611().method_30530(class_7924.field_41197).method_40272().map(t -> t.method_40251().comp_327()).forEach(name -> {
            String key = name.toString();
            if (ItemCategory.REGISTRY.get(key) == null) {
                ItemCategory.REGISTRY.register(key, new ItemCategory(key));
            }
        });
        class_2378<class_1959> biomeRegistry = server.method_30611().method_30530(class_7924.field_41236);
        biomeRegistry.method_40272().forEach(tag -> {
            String key = tag.method_40251().comp_327().toString();
            if (BiomeCategory.REGISTRY.get(key) == null) {
                BiomeCategory.REGISTRY.register(key, new BiomeCategory(
                    key,
                    () -> biomeRegistry.method_46733(tag.method_40251())
                        .stream()
                        .flatMap(class_6885.class_6888::method_40239)
                        .map(class_6880::comp_349)
                        .map(FabricAdapter::adapt)
                        .collect(Collectors.toSet()))
                );
            }
        });
        // Features
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41239).method_10235()) {
            String key = name.toString();
            if (ConfiguredFeatureType.REGISTRY.get(key) == null) {
                ConfiguredFeatureType.REGISTRY.register(key, new ConfiguredFeatureType(key));
            }
        }
        // Structures
        for (class_2960 name : server.method_30611().method_30530(class_7924.field_41246).method_10235()) {
            String key = name.toString();
            if (StructureType.REGISTRY.get(key) == null) {
                StructureType.REGISTRY.register(key, new StructureType(key));
            }
        }
        // Trees
        class_2378<class_6796> placedFeatureRegistry = server.method_30611().method_30530(class_7924.field_41245);
        for (class_2960 name : placedFeatureRegistry.method_10235()) {
            // Do some hackery to make sure this is a tree
            var underlyingFeature = placedFeatureRegistry.method_10223(name).get().comp_349().comp_334().comp_349().comp_332();
            if (underlyingFeature instanceof class_2944 || underlyingFeature instanceof class_10853 || underlyingFeature instanceof class_2979) {
                String key = name.toString();
                if (TreeType.REGISTRY.get(key) == null) {
                    TreeType.REGISTRY.register(key, new TreeType(key));
                }
            }
        }

        // ... :|
        GameModes.get("");
        WeatherTypes.get("");
        com.sk89q.worldedit.registry.Registries.get("");
    }

    private void onStartingServer(MinecraftServer minecraftServer) {
        final Path delChunks = workingDir.resolve(DELCHUNKS_FILE_NAME);
        if (Files.exists(delChunks)) {
            ChunkDeleter.runFromFile(delChunks, true);
        }
    }

    private void onStartServer(MinecraftServer minecraftServer) {
        setupRegistries(minecraftServer);

        config.load();
        WorldEdit.getInstance().getEventBus().post(new ConfigurationLoadEvent(config));
        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent(platform));
    }

    private void onStopServer(MinecraftServer minecraftServer) {
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        WorldEdit.getInstance().getEventBus().post(new PlatformUnreadyEvent(platform));
    }

    private boolean skipEvents() {
        return platform == null || !platform.isHookingEvents();
    }

    private boolean skipInteractionEvent(class_1657 player, class_1268 hand) {
        return skipEvents() || hand != class_1268.field_5808 || player.method_73183().method_8608() || !(player instanceof class_3222);
    }

    private class_1269 onLeftClickBlock(class_1657 playerEntity, class_1937 world, class_1268 hand, class_2338 blockPos, class_2350 direction) {
        if (skipInteractionEvent(playerEntity, hand)) {
            return class_1269.field_5811;
        }

        WorldEdit we = WorldEdit.getInstance();
        FabricPlayer player = adaptPlayer((class_3222) playerEntity);
        FabricWorld localWorld = getWorld(world);
        Location pos = new Location(localWorld,
            blockPos.method_10263(),
            blockPos.method_10264(),
            blockPos.method_10260()
        );
        com.sk89q.worldedit.util.Direction weDirection = FabricAdapter.adaptEnumFacing(direction);

        boolean result = we.handleBlockLeftClick(player, pos, weDirection) || we.handleArmSwing(player);
        debouncer.setLastInteraction(player, result);

        return result ? class_1269.field_5812 : class_1269.field_5811;
    }

    private class_1269 onRightClickBlock(class_1657 playerEntity, class_1937 world, class_1268 hand, class_3965 blockHitResult) {
        if (skipInteractionEvent(playerEntity, hand)) {
            return class_1269.field_5811;
        }

        WorldEdit we = WorldEdit.getInstance();
        FabricPlayer player = adaptPlayer((class_3222) playerEntity);
        FabricWorld localWorld = getWorld(world);
        Location pos = new Location(localWorld,
            blockHitResult.method_17777().method_10263(),
            blockHitResult.method_17777().method_10264(),
            blockHitResult.method_17777().method_10260()
        );
        com.sk89q.worldedit.util.Direction direction = FabricAdapter.adaptEnumFacing(blockHitResult.method_17780());

        boolean result = we.handleBlockRightClick(player, pos, direction) || we.handleRightClick(player);
        debouncer.setLastInteraction(player, result);

        return result ? class_1269.field_5812 : class_1269.field_5811;
    }

    public void onLeftClickAir(class_3222 playerEntity, class_1268 hand) {
        if (skipInteractionEvent(playerEntity, hand)) {
            return;
        }

        WorldEdit we = WorldEdit.getInstance();
        FabricPlayer player = adaptPlayer(playerEntity);

        Optional<Boolean> previousResult = debouncer.getDuplicateInteractionResult(player);
        if (previousResult.isPresent()) {
            return;
        }

        boolean result = we.handleArmSwing(player);
        debouncer.setLastInteraction(player, result);
    }

    private class_1269 onRightClickItem(class_1657 playerEntity, class_1937 world, class_1268 hand) {
        if (skipInteractionEvent(playerEntity, hand)) {
            return class_1269.field_5811;
        }

        WorldEdit we = WorldEdit.getInstance();
        FabricPlayer player = adaptPlayer((class_3222) playerEntity);

        Optional<Boolean> previousResult = debouncer.getDuplicateInteractionResult(player);
        if (previousResult.isPresent()) {
            return previousResult.get() ? class_1269.field_5812 : class_1269.field_5811;
        }

        boolean result = we.handleRightClick(player);
        debouncer.setLastInteraction(player, result);

        return result ? class_1269.field_5812 : class_1269.field_5811;
    }

    private void onPlayerDisconnect(class_3244 handler, MinecraftServer server) {
        debouncer.clearInteraction(adaptPlayer(handler.field_14140));

        WorldEdit.getInstance().getEventBus()
            .post(new SessionIdleEvent(new FabricPlayer.SessionKeyImpl(handler.field_14140)));
    }

    private void onCuiPacket(CUIPacket payload, CUIPacketHandler.PacketContext context) {
        if (!(context.player() instanceof class_3222 player)) {
            // Ignore - this is not a server-bound packet
            return;
        }

        FabricPlayer actor = FabricAdapter.adaptPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        session.handleCUIInitializationMessage(payload.eventType(), payload.args(), actor);
    }

    /**
     * Get the configuration.
     *
     * @return the Fabric configuration
     */
    FabricConfiguration getConfig() {
        return this.config;
    }

    /**
     * Get the session for a player.
     *
     * @param player the player
     * @return the session
     */
    public LocalSession getSession(class_3222 player) {
        checkNotNull(player);
        return WorldEdit.getInstance().getSessionManager().get(adaptPlayer(player));
    }

    /**
     * Get the WorldEdit proxy for the given world.
     *
     * @param world the world
     * @return the WorldEdit world
     */
    public FabricWorld getWorld(class_1937 world) {
        checkNotNull(world);
        return new FabricWorld(world);
    }

    /**
     * Get the WorldEdit proxy for the platform.
     *
     * @return the WorldEdit platform
     */
    public Platform getPlatform() {
        return this.platform;
    }

    /**
     * Get the working directory where WorldEdit's files are stored.
     *
     * @return the working directory
     */
    public Path getWorkingDir() {
        return this.workingDir;
    }

    /**
     * Get the version of the WorldEdit-Fabric implementation.
     *
     * @return a version string
     */
    String getInternalVersion() {
        return container.getMetadata().getVersion().getFriendlyString();
    }

    public void setPermissionsProvider(FabricPermissionsProvider provider) {
        this.provider = provider;
    }

    public FabricPermissionsProvider getPermissionsProvider() {
        return provider;
    }
}
