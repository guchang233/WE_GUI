package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //paste 的直接 API 实现。
 */
public final class PasteOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        World world = player.getWorld();
        var session = WeOperationHelper.requireSession(player);
        ClipboardHolder holder;
        Clipboard clipboard;
        try {
            holder = session.getClipboard();
            clipboard = holder.getClipboard();
        } catch (Exception e) {
            error(mc, "剪贴板为空");
            return false;
        }

        boolean ignoreAir = values.stream().anyMatch(v -> "-a".equalsIgnoreCase(v != null ? v.trim() : ""));
        boolean atOrigin = values.stream().anyMatch(v -> "-o".equalsIgnoreCase(v != null ? v.trim() : ""));
        boolean pasteEntities = values.stream().anyMatch(v -> "-e".equalsIgnoreCase(v != null ? v.trim() : ""));
        boolean pasteBiomes = values.stream().anyMatch(v -> "-b".equalsIgnoreCase(v != null ? v.trim() : ""));

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .actor(player)
                .build()) {
            BlockVector3 to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(player);
            Operation operation = holder
                    .createPaste(editSession)
                    .to(to)
                    .ignoreAirBlocks(ignoreAir)
                    .ignoreStructureVoidBlocks(true)
                    .copyBiomes(pasteBiomes)
                    .copyEntities(pasteEntities)
                    .build();
            Operations.completeLegacy(operation);
            session.remember(editSession);
            feedback(mc, "已粘贴");
            return true;
        } catch (Exception e) {
            error(mc, "粘贴失败: " + e.getMessage());
            return false;
        }
    }
}
