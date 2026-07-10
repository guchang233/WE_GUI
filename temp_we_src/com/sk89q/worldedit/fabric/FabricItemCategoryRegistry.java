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

import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.ItemCategoryRegistry;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.class_2960;
import net.minecraft.class_6862;
import net.minecraft.class_6880;
import net.minecraft.class_6885;
import net.minecraft.class_7924;

public class FabricItemCategoryRegistry implements ItemCategoryRegistry {
    @Override
    public Set<ItemType> getCategorisedByName(String category) {
        return FabricWorldEdit.getRegistry(class_7924.field_41197)
            .method_46733(class_6862.method_40092(class_7924.field_41197, class_2960.method_60654(category)))
            .stream()
            .flatMap(class_6885.class_6888::method_40239)
            .map(class_6880::comp_349)
            .map(FabricAdapter::adapt)
            .collect(Collectors.toSet());
    }
}
