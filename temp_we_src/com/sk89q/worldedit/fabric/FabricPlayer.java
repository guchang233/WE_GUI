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

import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.fabric.internal.ComponentConverter;
import com.sk89q.worldedit.fabric.internal.NBTConverter;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.component.TextUtils;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_124;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2591;
import net.minecraft.class_2622;
import net.minecraft.class_2626;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_5250;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.worldeditcui.protocol.CUIPacket;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class FabricPlayer extends AbstractPlayerActor {

    private final class_3222 player;

    protected FabricPlayer(class_3222 player) {
        this.player = player;
        ThreadSafeCache.getInstance().getOnlineIds().add(getUniqueId());
    }

    @Override
    public UUID getUniqueId() {
        return player.method_5667();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        class_1799 is = this.player.method_5998(handSide == HandSide.MAIN_HAND ? class_1268.field_5808 : class_1268.field_5810);
        return FabricAdapter.adapt(is);
    }

    @Override
    public String getName() {
        return this.player.method_5477().getString();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public Location getLocation() {
        Vector3 position = Vector3.at(this.player.method_23317(), this.player.method_23318(), this.player.method_23321());
        return new Location(
            FabricWorldEdit.inst.getWorld(this.player.method_51469()),
            position,
            this.player.method_36454(),
            this.player.method_36455());
    }

    @Override
    public boolean setLocation(Location location) {
        class_3218 level = (class_3218) FabricAdapter.adapt((World) location.getExtent());
        this.player.method_48105(
            level,
            location.getX(), location.getY(), location.getZ(),
            Set.of(),
            location.getYaw(), location.getPitch(),
            true
        );
        // This check doesn't really ever get to be false in Fabric
        // Since Fabric API doesn't allow cancelling the teleport.
        // However, other mods could theoretically mix this in, so allow the detection.
        return this.player.method_51469() == level;
    }

    @Override
    public World getWorld() {
        return FabricWorldEdit.inst.getWorld(this.player.method_51469());
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        this.player.method_31548().method_7394(FabricAdapter.adapt(itemStack));
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        ServerPlayNetworking.send(
            this.player,
            new CUIPacket(event.getTypeId(), event.getParameters())
        );
    }

    @Override
    public Locale getLocale() {
        return TextUtils.getLocaleByMinecraftTag(this.player.method_53823().comp_1951());
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        for (String part : msg.split("\n", 0)) {
            this.player.method_64398(
                net.minecraft.class_2561.method_43470(part)
            );
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
        this.player.method_64398(ComponentConverter.Serializer.fromJson(
            GsonComponentSerializer.INSTANCE.serialize(WorldEditText.format(component, getLocale())),
            player.method_51469().method_30349()
        ));
    }

    private void sendColorized(String msg, class_124 formatting) {
        for (String part : msg.split("\n", 0)) {
            class_5250 component = net.minecraft.class_2561.method_43470(part)
                .method_27694(style -> style.method_10977(formatting));
            this.player.method_64398(component);
        }
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        this.player.field_13987.method_14363(pos.x(), pos.y(), pos.z(), yaw, pitch);
        return true;
    }

    @Override
    public String[] getGroups() {
        return new String[]{}; // WorldEditMod.inst.getPermissionsResolver().getGroups(this.player.username);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return null;
    }

    @Override
    public boolean hasPermission(String perm) {
        return FabricWorldEdit.inst.getPermissionsProvider().hasPermission(player, perm);
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public boolean isAllowedToFly() {
        return player.method_31549().field_7478;
    }

    @Override
    public void setFlying(boolean flying) {
        if (player.method_31549().field_7479 != flying) {
            player.method_31549().field_7479 = flying;
            player.method_7355();
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        World world = getWorld();
        if (!(world instanceof FabricWorld fabricWorld)) {
            return;
        }
        class_2338 loc = FabricAdapter.toBlockPos(pos);
        if (block == null) {
            final class_2626 packetOut = new class_2626(fabricWorld.getWorld(), loc);
            player.field_13987.method_14364(packetOut);
        } else {
            final class_2626 packetOut = new class_2626(
                loc,
                FabricAdapter.adapt(block.toImmutableState())
            );
            player.field_13987.method_14364(packetOut);
            if (block instanceof BaseBlock baseBlock && block.getBlockType().equals(BlockTypes.STRUCTURE_BLOCK)) {
                final LinCompoundTag nbtData = baseBlock.getNbt();
                if (nbtData != null) {
                    player.field_13987.method_14364(new class_2622(
                        new class_2338(pos.x(), pos.y(), pos.z()),
                        class_2591.field_11895,
                        NBTConverter.toNative(nbtData)
                    ));
                }
            }
        }
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(player);
    }

    static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        SessionKeyImpl(class_3222 player) {
            this.uuid = player.method_5667();
            this.name = player.method_5477().getString();
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            // We can't directly check if the player is online because
            // the list of players is not thread safe
            return ThreadSafeCache.getInstance().getOnlineIds().contains(uuid);
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

}
