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

import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.lang.ref.WeakReference;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.class_11352;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_3194;
import net.minecraft.class_3215;

public class FabricWorldNativeAccess implements WorldNativeAccess<class_2818, class_2680, class_2338> {
    private static final int UPDATE = 1;
    private static final int NOTIFY = 2;

    private final WeakReference<class_1937> world;
    private SideEffectSet sideEffectSet;

    public FabricWorldNativeAccess(WeakReference<class_1937> world) {
        this.world = world;
    }

    private class_1937 getWorld() {
        return Objects.requireNonNull(world.get(), "The reference to the world was lost");
    }

    @Override
    public void setCurrentSideEffectSet(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    @Override
    public class_2818 getChunk(int x, int z) {
        return getWorld().method_8497(x, z);
    }

    @Override
    public class_2680 toNative(com.sk89q.worldedit.world.block.BlockState state) {
        int stateId = BlockStateIdAccess.getBlockStateId(state);
        return BlockStateIdAccess.isValidInternalId(stateId)
            ? class_2248.method_9531(stateId)
            : FabricAdapter.adapt(state);
    }

    @Override
    public class_2680 getBlockState(class_2818 chunk, class_2338 position) {
        return chunk.method_8320(position);
    }

    @Nullable
    @Override
    public class_2680 setBlockState(class_2818 chunk, class_2338 position, class_2680 state) {
        if (chunk instanceof ExtendedChunk extendedChunk) {
            return extendedChunk.setBlockState(
                position, state, 0, sideEffectSet.shouldApply(SideEffect.UPDATE)
            );
        }
        return chunk.method_12010(position, state, 0);
    }

    @Override
    public class_2680 getValidBlockForPosition(class_2680 block, class_2338 position) {
        return class_2248.method_9510(block, getWorld(), position);
    }

    @Override
    public class_2338 getPosition(int x, int y, int z) {
        return new class_2338(x, y, z);
    }

    @Override
    public void updateLightingForBlock(class_2338 position) {
        getWorld().method_8398().method_12130().method_15513(position);
    }

    @Override
    public boolean updateTileEntity(class_2338 position, LinCompoundTag tag) {
        class_2487 nativeTag = NBTConverter.toNative(tag);
        class_1937 level = getWorld();
        class_2586 tileEntity = level.method_8500(position).method_8321(position);
        if (tileEntity == null) {
            return false;
        }
        return FabricLoggingProblemReporter.with(
            () -> "loading tile entity at " + position,
            reporter -> {
                var tagValueInput = class_11352.method_71417(reporter, level.method_30349(), nativeTag);
                tileEntity.method_58690(tagValueInput);
                tileEntity.method_5431();
                return true;
            }
        );
    }

    @Override
    public void notifyBlockUpdate(class_2818 chunk, class_2338 position, class_2680 oldState, class_2680 newState) {
        if (chunk.method_12006()[getWorld().method_31602(position.method_10264())] != null) {
            getWorld().method_8413(position, oldState, newState, UPDATE | NOTIFY);
        }
    }

    @Override
    public boolean isChunkTicking(class_2818 chunk) {
        return chunk.method_12225().method_14014(class_3194.field_44856);
    }

    @Override
    public void markBlockChanged(class_2818 chunk, class_2338 position) {
        if (chunk.method_12006()[getWorld().method_31602(position.method_10264())] != null) {
            ((class_3215) getWorld().method_8398()).method_14128(position);
        }
    }

    @Override
    public void notifyNeighbors(class_2338 pos, class_2680 oldState, class_2680 newState) {
        getWorld().method_8408(pos, oldState.method_26204());
        if (newState.method_26221()) {
            getWorld().method_8455(pos, newState.method_26204());
        }
    }

    @Override
    public void updateBlock(class_2338 pos, class_2680 oldState, class_2680 newState) {
        class_1937 world = getWorld();
        newState.method_26182(world, pos, oldState, false);
    }

    @Override
    public void updateNeighbors(class_2338 pos, class_2680 oldState, class_2680 newState, int recursionLimit) {
        class_1937 world = getWorld();
        oldState.method_26198(world, pos, NOTIFY, recursionLimit);
        newState.method_26183(world, pos, NOTIFY, recursionLimit);
        newState.method_26198(world, pos, NOTIFY, recursionLimit);
    }

    @Override
    public void onBlockStateChange(class_2338 pos, class_2680 oldState, class_2680 newState) {
        getWorld().method_66016(pos, oldState, newState);
    }
}
