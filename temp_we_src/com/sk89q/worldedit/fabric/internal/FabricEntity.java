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

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.fabric.FabricEntityProperties;
import com.sk89q.worldedit.fabric.FabricWorldEdit;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.entity.EntityTypes;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import net.minecraft.class_11362;
import net.minecraft.class_2960;
import net.minecraft.class_7924;

import static com.google.common.base.Preconditions.checkNotNull;

public class FabricEntity implements Entity {

    private final WeakReference<net.minecraft.class_1297> entityRef;

    public FabricEntity(net.minecraft.class_1297 entity) {
        checkNotNull(entity);
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public BaseEntity getState() {
        net.minecraft.class_1297 entity = entityRef.get();
        if (entity == null) {
            return null;
        }

        net.minecraft.class_2487 tag = FabricLoggingProblemReporter.with(
            () -> "serializing entity " + entity.method_5845(),
            reporter -> {
                class_11362 tagValueOutput = class_11362.method_71459(reporter, entity.method_56673());
                if (!entity.method_5662(tagValueOutput)) {
                    return null;
                }
                return tagValueOutput.method_71475();
            }
        );

        if (tag == null) {
            return null;
        }

        class_2960 id = FabricWorldEdit.getRegistry(class_7924.field_41266).method_10221(entity.method_5864());
        return new BaseEntity(
            EntityTypes.get(id.toString()),
            LazyReference.from(() -> NBTConverter.fromNative(tag))
        );
    }

    @Override
    public Location getLocation() {
        net.minecraft.class_1297 entity = entityRef.get();
        if (entity != null) {
            Vector3 position = Vector3.at(entity.method_23317(), entity.method_23318(), entity.method_23321());
            float yaw = entity.method_36454();
            float pitch = entity.method_36455();

            return new Location(FabricAdapter.adapt(entity.method_73183()), position, yaw, pitch);
        } else {
            return new Location(NullWorld.getInstance());
        }
    }

    @Override
    public boolean setLocation(Location location) {
        // TODO unused atm
        return false;
    }

    @Override
    public Extent getExtent() {
        net.minecraft.class_1297 entity = entityRef.get();
        if (entity != null) {
            return FabricAdapter.adapt(entity.method_73183());
        } else {
            return NullWorld.getInstance();
        }
    }

    @Override
    public boolean remove() {
        net.minecraft.class_1297 entity = entityRef.get();
        if (entity != null) {
            entity.method_5650(net.minecraft.class_1297.class_5529.field_26998);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        net.minecraft.class_1297 entity = entityRef.get();
        if (entity != null) {
            if (EntityProperties.class.isAssignableFrom(cls)) {
                return (T) new FabricEntityProperties(entity);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
