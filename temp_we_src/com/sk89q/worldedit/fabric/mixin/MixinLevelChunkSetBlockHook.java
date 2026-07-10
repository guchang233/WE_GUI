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

package com.sk89q.worldedit.fabric.mixin;

import com.sk89q.worldedit.fabric.internal.ExtendedChunk;
import net.minecraft.class_11897;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_2791;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_2843;
import net.minecraft.class_5539;
import net.minecraft.class_6749;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.Nullable;

@Mixin(class_2818.class)
public abstract class MixinLevelChunkSetBlockHook extends class_2791 implements ExtendedChunk {
    @Unique
    private boolean shouldUpdate = true;

    public MixinLevelChunkSetBlockHook(class_1923 chunkPos, class_2843 upgradeData, class_5539 levelHeightAccessor, class_11897 palettedContainerFactory, long l, @org.jetbrains.annotations.Nullable class_2826[] levelChunkSections, @org.jetbrains.annotations.Nullable class_6749 blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, palettedContainerFactory, l, levelChunkSections, blendingData);
    }

    @Unique
    @Nullable
    @Override
    public class_2680 setBlockState(class_2338 pos, class_2680 state, int flag, boolean update) {
        // save the state for the hook
        shouldUpdate = update;
        try {
            return method_12010(pos, state, flag);
        } finally {
            // restore natural mode
            shouldUpdate = true;
        }
    }

    @Redirect(
        method = "setBlockState",
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
        ),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V")
    )
    public void setBlockStateHook(class_2680 target, class_1937 world, class_2338 pos, class_2680 old, boolean move) {
        boolean localShouldUpdate;
        MinecraftServer server = world.method_8503();
        if (server == null || Thread.currentThread() != server.method_3777()) {
            // We're not on the server thread for some reason, WorldEdit will never be here
            // so we'll just ignore our flag
            localShouldUpdate = true;
        } else {
            localShouldUpdate = shouldUpdate;
        }
        if (localShouldUpdate) {
            target.method_26182(world, pos, old, move);
        }
    }
}
