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

import com.mojang.serialization.Codec;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.fabric.internal.FabricTransmogrifier;
import com.sk89q.worldedit.fabric.internal.NBTConverter;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.class_11362;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1918;
import net.minecraft.class_1959;
import net.minecraft.class_2168;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2487;
import net.minecraft.class_2509;
import net.minecraft.class_2586;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3542;
import net.minecraft.class_5455;
import net.minecraft.class_7924;
import net.minecraft.class_9326;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FabricAdapter {

    private FabricAdapter() {
    }

    public static World adapt(net.minecraft.class_1937 world) {
        return new FabricWorld(world);
    }

    /**
     * Create a Fabric world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Fabric world
     */
    public static net.minecraft.class_1937 adapt(World world) {
        checkNotNull(world);
        if (world instanceof FabricWorld fabricWorld) {
            return fabricWorld.getWorld();
        } else {
            // TODO introduce a better cross-platform world API to match more easily
            throw new UnsupportedOperationException("Cannot adapt from a " + world.getClass());
        }
    }

    public static class_1959 adapt(BiomeType biomeType) {
        return FabricWorldEdit.getRegistry(class_7924.field_41236)
            .method_63535(class_2960.method_60654(biomeType.id()));
    }

    public static BiomeType adapt(class_1959 biome) {
        class_2960 id = FabricWorldEdit.getRegistry(class_7924.field_41236).method_10221(biome);
        Objects.requireNonNull(id, "biome is not registered");
        return BiomeTypes.get(id.toString());
    }

    public static Vector3 adapt(class_243 vector) {
        return Vector3.at(vector.field_1352, vector.field_1351, vector.field_1350);
    }

    public static BlockVector3 adapt(class_2338 pos) {
        return BlockVector3.at(pos.method_10263(), pos.method_10264(), pos.method_10260());
    }

    public static class_243 toVec3(BlockVector3 vector) {
        return new class_243(vector.x(), vector.y(), vector.z());
    }

    public static net.minecraft.class_2350 adapt(Direction face) {
        return switch (face) {
            case NORTH -> net.minecraft.class_2350.field_11043;
            case SOUTH -> net.minecraft.class_2350.field_11035;
            case WEST -> net.minecraft.class_2350.field_11039;
            case EAST -> net.minecraft.class_2350.field_11034;
            case DOWN -> net.minecraft.class_2350.field_11033;
            default -> net.minecraft.class_2350.field_11036;
        };
    }

    public static Direction adaptEnumFacing(@Nullable net.minecraft.class_2350 face) {
        if (face == null) {
            return null;
        }
        return switch (face) {
            case field_11043 -> Direction.NORTH;
            case field_11035 -> Direction.SOUTH;
            case field_11039 -> Direction.WEST;
            case field_11034 -> Direction.EAST;
            case field_11033 -> Direction.DOWN;
            default -> Direction.UP;
        };
    }

    public static class_2338 toBlockPos(BlockVector3 vector) {
        return new class_2338(vector.x(), vector.y(), vector.z());
    }

    /**
     * Adapts property.
     *
     * @deprecated without replacement, use the block adapter methods
     */
    // Suppress InlineMeSuggester: There is no replacement, so this shouldn't be inlined
    @SuppressWarnings("InlineMeSuggester")
    @Deprecated
    public static Property<?> adaptProperty(net.minecraft.class_2769<?> property) {
        return FabricTransmogrifier.transmogToWorldEditProperty(property);
    }

    /**
     * Adapts properties.
     *
     * @deprecated without replacement, use the block adapter methods
     */
    @Deprecated
    public static Map<Property<?>, Object> adaptProperties(BlockType block, Map<net.minecraft.class_2769<?>, Comparable<?>> mcProps) {
        Map<Property<?>, Object> props = new TreeMap<>(Comparator.comparing(Property::getName));
        for (Map.Entry<net.minecraft.class_2769<?>, Comparable<?>> prop : mcProps.entrySet()) {
            Object value = prop.getValue();
            if (prop.getKey() instanceof net.minecraft.class_2754) {
                if (prop.getKey().method_11902() == net.minecraft.class_2350.class) {
                    value = adaptEnumFacing((net.minecraft.class_2350) value);
                } else {
                    value = ((class_3542) value).method_15434();
                }
            }
            props.put(block.getProperty(prop.getKey().method_11899()), value);
        }
        return props;
    }

    public static net.minecraft.class_2680 adapt(BlockState blockState) {
        int blockStateId = BlockStateIdAccess.getBlockStateId(blockState);
        if (!BlockStateIdAccess.isValidInternalId(blockStateId)) {
            return FabricTransmogrifier.transmogToMinecraft(blockState);
        }
        return class_2248.method_9531(blockStateId);
    }

    public static BlockState adapt(net.minecraft.class_2680 blockState) {
        int blockStateId = class_2248.method_9507(blockState);
        BlockState worldEdit = BlockStateIdAccess.getBlockStateById(blockStateId);
        if (worldEdit == null) {
            return FabricTransmogrifier.transmogToWorldEdit(blockState);
        }
        return worldEdit;
    }

    public static BaseBlock adapt(class_2586 blockEntity) {
        if (!blockEntity.method_11002()) {
            throw new IllegalArgumentException("BlockEntity must have a level");
        }
        class_5455 registries = blockEntity.method_10997().method_30349();
        return adapt(blockEntity, registries);
    }

    public static BaseBlock adapt(class_2586 blockEntity, class_5455 registries) {
        int blockStateId = class_2248.method_9507(blockEntity.method_11010());
        BlockState worldEdit = BlockStateIdAccess.getBlockStateById(blockStateId);
        if (worldEdit == null) {
            worldEdit = FabricTransmogrifier.transmogToWorldEdit(blockEntity.method_11010());
        }
        // Save this outside the reference to ensure it doesn't mutate
        net.minecraft.class_2487 savedNative = com.sk89q.worldedit.fabric.internal.FabricLoggingProblemReporter.with(
            () -> "serializing block entity " + blockEntity.getClass().getSimpleName(),
            reporter -> {
                var tagValueOutput = class_11362.method_71459(reporter, registries);
                blockEntity.method_38243(tagValueOutput);
                return tagValueOutput.method_71475();
            }
        );

        return worldEdit.toBaseBlock(LazyReference.from(() -> NBTConverter.fromNative(savedNative)));
    }

    public static class_2248 adapt(BlockType blockType) {
        return FabricWorldEdit.getRegistry(class_7924.field_41254).method_63535(class_2960.method_60654(blockType.id()));
    }

    public static BlockType adapt(class_2248 block) {
        return BlockTypes.get(FabricWorldEdit.getRegistry(class_7924.field_41254).method_10221(block).toString());
    }

    public static class_1792 adapt(ItemType itemType) {
        return FabricWorldEdit.getRegistry(class_7924.field_41197).method_63535(class_2960.method_60654(itemType.id()));
    }

    public static ItemType adapt(class_1792 item) {
        return ItemTypes.get(FabricWorldEdit.getRegistry(class_7924.field_41197).method_10221(item).toString());
    }

    /**
     * For serializing and deserializing components.
     */
    private static final Codec<class_9326> COMPONENTS_CODEC = class_9326.field_49589.optionalFieldOf(
        "components", class_9326.field_49588
    ).codec();

    public static class_1799 adapt(BaseItemStack baseItemStack) {
        final class_1799 itemStack = new class_1799(adapt(baseItemStack.getType()), baseItemStack.getAmount());
        LinCompoundTag nbt = baseItemStack.getNbt();
        if (nbt != null) {
            class_9326 componentPatch = COMPONENTS_CODEC.parse(
                FabricWorldEdit.registryAccess().method_57093(class_2509.field_11560),
                NBTConverter.toNative(nbt)
            ).getOrThrow();
            itemStack.method_57366(componentPatch);
        }
        return itemStack;
    }

    public static BaseItemStack adapt(class_1799 itemStack) {
        class_2487 tag = (class_2487) COMPONENTS_CODEC.encodeStart(
            FabricWorldEdit.registryAccess().method_57093(class_2509.field_11560),
            itemStack.method_57380()
        ).getOrThrow();
        return new BaseItemStack(
            adapt(itemStack.method_7909()), LazyReference.from(() -> NBTConverter.fromNative(tag)), itemStack.method_7947()
        );
    }

    /**
     * Get the WorldEdit proxy for the given player.
     *
     * @param player the player
     * @return the WorldEdit player
     */
    public static FabricPlayer adaptPlayer(class_3222 player) {
        checkNotNull(player);
        return new FabricPlayer(player);
    }

    /**
     * Get the WorldEdit proxy for the given command source.
     *
     * @param commandSourceStack the command source
     * @return the WorldEdit actor
     */
    public static Actor adaptCommandSource(class_2168 commandSourceStack) {
        checkNotNull(commandSourceStack);
        if (commandSourceStack.method_43737()) {
            return adaptPlayer(commandSourceStack.method_44023());
        }
        if (FabricWorldEdit.inst.getConfig().commandBlockSupport && commandSourceStack.field_9819 instanceof class_1918 commandBlock) {
            return new FabricBlockCommandSender(commandBlock, commandSourceStack.method_9225(), commandSourceStack.method_9222());
        }

        return new FabricCommandSender(commandSourceStack);
    }
}
