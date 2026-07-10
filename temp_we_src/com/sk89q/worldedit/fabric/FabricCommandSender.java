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
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.fabric.internal.ComponentConverter;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.class_124;
import net.minecraft.class_2168;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FabricCommandSender extends AbstractNonPlayerActor {

    /**
     * One time generated ID.
     */
    private static final UUID DEFAULT_ID = UUID.fromString("a233eb4b-4cab-42cd-9fd9-7e7b9a3f74be");

    private final class_2168 sender;

    public FabricCommandSender(class_2168 sender) {
        checkNotNull(sender);
        checkArgument(!sender.method_43737(), "Cannot wrap a player");

        this.sender = sender;
    }

    @Override
    public UUID getUniqueId() {
        return DEFAULT_ID;
    }

    @Override
    public String getName() {
        return sender.method_9214();
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
            this.sender.method_30497()
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
        this.sender.method_45068(textComponent);
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public boolean hasPermission(String perm) {
        return true;
    }

    @Override
    public void checkPermission(String permission) {
    }

    @Override
    public Locale getLocale() {
        return WorldEdit.getInstance().getConfiguration().defaultLocale;
    }

    public class_2168 getSender() {
        return this.sender;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Nullable
            @Override
            public String getName() {
                return sender.method_9214();
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }

            @Override
            public UUID getUniqueId() {
                return DEFAULT_ID;
            }
        };
    }
}
