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

import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2680;
import net.minecraft.class_2682;
import net.minecraft.class_3619;
import net.minecraft.class_3829;

/**
 * Fabric block material that pulls as much info as possible from the Minecraft
 * Material, and passes the rest to another implementation, typically the
 * bundled block info.
 */
public class FabricBlockMaterial implements BlockMaterial {

    private final class_2680 block;

    public FabricBlockMaterial(class_2680 block) {
        this.block = block;
    }

    @Override
    public boolean isAir() {
        return block.method_26215();
    }

    @Override
    public boolean isFullCube() {
        return class_2248.method_9614(block.method_26218(class_2682.field_12294, class_2338.field_10980));
    }

    @Override
    public boolean isOpaque() {
        return block.method_26225();
    }

    @Override
    public boolean isPowerSource() {
        return block.method_26219();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isLiquid() {
        return block.method_51176();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSolid() {
        return block.method_51367();
    }

    @Override
    public float getHardness() {
        return block.method_26214(class_2682.field_12294, class_2338.field_10980);
    }

    @Override
    public float getResistance() {
        return block.method_26204().method_9520();
    }

    @Override
    public float getSlipperiness() {
        return block.method_26204().method_9499();
    }

    @Override
    public int getLightValue() {
        return block.method_26213();
    }

    @Override
    public boolean isFragileWhenPushed() {
        return block.method_26223() == class_3619.field_15971;
    }

    @Override
    public boolean isUnpushable() {
        return block.method_26223() == class_3619.field_15972;
    }

    @Override
    public boolean isTicksRandomly() {
        return block.method_26229();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isMovementBlocker() {
        return block.method_51366();
    }

    @Override
    public boolean isBurnable() {
        return block.method_50011();
    }

    @Override
    public boolean isToolRequired() {
        return block.method_29291();
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return block.method_45474();
    }

    @Override
    public boolean isTranslucent() {
        return !block.method_26225();
    }

    @Override
    public boolean hasContainer() {
        return block.method_26204() instanceof class_2343 entityBlock
                && entityBlock.method_10123(class_2338.field_10980, block) instanceof class_3829;
    }

}
