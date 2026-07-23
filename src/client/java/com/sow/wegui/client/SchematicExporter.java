package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将当前 Litematica 投影导出为原理图文件。
 * 默认目录：./we_schematics（相对 Minecraft 运行目录）。
 * 支持 litematic（Litematica 原生）与 nbt（NBT 编码）两种格式。
 */
public final class SchematicExporter {
    private SchematicExporter() {}

    /** 原理图导出格式。 */
    public enum Format {
        LITEMATIC("litematic", ".litematic"),
        NBT("nbt", ".nbt");

        private final String key;
        private final String extension;

        Format(String key, String extension) {
            this.key = key;
            this.extension = extension;
        }

        public String getExtension() { return extension; }

        public String getTranslationKey() {
            return "wegui.schematic.format." + key;
        }

        @Nullable
        public static Format fromKey(String key) {
            for (Format f : values()) {
                if (f.key.equals(key)) return f;
            }
            return null;
        }
    }

    /** 默认原理图保存目录：./we_schematics。 */
    public static Path getSchematicsDir() {
        return Path.of(".", "we_schematics");
    }

    /** 保存 schematic 到指定目录。成功返回 true。 */
    public static boolean save(LitematicaSchematic schematic, Path dir, String fileName, Format format) {
        if (schematic == null) {
            WeGuiMod.LOGGER.warn("[WeGui] 导出失败：schematic 为 null");
            return false;
        }
        try {
            Files.createDirectories(dir);
            if (format == Format.LITEMATIC) {
                boolean ok = schematic.writeToFile(dir.toFile(), fileName, true);
                if (ok) {
                    WeGuiMod.LOGGER.info("[WeGui] 原理图已保存为 litematic: {}/{}", dir, fileName);
                }
                return ok;
            } else if (format == Format.NBT) {
                CompoundTag nbt = schematic.writeToNBT();
                Path filePath = dir.resolve(fileName + format.getExtension());
                NbtIo.writeCompressed(nbt, filePath);
                WeGuiMod.LOGGER.info("[WeGui] 原理图已保存为 NBT: {}", filePath);
                return true;
            }
            return false;
        } catch (Throwable e) {
            WeGuiMod.LOGGER.error("[WeGui] 原理图保存失败 ({}): {}", format, e);
            return false;
        }
    }
}
