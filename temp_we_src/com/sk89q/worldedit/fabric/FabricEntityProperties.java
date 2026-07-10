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

import com.sk89q.worldedit.entity.metadata.EntityProperties;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1308;
import net.minecraft.class_1321;
import net.minecraft.class_1421;
import net.minecraft.class_1427;
import net.minecraft.class_1429;
import net.minecraft.class_1480;
import net.minecraft.class_1510;
import net.minecraft.class_1531;
import net.minecraft.class_1533;
import net.minecraft.class_1534;
import net.minecraft.class_1540;
import net.minecraft.class_1541;
import net.minecraft.class_1542;
import net.minecraft.class_1655;
import net.minecraft.class_1657;
import net.minecraft.class_1676;
import net.minecraft.class_1688;
import net.minecraft.class_1690;
import net.minecraft.class_1915;
import net.minecraft.class_3222;

import static com.google.common.base.Preconditions.checkNotNull;

public class FabricEntityProperties implements EntityProperties {

    private final class_1297 entity;

    public FabricEntityProperties(class_1297 entity) {
        checkNotNull(entity);
        this.entity = entity;
    }

    @Override
    public boolean isPlayerDerived() {
        return entity instanceof class_1657;
    }

    @Override
    public boolean isProjectile() {
        return entity instanceof class_1676;
    }

    @Override
    public boolean isItem() {
        return entity instanceof class_1542;
    }

    @Override
    public boolean isFallingBlock() {
        return entity instanceof class_1540;
    }

    @Override
    public boolean isPainting() {
        return entity instanceof class_1534;
    }

    @Override
    public boolean isItemFrame() {
        return entity instanceof class_1533;
    }

    @Override
    public boolean isBoat() {
        return entity instanceof class_1690;
    }

    @Override
    public boolean isMinecart() {
        return entity instanceof class_1688;
    }

    @Override
    public boolean isTNT() {
        return entity instanceof class_1541;
    }

    @Override
    public boolean isExperienceOrb() {
        return entity instanceof class_1303;
    }

    @Override
    public boolean isLiving() {
        return entity instanceof class_1308;
    }

    @Override
    public boolean isAnimal() {
        return entity instanceof class_1429;
    }

    @Override
    public boolean isAmbient() {
        return entity instanceof class_1421;
    }

    @Override
    public boolean isNPC() {
        return entity instanceof class_1655 || entity instanceof class_1915;
    }

    @Override
    public boolean isGolem() {
        return entity instanceof class_1427;
    }

    @Override
    public boolean isTamed() {
        return entity instanceof class_1321 tamableAnimal && tamableAnimal.method_6181();
    }

    @Override
    public boolean isTagged() {
        return entity.method_16914();
    }

    @Override
    public boolean isArmorStand() {
        return entity instanceof class_1531;
    }

    @Override
    public boolean isPasteable() {
        return !(entity instanceof class_3222 || entity instanceof class_1510);
    }

    @Override
    public boolean isWaterCreature() {
        return entity instanceof class_1480;
    }
}
