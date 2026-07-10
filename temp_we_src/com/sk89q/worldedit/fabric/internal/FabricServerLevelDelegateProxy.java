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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.class_1297;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import net.minecraft.class_5281;

public class FabricServerLevelDelegateProxy implements InvocationHandler, AutoCloseable {

    private final EditSession editSession;
    private final class_3218 serverLevel;
    private final Map<BlockVector3, class_2586> createdBlockEntities = new HashMap<>();

    private FabricServerLevelDelegateProxy(EditSession editSession, class_3218 serverLevel) {
        this.editSession = editSession;
        this.serverLevel = serverLevel;
    }

    public record LevelAndProxy(class_5281 level, FabricServerLevelDelegateProxy proxy) implements AutoCloseable {
        @Override
        public void close() throws MaxChangedBlocksException {
            proxy.close();
        }
    }

    public static LevelAndProxy newInstance(EditSession editSession, class_3218 serverLevel) {
        FabricServerLevelDelegateProxy proxy = new FabricServerLevelDelegateProxy(editSession, serverLevel);
        return new LevelAndProxy(
            (class_5281) Proxy.newProxyInstance(
                serverLevel.getClass().getClassLoader(),
                serverLevel.getClass().getInterfaces(),
                proxy
            ),
            proxy
        );
    }

    @Nullable
    private class_2586 getBlockEntity(class_2338 blockPos) {
        // This doesn't synthesize or load from world. I think editing existing block entities without setting the block
        // (in the context of features) should not be supported in the first place.
        BlockVector3 pos = FabricAdapter.adapt(blockPos);
        return createdBlockEntities.get(pos);
    }

    private class_2680 getBlockState(class_2338 blockPos) {
        return FabricAdapter.adapt(this.editSession.getBlockWithBuffer(FabricAdapter.adapt(blockPos)));
    }

    private boolean setBlock(class_2338 blockPos, class_2680 blockState) {
        try {
            handleBlockEntity(blockPos, blockState);
            return editSession.setBlock(FabricAdapter.adapt(blockPos), FabricAdapter.adapt(blockState));
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    // For BlockEntity#setBlockState, not sure why it's deprecated
    @SuppressWarnings("deprecation")
    private void handleBlockEntity(class_2338 blockPos, class_2680 blockState) {
        BlockVector3 pos = FabricAdapter.adapt(blockPos);
        if (blockState.method_31709()) {
            if (!(blockState.method_26204() instanceof class_2343 entityBlock)) {
                // This will probably never happen, as Mojang's own code assumes that
                // hasBlockEntity implies instanceof EntityBlock, but just to be safe...
                throw new AssertionError("BlockState has block entity but block is not an EntityBlock: " + blockState);
            }
            class_2586 newEntity = entityBlock.method_10123(blockPos, blockState);
            if (newEntity != null) {
                newEntity.method_31664(blockState);
                createdBlockEntities.put(pos, newEntity);
                // Should we load existing NBT here? This is for feature / structure gen so it seems unnecessary.
                // But it would align with the behavior of the real setBlock method.
                return;
            }
        }
        // Discard any block entity that was previously created if new block is set without block entity
        createdBlockEntities.remove(pos);
    }

    private boolean removeBlock(class_2338 blockPos) {
        return setBlock(blockPos, class_2246.field_10124.method_9564());
    }

    private boolean addEntity(class_1297 entity) {
        Vector3 pos = FabricAdapter.adapt(entity.method_30950(0.0f));
        Location location = new Location(FabricAdapter.adapt(serverLevel), pos.x(), pos.y(), pos.z());
        BaseEntity baseEntity = new FabricEntity(entity).getState();
        return editSession.createEntity(location, baseEntity) != null;
    }

    @Override
    public void close() throws MaxChangedBlocksException {
        for (Map.Entry<BlockVector3, class_2586> entry : createdBlockEntities.entrySet()) {
            BlockVector3 blockPos = entry.getKey();
            class_2586 blockEntity = entry.getValue();
            editSession.setBlock(
                blockPos,
                FabricAdapter.adapt(blockEntity, serverLevel.method_30349())
            );
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "getBlockState", "method_8320" -> {
                if (args.length == 1 && args[0] instanceof class_2338 blockPos) {
                    return getBlockState(blockPos);
                }
            }
            case "isStateAtPosition", "method_16358" -> {
                if (args.length == 2 && args[0] instanceof class_2338 blockPos && args[1] instanceof Predicate) {
                    @SuppressWarnings("unchecked")
                    Predicate<class_2680> predicate = (Predicate<class_2680>) args[1];
                    return predicate.test(getBlockState(blockPos));
                }
            }
            case "getBlockEntity", "method_8321" -> {
                if (args.length == 1 && args[0] instanceof class_2338 blockPos) {
                    return getBlockEntity(blockPos);
                }
            }
            case "setBlock", "method_8652" -> {
                if (args.length >= 2 && args[0] instanceof class_2338 blockPos && args[1] instanceof class_2680 blockState) {
                    return setBlock(blockPos, blockState);
                }
            }
            case "removeBlock", "destroyBlock", "method_8650", "method_8651" -> {
                if (args.length >= 2 && args[0] instanceof class_2338 blockPos && args[1] instanceof Boolean) {
                    return removeBlock(blockPos);
                }
            }
            case "addEntity", "method_14175", "addFreshEntityWithPassengers", "method_30771" -> {
                if (args.length >= 1 && args[0] instanceof class_1297 entity) {
                    return addEntity(entity);
                }
            }
            default -> {
            }
        }

        return method.invoke(this.serverLevel, args);
    }

}
