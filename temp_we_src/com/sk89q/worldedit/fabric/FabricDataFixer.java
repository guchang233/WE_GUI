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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.sk89q.worldedit.fabric.internal.ComponentConverter;
import com.sk89q.worldedit.fabric.internal.NBTConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.class_1208;
import net.minecraft.class_1767;
import net.minecraft.class_2350;
import net.minecraft.class_2487;
import net.minecraft.class_2494;
import net.minecraft.class_2499;
import net.minecraft.class_2509;
import net.minecraft.class_2519;
import net.minecraft.class_2520;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3518;
import net.minecraft.class_3551;
import net.minecraft.class_4844;
import net.minecraft.class_5250;

/**
 * Handles converting all Pre 1.13.2 data using the Legacy DataFix System (ported to 1.13.2)
 *
 * <p>
 * We register a DFU Fixer per Legacy Data Version and apply the fixes using legacy strategy
 * which is safer, faster and cleaner code.
 * </p>
 *
 * <p>
 * The pre DFU code did not fail when the Source version was unknown.
 * </p>
 *
 * <p>
 * This class also provides util methods for converting compounds to wrap the update call to
 * receive the source version in the compound.
 * </p>
 */
@SuppressWarnings({
    "UnnecessarilyQualifiedStaticUsage",
    "StringSplitter",
    "ImmutableEnumChecker",
    "MissingOverride",
    "StaticAssignmentInConstructor",
    "EffectivelyPrivate",
    "FallThrough",
    "MutablePublicArray",
    "unused",
    "unchecked",
    "rawtypes"
})
class FabricDataFixer implements com.sk89q.worldedit.world.DataFixer {

    @Override
    public <T> T fixUp(FixType<T> type, T original, int srcVer) {
        if (type == FixTypes.CHUNK) {
            return (T) fixChunk((LinCompoundTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_ENTITY) {
            return (T) fixBlockEntity((LinCompoundTag) original, srcVer);
        } else if (type == FixTypes.ENTITY) {
            return (T) fixEntity((LinCompoundTag) original, srcVer);
        } else if (type == FixTypes.BLOCK_STATE) {
            return (T) fixBlockState((String) original, srcVer);
        } else if (type == FixTypes.ITEM_TYPE) {
            return (T) fixItemType((String) original, srcVer);
        } else if (type == FixTypes.BIOME) {
            return (T) fixBiome((String) original, srcVer);
        }
        return original;
    }

    private static LinCompoundTag applyAsNbt(LinCompoundTag original, Function<class_2487, class_2487> function) {
        class_2487 originalNative = NBTConverter.toNative(original);
        class_2487 fixedNative = function.apply(originalNative);
        return NBTConverter.fromNative(fixedNative);
    }

    private LinCompoundTag fixChunk(LinCompoundTag originalChunk, int srcVer) {
        return applyAsNbt(originalChunk, tag -> convert(class_1208.field_5726, tag, srcVer));
    }

    private LinCompoundTag fixBlockEntity(LinCompoundTag origTileEnt, int srcVer) {
        return applyAsNbt(origTileEnt, tag -> convert(class_1208.field_5727, tag, srcVer));
    }

    private LinCompoundTag fixEntity(LinCompoundTag origEnt, int srcVer) {
        return applyAsNbt(origEnt, tag -> convert(class_1208.field_5729, tag, srcVer));
    }

    private String fixBlockState(String blockState, int srcVer) {
        net.minecraft.class_2487 stateNBT = stateToNBT(blockState);
        Dynamic<class_2520> dynamic = new Dynamic<>(OPS_NBT, stateNBT);
        net.minecraft.class_2487 fixed = (net.minecraft.class_2487) INSTANCE.fixer.update(class_1208.field_5720, dynamic, srcVer, DATA_VERSION).getValue();
        return nbtToState(fixed);
    }

    private String nbtToState(net.minecraft.class_2487 tagCompound) {
        StringBuilder sb = new StringBuilder();
        sb.append(tagCompound.method_10558("Name").get());
        tagCompound.method_10562("Properties").ifPresent(props -> {
            sb.append('[');
            sb.append(props.method_10541().stream().map(k -> k + "=" + props.method_10558(k).get().replace("\"", "")).collect(Collectors.joining(",")));
            sb.append(']');
        });
        return sb.toString();
    }

    private static net.minecraft.class_2487 stateToNBT(String blockState) {
        int propIdx = blockState.indexOf('[');
        net.minecraft.class_2487 tag = new net.minecraft.class_2487();
        if (propIdx < 0) {
            tag.method_10582("Name", blockState);
        } else {
            tag.method_10582("Name", blockState.substring(0, propIdx));
            net.minecraft.class_2487 propTag = new net.minecraft.class_2487();
            String props = blockState.substring(propIdx + 1, blockState.length() - 1);
            String[] propArr = props.split(",");
            for (String pair : propArr) {
                final String[] split = pair.split("=");
                propTag.method_10582(split[0], split[1]);
            }
            tag.method_10566("Properties", propTag);
        }
        return tag;
    }

    private String fixBiome(String key, int srcVer) {
        return fixName(key, srcVer, class_1208.field_5728);
    }

    private String fixItemType(String key, int srcVer) {
        return fixName(key, srcVer, class_1208.field_5713);
    }

    private static String fixName(String key, int srcVer, TypeReference type) {
        return INSTANCE.fixer.update(type, new Dynamic<>(OPS_NBT, class_2519.method_23256(key)), srcVer, DATA_VERSION)
            .asString().result().orElse(key);
    }

    private static final class_2509 OPS_NBT = class_2509.field_11560;
    private static final int LEGACY_VERSION = 1343;
    private static int DATA_VERSION;
    private static FabricDataFixer INSTANCE;

    private final Map<LegacyType, List<DataConverter>> converters = new EnumMap<>(LegacyType.class);
    private final Map<LegacyType, List<DataInspector>> inspectors = new EnumMap<>(LegacyType.class);

    // Set on build
    private final DataFixer fixer;
    private static final Map<String, LegacyType> DFU_TO_LEGACY = new HashMap<>();

    public enum LegacyType {
        LEVEL(class_1208.field_5710),
        PLAYER(class_1208.field_5715),
        CHUNK(class_1208.field_5726),
        BLOCK_ENTITY(class_1208.field_5727),
        ENTITY(class_1208.field_5729),
        ITEM_INSTANCE(class_1208.field_5712),
        OPTIONS(class_1208.field_5717),
        STRUCTURE(class_1208.field_5716);

        private final TypeReference type;

        LegacyType(TypeReference type) {
            this.type = type;
            DFU_TO_LEGACY.put(type.typeName(), this);
        }

        public TypeReference getDFUType() {
            return type;
        }
    }

    FabricDataFixer(int dataVersion) {
        DATA_VERSION = dataVersion;
        INSTANCE = this;
        registerConverters();
        registerInspectors();
        this.fixer = new WrappedDataFixer(class_3551.method_15450());
    }

    private class WrappedDataFixer implements DataFixer {
        private final DataFixer realFixer;

        WrappedDataFixer(DataFixer realFixer) {
            this.realFixer = realFixer;
        }

        @Override
        public <T> Dynamic<T> update(TypeReference type, Dynamic<T> dynamic, int sourceVer, int targetVer) {
            LegacyType legacyType = DFU_TO_LEGACY.get(type.typeName());
            if (sourceVer < LEGACY_VERSION && legacyType != null) {
                net.minecraft.class_2487 cmp = (net.minecraft.class_2487) dynamic.getValue();
                int desiredVersion = Math.min(targetVer, LEGACY_VERSION);

                cmp = convert(legacyType, cmp, sourceVer, desiredVersion);
                sourceVer = desiredVersion;
                dynamic = new Dynamic(OPS_NBT, cmp);
            }
            return realFixer.update(type, dynamic, sourceVer, targetVer);
        }

        private net.minecraft.class_2487 convert(LegacyType type, net.minecraft.class_2487 cmp, int sourceVer, int desiredVersion) {
            List<DataConverter> converters = FabricDataFixer.this.converters.get(type);
            if (converters != null && !converters.isEmpty()) {
                for (DataConverter converter : converters) {
                    int dataVersion = converter.getDataVersion();
                    if (dataVersion > sourceVer && dataVersion <= desiredVersion) {
                        cmp = converter.convert(cmp);
                    }
                }
            }

            List<DataInspector> inspectors = FabricDataFixer.this.inspectors.get(type);
            if (inspectors != null && !inspectors.isEmpty()) {
                for (DataInspector inspector : inspectors) {
                    cmp = inspector.inspect(cmp, sourceVer, desiredVersion);
                }
            }

            return cmp;
        }

        @Override
        public Schema getSchema(int i) {
            return realFixer.getSchema(i);
        }
    }

    public static net.minecraft.class_2487 convert(LegacyType type, net.minecraft.class_2487 cmp) {
        return convert(type.getDFUType(), cmp);
    }

    public static net.minecraft.class_2487 convert(LegacyType type, net.minecraft.class_2487 cmp, int sourceVer) {
        return convert(type.getDFUType(), cmp, sourceVer);
    }

    public static net.minecraft.class_2487 convert(LegacyType type, net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
        return convert(type.getDFUType(), cmp, sourceVer, targetVer);
    }

    public static net.minecraft.class_2487 convert(TypeReference type, net.minecraft.class_2487 cmp) {
        int i = cmp.method_68083("DataVersion", -1);
        return convert(type, cmp, i);
    }

    public static net.minecraft.class_2487 convert(TypeReference type, net.minecraft.class_2487 cmp, int sourceVer) {
        return convert(type, cmp, sourceVer, DATA_VERSION);
    }

    public static net.minecraft.class_2487 convert(TypeReference type, net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
        if (sourceVer >= targetVer) {
            return cmp;
        }
        return (net.minecraft.class_2487) INSTANCE.fixer.update(type, new Dynamic<>(OPS_NBT, cmp), sourceVer, targetVer).getValue();
    }


    public interface DataInspector {
        net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer);
    }

    public interface DataConverter {

        int getDataVersion();

        net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp);
    }


    private void registerInspector(LegacyType type, DataInspector inspector) {
        this.inspectors.computeIfAbsent(type, k -> new ArrayList<>()).add(inspector);
    }

    private void registerConverter(LegacyType type, DataConverter converter) {
        int version = converter.getDataVersion();

        List<DataConverter> list = this.converters.computeIfAbsent(type, k -> new ArrayList<>());
        if (!list.isEmpty() && list.getLast().getDataVersion() > version) {
            for (int j = 0; j < list.size(); ++j) {
                if (list.get(j).getDataVersion() > version) {
                    list.add(j, converter);
                    break;
                }
            }
        } else {
            list.add(converter);
        }
    }

    private void registerInspectors() {
        registerEntityItemList("EntityHorseDonkey", "SaddleItem", "Items");
        registerEntityItemList("EntityHorseMule", "Items");
        registerEntityItemList("EntityMinecartChest", "Items");
        registerEntityItemList("EntityMinecartHopper", "Items");
        registerEntityItemList("EntityVillager", "Inventory");
        registerEntityItemListEquipment("EntityArmorStand");
        registerEntityItemListEquipment("EntityBat");
        registerEntityItemListEquipment("EntityBlaze");
        registerEntityItemListEquipment("EntityCaveSpider");
        registerEntityItemListEquipment("EntityChicken");
        registerEntityItemListEquipment("EntityCow");
        registerEntityItemListEquipment("EntityCreeper");
        registerEntityItemListEquipment("EntityEnderDragon");
        registerEntityItemListEquipment("EntityEnderman");
        registerEntityItemListEquipment("EntityEndermite");
        registerEntityItemListEquipment("EntityEvoker");
        registerEntityItemListEquipment("EntityGhast");
        registerEntityItemListEquipment("EntityGiantZombie");
        registerEntityItemListEquipment("EntityGuardian");
        registerEntityItemListEquipment("EntityGuardianElder");
        registerEntityItemListEquipment("EntityHorse");
        registerEntityItemListEquipment("EntityHorseDonkey");
        registerEntityItemListEquipment("EntityHorseMule");
        registerEntityItemListEquipment("EntityHorseSkeleton");
        registerEntityItemListEquipment("EntityHorseZombie");
        registerEntityItemListEquipment("EntityIronGolem");
        registerEntityItemListEquipment("EntityMagmaCube");
        registerEntityItemListEquipment("EntityMushroomCow");
        registerEntityItemListEquipment("EntityOcelot");
        registerEntityItemListEquipment("EntityPig");
        registerEntityItemListEquipment("EntityPigZombie");
        registerEntityItemListEquipment("EntityRabbit");
        registerEntityItemListEquipment("EntitySheep");
        registerEntityItemListEquipment("EntityShulker");
        registerEntityItemListEquipment("EntitySilverfish");
        registerEntityItemListEquipment("EntitySkeleton");
        registerEntityItemListEquipment("EntitySkeletonStray");
        registerEntityItemListEquipment("EntitySkeletonWither");
        registerEntityItemListEquipment("EntitySlime");
        registerEntityItemListEquipment("EntitySnowman");
        registerEntityItemListEquipment("EntitySpider");
        registerEntityItemListEquipment("EntitySquid");
        registerEntityItemListEquipment("EntityVex");
        registerEntityItemListEquipment("EntityVillager");
        registerEntityItemListEquipment("EntityVindicator");
        registerEntityItemListEquipment("EntityWitch");
        registerEntityItemListEquipment("EntityWither");
        registerEntityItemListEquipment("EntityWolf");
        registerEntityItemListEquipment("EntityZombie");
        registerEntityItemListEquipment("EntityZombieHusk");
        registerEntityItemListEquipment("EntityZombieVillager");
        registerEntityItemSingle("EntityFireworks", "FireworksItem");
        registerEntityItemSingle("EntityHorse", "ArmorItem");
        registerEntityItemSingle("EntityHorse", "SaddleItem");
        registerEntityItemSingle("EntityHorseMule", "SaddleItem");
        registerEntityItemSingle("EntityHorseSkeleton", "SaddleItem");
        registerEntityItemSingle("EntityHorseZombie", "SaddleItem");
        registerEntityItemSingle("EntityItem", "Item");
        registerEntityItemSingle("EntityItemFrame", "Item");
        registerEntityItemSingle("EntityPotion", "Potion");

        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItem("TileEntityRecordPlayer", "RecordItem"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityBrewingStand", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityChest", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityDispenser", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityDropper", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityFurnace", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityHopper", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorItemList("TileEntityShulkerBox", "Items"));
        registerInspector(LegacyType.BLOCK_ENTITY, new DataInspectorMobSpawnerMobs());
        registerInspector(LegacyType.CHUNK, new DataInspectorChunks());
        registerInspector(LegacyType.ENTITY, new DataInspectorCommandBlock());
        registerInspector(LegacyType.ENTITY, new DataInspectorEntityPassengers());
        registerInspector(LegacyType.ENTITY, new DataInspectorMobSpawnerMinecart());
        registerInspector(LegacyType.ENTITY, new DataInspectorVillagers());
        registerInspector(LegacyType.ITEM_INSTANCE, new DataInspectorBlockEntity());
        registerInspector(LegacyType.ITEM_INSTANCE, new DataInspectorEntity());
        registerInspector(LegacyType.LEVEL, new DataInspectorLevelPlayer());
        registerInspector(LegacyType.PLAYER, new DataInspectorPlayer());
        registerInspector(LegacyType.PLAYER, new DataInspectorPlayerVehicle());
        registerInspector(LegacyType.STRUCTURE, new DataInspectorStructure());
    }

    private void registerConverters() {
        registerConverter(LegacyType.ENTITY, new DataConverterEquipment());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterSignText());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterMaterialId());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterPotionId());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterSpawnEgg());
        registerConverter(LegacyType.ENTITY, new DataConverterMinecart());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterMobSpawner());
        registerConverter(LegacyType.ENTITY, new DataConverterUUID());
        registerConverter(LegacyType.ENTITY, new DataConverterHealth());
        registerConverter(LegacyType.ENTITY, new DataConverterSaddle());
        registerConverter(LegacyType.ENTITY, new DataConverterHanging());
        registerConverter(LegacyType.ENTITY, new DataConverterDropChances());
        registerConverter(LegacyType.ENTITY, new DataConverterRiding());
        registerConverter(LegacyType.ENTITY, new DataConverterArmorStand());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBook());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterCookedFish());
        registerConverter(LegacyType.ENTITY, new DataConverterZombie());
        registerConverter(LegacyType.OPTIONS, new DataConverterVBO());
        registerConverter(LegacyType.ENTITY, new DataConverterGuardian());
        registerConverter(LegacyType.ENTITY, new DataConverterSkeleton());
        registerConverter(LegacyType.ENTITY, new DataConverterZombieType());
        registerConverter(LegacyType.ENTITY, new DataConverterHorse());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterTileEntity());
        registerConverter(LegacyType.ENTITY, new DataConverterEntity());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBanner());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterPotionWater());
        registerConverter(LegacyType.ENTITY, new DataConverterShulker());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterShulkerBoxItem());
        registerConverter(LegacyType.BLOCK_ENTITY, new DataConverterShulkerBoxBlock());
        registerConverter(LegacyType.OPTIONS, new DataConverterLang());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterTotem());
        registerConverter(LegacyType.CHUNK, new DataConverterBedBlock());
        registerConverter(LegacyType.ITEM_INSTANCE, new DataConverterBedItem());
    }

    private void registerEntityItemList(String type, String... keys) {
        registerInspector(LegacyType.ENTITY, new DataInspectorItemList(type, keys));
    }

    private void registerEntityItemSingle(String type, String key) {
        registerInspector(LegacyType.ENTITY, new DataInspectorItem(type, key));
    }

    private void registerEntityItemListEquipment(String type) {
        registerEntityItemList(type, "ArmorItems", "HandItems");
    }

    private static final Map<String, class_2960> OLD_ID_TO_KEY_MAP = new HashMap<>();

    static {
        final Map<String, class_2960> map = OLD_ID_TO_KEY_MAP;
        map.put("EntityItem", class_2960.method_60654("item"));
        map.put("EntityExperienceOrb", class_2960.method_60654("xp_orb"));
        map.put("EntityAreaEffectCloud", class_2960.method_60654("area_effect_cloud"));
        map.put("EntityGuardianElder", class_2960.method_60654("elder_guardian"));
        map.put("EntitySkeletonWither", class_2960.method_60654("wither_skeleton"));
        map.put("EntitySkeletonStray", class_2960.method_60654("stray"));
        map.put("EntityEgg", class_2960.method_60654("egg"));
        map.put("EntityLeash", class_2960.method_60654("leash_knot"));
        map.put("EntityPainting", class_2960.method_60654("painting"));
        map.put("EntityTippedArrow", class_2960.method_60654("arrow"));
        map.put("EntitySnowball", class_2960.method_60654("snowball"));
        map.put("EntityLargeFireball", class_2960.method_60654("fireball"));
        map.put("EntitySmallFireball", class_2960.method_60654("small_fireball"));
        map.put("EntityEnderPearl", class_2960.method_60654("ender_pearl"));
        map.put("EntityEnderSignal", class_2960.method_60654("eye_of_ender_signal"));
        map.put("EntityPotion", class_2960.method_60654("potion"));
        map.put("EntityThrownExpBottle", class_2960.method_60654("xp_bottle"));
        map.put("EntityItemFrame", class_2960.method_60654("item_frame"));
        map.put("EntityWitherSkull", class_2960.method_60654("wither_skull"));
        map.put("EntityTNTPrimed", class_2960.method_60654("tnt"));
        map.put("EntityFallingBlock", class_2960.method_60654("falling_block"));
        map.put("EntityFireworks", class_2960.method_60654("fireworks_rocket"));
        map.put("EntityZombieHusk", class_2960.method_60654("husk"));
        map.put("EntitySpectralArrow", class_2960.method_60654("spectral_arrow"));
        map.put("EntityShulkerBullet", class_2960.method_60654("shulker_bullet"));
        map.put("EntityDragonFireball", class_2960.method_60654("dragon_fireball"));
        map.put("EntityZombieVillager", class_2960.method_60654("zombie_villager"));
        map.put("EntityHorseSkeleton", class_2960.method_60654("skeleton_horse"));
        map.put("EntityHorseZombie", class_2960.method_60654("zombie_horse"));
        map.put("EntityArmorStand", class_2960.method_60654("armor_stand"));
        map.put("EntityHorseDonkey", class_2960.method_60654("donkey"));
        map.put("EntityHorseMule", class_2960.method_60654("mule"));
        map.put("EntityEvokerFangs", class_2960.method_60654("evocation_fangs"));
        map.put("EntityEvoker", class_2960.method_60654("evocation_illager"));
        map.put("EntityVex", class_2960.method_60654("vex"));
        map.put("EntityVindicator", class_2960.method_60654("vindication_illager"));
        map.put("EntityIllagerIllusioner", class_2960.method_60654("illusion_illager"));
        map.put("EntityMinecartCommandBlock", class_2960.method_60654("commandblock_minecart"));
        map.put("EntityBoat", class_2960.method_60654("boat"));
        map.put("EntityMinecartRideable", class_2960.method_60654("minecart"));
        map.put("EntityMinecartChest", class_2960.method_60654("chest_minecart"));
        map.put("EntityMinecartFurnace", class_2960.method_60654("furnace_minecart"));
        map.put("EntityMinecartTNT", class_2960.method_60654("tnt_minecart"));
        map.put("EntityMinecartHopper", class_2960.method_60654("hopper_minecart"));
        map.put("EntityMinecartMobSpawner", class_2960.method_60654("spawner_minecart"));
        map.put("EntityCreeper", class_2960.method_60654("creeper"));
        map.put("EntitySkeleton", class_2960.method_60654("skeleton"));
        map.put("EntitySpider", class_2960.method_60654("spider"));
        map.put("EntityGiantZombie", class_2960.method_60654("giant"));
        map.put("EntityZombie", class_2960.method_60654("zombie"));
        map.put("EntitySlime", class_2960.method_60654("slime"));
        map.put("EntityGhast", class_2960.method_60654("ghast"));
        map.put("EntityPigZombie", class_2960.method_60654("zombie_pigman"));
        map.put("EntityEnderman", class_2960.method_60654("enderman"));
        map.put("EntityCaveSpider", class_2960.method_60654("cave_spider"));
        map.put("EntitySilverfish", class_2960.method_60654("silverfish"));
        map.put("EntityBlaze", class_2960.method_60654("blaze"));
        map.put("EntityMagmaCube", class_2960.method_60654("magma_cube"));
        map.put("EntityEnderDragon", class_2960.method_60654("ender_dragon"));
        map.put("EntityWither", class_2960.method_60654("wither"));
        map.put("EntityBat", class_2960.method_60654("bat"));
        map.put("EntityWitch", class_2960.method_60654("witch"));
        map.put("EntityEndermite", class_2960.method_60654("endermite"));
        map.put("EntityGuardian", class_2960.method_60654("guardian"));
        map.put("EntityShulker", class_2960.method_60654("shulker"));
        map.put("EntityPig", class_2960.method_60654("pig"));
        map.put("EntitySheep", class_2960.method_60654("sheep"));
        map.put("EntityCow", class_2960.method_60654("cow"));
        map.put("EntityChicken", class_2960.method_60654("chicken"));
        map.put("EntitySquid", class_2960.method_60654("squid"));
        map.put("EntityWolf", class_2960.method_60654("wolf"));
        map.put("EntityMushroomCow", class_2960.method_60654("mooshroom"));
        map.put("EntitySnowman", class_2960.method_60654("snowman"));
        map.put("EntityOcelot", class_2960.method_60654("ocelot"));
        map.put("EntityIronGolem", class_2960.method_60654("villager_golem"));
        map.put("EntityHorse", class_2960.method_60654("horse"));
        map.put("EntityRabbit", class_2960.method_60654("rabbit"));
        map.put("EntityPolarBear", class_2960.method_60654("polar_bear"));
        map.put("EntityLlama", class_2960.method_60654("llama"));
        map.put("EntityLlamaSpit", class_2960.method_60654("llama_spit"));
        map.put("EntityParrot", class_2960.method_60654("parrot"));
        map.put("EntityVillager", class_2960.method_60654("villager"));
        map.put("EntityEnderCrystal", class_2960.method_60654("ender_crystal"));
        map.put("TileEntityFurnace", class_2960.method_60654("furnace"));
        map.put("TileEntityChest", class_2960.method_60654("chest"));
        map.put("TileEntityEnderChest", class_2960.method_60654("ender_chest"));
        map.put("TileEntityRecordPlayer", class_2960.method_60654("jukebox"));
        map.put("TileEntityDispenser", class_2960.method_60654("dispenser"));
        map.put("TileEntityDropper", class_2960.method_60654("dropper"));
        map.put("TileEntitySign", class_2960.method_60654("sign"));
        map.put("TileEntityMobSpawner", class_2960.method_60654("mob_spawner"));
        map.put("TileEntityNote", class_2960.method_60654("noteblock"));
        map.put("TileEntityPiston", class_2960.method_60654("piston"));
        map.put("TileEntityBrewingStand", class_2960.method_60654("brewing_stand"));
        map.put("TileEntityEnchantTable", class_2960.method_60654("enchanting_table"));
        map.put("TileEntityEnderPortal", class_2960.method_60654("end_portal"));
        map.put("TileEntityBeacon", class_2960.method_60654("beacon"));
        map.put("TileEntitySkull", class_2960.method_60654("skull"));
        map.put("TileEntityLightDetector", class_2960.method_60654("daylight_detector"));
        map.put("TileEntityHopper", class_2960.method_60654("hopper"));
        map.put("TileEntityComparator", class_2960.method_60654("comparator"));
        map.put("TileEntityFlowerPot", class_2960.method_60654("flower_pot"));
        map.put("TileEntityBanner", class_2960.method_60654("banner"));
        map.put("TileEntityStructure", class_2960.method_60654("structure_block"));
        map.put("TileEntityEndGateway", class_2960.method_60654("end_gateway"));
        map.put("TileEntityCommand", class_2960.method_60654("command_block"));
        map.put("TileEntityShulkerBox", class_2960.method_60654("shulker_box"));
        map.put("TileEntityBed", class_2960.method_60654("bed"));
    }

    private static class_2960 getKey(String type) {
        final class_2960 key = OLD_ID_TO_KEY_MAP.get(type);
        if (key == null) {
            throw new IllegalArgumentException("Unknown mapping for " + type);
        }
        return key;
    }

    private static void convertCompound(LegacyType type, net.minecraft.class_2487 cmp, String key, int sourceVer, int targetVer) {
        cmp.method_10566(key, convert(type, cmp.method_68568(key), sourceVer, targetVer));
    }

    private static void convertItem(net.minecraft.class_2487 nbttagcompound, String key, int sourceVer, int targetVer) {
        if (nbttagcompound.method_10562(key).isPresent()) {
            convertCompound(LegacyType.ITEM_INSTANCE, nbttagcompound, key, sourceVer, targetVer);
        }
    }

    private static void convertItems(net.minecraft.class_2487 nbttagcompound, String key, int sourceVer, int targetVer) {
        nbttagcompound.method_10554(key).ifPresent(nbttaglist -> {
            for (int j = 0; j < nbttaglist.size(); ++j) {
                nbttaglist.method_68585(j, convert(LegacyType.ITEM_INSTANCE, nbttaglist.method_68582(j), sourceVer, targetVer));
            }
        });
    }

    private static class DataConverterEquipment implements DataConverter {

        DataConverterEquipment() {
        }

        @Override
        public int getDataVersion() {
            return 100;
        }

        @Override
        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            class_2499 nbttaglist = cmp.method_68569("Equipment");

            if (!nbttaglist.isEmpty() && cmp.method_10562("HandItems").isEmpty()) {
                class_2499 nbttaglist1 = new class_2499();
                nbttaglist1.add(nbttaglist.method_10534(0));
                nbttaglist1.add(new net.minecraft.class_2487());
                cmp.method_10566("HandItems", nbttaglist1);
            }

            if (nbttaglist.size() > 1 && cmp.method_10562("ArmorItem").isEmpty()) {
                class_2499 nbttaglist1 = new class_2499();
                nbttaglist1.add(nbttaglist.method_10534(1));
                nbttaglist1.add(nbttaglist.method_10534(2));
                nbttaglist1.add(nbttaglist.method_10534(3));
                nbttaglist1.add(nbttaglist.method_10534(4));
                cmp.method_10566("ArmorItems", nbttaglist1);
            }

            cmp.method_10551("Equipment");
            cmp.method_10554("DropChances").ifPresent(nbttaglist1 -> {
                class_2499 nbttaglist2;

                if (cmp.method_10562("HandDropChances").isEmpty()) {
                    nbttaglist2 = new class_2499();
                    nbttaglist2.add(class_2494.method_23244(nbttaglist1.method_68575(0, 0F)));
                    nbttaglist2.add(class_2494.method_23244(0.0F));
                    cmp.method_10566("HandDropChances", nbttaglist2);
                }

                if (cmp.method_10562("ArmorDropChances").isEmpty()) {
                    nbttaglist2 = new class_2499();
                    nbttaglist2.add(class_2494.method_23244(nbttaglist1.method_68575(1, 0F)));
                    nbttaglist2.add(class_2494.method_23244(nbttaglist1.method_68575(2, 0F)));
                    nbttaglist2.add(class_2494.method_23244(nbttaglist1.method_68575(3, 0F)));
                    nbttaglist2.add(class_2494.method_23244(nbttaglist1.method_68575(4, 0F)));
                    cmp.method_10566("ArmorDropChances", nbttaglist2);
                }

                cmp.method_10551("DropChances");
            });

            return cmp;
        }
    }

    private static class DataInspectorBlockEntity implements DataInspector {

        private static final Map<String, String> b = Maps.newHashMap();
        private static final Map<String, String> c = Maps.newHashMap();

        DataInspectorBlockEntity() {
        }

        @Nullable
        private static String convertEntityId(int i, String s) {
            String key = class_2960.method_60654(s).toString();
            if (i < 515 && DataInspectorBlockEntity.b.containsKey(key)) {
                return DataInspectorBlockEntity.b.get(key);
            } else {
                return DataInspectorBlockEntity.c.get(key);
            }
        }

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            Optional<net.minecraft.class_2487> nbttagcompound1Optional = cmp.method_10562("tag");

            if (nbttagcompound1Optional.isPresent()) {
                var nbttagcompound1 = nbttagcompound1Optional.get();

                nbttagcompound1.method_10562("BlockEntityTag").ifPresent(nbttagcompound2 -> {
                    String s = cmp.method_10558("id").get();
                    String s1 = convertEntityId(sourceVer, s);
                    boolean flag;

                    if (s1 == null) {
                        // CraftBukkit - Remove unnecessary warning (occurs when deserializing a Shulker Box item)
                        // DataInspectorBlockEntity.a.warn("Unable to resolve BlockEntity for ItemInstance: {}", s);
                        flag = false;
                    } else {
                        flag = !nbttagcompound2.method_10545("id");
                        nbttagcompound2.method_10582("id", s1);
                    }

                    convert(LegacyType.BLOCK_ENTITY, nbttagcompound2, sourceVer, targetVer);
                    if (flag) {
                        nbttagcompound2.method_10551("id");
                    }
                });
            }

            return cmp;
        }

        static {
            Map map = DataInspectorBlockEntity.b;

            map.put("minecraft:furnace", "Furnace");
            map.put("minecraft:lit_furnace", "Furnace");
            map.put("minecraft:chest", "Chest");
            map.put("minecraft:trapped_chest", "Chest");
            map.put("minecraft:ender_chest", "EnderChest");
            map.put("minecraft:jukebox", "RecordPlayer");
            map.put("minecraft:dispenser", "Trap");
            map.put("minecraft:dropper", "Dropper");
            map.put("minecraft:sign", "Sign");
            map.put("minecraft:mob_spawner", "MobSpawner");
            map.put("minecraft:noteblock", "Music");
            map.put("minecraft:brewing_stand", "Cauldron");
            map.put("minecraft:enhanting_table", "EnchantTable");
            map.put("minecraft:command_block", "CommandBlock");
            map.put("minecraft:beacon", "Beacon");
            map.put("minecraft:skull", "Skull");
            map.put("minecraft:daylight_detector", "DLDetector");
            map.put("minecraft:hopper", "Hopper");
            map.put("minecraft:banner", "Banner");
            map.put("minecraft:flower_pot", "FlowerPot");
            map.put("minecraft:repeating_command_block", "CommandBlock");
            map.put("minecraft:chain_command_block", "CommandBlock");
            map.put("minecraft:standing_sign", "Sign");
            map.put("minecraft:wall_sign", "Sign");
            map.put("minecraft:piston_head", "Piston");
            map.put("minecraft:daylight_detector_inverted", "DLDetector");
            map.put("minecraft:unpowered_comparator", "Comparator");
            map.put("minecraft:powered_comparator", "Comparator");
            map.put("minecraft:wall_banner", "Banner");
            map.put("minecraft:standing_banner", "Banner");
            map.put("minecraft:structure_block", "Structure");
            map.put("minecraft:end_portal", "Airportal");
            map.put("minecraft:end_gateway", "EndGateway");
            map.put("minecraft:shield", "Shield");
            map = DataInspectorBlockEntity.c;
            map.put("minecraft:furnace", "minecraft:furnace");
            map.put("minecraft:lit_furnace", "minecraft:furnace");
            map.put("minecraft:chest", "minecraft:chest");
            map.put("minecraft:trapped_chest", "minecraft:chest");
            map.put("minecraft:ender_chest", "minecraft:enderchest");
            map.put("minecraft:jukebox", "minecraft:jukebox");
            map.put("minecraft:dispenser", "minecraft:dispenser");
            map.put("minecraft:dropper", "minecraft:dropper");
            map.put("minecraft:sign", "minecraft:sign");
            map.put("minecraft:mob_spawner", "minecraft:mob_spawner");
            map.put("minecraft:noteblock", "minecraft:noteblock");
            map.put("minecraft:brewing_stand", "minecraft:brewing_stand");
            map.put("minecraft:enhanting_table", "minecraft:enchanting_table");
            map.put("minecraft:command_block", "minecraft:command_block");
            map.put("minecraft:beacon", "minecraft:beacon");
            map.put("minecraft:skull", "minecraft:skull");
            map.put("minecraft:daylight_detector", "minecraft:daylight_detector");
            map.put("minecraft:hopper", "minecraft:hopper");
            map.put("minecraft:banner", "minecraft:banner");
            map.put("minecraft:flower_pot", "minecraft:flower_pot");
            map.put("minecraft:repeating_command_block", "minecraft:command_block");
            map.put("minecraft:chain_command_block", "minecraft:command_block");
            map.put("minecraft:shulker_box", "minecraft:shulker_box");
            map.put("minecraft:white_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:orange_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:magenta_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:light_blue_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:yellow_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:lime_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:pink_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:gray_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:silver_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:cyan_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:purple_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:blue_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:brown_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:green_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:red_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:black_shulker_box", "minecraft:shulker_box");
            map.put("minecraft:bed", "minecraft:bed");
            map.put("minecraft:standing_sign", "minecraft:sign");
            map.put("minecraft:wall_sign", "minecraft:sign");
            map.put("minecraft:piston_head", "minecraft:piston");
            map.put("minecraft:daylight_detector_inverted", "minecraft:daylight_detector");
            map.put("minecraft:unpowered_comparator", "minecraft:comparator");
            map.put("minecraft:powered_comparator", "minecraft:comparator");
            map.put("minecraft:wall_banner", "minecraft:banner");
            map.put("minecraft:standing_banner", "minecraft:banner");
            map.put("minecraft:structure_block", "minecraft:structure_block");
            map.put("minecraft:end_portal", "minecraft:end_portal");
            map.put("minecraft:end_gateway", "minecraft:end_gateway");
            map.put("minecraft:shield", "minecraft:shield");
        }
    }

    private static class DataInspectorEntity implements DataInspector {

        private static final Logger a = LogManager.getLogger(FabricDataFixer.class);

        DataInspectorEntity() {
        }

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            cmp.method_10562("tag").flatMap(nbttagcompound1 -> nbttagcompound1.method_10562("EntityTag")).ifPresent(nbttagcompound2 -> {
                String s = cmp.method_10558("id").orElse(null);
                String s1;

                if ("minecraft:armor_stand".equals(s)) {
                    s1 = sourceVer < 515 ? "ArmorStand" : "minecraft:armor_stand";
                } else {
                    if (!"minecraft:spawn_egg".equals(s)) {
                        return;
                    }

                    s1 = nbttagcompound2.method_10558("id").orElse(null);
                }

                boolean flag;

                if (s1 == null) {
                    DataInspectorEntity.a.warn("Unable to resolve Entity for ItemInstance: {}", s);
                    flag = false;
                } else {
                    flag = nbttagcompound2.method_10558("id").isEmpty();
                    nbttagcompound2.method_10582("id", s1);
                }

                convert(LegacyType.ENTITY, nbttagcompound2, sourceVer, targetVer);
                if (flag) {
                    nbttagcompound2.method_10551("id");
                }
            });

            return cmp;
        }
    }


    private abstract static class DataInspectorTagged implements DataInspector {

        private final class_2960 key;

        DataInspectorTagged(String type) {
            this.key = getKey(type);
        }

        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            if (cmp.method_10558("id").isPresent() && this.key.equals(class_2960.method_60654(cmp.method_10558("id").get()))) {
                cmp = this.inspectChecked(cmp, sourceVer, targetVer);
            }

            return cmp;
        }

        abstract net.minecraft.class_2487 inspectChecked(net.minecraft.class_2487 nbttagcompound, int sourceVer, int targetVer);
    }

    private static class DataInspectorItemList extends DataInspectorTagged {

        private final String[] keys;

        DataInspectorItemList(String oclass, String... astring) {
            super(oclass);
            this.keys = astring;
        }

        net.minecraft.class_2487 inspectChecked(net.minecraft.class_2487 nbttagcompound, int sourceVer, int targetVer) {
            for (String s : this.keys) {
                FabricDataFixer.convertItems(nbttagcompound, s, sourceVer, targetVer);
            }

            return nbttagcompound;
        }
    }

    private static class DataInspectorItem extends DataInspectorTagged {

        private final String[] keys;

        DataInspectorItem(String oclass, String... astring) {
            super(oclass);
            this.keys = astring;
        }

        net.minecraft.class_2487 inspectChecked(net.minecraft.class_2487 nbttagcompound, int sourceVer, int targetVer) {
            for (String key : this.keys) {
                FabricDataFixer.convertItem(nbttagcompound, key, sourceVer, targetVer);
            }

            return nbttagcompound;
        }
    }

    private static class DataConverterMaterialId implements DataConverter {

        private static final String[] materials = new String[2268];

        DataConverterMaterialId() {
        }

        public int getDataVersion() {
            return 102;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            cmp.method_10568("id").ifPresent(short0 -> {
                if (short0 > 0 && short0 < materials.length && materials[short0] != null) {
                    cmp.method_10582("id", materials[short0]);
                }
            });

            return cmp;
        }

        static {
            materials[1] = "minecraft:stone";
            materials[2] = "minecraft:grass";
            materials[3] = "minecraft:dirt";
            materials[4] = "minecraft:cobblestone";
            materials[5] = "minecraft:planks";
            materials[6] = "minecraft:sapling";
            materials[7] = "minecraft:bedrock";
            materials[8] = "minecraft:flowing_water";
            materials[9] = "minecraft:water";
            materials[10] = "minecraft:flowing_lava";
            materials[11] = "minecraft:lava";
            materials[12] = "minecraft:sand";
            materials[13] = "minecraft:gravel";
            materials[14] = "minecraft:gold_ore";
            materials[15] = "minecraft:iron_ore";
            materials[16] = "minecraft:coal_ore";
            materials[17] = "minecraft:log";
            materials[18] = "minecraft:leaves";
            materials[19] = "minecraft:sponge";
            materials[20] = "minecraft:glass";
            materials[21] = "minecraft:lapis_ore";
            materials[22] = "minecraft:lapis_block";
            materials[23] = "minecraft:dispenser";
            materials[24] = "minecraft:sandstone";
            materials[25] = "minecraft:noteblock";
            materials[27] = "minecraft:golden_rail";
            materials[28] = "minecraft:detector_rail";
            materials[29] = "minecraft:sticky_piston";
            materials[30] = "minecraft:web";
            materials[31] = "minecraft:tallgrass";
            materials[32] = "minecraft:deadbush";
            materials[33] = "minecraft:piston";
            materials[35] = "minecraft:wool";
            materials[37] = "minecraft:yellow_flower";
            materials[38] = "minecraft:red_flower";
            materials[39] = "minecraft:brown_mushroom";
            materials[40] = "minecraft:red_mushroom";
            materials[41] = "minecraft:gold_block";
            materials[42] = "minecraft:iron_block";
            materials[43] = "minecraft:double_stone_slab";
            materials[44] = "minecraft:stone_slab";
            materials[45] = "minecraft:brick_block";
            materials[46] = "minecraft:tnt";
            materials[47] = "minecraft:bookshelf";
            materials[48] = "minecraft:mossy_cobblestone";
            materials[49] = "minecraft:obsidian";
            materials[50] = "minecraft:torch";
            materials[51] = "minecraft:fire";
            materials[52] = "minecraft:mob_spawner";
            materials[53] = "minecraft:oak_stairs";
            materials[54] = "minecraft:chest";
            materials[56] = "minecraft:diamond_ore";
            materials[57] = "minecraft:diamond_block";
            materials[58] = "minecraft:crafting_table";
            materials[60] = "minecraft:farmland";
            materials[61] = "minecraft:furnace";
            materials[62] = "minecraft:lit_furnace";
            materials[65] = "minecraft:ladder";
            materials[66] = "minecraft:rail";
            materials[67] = "minecraft:stone_stairs";
            materials[69] = "minecraft:lever";
            materials[70] = "minecraft:stone_pressure_plate";
            materials[72] = "minecraft:wooden_pressure_plate";
            materials[73] = "minecraft:redstone_ore";
            materials[76] = "minecraft:redstone_torch";
            materials[77] = "minecraft:stone_button";
            materials[78] = "minecraft:snow_layer";
            materials[79] = "minecraft:ice";
            materials[80] = "minecraft:snow";
            materials[81] = "minecraft:cactus";
            materials[82] = "minecraft:clay";
            materials[84] = "minecraft:jukebox";
            materials[85] = "minecraft:fence";
            materials[86] = "minecraft:pumpkin";
            materials[87] = "minecraft:netherrack";
            materials[88] = "minecraft:soul_sand";
            materials[89] = "minecraft:glowstone";
            materials[90] = "minecraft:portal";
            materials[91] = "minecraft:lit_pumpkin";
            materials[95] = "minecraft:stained_glass";
            materials[96] = "minecraft:trapdoor";
            materials[97] = "minecraft:monster_egg";
            materials[98] = "minecraft:stonebrick";
            materials[99] = "minecraft:brown_mushroom_block";
            materials[100] = "minecraft:red_mushroom_block";
            materials[101] = "minecraft:iron_bars";
            materials[102] = "minecraft:glass_pane";
            materials[103] = "minecraft:melon_block";
            materials[106] = "minecraft:vine";
            materials[107] = "minecraft:fence_gate";
            materials[108] = "minecraft:brick_stairs";
            materials[109] = "minecraft:stone_brick_stairs";
            materials[110] = "minecraft:mycelium";
            materials[111] = "minecraft:waterlily";
            materials[112] = "minecraft:nether_brick";
            materials[113] = "minecraft:nether_brick_fence";
            materials[114] = "minecraft:nether_brick_stairs";
            materials[116] = "minecraft:enchanting_table";
            materials[119] = "minecraft:end_portal";
            materials[120] = "minecraft:end_portal_frame";
            materials[121] = "minecraft:end_stone";
            materials[122] = "minecraft:dragon_egg";
            materials[123] = "minecraft:redstone_lamp";
            materials[125] = "minecraft:double_wooden_slab";
            materials[126] = "minecraft:wooden_slab";
            materials[127] = "minecraft:cocoa";
            materials[128] = "minecraft:sandstone_stairs";
            materials[129] = "minecraft:emerald_ore";
            materials[130] = "minecraft:ender_chest";
            materials[131] = "minecraft:tripwire_hook";
            materials[133] = "minecraft:emerald_block";
            materials[134] = "minecraft:spruce_stairs";
            materials[135] = "minecraft:birch_stairs";
            materials[136] = "minecraft:jungle_stairs";
            materials[137] = "minecraft:command_block";
            materials[138] = "minecraft:beacon";
            materials[139] = "minecraft:cobblestone_wall";
            materials[141] = "minecraft:carrots";
            materials[142] = "minecraft:potatoes";
            materials[143] = "minecraft:wooden_button";
            materials[145] = "minecraft:anvil";
            materials[146] = "minecraft:trapped_chest";
            materials[147] = "minecraft:light_weighted_pressure_plate";
            materials[148] = "minecraft:heavy_weighted_pressure_plate";
            materials[151] = "minecraft:daylight_detector";
            materials[152] = "minecraft:redstone_block";
            materials[153] = "minecraft:quartz_ore";
            materials[154] = "minecraft:hopper";
            materials[155] = "minecraft:quartz_block";
            materials[156] = "minecraft:quartz_stairs";
            materials[157] = "minecraft:activator_rail";
            materials[158] = "minecraft:dropper";
            materials[159] = "minecraft:stained_hardened_clay";
            materials[160] = "minecraft:stained_glass_pane";
            materials[161] = "minecraft:leaves2";
            materials[162] = "minecraft:log2";
            materials[163] = "minecraft:acacia_stairs";
            materials[164] = "minecraft:dark_oak_stairs";
            materials[170] = "minecraft:hay_block";
            materials[171] = "minecraft:carpet";
            materials[172] = "minecraft:hardened_clay";
            materials[173] = "minecraft:coal_block";
            materials[174] = "minecraft:packed_ice";
            materials[175] = "minecraft:double_plant";
            materials[256] = "minecraft:iron_shovel";
            materials[257] = "minecraft:iron_pickaxe";
            materials[258] = "minecraft:iron_axe";
            materials[259] = "minecraft:flint_and_steel";
            materials[260] = "minecraft:apple";
            materials[261] = "minecraft:bow";
            materials[262] = "minecraft:arrow";
            materials[263] = "minecraft:coal";
            materials[264] = "minecraft:diamond";
            materials[265] = "minecraft:iron_ingot";
            materials[266] = "minecraft:gold_ingot";
            materials[267] = "minecraft:iron_sword";
            materials[268] = "minecraft:wooden_sword";
            materials[269] = "minecraft:wooden_shovel";
            materials[270] = "minecraft:wooden_pickaxe";
            materials[271] = "minecraft:wooden_axe";
            materials[272] = "minecraft:stone_sword";
            materials[273] = "minecraft:stone_shovel";
            materials[274] = "minecraft:stone_pickaxe";
            materials[275] = "minecraft:stone_axe";
            materials[276] = "minecraft:diamond_sword";
            materials[277] = "minecraft:diamond_shovel";
            materials[278] = "minecraft:diamond_pickaxe";
            materials[279] = "minecraft:diamond_axe";
            materials[280] = "minecraft:stick";
            materials[281] = "minecraft:bowl";
            materials[282] = "minecraft:mushroom_stew";
            materials[283] = "minecraft:golden_sword";
            materials[284] = "minecraft:golden_shovel";
            materials[285] = "minecraft:golden_pickaxe";
            materials[286] = "minecraft:golden_axe";
            materials[287] = "minecraft:string";
            materials[288] = "minecraft:feather";
            materials[289] = "minecraft:gunpowder";
            materials[290] = "minecraft:wooden_hoe";
            materials[291] = "minecraft:stone_hoe";
            materials[292] = "minecraft:iron_hoe";
            materials[293] = "minecraft:diamond_hoe";
            materials[294] = "minecraft:golden_hoe";
            materials[295] = "minecraft:wheat_seeds";
            materials[296] = "minecraft:wheat";
            materials[297] = "minecraft:bread";
            materials[298] = "minecraft:leather_helmet";
            materials[299] = "minecraft:leather_chestplate";
            materials[300] = "minecraft:leather_leggings";
            materials[301] = "minecraft:leather_boots";
            materials[302] = "minecraft:chainmail_helmet";
            materials[303] = "minecraft:chainmail_chestplate";
            materials[304] = "minecraft:chainmail_leggings";
            materials[305] = "minecraft:chainmail_boots";
            materials[306] = "minecraft:iron_helmet";
            materials[307] = "minecraft:iron_chestplate";
            materials[308] = "minecraft:iron_leggings";
            materials[309] = "minecraft:iron_boots";
            materials[310] = "minecraft:diamond_helmet";
            materials[311] = "minecraft:diamond_chestplate";
            materials[312] = "minecraft:diamond_leggings";
            materials[313] = "minecraft:diamond_boots";
            materials[314] = "minecraft:golden_helmet";
            materials[315] = "minecraft:golden_chestplate";
            materials[316] = "minecraft:golden_leggings";
            materials[317] = "minecraft:golden_boots";
            materials[318] = "minecraft:flint";
            materials[319] = "minecraft:porkchop";
            materials[320] = "minecraft:cooked_porkchop";
            materials[321] = "minecraft:painting";
            materials[322] = "minecraft:golden_apple";
            materials[323] = "minecraft:sign";
            materials[324] = "minecraft:wooden_door";
            materials[325] = "minecraft:bucket";
            materials[326] = "minecraft:water_bucket";
            materials[327] = "minecraft:lava_bucket";
            materials[328] = "minecraft:minecart";
            materials[329] = "minecraft:saddle";
            materials[330] = "minecraft:iron_door";
            materials[331] = "minecraft:redstone";
            materials[332] = "minecraft:snowball";
            materials[333] = "minecraft:boat";
            materials[334] = "minecraft:leather";
            materials[335] = "minecraft:milk_bucket";
            materials[336] = "minecraft:brick";
            materials[337] = "minecraft:clay_ball";
            materials[338] = "minecraft:reeds";
            materials[339] = "minecraft:paper";
            materials[340] = "minecraft:book";
            materials[341] = "minecraft:slime_ball";
            materials[342] = "minecraft:chest_minecart";
            materials[343] = "minecraft:furnace_minecart";
            materials[344] = "minecraft:egg";
            materials[345] = "minecraft:compass";
            materials[346] = "minecraft:fishing_rod";
            materials[347] = "minecraft:clock";
            materials[348] = "minecraft:glowstone_dust";
            materials[349] = "minecraft:fish";
            materials[350] = "minecraft:cooked_fish"; // Paper - cooked_fished -> cooked_fish
            materials[351] = "minecraft:dye";
            materials[352] = "minecraft:bone";
            materials[353] = "minecraft:sugar";
            materials[354] = "minecraft:cake";
            materials[355] = "minecraft:bed";
            materials[356] = "minecraft:repeater";
            materials[357] = "minecraft:cookie";
            materials[358] = "minecraft:filled_map";
            materials[359] = "minecraft:shears";
            materials[360] = "minecraft:melon";
            materials[361] = "minecraft:pumpkin_seeds";
            materials[362] = "minecraft:melon_seeds";
            materials[363] = "minecraft:beef";
            materials[364] = "minecraft:cooked_beef";
            materials[365] = "minecraft:chicken";
            materials[366] = "minecraft:cooked_chicken";
            materials[367] = "minecraft:rotten_flesh";
            materials[368] = "minecraft:ender_pearl";
            materials[369] = "minecraft:blaze_rod";
            materials[370] = "minecraft:ghast_tear";
            materials[371] = "minecraft:gold_nugget";
            materials[372] = "minecraft:nether_wart";
            materials[373] = "minecraft:potion";
            materials[374] = "minecraft:glass_bottle";
            materials[375] = "minecraft:spider_eye";
            materials[376] = "minecraft:fermented_spider_eye";
            materials[377] = "minecraft:blaze_powder";
            materials[378] = "minecraft:magma_cream";
            materials[379] = "minecraft:brewing_stand";
            materials[380] = "minecraft:cauldron";
            materials[381] = "minecraft:ender_eye";
            materials[382] = "minecraft:speckled_melon";
            materials[383] = "minecraft:spawn_egg";
            materials[384] = "minecraft:experience_bottle";
            materials[385] = "minecraft:fire_charge";
            materials[386] = "minecraft:writable_book";
            materials[387] = "minecraft:written_book";
            materials[388] = "minecraft:emerald";
            materials[389] = "minecraft:item_frame";
            materials[390] = "minecraft:flower_pot";
            materials[391] = "minecraft:carrot";
            materials[392] = "minecraft:potato";
            materials[393] = "minecraft:baked_potato";
            materials[394] = "minecraft:poisonous_potato";
            materials[395] = "minecraft:map";
            materials[396] = "minecraft:golden_carrot";
            materials[397] = "minecraft:skull";
            materials[398] = "minecraft:carrot_on_a_stick";
            materials[399] = "minecraft:nether_star";
            materials[400] = "minecraft:pumpkin_pie";
            materials[401] = "minecraft:fireworks";
            materials[402] = "minecraft:firework_charge";
            materials[403] = "minecraft:enchanted_book";
            materials[404] = "minecraft:comparator";
            materials[405] = "minecraft:netherbrick";
            materials[406] = "minecraft:quartz";
            materials[407] = "minecraft:tnt_minecart";
            materials[408] = "minecraft:hopper_minecart";
            materials[417] = "minecraft:iron_horse_armor";
            materials[418] = "minecraft:golden_horse_armor";
            materials[419] = "minecraft:diamond_horse_armor";
            materials[420] = "minecraft:lead";
            materials[421] = "minecraft:name_tag";
            materials[422] = "minecraft:command_block_minecart";
            materials[2256] = "minecraft:record_13";
            materials[2257] = "minecraft:record_cat";
            materials[2258] = "minecraft:record_blocks";
            materials[2259] = "minecraft:record_chirp";
            materials[2260] = "minecraft:record_far";
            materials[2261] = "minecraft:record_mall";
            materials[2262] = "minecraft:record_mellohi";
            materials[2263] = "minecraft:record_stal";
            materials[2264] = "minecraft:record_strad";
            materials[2265] = "minecraft:record_ward";
            materials[2266] = "minecraft:record_11";
            materials[2267] = "minecraft:record_wait";
        }
    }

    private static class DataConverterArmorStand implements DataConverter {

        DataConverterArmorStand() {
        }

        public int getDataVersion() {
            return 147;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("ArmorStand".equals(cmp.method_10558("id").orElse(null)) && cmp.method_10577("Silent").orElse(false) && !cmp.method_10577("Marker").orElse(false)) {
                cmp.method_10551("Silent");
            }

            return cmp;
        }
    }

    private static class DataConverterBanner implements DataConverter {

        DataConverterBanner() {
        }

        public int getDataVersion() {
            return 804;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:banner".equals(cmp.method_10558("id").orElse(null))) {
                cmp.method_10562("tag").ifPresent(nbttagcompound1 -> nbttagcompound1.method_10562("BlockEntityTag").ifPresent(nbttagcompound2 -> {
                    if (nbttagcompound2.method_10568("Base").isPresent()) {
                        cmp.method_10575("Damage", (short) (nbttagcompound2.method_10568("Base").get() & 15));
                        if (nbttagcompound1.method_10562("display").isPresent()) {
                            class_2487 nbttagcompound3 = nbttagcompound1.method_10562("display").get();

                            if (nbttagcompound3.method_10554("Lore").isPresent()) {
                                class_2499 nbttaglist = nbttagcompound3.method_10554("Lore").get();

                                if (nbttaglist.size() == 1 && "(+NBT)".equals(nbttaglist.method_10608(0).orElse(null))) {
                                    return;
                                }
                            }
                        }

                        nbttagcompound2.method_10551("Base");
                        if (nbttagcompound2.method_33133()) {
                            nbttagcompound1.method_10551("BlockEntityTag");
                        }

                        if (nbttagcompound1.method_33133()) {
                            cmp.method_10551("tag");
                        }
                    }
                }));
            }

            return cmp;
        }
    }

    private static class DataConverterPotionId implements DataConverter {

        private static final String[] potions = new String[128];

        DataConverterPotionId() {
        }

        public int getDataVersion() {
            return 102;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:potion".equals(cmp.method_10558("id").orElse(null))) {
                net.minecraft.class_2487 nbttagcompound1 = cmp.method_68568("tag");
                short short0 = cmp.method_68565("Damage", (short) 0);

                if (nbttagcompound1.method_10558("Potion").isEmpty()) {
                    String s = DataConverterPotionId.potions[short0 & 127];

                    nbttagcompound1.method_10582("Potion", s == null ? "minecraft:water" : s);
                    cmp.method_10566("tag", nbttagcompound1);
                    if ((short0 & 16384) == 16384) {
                        cmp.method_10582("id", "minecraft:splash_potion");
                    }
                }

                if (short0 != 0) {
                    cmp.method_10575("Damage", (short) 0);
                }
            }

            return cmp;
        }

        static {
            DataConverterPotionId.potions[0] = "minecraft:water";
            DataConverterPotionId.potions[1] = "minecraft:regeneration";
            DataConverterPotionId.potions[2] = "minecraft:swiftness";
            DataConverterPotionId.potions[3] = "minecraft:fire_resistance";
            DataConverterPotionId.potions[4] = "minecraft:poison";
            DataConverterPotionId.potions[5] = "minecraft:healing";
            DataConverterPotionId.potions[6] = "minecraft:night_vision";
            DataConverterPotionId.potions[7] = null;
            DataConverterPotionId.potions[8] = "minecraft:weakness";
            DataConverterPotionId.potions[9] = "minecraft:strength";
            DataConverterPotionId.potions[10] = "minecraft:slowness";
            DataConverterPotionId.potions[11] = "minecraft:leaping";
            DataConverterPotionId.potions[12] = "minecraft:harming";
            DataConverterPotionId.potions[13] = "minecraft:water_breathing";
            DataConverterPotionId.potions[14] = "minecraft:invisibility";
            DataConverterPotionId.potions[15] = null;
            DataConverterPotionId.potions[16] = "minecraft:awkward";
            DataConverterPotionId.potions[17] = "minecraft:regeneration";
            DataConverterPotionId.potions[18] = "minecraft:swiftness";
            DataConverterPotionId.potions[19] = "minecraft:fire_resistance";
            DataConverterPotionId.potions[20] = "minecraft:poison";
            DataConverterPotionId.potions[21] = "minecraft:healing";
            DataConverterPotionId.potions[22] = "minecraft:night_vision";
            DataConverterPotionId.potions[23] = null;
            DataConverterPotionId.potions[24] = "minecraft:weakness";
            DataConverterPotionId.potions[25] = "minecraft:strength";
            DataConverterPotionId.potions[26] = "minecraft:slowness";
            DataConverterPotionId.potions[27] = "minecraft:leaping";
            DataConverterPotionId.potions[28] = "minecraft:harming";
            DataConverterPotionId.potions[29] = "minecraft:water_breathing";
            DataConverterPotionId.potions[30] = "minecraft:invisibility";
            DataConverterPotionId.potions[31] = null;
            DataConverterPotionId.potions[32] = "minecraft:thick";
            DataConverterPotionId.potions[33] = "minecraft:strong_regeneration";
            DataConverterPotionId.potions[34] = "minecraft:strong_swiftness";
            DataConverterPotionId.potions[35] = "minecraft:fire_resistance";
            DataConverterPotionId.potions[36] = "minecraft:strong_poison";
            DataConverterPotionId.potions[37] = "minecraft:strong_healing";
            DataConverterPotionId.potions[38] = "minecraft:night_vision";
            DataConverterPotionId.potions[39] = null;
            DataConverterPotionId.potions[40] = "minecraft:weakness";
            DataConverterPotionId.potions[41] = "minecraft:strong_strength";
            DataConverterPotionId.potions[42] = "minecraft:slowness";
            DataConverterPotionId.potions[43] = "minecraft:strong_leaping";
            DataConverterPotionId.potions[44] = "minecraft:strong_harming";
            DataConverterPotionId.potions[45] = "minecraft:water_breathing";
            DataConverterPotionId.potions[46] = "minecraft:invisibility";
            DataConverterPotionId.potions[47] = null;
            DataConverterPotionId.potions[48] = null;
            DataConverterPotionId.potions[49] = "minecraft:strong_regeneration";
            DataConverterPotionId.potions[50] = "minecraft:strong_swiftness";
            DataConverterPotionId.potions[51] = "minecraft:fire_resistance";
            DataConverterPotionId.potions[52] = "minecraft:strong_poison";
            DataConverterPotionId.potions[53] = "minecraft:strong_healing";
            DataConverterPotionId.potions[54] = "minecraft:night_vision";
            DataConverterPotionId.potions[55] = null;
            DataConverterPotionId.potions[56] = "minecraft:weakness";
            DataConverterPotionId.potions[57] = "minecraft:strong_strength";
            DataConverterPotionId.potions[58] = "minecraft:slowness";
            DataConverterPotionId.potions[59] = "minecraft:strong_leaping";
            DataConverterPotionId.potions[60] = "minecraft:strong_harming";
            DataConverterPotionId.potions[61] = "minecraft:water_breathing";
            DataConverterPotionId.potions[62] = "minecraft:invisibility";
            DataConverterPotionId.potions[63] = null;
            DataConverterPotionId.potions[64] = "minecraft:mundane";
            DataConverterPotionId.potions[65] = "minecraft:long_regeneration";
            DataConverterPotionId.potions[66] = "minecraft:long_swiftness";
            DataConverterPotionId.potions[67] = "minecraft:long_fire_resistance";
            DataConverterPotionId.potions[68] = "minecraft:long_poison";
            DataConverterPotionId.potions[69] = "minecraft:healing";
            DataConverterPotionId.potions[70] = "minecraft:long_night_vision";
            DataConverterPotionId.potions[71] = null;
            DataConverterPotionId.potions[72] = "minecraft:long_weakness";
            DataConverterPotionId.potions[73] = "minecraft:long_strength";
            DataConverterPotionId.potions[74] = "minecraft:long_slowness";
            DataConverterPotionId.potions[75] = "minecraft:long_leaping";
            DataConverterPotionId.potions[76] = "minecraft:harming";
            DataConverterPotionId.potions[77] = "minecraft:long_water_breathing";
            DataConverterPotionId.potions[78] = "minecraft:long_invisibility";
            DataConverterPotionId.potions[79] = null;
            DataConverterPotionId.potions[80] = "minecraft:awkward";
            DataConverterPotionId.potions[81] = "minecraft:long_regeneration";
            DataConverterPotionId.potions[82] = "minecraft:long_swiftness";
            DataConverterPotionId.potions[83] = "minecraft:long_fire_resistance";
            DataConverterPotionId.potions[84] = "minecraft:long_poison";
            DataConverterPotionId.potions[85] = "minecraft:healing";
            DataConverterPotionId.potions[86] = "minecraft:long_night_vision";
            DataConverterPotionId.potions[87] = null;
            DataConverterPotionId.potions[88] = "minecraft:long_weakness";
            DataConverterPotionId.potions[89] = "minecraft:long_strength";
            DataConverterPotionId.potions[90] = "minecraft:long_slowness";
            DataConverterPotionId.potions[91] = "minecraft:long_leaping";
            DataConverterPotionId.potions[92] = "minecraft:harming";
            DataConverterPotionId.potions[93] = "minecraft:long_water_breathing";
            DataConverterPotionId.potions[94] = "minecraft:long_invisibility";
            DataConverterPotionId.potions[95] = null;
            DataConverterPotionId.potions[96] = "minecraft:thick";
            DataConverterPotionId.potions[97] = "minecraft:regeneration";
            DataConverterPotionId.potions[98] = "minecraft:swiftness";
            DataConverterPotionId.potions[99] = "minecraft:long_fire_resistance";
            DataConverterPotionId.potions[100] = "minecraft:poison";
            DataConverterPotionId.potions[101] = "minecraft:strong_healing";
            DataConverterPotionId.potions[102] = "minecraft:long_night_vision";
            DataConverterPotionId.potions[103] = null;
            DataConverterPotionId.potions[104] = "minecraft:long_weakness";
            DataConverterPotionId.potions[105] = "minecraft:strength";
            DataConverterPotionId.potions[106] = "minecraft:long_slowness";
            DataConverterPotionId.potions[107] = "minecraft:leaping";
            DataConverterPotionId.potions[108] = "minecraft:strong_harming";
            DataConverterPotionId.potions[109] = "minecraft:long_water_breathing";
            DataConverterPotionId.potions[110] = "minecraft:long_invisibility";
            DataConverterPotionId.potions[111] = null;
            DataConverterPotionId.potions[112] = null;
            DataConverterPotionId.potions[113] = "minecraft:regeneration";
            DataConverterPotionId.potions[114] = "minecraft:swiftness";
            DataConverterPotionId.potions[115] = "minecraft:long_fire_resistance";
            DataConverterPotionId.potions[116] = "minecraft:poison";
            DataConverterPotionId.potions[117] = "minecraft:strong_healing";
            DataConverterPotionId.potions[118] = "minecraft:long_night_vision";
            DataConverterPotionId.potions[119] = null;
            DataConverterPotionId.potions[120] = "minecraft:long_weakness";
            DataConverterPotionId.potions[121] = "minecraft:strength";
            DataConverterPotionId.potions[122] = "minecraft:long_slowness";
            DataConverterPotionId.potions[123] = "minecraft:leaping";
            DataConverterPotionId.potions[124] = "minecraft:strong_harming";
            DataConverterPotionId.potions[125] = "minecraft:long_water_breathing";
            DataConverterPotionId.potions[126] = "minecraft:long_invisibility";
            DataConverterPotionId.potions[127] = null;
        }
    }

    private static class DataConverterSpawnEgg implements DataConverter {

        private static final String[] eggs = new String[256];

        DataConverterSpawnEgg() {
        }

        public int getDataVersion() {
            return 105;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:spawn_egg".equals(cmp.method_10558("id").orElse(null))) {
                net.minecraft.class_2487 nbttagcompound1 = cmp.method_68568("tag");
                net.minecraft.class_2487 nbttagcompound2 = nbttagcompound1.method_68568("EntityTag");
                short short0 = cmp.method_68565("Damage", (short) 0);

                if (nbttagcompound2.method_10558("id").isEmpty()) {
                    String s = DataConverterSpawnEgg.eggs[short0 & 255];

                    if (s != null) {
                        nbttagcompound2.method_10582("id", s);
                        nbttagcompound1.method_10566("EntityTag", nbttagcompound2);
                        cmp.method_10566("tag", nbttagcompound1);
                    }
                }

                if (short0 != 0) {
                    cmp.method_10575("Damage", (short) 0);
                }
            }

            return cmp;
        }

        static {

            DataConverterSpawnEgg.eggs[1] = "Item";
            DataConverterSpawnEgg.eggs[2] = "XPOrb";
            DataConverterSpawnEgg.eggs[7] = "ThrownEgg";
            DataConverterSpawnEgg.eggs[8] = "LeashKnot";
            DataConverterSpawnEgg.eggs[9] = "Painting";
            DataConverterSpawnEgg.eggs[10] = "Arrow";
            DataConverterSpawnEgg.eggs[11] = "Snowball";
            DataConverterSpawnEgg.eggs[12] = "Fireball";
            DataConverterSpawnEgg.eggs[13] = "SmallFireball";
            DataConverterSpawnEgg.eggs[14] = "ThrownEnderpearl";
            DataConverterSpawnEgg.eggs[15] = "EyeOfEnderSignal";
            DataConverterSpawnEgg.eggs[16] = "ThrownPotion";
            DataConverterSpawnEgg.eggs[17] = "ThrownExpBottle";
            DataConverterSpawnEgg.eggs[18] = "ItemFrame";
            DataConverterSpawnEgg.eggs[19] = "WitherSkull";
            DataConverterSpawnEgg.eggs[20] = "PrimedTnt";
            DataConverterSpawnEgg.eggs[21] = "FallingSand";
            DataConverterSpawnEgg.eggs[22] = "FireworksRocketEntity";
            DataConverterSpawnEgg.eggs[23] = "TippedArrow";
            DataConverterSpawnEgg.eggs[24] = "SpectralArrow";
            DataConverterSpawnEgg.eggs[25] = "ShulkerBullet";
            DataConverterSpawnEgg.eggs[26] = "DragonFireball";
            DataConverterSpawnEgg.eggs[30] = "ArmorStand";
            DataConverterSpawnEgg.eggs[41] = "Boat";
            DataConverterSpawnEgg.eggs[42] = "MinecartRideable";
            DataConverterSpawnEgg.eggs[43] = "MinecartChest";
            DataConverterSpawnEgg.eggs[44] = "MinecartFurnace";
            DataConverterSpawnEgg.eggs[45] = "MinecartTNT";
            DataConverterSpawnEgg.eggs[46] = "MinecartHopper";
            DataConverterSpawnEgg.eggs[47] = "MinecartSpawner";
            DataConverterSpawnEgg.eggs[40] = "MinecartCommandBlock";
            DataConverterSpawnEgg.eggs[48] = "Mob";
            DataConverterSpawnEgg.eggs[49] = "Monster";
            DataConverterSpawnEgg.eggs[50] = "Creeper";
            DataConverterSpawnEgg.eggs[51] = "Skeleton";
            DataConverterSpawnEgg.eggs[52] = "Spider";
            DataConverterSpawnEgg.eggs[53] = "Giant";
            DataConverterSpawnEgg.eggs[54] = "Zombie";
            DataConverterSpawnEgg.eggs[55] = "Slime";
            DataConverterSpawnEgg.eggs[56] = "Ghast";
            DataConverterSpawnEgg.eggs[57] = "PigZombie";
            DataConverterSpawnEgg.eggs[58] = "Enderman";
            DataConverterSpawnEgg.eggs[59] = "CaveSpider";
            DataConverterSpawnEgg.eggs[60] = "Silverfish";
            DataConverterSpawnEgg.eggs[61] = "Blaze";
            DataConverterSpawnEgg.eggs[62] = "LavaSlime";
            DataConverterSpawnEgg.eggs[63] = "EnderDragon";
            DataConverterSpawnEgg.eggs[64] = "WitherBoss";
            DataConverterSpawnEgg.eggs[65] = "Bat";
            DataConverterSpawnEgg.eggs[66] = "Witch";
            DataConverterSpawnEgg.eggs[67] = "Endermite";
            DataConverterSpawnEgg.eggs[68] = "Guardian";
            DataConverterSpawnEgg.eggs[69] = "Shulker";
            DataConverterSpawnEgg.eggs[90] = "Pig";
            DataConverterSpawnEgg.eggs[91] = "Sheep";
            DataConverterSpawnEgg.eggs[92] = "Cow";
            DataConverterSpawnEgg.eggs[93] = "Chicken";
            DataConverterSpawnEgg.eggs[94] = "Squid";
            DataConverterSpawnEgg.eggs[95] = "Wolf";
            DataConverterSpawnEgg.eggs[96] = "MushroomCow";
            DataConverterSpawnEgg.eggs[97] = "SnowMan";
            DataConverterSpawnEgg.eggs[98] = "Ozelot";
            DataConverterSpawnEgg.eggs[99] = "VillagerGolem";
            DataConverterSpawnEgg.eggs[100] = "EntityHorse";
            DataConverterSpawnEgg.eggs[101] = "Rabbit";
            DataConverterSpawnEgg.eggs[120] = "Villager";
            DataConverterSpawnEgg.eggs[200] = "EnderCrystal";
        }
    }

    private static class DataConverterMinecart implements DataConverter {

        private static final List<String> a = List.of("MinecartRideable", "MinecartChest", "MinecartFurnace", "MinecartTNT", "MinecartSpawner", "MinecartHopper", "MinecartCommandBlock");

        DataConverterMinecart() {
        }

        public int getDataVersion() {
            return 106;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("Minecart".equals(cmp.method_10558("id").orElse(null))) {
                String s = "MinecartRideable";
                int i = cmp.method_68083("Type", 0);

                if (i > 0 && i < DataConverterMinecart.a.size()) {
                    s = DataConverterMinecart.a.get(i);
                }

                cmp.method_10582("id", s);
                cmp.method_10551("Type");
            }

            return cmp;
        }
    }

    private static class DataConverterMobSpawner implements DataConverter {

        DataConverterMobSpawner() {
        }

        public int getDataVersion() {
            return 107;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("MobSpawner".equals(cmp.method_10558("id").orElse(null))) {
                cmp.method_10558("EntityId").ifPresent(s -> {
                    class_2487 nbttagcompound1 = cmp.method_68568("SpawnData");

                    nbttagcompound1.method_10582("id", s.isEmpty() ? "Pig" : s);
                    cmp.method_10566("SpawnData", nbttagcompound1);
                    cmp.method_10551("EntityId");
                });

                cmp.method_10554("SpawnPotentials").ifPresent(nbttaglist -> {
                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        class_2487 nbttagcompound2 = nbttaglist.method_68582(i);

                        if (nbttagcompound2.method_10558("Type").isPresent()) {
                            class_2487 nbttagcompound3 = nbttagcompound2.method_68568("Properties");

                            nbttagcompound3.method_10582("id", nbttagcompound2.method_10558("Type").get());
                            nbttagcompound2.method_10566("Entity", nbttagcompound3);
                            nbttagcompound2.method_10551("Type");
                            nbttagcompound2.method_10551("Properties");
                        }
                    }
                });

            }

            return cmp;
        }
    }

    private static class DataConverterUUID implements DataConverter {

        DataConverterUUID() {
        }

        public int getDataVersion() {
            return 108;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            cmp.method_10558("UUID").ifPresent(uuid -> {
                cmp.method_10539("UUID", class_4844.method_26275(UUID.fromString(uuid)));
            });

            return cmp;
        }
    }

    private static class DataConverterHealth implements DataConverter {

        private static final Set<String> a = Sets.newHashSet("ArmorStand", "Bat", "Blaze", "CaveSpider", "Chicken", "Cow", "Creeper", "EnderDragon", "Enderman", "Endermite", "EntityHorse", "Ghast", "Giant", "Guardian", "LavaSlime", "MushroomCow", "Ozelot", "Pig", "PigZombie", "Rabbit", "Sheep", "Shulker", "Silverfish", "Skeleton", "Slime", "SnowMan", "Spider", "Squid", "Villager", "VillagerGolem", "Witch", "WitherBoss", "Wolf", "Zombie");

        DataConverterHealth() {
        }

        public int getDataVersion() {
            return 109;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if (DataConverterHealth.a.contains(cmp.method_10558("id").orElse(null))) {
                float f;

                if (cmp.method_10583("HealF").isPresent()) {
                    f = cmp.method_10583("HealF").get();
                    cmp.method_10551("HealF");
                } else {
                    if (cmp.method_10583("Health").isEmpty()) {
                        return cmp;
                    }

                    f = cmp.method_10583("Health").get();
                }

                cmp.method_10548("Health", f);
            }

            return cmp;
        }
    }

    private static class DataConverterSaddle implements DataConverter {

        DataConverterSaddle() {
        }

        public int getDataVersion() {
            return 110;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("EntityHorse".equals(cmp.method_10558("id").orElse(null)) && cmp.method_10562("SaddleItem").isEmpty() && cmp.method_10577("Saddle").orElse(false)) {
                net.minecraft.class_2487 nbttagcompound1 = new net.minecraft.class_2487();

                nbttagcompound1.method_10582("id", "minecraft:saddle");
                nbttagcompound1.method_10567("Count", (byte) 1);
                nbttagcompound1.method_10575("Damage", (short) 0);
                cmp.method_10566("SaddleItem", nbttagcompound1);
                cmp.method_10551("Saddle");
            }

            return cmp;
        }
    }

    private static class DataConverterHanging implements DataConverter {

        DataConverterHanging() {
        }

        public int getDataVersion() {
            return 111;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            String s = cmp.method_10558("id").orElse(null);
            boolean flag = "Painting".equals(s);
            boolean flag1 = "ItemFrame".equals(s);

            if ((flag || flag1) && cmp.method_10571("Facing").isEmpty()) {
                class_2350 enumdirection;

                if (cmp.method_10571("Direction").isPresent()) {
                    enumdirection = class_2350.method_10139(cmp.method_10571("Direction").get());
                    cmp.method_10569("TileX", cmp.method_68083("TileX", 0) + enumdirection.method_10148());
                    cmp.method_10569("TileY", cmp.method_68083("TileY", 0) + enumdirection.method_10164());
                    cmp.method_10569("TileZ", cmp.method_68083("TileZ", 0) + enumdirection.method_10165());
                    cmp.method_10551("Direction");
                    if (flag1 && cmp.method_10571("ItemRotation").isPresent()) {
                        cmp.method_10567("ItemRotation", (byte) (cmp.method_10571("ItemRotation").get() * 2));
                    }
                } else {
                    enumdirection = class_2350.method_10139(cmp.method_10571("Dir").get());
                    cmp.method_10551("Dir");
                }

                cmp.method_10567("Facing", (byte) enumdirection.method_10161());
            }

            return cmp;
        }
    }

    private static class DataConverterDropChances implements DataConverter {

        DataConverterDropChances() {
        }

        public int getDataVersion() {
            return 113;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            cmp.method_10554("HandDropChances").ifPresent(nbttaglist -> {
                if (nbttaglist.size() == 2 && nbttaglist.method_68575(0, 0.0F) == 0.0F && nbttaglist.method_68575(1, 0.0F) == 0.0F) {
                    cmp.method_10551("HandDropChances");
                }
            });

            cmp.method_10554("ArmorDropChances").ifPresent(nbttaglist -> {
                if (nbttaglist.size() == 4 && nbttaglist.method_68575(0, 0.0F) == 0.0F && nbttaglist.method_68575(1, 0.0F) == 0.0F && nbttaglist.method_68575(2, 0.0F) == 0.0F && nbttaglist.method_68575(3, 0.0F) == 0.0F) {
                    cmp.method_10551("ArmorDropChances");
                }
            });

            return cmp;
        }
    }

    private static class DataConverterRiding implements DataConverter {

        DataConverterRiding() {
        }

        public int getDataVersion() {
            return 135;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            while (cmp.method_10562("Riding").isPresent()) {
                net.minecraft.class_2487 nbttagcompound1 = this.b(cmp);

                this.convert(cmp, nbttagcompound1);
                cmp = nbttagcompound1;
            }

            return cmp;
        }

        protected void convert(net.minecraft.class_2487 nbttagcompound, net.minecraft.class_2487 nbttagcompound1) {
            class_2499 nbttaglist = new class_2499();

            nbttaglist.add(nbttagcompound);
            nbttagcompound1.method_10566("Passengers", nbttaglist);
        }

        protected net.minecraft.class_2487 b(net.minecraft.class_2487 nbttagcompound) {
            net.minecraft.class_2487 nbttagcompound1 = nbttagcompound.method_68568("Riding");

            nbttagcompound.method_10551("Riding");
            return nbttagcompound1;
        }
    }

    private static class DataConverterBook implements DataConverter {

        DataConverterBook() {
        }

        public int getDataVersion() {
            return 165;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:written_book".equals(cmp.method_10558("id").orElse(null))) {
                net.minecraft.class_2487 nbttagcompound1 = cmp.method_68568("tag");

                nbttagcompound1.method_10554("pages").ifPresent(nbttaglist -> {
                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        String s = nbttaglist.method_10608(i).orElse(null);
                        Object object = null;

                        if (!"null".equals(s) && !Strings.isNullOrEmpty(s)) {
                            if ((s.charAt(0) != 34 || s.charAt(s.length() - 1) != 34) && (s.charAt(0) != 123 || s.charAt(s.length() - 1) != 125)) {
                                object = class_2561.method_43470(s);
                            } else {
                                try {
                                    object = class_3518.method_15284(DataConverterSignText.a, s, class_2561.class);
                                    if (object == null) {
                                        object = class_2561.method_43470("");
                                    }
                                } catch (JsonParseException jsonparseexception) {
                                    ;
                                }

                                if (object == null) {
                                    try {
                                        object = ComponentConverter.Serializer.fromJson(s, FabricWorldEdit.registryAccess());
                                    } catch (JsonParseException jsonparseexception1) {
                                        ;
                                    }
                                }

                                if (object == null) {
                                    try {
                                        object = ComponentConverter.Serializer.fromJsonLenient(s, FabricWorldEdit.registryAccess());
                                    } catch (JsonParseException jsonparseexception2) {
                                        ;
                                    }
                                }

                                if (object == null) {
                                    object = class_2561.method_43470(s);
                                }
                            }
                        } else {
                            object = class_2561.method_43470("");
                        }

                        nbttaglist.method_68585(i, class_2519.method_23256(ComponentConverter.Serializer.toJson((class_2561) object, FabricWorldEdit.registryAccess())));
                    }

                    nbttagcompound1.method_10566("pages", nbttaglist);
                });
            }

            return cmp;
        }
    }

    private static class DataConverterCookedFish implements DataConverter {

        private static final class_2960 a = class_2960.method_60654("cooked_fished");

        DataConverterCookedFish() {
        }

        public int getDataVersion() {
            return 502;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if (cmp.method_10558("id").isPresent() && DataConverterCookedFish.a.equals(class_2960.method_60654(cmp.method_10558("id").get()))) {
                cmp.method_10582("id", "minecraft:cooked_fish");
            }

            return cmp;
        }
    }

    private static class DataConverterZombie implements DataConverter {

        private static final Random a = new Random();

        DataConverterZombie() {
        }

        public int getDataVersion() {
            return 502;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("Zombie".equals(cmp.method_10558("id").orElse(null)) && cmp.method_10577("IsVillager").orElse(false)) {
                if (!cmp.method_10545("ZombieType")) {
                    int i = -1;

                    i = cmp.method_10550("VillagerProfession").flatMap(profession -> {
                        try {
                            return Optional.of(this.convert(profession));
                        } catch (RuntimeException runtimeexception) {
                            return Optional.empty();
                        }
                    }).orElse(i);

                    if (i == -1) {
                        i = this.convert(DataConverterZombie.a.nextInt(6));
                    }

                    cmp.method_10569("ZombieType", i);
                }

                cmp.method_10551("IsVillager");
            }

            return cmp;
        }

        private int convert(int i) {
            return i >= 0 && i < 6 ? i : -1;
        }
    }

    private static class DataConverterVBO implements DataConverter {

        DataConverterVBO() {
        }

        public int getDataVersion() {
            return 505;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            cmp.method_10582("useVbo", "true");
            return cmp;
        }
    }

    private static class DataConverterGuardian implements DataConverter {

        DataConverterGuardian() {
        }

        public int getDataVersion() {
            return 700;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("Guardian".equals(cmp.method_10558("id").orElse(null))) {
                if (cmp.method_10577("Elder").orElse(false)) {
                    cmp.method_10582("id", "ElderGuardian");
                }

                cmp.method_10551("Elder");
            }

            return cmp;
        }
    }

    private static class DataConverterSkeleton implements DataConverter {

        DataConverterSkeleton() {
        }

        public int getDataVersion() {
            return 701;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            String s = cmp.method_10558("id").orElse(null);

            if ("Skeleton".equals(s)) {
                int i = cmp.method_68083("SkeletonType", 0);

                if (i == 1) {
                    cmp.method_10582("id", "WitherSkeleton");
                } else if (i == 2) {
                    cmp.method_10582("id", "Stray");
                }

                cmp.method_10551("SkeletonType");
            }

            return cmp;
        }
    }

    private static class DataConverterZombieType implements DataConverter {

        DataConverterZombieType() {
        }

        public int getDataVersion() {
            return 702;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("Zombie".equals(cmp.method_10558("id").orElse(null))) {
                int i = cmp.method_68083("ZombieType", 0);

                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        cmp.method_10582("id", "ZombieVillager");
                        cmp.method_10569("Profession", i - 1);
                        break;
                    case 6:
                        cmp.method_10582("id", "Husk");
                    case 0:
                    default:
                        break;
                }

                cmp.method_10551("ZombieType");
            }

            return cmp;
        }
    }

    private static class DataConverterHorse implements DataConverter {

        DataConverterHorse() {
        }

        public int getDataVersion() {
            return 703;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("EntityHorse".equals(cmp.method_10558("id").orElse(null))) {
                int i = cmp.method_68083("Type", 0);

                switch (i) {
                    case 1:
                        cmp.method_10582("id", "Donkey");
                        break;

                    case 2:
                        cmp.method_10582("id", "Mule");
                        break;

                    case 3:
                        cmp.method_10582("id", "ZombieHorse");
                        break;

                    case 4:
                        cmp.method_10582("id", "SkeletonHorse");
                        break;

                    case 0:
                    default:
                        cmp.method_10582("id", "Horse");
                        break;
                }

                cmp.method_10551("Type");
            }

            return cmp;
        }
    }

    private static class DataConverterTileEntity implements DataConverter {

        private static final Map<String, String> a = Maps.newHashMap();

        DataConverterTileEntity() {
        }

        public int getDataVersion() {
            return 704;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            String s = DataConverterTileEntity.a.get(cmp.method_10558("id").orElse(null));

            if (s != null) {
                cmp.method_10582("id", s);
            }

            return cmp;
        }

        static {
            DataConverterTileEntity.a.put("Airportal", "minecraft:end_portal");
            DataConverterTileEntity.a.put("Banner", "minecraft:banner");
            DataConverterTileEntity.a.put("Beacon", "minecraft:beacon");
            DataConverterTileEntity.a.put("Cauldron", "minecraft:brewing_stand");
            DataConverterTileEntity.a.put("Chest", "minecraft:chest");
            DataConverterTileEntity.a.put("Comparator", "minecraft:comparator");
            DataConverterTileEntity.a.put("Control", "minecraft:command_block");
            DataConverterTileEntity.a.put("DLDetector", "minecraft:daylight_detector");
            DataConverterTileEntity.a.put("Dropper", "minecraft:dropper");
            DataConverterTileEntity.a.put("EnchantTable", "minecraft:enchanting_table");
            DataConverterTileEntity.a.put("EndGateway", "minecraft:end_gateway");
            DataConverterTileEntity.a.put("EnderChest", "minecraft:ender_chest");
            DataConverterTileEntity.a.put("FlowerPot", "minecraft:flower_pot");
            DataConverterTileEntity.a.put("Furnace", "minecraft:furnace");
            DataConverterTileEntity.a.put("Hopper", "minecraft:hopper");
            DataConverterTileEntity.a.put("MobSpawner", "minecraft:mob_spawner");
            DataConverterTileEntity.a.put("Music", "minecraft:noteblock");
            DataConverterTileEntity.a.put("Piston", "minecraft:piston");
            DataConverterTileEntity.a.put("RecordPlayer", "minecraft:jukebox");
            DataConverterTileEntity.a.put("Sign", "minecraft:sign");
            DataConverterTileEntity.a.put("Skull", "minecraft:skull");
            DataConverterTileEntity.a.put("Structure", "minecraft:structure_block");
            DataConverterTileEntity.a.put("Trap", "minecraft:dispenser");
        }
    }

    private static class DataConverterEntity implements DataConverter {

        private static final Map<String, String> a = Maps.newHashMap();

        DataConverterEntity() {
        }

        public int getDataVersion() {
            return 704;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            String s = DataConverterEntity.a.get(cmp.method_10558("id").orElse(null));

            if (s != null) {
                cmp.method_10582("id", s);
            }

            return cmp;
        }

        static {
            DataConverterEntity.a.put("AreaEffectCloud", "minecraft:area_effect_cloud");
            DataConverterEntity.a.put("ArmorStand", "minecraft:armor_stand");
            DataConverterEntity.a.put("Arrow", "minecraft:arrow");
            DataConverterEntity.a.put("Bat", "minecraft:bat");
            DataConverterEntity.a.put("Blaze", "minecraft:blaze");
            DataConverterEntity.a.put("Boat", "minecraft:boat");
            DataConverterEntity.a.put("CaveSpider", "minecraft:cave_spider");
            DataConverterEntity.a.put("Chicken", "minecraft:chicken");
            DataConverterEntity.a.put("Cow", "minecraft:cow");
            DataConverterEntity.a.put("Creeper", "minecraft:creeper");
            DataConverterEntity.a.put("Donkey", "minecraft:donkey");
            DataConverterEntity.a.put("DragonFireball", "minecraft:dragon_fireball");
            DataConverterEntity.a.put("ElderGuardian", "minecraft:elder_guardian");
            DataConverterEntity.a.put("EnderCrystal", "minecraft:ender_crystal");
            DataConverterEntity.a.put("EnderDragon", "minecraft:ender_dragon");
            DataConverterEntity.a.put("Enderman", "minecraft:enderman");
            DataConverterEntity.a.put("Endermite", "minecraft:endermite");
            DataConverterEntity.a.put("EyeOfEnderSignal", "minecraft:eye_of_ender_signal");
            DataConverterEntity.a.put("FallingSand", "minecraft:falling_block");
            DataConverterEntity.a.put("Fireball", "minecraft:fireball");
            DataConverterEntity.a.put("FireworksRocketEntity", "minecraft:fireworks_rocket");
            DataConverterEntity.a.put("Ghast", "minecraft:ghast");
            DataConverterEntity.a.put("Giant", "minecraft:giant");
            DataConverterEntity.a.put("Guardian", "minecraft:guardian");
            DataConverterEntity.a.put("Horse", "minecraft:horse");
            DataConverterEntity.a.put("Husk", "minecraft:husk");
            DataConverterEntity.a.put("Item", "minecraft:item");
            DataConverterEntity.a.put("ItemFrame", "minecraft:item_frame");
            DataConverterEntity.a.put("LavaSlime", "minecraft:magma_cube");
            DataConverterEntity.a.put("LeashKnot", "minecraft:leash_knot");
            DataConverterEntity.a.put("MinecartChest", "minecraft:chest_minecart");
            DataConverterEntity.a.put("MinecartCommandBlock", "minecraft:commandblock_minecart");
            DataConverterEntity.a.put("MinecartFurnace", "minecraft:furnace_minecart");
            DataConverterEntity.a.put("MinecartHopper", "minecraft:hopper_minecart");
            DataConverterEntity.a.put("MinecartRideable", "minecraft:minecart");
            DataConverterEntity.a.put("MinecartSpawner", "minecraft:spawner_minecart");
            DataConverterEntity.a.put("MinecartTNT", "minecraft:tnt_minecart");
            DataConverterEntity.a.put("Mule", "minecraft:mule");
            DataConverterEntity.a.put("MushroomCow", "minecraft:mooshroom");
            DataConverterEntity.a.put("Ozelot", "minecraft:ocelot");
            DataConverterEntity.a.put("Painting", "minecraft:painting");
            DataConverterEntity.a.put("Pig", "minecraft:pig");
            DataConverterEntity.a.put("PigZombie", "minecraft:zombie_pigman");
            DataConverterEntity.a.put("PolarBear", "minecraft:polar_bear");
            DataConverterEntity.a.put("PrimedTnt", "minecraft:tnt");
            DataConverterEntity.a.put("Rabbit", "minecraft:rabbit");
            DataConverterEntity.a.put("Sheep", "minecraft:sheep");
            DataConverterEntity.a.put("Shulker", "minecraft:shulker");
            DataConverterEntity.a.put("ShulkerBullet", "minecraft:shulker_bullet");
            DataConverterEntity.a.put("Silverfish", "minecraft:silverfish");
            DataConverterEntity.a.put("Skeleton", "minecraft:skeleton");
            DataConverterEntity.a.put("SkeletonHorse", "minecraft:skeleton_horse");
            DataConverterEntity.a.put("Slime", "minecraft:slime");
            DataConverterEntity.a.put("SmallFireball", "minecraft:small_fireball");
            DataConverterEntity.a.put("SnowMan", "minecraft:snowman");
            DataConverterEntity.a.put("Snowball", "minecraft:snowball");
            DataConverterEntity.a.put("SpectralArrow", "minecraft:spectral_arrow");
            DataConverterEntity.a.put("Spider", "minecraft:spider");
            DataConverterEntity.a.put("Squid", "minecraft:squid");
            DataConverterEntity.a.put("Stray", "minecraft:stray");
            DataConverterEntity.a.put("ThrownEgg", "minecraft:egg");
            DataConverterEntity.a.put("ThrownEnderpearl", "minecraft:ender_pearl");
            DataConverterEntity.a.put("ThrownExpBottle", "minecraft:xp_bottle");
            DataConverterEntity.a.put("ThrownPotion", "minecraft:potion");
            DataConverterEntity.a.put("Villager", "minecraft:villager");
            DataConverterEntity.a.put("VillagerGolem", "minecraft:villager_golem");
            DataConverterEntity.a.put("Witch", "minecraft:witch");
            DataConverterEntity.a.put("WitherBoss", "minecraft:wither");
            DataConverterEntity.a.put("WitherSkeleton", "minecraft:wither_skeleton");
            DataConverterEntity.a.put("WitherSkull", "minecraft:wither_skull");
            DataConverterEntity.a.put("Wolf", "minecraft:wolf");
            DataConverterEntity.a.put("XPOrb", "minecraft:xp_orb");
            DataConverterEntity.a.put("Zombie", "minecraft:zombie");
            DataConverterEntity.a.put("ZombieHorse", "minecraft:zombie_horse");
            DataConverterEntity.a.put("ZombieVillager", "minecraft:zombie_villager");
        }
    }

    private static class DataConverterPotionWater implements DataConverter {

        DataConverterPotionWater() {
        }

        public int getDataVersion() {
            return 806;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            String s = cmp.method_10558("id").orElse(null);

            if ("minecraft:potion".equals(s) || "minecraft:splash_potion".equals(s) || "minecraft:lingering_potion".equals(s) || "minecraft:tipped_arrow".equals(s)) {
                net.minecraft.class_2487 nbttagcompound1 = cmp.method_68568("tag");

                if (nbttagcompound1.method_10558("Potion").isEmpty()) {
                    nbttagcompound1.method_10582("Potion", "minecraft:water");
                }

                if (cmp.method_10562("tag").isEmpty()) {
                    cmp.method_10566("tag", nbttagcompound1);
                }
            }

            return cmp;
        }
    }

    private static class DataConverterShulker implements DataConverter {

        DataConverterShulker() {
        }

        public int getDataVersion() {
            return 808;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:shulker".equals(cmp.method_10558("id").orElse(null)) && cmp.method_10571("Color").isEmpty()) {
                cmp.method_10567("Color", (byte) 10);
            }

            return cmp;
        }
    }

    private static class DataConverterShulkerBoxItem implements DataConverter {

        public static final String[] a = new String[] { "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box", "minecraft:black_shulker_box" };

        DataConverterShulkerBoxItem() {
        }

        public int getDataVersion() {
            return 813;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:shulker_box".equals(cmp.method_10558("id").orElse(null)) ) {
                cmp.method_10562("tag").ifPresent(nbttagcompound1 -> {
                    nbttagcompound1.method_10562("BlockEntityTag").ifPresent(nbttagcompound2 -> {
                        if (nbttagcompound2.method_10554("Items").map(class_2499::isEmpty).orElse(true)) {
                            nbttagcompound2.method_10551("Items");
                        }

                        int i = nbttagcompound2.method_68083("Color", 0);

                        nbttagcompound2.method_10551("Color");
                        if (nbttagcompound2.method_33133()) {
                            nbttagcompound1.method_10551("BlockEntityTag");
                        }

                        if (nbttagcompound1.method_33133()) {
                            cmp.method_10551("tag");
                        }

                        cmp.method_10582("id", DataConverterShulkerBoxItem.a[i % 16]);
                    });
                });
            }

            return cmp;
        }
    }

    private static class DataConverterShulkerBoxBlock implements DataConverter {

        DataConverterShulkerBoxBlock() {
        }

        public int getDataVersion() {
            return 813;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:shulker".equals(cmp.method_10558("id").orElse(null))) {
                cmp.method_10551("Color");
            }

            return cmp;
        }
    }

    private static class DataConverterLang implements DataConverter {

        DataConverterLang() {
        }

        public int getDataVersion() {
            return 816;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            cmp.method_10558("lang").ifPresent(lang -> {
                cmp.method_10582("lang", lang.toLowerCase(Locale.ROOT));
            });

            return cmp;
        }
    }

    private static class DataConverterTotem implements DataConverter {

        DataConverterTotem() {
        }

        public int getDataVersion() {
            return 820;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:totem".equals(cmp.method_10558("id").orElse(null))) {
                cmp.method_10582("id", "minecraft:totem_of_undying");
            }

            return cmp;
        }
    }

    private static class DataConverterBedBlock implements DataConverter {

        private static final Logger a = LogManager.getLogger(FabricDataFixer.class);

        DataConverterBedBlock() {
        }

        public int getDataVersion() {
            return 1125;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            boolean flag = true;

            try {
                net.minecraft.class_2487 nbttagcompound1 = cmp.method_68568("Level");
                int i = nbttagcompound1.method_68083("xPos", 0);
                int j = nbttagcompound1.method_68083("zPos", 0);
                class_2499 nbttaglist = nbttagcompound1.method_68569("TileEntities");
                class_2499 nbttaglist1 = nbttagcompound1.method_68569("Sections");

                for (int k = 0; k < nbttaglist1.size(); ++k) {
                    net.minecraft.class_2487 nbttagcompound2 = nbttaglist1.method_68582(k);
                    byte b0 = nbttagcompound2.method_68562("Y", (byte) 0);
                    byte[] abyte = nbttagcompound2.method_10547("Blocks").orElse(new byte[]{});

                    for (int l = 0; l < abyte.length; ++l) {
                        if (416 == (abyte[l] & 255) << 4) {
                            int i1 = l & 15;
                            int j1 = l >> 8 & 15;
                            int k1 = l >> 4 & 15;
                            net.minecraft.class_2487 nbttagcompound3 = new net.minecraft.class_2487();

                            nbttagcompound3.method_10582("id", "bed");
                            nbttagcompound3.method_10569("x", i1 + (i << 4));
                            nbttagcompound3.method_10569("y", j1 + (b0 << 4));
                            nbttagcompound3.method_10569("z", k1 + (j << 4));
                            nbttaglist.add(nbttagcompound3);
                        }
                    }
                }
            } catch (Exception exception) {
                DataConverterBedBlock.a.warn("Unable to datafix Bed blocks, level format may be missing tags.");
            }

            return cmp;
        }
    }

    private static class DataConverterBedItem implements DataConverter {

        DataConverterBedItem() {
        }

        public int getDataVersion() {
            return 1125;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("minecraft:bed".equals(cmp.method_10558("id").orElse(null)) && cmp.method_68565("Damage", (short) 0) == 0) {
                cmp.method_10575("Damage", (short) class_1767.field_7964.method_7789());
            }

            return cmp;
        }
    }

    private static class DataConverterSignText implements DataConverter {

        public static final Gson a = new GsonBuilder().registerTypeAdapter(class_2561.class, new JsonDeserializer() {
            class_5250 a(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws JsonParseException {
                if (jsonelement.isJsonPrimitive()) {
                    return class_2561.method_43470(jsonelement.getAsString());
                } else if (jsonelement.isJsonArray()) {
                    JsonArray jsonarray = jsonelement.getAsJsonArray();
                    class_5250 iTextComponent = null;
                    Iterator<JsonElement> iterator = jsonarray.iterator();

                    while (iterator.hasNext()) {
                        JsonElement jsonelement1 = iterator.next();
                        class_5250 iTextComponent1 = this.a(jsonelement1, jsonelement1.getClass(), jsondeserializationcontext);

                        if (iTextComponent == null) {
                            iTextComponent = iTextComponent1;
                        } else {
                            iTextComponent.method_10852(iTextComponent1);
                        }
                    }

                    return iTextComponent;
                } else {
                    throw new JsonParseException("Don't know how to turn " + jsonelement + " into a Component");
                }
            }

            public Object deserialize(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws JsonParseException {
                return this.a(jsonelement, type, jsondeserializationcontext);
            }
        }).create();

        DataConverterSignText() {
        }

        public int getDataVersion() {
            return 101;
        }

        public net.minecraft.class_2487 convert(net.minecraft.class_2487 cmp) {
            if ("Sign".equals(cmp.method_10558("id").orElse(null))) {
                this.convert(cmp, "Text1");
                this.convert(cmp, "Text2");
                this.convert(cmp, "Text3");
                this.convert(cmp, "Text4");
            }

            return cmp;
        }

        private void convert(net.minecraft.class_2487 nbttagcompound, String s) {
            String s1 = nbttagcompound.method_10558(s).orElse(null);
            Object object = null;

            if (!"null".equals(s1) && !Strings.isNullOrEmpty(s1)) {
                if ((s1.charAt(0) != 34 || s1.charAt(s1.length() - 1) != 34) && (s1.charAt(0) != 123 || s1.charAt(s1.length() - 1) != 125)) {
                    object = class_2561.method_43470(s1);
                } else {
                    try {
                        object = class_3518.method_15284(DataConverterSignText.a, s1, class_2561.class);
                        if (object == null) {
                            object = class_2561.method_43470("");
                        }
                    } catch (JsonParseException jsonparseexception) {
                        ;
                    }

                    if (object == null) {
                        try {
                            object = ComponentConverter.Serializer.fromJson(s1, FabricWorldEdit.registryAccess());
                        } catch (JsonParseException jsonparseexception1) {
                            ;
                        }
                    }

                    if (object == null) {
                        try {
                            object = ComponentConverter.Serializer.fromJsonLenient(s1, FabricWorldEdit.registryAccess());
                        } catch (JsonParseException jsonparseexception2) {
                            ;
                        }
                    }

                    if (object == null) {
                        object = class_2561.method_43470(s1);
                    }
                }
            } else {
                object = class_2561.method_43470("");
            }

            nbttagcompound.method_10582(s, ComponentConverter.Serializer.toJson((class_2561) object, FabricWorldEdit.registryAccess()));
        }
    }

    private static class DataInspectorPlayerVehicle implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            cmp.method_10562("RootVehicle").ifPresent(nbttagcompound1 -> {
                if (nbttagcompound1.method_10562("Entity").isPresent()) {
                    convertCompound(LegacyType.ENTITY, nbttagcompound1, "Entity", sourceVer, targetVer);
                }
            });

            return cmp;
        }
    }

    private static class DataInspectorLevelPlayer implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            if (cmp.method_10562("Player").isPresent()) {
                convertCompound(LegacyType.PLAYER, cmp, "Player", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorStructure implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            cmp.method_10554("entities").ifPresent(nbttaglist -> {
                for (int j = 0; j < nbttaglist.size(); ++j) {
                    net.minecraft.class_2487 nbttagcompound1 = (net.minecraft.class_2487) nbttaglist.method_10534(j);
                    if (nbttagcompound1.method_10562("nbt").isPresent()) {
                        convertCompound(LegacyType.ENTITY, nbttagcompound1, "nbt", sourceVer, targetVer);
                    }
                }
            });

            cmp.method_10554("blocks").ifPresent(nbttaglist -> {
                for (int j = 0; j < nbttaglist.size(); ++j) {
                    net.minecraft.class_2487 nbttagcompound1 = (net.minecraft.class_2487) nbttaglist.method_10534(j);
                    if (nbttagcompound1.method_10562("nbt").isPresent()) {
                        convertCompound(LegacyType.BLOCK_ENTITY, nbttagcompound1, "nbt", sourceVer, targetVer);
                    }
                }
            });

            return cmp;
        }
    }

    private static class DataInspectorChunks implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            cmp.method_10562("Level").ifPresent(nbttagcompound1 -> {
                nbttagcompound1.method_10554("Entities").ifPresent(nbttaglist -> {
                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        nbttaglist.method_68585(j, convert(LegacyType.ENTITY, (net.minecraft.class_2487) nbttaglist.method_10534(j), sourceVer, targetVer));
                    }
                });

                nbttagcompound1.method_10554("TileEntities").ifPresent(nbttaglist -> {
                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        nbttaglist.method_68585(j, convert(LegacyType.BLOCK_ENTITY, (net.minecraft.class_2487) nbttaglist.method_10534(j), sourceVer, targetVer));
                    }
                });
            });

            return cmp;
        }
    }

    private static class DataInspectorEntityPassengers implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            cmp.method_10554("Passengers").ifPresent(nbttaglist -> {
                for (int j = 0; j < nbttaglist.size(); ++j) {
                    nbttaglist.method_68585(j, convert(LegacyType.ENTITY, nbttaglist.method_68582(j), sourceVer, targetVer));
                }
            });

            return cmp;
        }
    }

    private static class DataInspectorPlayer implements DataInspector {
        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            convertItems(cmp, "Inventory", sourceVer, targetVer);
            convertItems(cmp, "EnderItems", sourceVer, targetVer);
            if (cmp.method_10562("ShoulderEntityLeft").isPresent()) {
                convertCompound(LegacyType.ENTITY, cmp, "ShoulderEntityLeft", sourceVer, targetVer);
            }

            if (cmp.method_10562("ShoulderEntityRight").isPresent()) {
                convertCompound(LegacyType.ENTITY, cmp, "ShoulderEntityRight", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorVillagers implements DataInspector {
        class_2960 entityVillager = getKey("EntityVillager");

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            if (cmp.method_10558("id").isPresent() && entityVillager.equals(class_2960.method_60654(cmp.method_10558("id").get()))) {
                cmp.method_10562("Offers").flatMap(nbttagcompound1 -> nbttagcompound1.method_10554("Recipes")).ifPresent(nbttaglist -> {
                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        class_2487 nbttagcompound2 = nbttaglist.method_68582(j);

                        convertItem(nbttagcompound2, "buy", sourceVer, targetVer);
                        convertItem(nbttagcompound2, "buyB", sourceVer, targetVer);
                        convertItem(nbttagcompound2, "sell", sourceVer, targetVer);
                        nbttaglist.method_68585(j, nbttagcompound2);
                    }
                });
            }

            return cmp;
        }
    }

    private static class DataInspectorMobSpawnerMinecart implements DataInspector {
        class_2960 entityMinecartMobSpawner = getKey("EntityMinecartMobSpawner");
        class_2960 tileEntityMobSpawner = getKey("TileEntityMobSpawner");

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            String s = cmp.method_10558("id").get();
            if (entityMinecartMobSpawner.equals(class_2960.method_60654(s))) {
                cmp.method_10582("id", tileEntityMobSpawner.toString());
                convert(LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
                cmp.method_10582("id", s);
            }

            return cmp;
        }
    }

    private static class DataInspectorMobSpawnerMobs implements DataInspector {
        class_2960 tileEntityMobSpawner = getKey("TileEntityMobSpawner");

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            if (cmp.method_10558("id").isPresent() && tileEntityMobSpawner.equals(class_2960.method_60654(cmp.method_10558("id").get()))) {
                cmp.method_10554("SpawnPotentials").ifPresent(nbttaglist -> {
                    for (int j = 0; j < nbttaglist.size(); ++j) {
                        net.minecraft.class_2487 nbttagcompound1 = nbttaglist.method_68582(j);

                        convertCompound(LegacyType.ENTITY, nbttagcompound1, "Entity", sourceVer, targetVer);
                    }
                });

                convertCompound(LegacyType.ENTITY, cmp, "SpawnData", sourceVer, targetVer);
            }

            return cmp;
        }
    }

    private static class DataInspectorCommandBlock implements DataInspector {
        class_2960 tileEntityCommand = getKey("TileEntityCommand");

        @Override
        public net.minecraft.class_2487 inspect(net.minecraft.class_2487 cmp, int sourceVer, int targetVer) {
            if (cmp.method_10558("id").isPresent() && tileEntityCommand.equals(class_2960.method_60654(cmp.method_10558("id").get()))) {
                cmp.method_10582("id", "Control");
                convert(LegacyType.BLOCK_ENTITY, cmp, sourceVer, targetVer);
                cmp.method_10582("id", "MinecartCommandBlock");
            }

            return cmp;
        }
    }
}
