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

import org.enginehub.linbus.common.LinTagId;
import org.enginehub.linbus.tree.LinByteArrayTag;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinEndTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinLongArrayTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import java.util.Arrays;
import java.util.Set;
import net.minecraft.class_2491;
import net.minecraft.class_2520;

/**
 * Converts between LinBus and Minecraft NBT classes.
 */
public final class NBTConverter {

    private NBTConverter() {
    }

    public static net.minecraft.class_2520 toNative(LinTag<?> tag) {
        return switch (tag) {
            case LinIntArrayTag t -> toNative(t);
            case LinListTag<?> t -> toNative(t);
            case LinLongTag t -> toNative(t);
            case LinLongArrayTag t -> toNative(t);
            case LinStringTag t -> toNative(t);
            case LinIntTag t -> toNative(t);
            case LinByteTag t -> toNative(t);
            case LinByteArrayTag t -> toNative(t);
            case LinCompoundTag t -> toNative(t);
            case LinFloatTag t -> toNative(t);
            case LinShortTag t -> toNative(t);
            case LinDoubleTag t -> toNative(t);
            case LinEndTag ignored -> class_2491.field_21033;
        };
    }

    public static net.minecraft.class_2495 toNative(LinIntArrayTag tag) {
        int[] value = tag.value();
        return new net.minecraft.class_2495(Arrays.copyOf(value, value.length));
    }

    public static net.minecraft.class_2499 toNative(LinListTag<?> tag) {
        net.minecraft.class_2499 list = new net.minecraft.class_2499();
        for (LinTag<?> child : tag.value()) {
            list.method_68580(toNative(child));
        }
        return list;
    }

    public static net.minecraft.class_2503 toNative(LinLongTag tag) {
        return net.minecraft.class_2503.method_23251(tag.value());
    }

    public static net.minecraft.class_2501 toNative(LinLongArrayTag tag) {
        return new net.minecraft.class_2501(tag.value().clone());
    }

    public static net.minecraft.class_2519 toNative(LinStringTag tag) {
        return net.minecraft.class_2519.method_23256(tag.value());
    }

    public static net.minecraft.class_2497 toNative(LinIntTag tag) {
        return net.minecraft.class_2497.method_23247(tag.value());
    }

    public static net.minecraft.class_2481 toNative(LinByteTag tag) {
        return net.minecraft.class_2481.method_23233(tag.value());
    }

    public static net.minecraft.class_2479 toNative(LinByteArrayTag tag) {
        return new net.minecraft.class_2479(tag.value().clone());
    }

    public static net.minecraft.class_2487 toNative(LinCompoundTag tag) {
        net.minecraft.class_2487 compound = new net.minecraft.class_2487();
        tag.value().forEach((key, value) -> compound.method_10566(key, toNative(value)));
        return compound;
    }

    public static net.minecraft.class_2494 toNative(LinFloatTag tag) {
        return net.minecraft.class_2494.method_23244(tag.value());
    }

    public static net.minecraft.class_2516 toNative(LinShortTag tag) {
        return net.minecraft.class_2516.method_23254(tag.value());
    }

    public static net.minecraft.class_2489 toNative(LinDoubleTag tag) {
        return net.minecraft.class_2489.method_23241(tag.value());
    }

    public static LinTag<?> fromNative(net.minecraft.class_2520 other) {
        return switch (other) {
            case net.minecraft.class_2495 tags -> fromNative(tags);
            case net.minecraft.class_2499 tags -> fromNative(tags);
            case net.minecraft.class_2491 endTag -> fromNative(endTag);
            case net.minecraft.class_2503 longTag -> fromNative(longTag);
            case net.minecraft.class_2501 tags -> fromNative(tags);
            case net.minecraft.class_2519 stringTag -> fromNative(stringTag);
            case net.minecraft.class_2497 intTag -> fromNative(intTag);
            case net.minecraft.class_2481 byteTag -> fromNative(byteTag);
            case net.minecraft.class_2479 tags -> fromNative(tags);
            case net.minecraft.class_2487 compoundTag -> fromNative(compoundTag);
            case net.minecraft.class_2494 floatTag -> fromNative(floatTag);
            case net.minecraft.class_2516 shortTag -> fromNative(shortTag);
            case net.minecraft.class_2489 doubleTag -> fromNative(doubleTag);
        };
    }

    public static LinIntArrayTag fromNative(net.minecraft.class_2495 other) {
        int[] value = other.method_10588();
        return LinIntArrayTag.of(Arrays.copyOf(value, value.length));
    }

    private static byte identifyRawElementType(net.minecraft.class_2499 list) {
        byte b = 0;

        for (class_2520 tag : list) {
            byte c = tag.method_10711();
            if (b == 0) {
                b = c;
            } else if (b != c) {
                return 10;
            }
        }

        return b;
    }

    private static net.minecraft.class_2487 wrapTag(net.minecraft.class_2520 tag) {
        if (tag instanceof net.minecraft.class_2487 compoundTag) {
            return compoundTag;
        }
        var compoundTag = new net.minecraft.class_2487();
        compoundTag.method_10566("", tag);
        return compoundTag;
    }

    public static LinListTag<?> fromNative(net.minecraft.class_2499 other) {
        byte rawType = identifyRawElementType(other);
        LinListTag.Builder<LinTag<?>> list = LinListTag.builder(LinTagType.fromId(
                LinTagId.fromId(rawType)
        ));
        for (net.minecraft.class_2520 tag : other) {
            if (rawType == LinTagId.COMPOUND.id() && !(tag instanceof net.minecraft.class_2487)) {
                list.add(fromNative(wrapTag(tag)));
            } else {
                list.add(fromNative(tag));
            }
        }
        return list.build();
    }

    public static LinEndTag fromNative(net.minecraft.class_2491 other) {
        return LinEndTag.instance();
    }

    public static LinLongTag fromNative(net.minecraft.class_2503 other) {
        return LinLongTag.of(other.comp_3821());
    }

    public static LinLongArrayTag fromNative(net.minecraft.class_2501 other) {
        return LinLongArrayTag.of(other.method_10615().clone());
    }

    public static LinStringTag fromNative(net.minecraft.class_2519 other) {
        return LinStringTag.of(other.comp_3831());
    }

    public static LinIntTag fromNative(net.minecraft.class_2497 other) {
        return LinIntTag.of(other.comp_3820());
    }

    public static LinByteTag fromNative(net.minecraft.class_2481 other) {
        return LinByteTag.of(other.comp_3817());
    }

    public static LinByteArrayTag fromNative(net.minecraft.class_2479 other) {
        return LinByteArrayTag.of(other.method_10521().clone());
    }

    public static LinCompoundTag fromNative(net.minecraft.class_2487 other) {
        Set<String> tags = other.method_10541();
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        for (String tagName : tags) {
            builder.put(tagName, fromNative(other.method_10580(tagName)));
        }
        return builder.build();
    }

    public static LinFloatTag fromNative(net.minecraft.class_2494 other) {
        return LinFloatTag.of(other.comp_3819());
    }

    public static LinShortTag fromNative(net.minecraft.class_2516 other) {
        return LinShortTag.of(other.comp_3822());
    }

    public static LinDoubleTag fromNative(net.minecraft.class_2489 other) {
        return LinDoubleTag.of(other.comp_3818());
    }

}
