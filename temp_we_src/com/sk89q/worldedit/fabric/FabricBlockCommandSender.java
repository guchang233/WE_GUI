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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.AbstractCommandBlockActor;
import com.sk89q.worldedit.fabric.internal.ComponentConverter;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.class_124;
import net.minecraft.class_1918;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_4076;

import static com.google.common.base.Preconditions.checkNotNull;

public class FabricBlockCommandSender extends AbstractCommandBlockActor {
    private final class_1918 sender;
    private final UUID uuid;
    private final class_3218 level;
    private final class_243 pos;

    public FabricBlockCommandSender(class_1918 sender, class_3218 level, class_243 pos) {
        super(new Location(FabricAdapter.adapt(checkNotNull(level)), FabricAdapter.adapt(checkNotNull(pos))));

        this.sender = sender;
        this.level = level;
        this.pos = pos;
        this.uuid = UUID.nameUUIDFromBytes((UUID_PREFIX + sender.method_8299()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getName() {
        return sender.method_8299().getString();
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        for (String part : msg.split("\n", 0)) {
            sendMessage(net.minecraft.class_2561.method_43470(part));
        }
    }

    @Override
    @Deprecated
    public void printDebug(String msg) {
        sendColorized(msg, class_124.field_1080);
    }

    @Override
    @Deprecated
    public void print(String msg) {
        sendColorized(msg, class_124.field_1076);
    }

    @Override
    @Deprecated
    public void printError(String msg) {
        sendColorized(msg, class_124.field_1061);
    }

    @Override
    public void print(Component component) {
        sendMessage(ComponentConverter.Serializer.fromJson(
            GsonComponentSerializer.INSTANCE.serialize(WorldEditText.format(component, getLocale())),
            this.level.method_30349()
        ));
    }

    private void sendColorized(String msg, class_124 formatting) {
        for (String part : msg.split("\n", 0)) {
            var component = net.minecraft.class_2561.method_43470(part);
            component.method_27692(formatting);
            sendMessage(component);
        }
    }

    private void sendMessage(net.minecraft.class_2561 textComponent) {
        this.sender.method_8291(textComponent);
    }

    @Override
    public Locale getLocale() {
        return WorldEdit.getInstance().getConfiguration().defaultLocale;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {
        if (!hasPermission(permission)) {
            throw new AuthorizationException();
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    public class_1918 getSender() {
        return this.sender;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {

            private volatile boolean active = true;

            private void updateActive() {
                class_2338 blockPos = new class_2338((int) pos.field_1352, (int) pos.field_1351, (int) pos.field_1350);
                int chunkX = class_4076.method_18675(blockPos.method_10263());
                int chunkZ = class_4076.method_18675(blockPos.method_10260());
                if (!level.method_14178().method_12123(chunkX, chunkZ)) {
                    active = false;
                    return;
                }
                class_2248 type = level.method_8320(blockPos).method_26204();
                active = type == class_2246.field_10525
                    || type == class_2246.field_10395
                    || type == class_2246.field_10263;
            }

            @Override
            public String getName() {
                return sender.method_8299().getString();
            }

            @Override
            public boolean isActive() {
                level.method_8503().execute(this::updateActive);
                return active;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }

            @Override
            public UUID getUniqueId() {
                return uuid;
            }
        };
    }
}
