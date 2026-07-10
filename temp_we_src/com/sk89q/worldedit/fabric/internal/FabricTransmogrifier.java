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

package com.sk89q.worldedit.fabric.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.class_2248;
import net.minecraft.class_2689;
import net.minecraft.class_3542;

/**
 * Raw, un-cached transformations.
 */
public class FabricTransmogrifier {

    private static final LoadingCache<net.minecraft.class_2769<?>, Property<?>> PROPERTY_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Property<?> load(net.minecraft.class_2769<?> property) throws Exception {
            return switch (property) {
                case net.minecraft.class_2746 booleanProperty ->
                    new BooleanProperty(property.method_11899(), ImmutableList.copyOf(booleanProperty.method_11898()));
                case net.minecraft.class_2758 integerProperty ->
                    new IntegerProperty(property.method_11899(), ImmutableList.copyOf(integerProperty.method_11898()));
                case net.minecraft.class_2754<?> enumProperty -> {
                    if (property.method_11902() == net.minecraft.class_2350.class) {
                        yield new DirectionalProperty(property.method_11899(), property.method_11898().stream()
                            .map(v -> FabricAdapter.adaptEnumFacing((net.minecraft.class_2350) v))
                            .collect(ImmutableList.toImmutableList()));
                    }
                    // Note: do not make x.asString a method reference.
                    // It will cause runtime bootstrap exceptions.
                    //noinspection Convert2MethodRef
                    yield new EnumProperty(property.method_11899(), enumProperty.method_11898().stream()
                        .map(x -> x.method_15434())
                        .collect(ImmutableList.toImmutableList()));
                }
                default -> new FabricPropertyAdapter<>(property);
            };
        }
    });

    public static Property<?> transmogToWorldEditProperty(net.minecraft.class_2769<?> property) {
        return PROPERTY_CACHE.getUnchecked(property);
    }

    private static Map<Property<?>, Object> transmogToWorldEditProperties(BlockType block, Map<net.minecraft.class_2769<?>, Comparable<?>> mcProps) {
        Map<Property<?>, Object> props = new TreeMap<>(Comparator.comparing(Property::name));
        for (Map.Entry<net.minecraft.class_2769<?>, Comparable<?>> prop : mcProps.entrySet()) {
            Object value = prop.getValue();
            if (prop.getKey() instanceof net.minecraft.class_2754) {
                if (prop.getKey().method_11902() == net.minecraft.class_2350.class) {
                    value = FabricAdapter.adaptEnumFacing((net.minecraft.class_2350) value);
                } else {
                    value = ((class_3542) value).method_15434();
                }
            }
            props.put(block.getProperty(prop.getKey().method_11899()), value);
        }
        return props;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static net.minecraft.class_2680 transmogToMinecraftProperties(
        class_2689<class_2248, net.minecraft.class_2680> stateContainer,
        net.minecraft.class_2680 newState,
        Map<Property<?>, Object> states
    ) {
        for (Map.Entry<Property<?>, Object> state : states.entrySet()) {
            net.minecraft.class_2769 property = stateContainer.method_11663(state.getKey().name());
            Comparable value = (Comparable) state.getValue();
            // we may need to adapt this value, depending on the source prop
            if (property instanceof net.minecraft.class_2754) {
                if (property.method_11902() == net.minecraft.class_2350.class) {
                    Direction dir = (Direction) value;
                    value = FabricAdapter.adapt(dir);
                } else {
                    String enumName = (String) value;
                    value = ((net.minecraft.class_2754<?>) property).method_11900((String) value).orElseThrow(() ->
                        new IllegalStateException("Enum property " + property.method_11899() + " does not contain " + enumName)
                    );
                }
            }

            newState = newState.method_11657(property, value);
        }
        return newState;
    }

    public static net.minecraft.class_2680 transmogToMinecraft(BlockState blockState) {
        class_2248 mcBlock = FabricAdapter.adapt(blockState.getBlockType());
        net.minecraft.class_2680 newState = mcBlock.method_9564();
        Map<Property<?>, Object> states = blockState.getStates();
        return transmogToMinecraftProperties(mcBlock.method_9595(), newState, states);
    }

    public static com.sk89q.worldedit.world.block.BlockState transmogToWorldEdit(net.minecraft.class_2680 blockState) {
        BlockType blockType = FabricAdapter.adapt(blockState.method_26204());
        return blockType.getState(transmogToWorldEditProperties(blockType, blockState.method_11656()));
    }

    private FabricTransmogrifier() {
    }
}
