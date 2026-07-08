package com.sow.wegui.client;

import com.sow.wegui.WeCommandUsage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * //cut 的直接 API 实现。
 */
public final class CutOperation implements WeOperation {
    @Override
    public boolean execute(Minecraft mc, WeCommandUsage usage, List<String> values) {
        Player player = WeOperationHelper.requirePlayer(mc);
        if (player == null) {
            error(mc, "无法获取 WorldEdit 玩家对象");
            return false;
        }

        World world = player.getWorld();
        var session = WeOperationHelper.requireSession(player);
        Region region;
        try {
            region = session.getSelection(world);
        } catch (Exception e) {
            error(mc, "请先选择区域");
            return false;
        }

        boolean copyEntities = values.stream().anyMatch(v -> "-e".equalsIgnoreCase(v != null ? v.trim() : ""));
        Pattern leavePattern = BlockTypes.AIR.getDefaultState();

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .actor(player)
                .build()) {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(session.getPlacementPosition(player));
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
            copy.setCopyingEntities(copyEntities);
            copy.setRemovingEntities(true);
            Operations.completeLegacy(copy);
            session.setClipboard(new ClipboardHolder(clipboard));
            session.remember(editSession);
            feedback(mc, "已剪切选区");
            return true;
        } catch (Exception e) {
            error(mc, "剪切失败: " + e.getMessage());
            return false;
        }
    }
}
