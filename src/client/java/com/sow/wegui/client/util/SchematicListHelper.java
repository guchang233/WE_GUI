package com.sow.wegui.client.util;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 读取 WorldEdit schematic / snapshot 目录，返回文件名建议列表。
 */
public final class SchematicListHelper {
    private SchematicListHelper() {}

    public static List<String> getSchematicNames() {
        return listFiles(Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("worldedit").resolve("schematics"));
    }

    public static List<String> getSnapshotNames() {
        return listFiles(Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("worldedit").resolve("snapshots"));
    }

    private static List<String> listFiles(Path dir) {
        if (!Files.isDirectory(dir)) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(s -> !s.isBlank())
                    .sorted(String::compareToIgnoreCase)
                    .forEach(names::add);
        } catch (IOException ignored) {
        }
        return names;
    }
}
