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

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.class_1306;
import net.minecraft.class_1659;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3445;
import net.minecraft.class_4066;
import net.minecraft.class_8791;

public class FabricFakePlayer extends class_3222 {
    private static final GameProfile FAKE_WORLDEDIT_PROFILE = new GameProfile(
        UUID.nameUUIDFromBytes("worldedit".getBytes(StandardCharsets.UTF_8)),
        "[WorldEdit]"
    );
    private static final class_8791 FAKE_CLIENT_INFO = new class_8791(
        "en_US", 16, class_1659.field_7538, true, 0, class_1306.field_6182, false, false, class_4066.field_18199
    );

    public FabricFakePlayer(class_3218 world) {
        super(world.method_8503(), world, FAKE_WORLDEDIT_PROFILE, FAKE_CLIENT_INFO);
    }

    @Override
    public void method_5773() {
    }

    @Override
    public void method_7342(class_3445<?> stat, int incrementer) {
    }

    @Override
    public void method_7259(class_3445<?> stat) {
    }

    @Override
    public void method_7353(class_2561 message, boolean actionBar) {
        super.method_7353(message, actionBar);
    }
}
