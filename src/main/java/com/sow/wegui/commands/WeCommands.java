package com.sow.wegui.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

/**
 * WorldEdit 命令定义与注册表。
 * 合并了命令、用法、参数、分类等数据模型，避免旧代码中类过于分散。
 */
public final class WeCommands {
    private WeCommands() {}

    private static final Map<String, Command> BY_ID = new LinkedHashMap<>();
    private static final Map<Category, List<Command>> BY_CATEGORY = new EnumMap<>(Category.class);
    private static boolean initialized = false;
    private static Category currentCategory;
    private static List<Option> BIOME_OPTIONS;
    private static List<Option> ENTITY_TYPE_OPTIONS;

    public static void init() {
        if (initialized) return;
        initialized = true;

        initBiomeOptions();
        initEntityTypeOptions();

        registerGeneral();
        registerSelection();
        registerRegion();
        registerGeneration();
        registerClipboard();
        registerSuperPickaxe();
        registerTool();
        registerBrush();
        registerNavigation();
        registerBiome();
        registerChunk();
        registerSnapshot();
        registerScript();
        registerUtility();

        for (Category c : Category.values()) {
            BY_CATEGORY.putIfAbsent(c, Collections.emptyList());
        }
    }

    public static Command get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<Command> all() {
        return BY_ID.values();
    }

    public static List<Command> byCategory(Category category) {
        return BY_CATEGORY.getOrDefault(category, List.of());
    }

    // ==================== 数据模型 ====================

    public enum Category {
        GENERAL("wegui.category.general"),
        SELECTION("wegui.category.selection"),
        REGION("wegui.category.region"),
        GENERATION("wegui.category.generation"),
        CLIPBOARD("wegui.category.clipboard"),
        SUPER_PICKAXE("wegui.category.super_pickaxe"),
        TOOL("wegui.category.tool"),
        BRUSH("wegui.category.brush"),
        NAVIGATION("wegui.category.navigation"),
        BIOME("wegui.category.biome"),
        CHUNK("wegui.category.chunk"),
        SNAPSHOT("wegui.category.snapshot"),
        SCRIPT("wegui.category.script"),
        UTILITY("wegui.category.utility");

        private final String translationKey;

        Category(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum Type {
        INSTANT,
        PARAMETRIC,
        BIND
    }

    public enum ParamType {
        INTEGER("wegui.param_type.integer"),
        DECIMAL("wegui.param_type.decimal"),
        STRING("wegui.param_type.string"),
        BOOLEAN("wegui.param_type.boolean"),
        FLAG("wegui.param_type.flag"),
        PATTERN("wegui.param_type.pattern"),
        MASK("wegui.param_type.mask"),
        AXIS("wegui.param_type.axis"),
        FILENAME("wegui.param_type.filename"),
        PLAYER("wegui.param_type.player"),
        ENUM("wegui.param_type.enum");

        private final String translationKey;

        ParamType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public record Command(
            String id,
            String displayName,
            String description,
            Category category,
            List<Usage> usages
    ) {}

    public record Usage(
            String id,
            String displayTemplate,
            String baseCommand,
            String description,
            List<Param> params,
            Type type
    ) {
        public String buildCommand(List<String> values) {
            List<String> parts = new ArrayList<>();
            parts.add(baseCommand);
            for (int i = 0; i < params.size(); i++) {
                String v = i < values.size() ? values.get(i) : "";
                if (v == null || v.isBlank()) continue;
                parts.add(v);
            }
            return String.join(" ", parts);
        }
    }

    public record Param(
            String name,
            ParamType paramType,
            boolean optional,
            String defaultValue,
            String hint,
            String description,
            List<Option> options
    ) {
        public Param(String name, ParamType paramType, String defaultValue) {
            this(name, paramType, false, defaultValue, null, "", List.of());
        }

        public Param(String name, ParamType paramType, String defaultValue, String description) {
            this(name, paramType, false, defaultValue, null, description, List.of());
        }

        public Param(String name, ParamType paramType, String defaultValue, boolean optional, String description) {
            this(name, paramType, optional, defaultValue, null, description, List.of());
        }

        public Param(String name, ParamType paramType, List<Option> options, String defaultValue, boolean optional) {
            this(name, paramType, optional, defaultValue, null, "", List.copyOf(options));
        }

        public Param(String name, ParamType paramType, boolean optional, String defaultValue, String hint) {
            this(name, paramType, optional, defaultValue, hint, "", List.of());
        }

        public Param(String name, ParamType paramType, String defaultValue, boolean optional) {
            this(name, paramType, optional, defaultValue, null, "", List.of());
        }

        public Param(String name, ParamType paramType, String defaultValue, String hint, boolean optional) {
            this(name, paramType, optional, defaultValue, hint, "", List.of());
        }

        public String valueOfLabel(String label) {
            if (label == null) return "";
            for (Option opt : options) {
                if (opt.label().equals(label)) return opt.value();
            }
            return label;
        }
    }

    public record Option(String value, String label, String tooltip) {
        public Option(String value, String label) {
            this(value, label, "");
        }
    }

    // ==================== 注册辅助 ====================

    private static void register(String id, String displayName, String description, Usage... usages) {
        Command cmd = new Command(id, displayName, description, currentCategory, List.of(usages));
        BY_ID.put(id, cmd);
        BY_CATEGORY.computeIfAbsent(currentCategory, k -> new ArrayList<>()).add(cmd);
    }

    private static Usage instant(String id, String command, String description) {
        return new Usage(id, command, command, description, List.of(), Type.INSTANT);
    }

    private static Usage param(String id, String base, String template, String description, Param... params) {
        return new Usage(id, template, base, description, List.of(params), Type.PARAMETRIC);
    }

    private static Usage bind(String id, String base, String template, String description, Param... params) {
        return new Usage(id, template, base, description, List.of(params), Type.BIND);
    }

    // ==================== 常用参数 ====================

    private static final Param PATTERN = new Param("wegui.param.pattern.name", ParamType.PATTERN, "minecraft:stone", "wegui.param.pattern.description");
    private static final Param MASK = new Param("wegui.param.mask.name", ParamType.MASK, "", "wegui.param.mask.description");
    private static final Param FROM_MASK = new Param("wegui.param.from_mask.name", ParamType.MASK, "", "wegui.param.from_mask.description");
    private static final Param TO_PATTERN = new Param("wegui.param.to_pattern.name", ParamType.PATTERN, "", "wegui.param.to_pattern.description");

    private static final List<Option> DIRECTION_OPTIONS = List.of(
            new Option("north", "wegui.option.direction.north.label", "wegui.option.direction.north.tooltip"),
            new Option("south", "wegui.option.direction.south.label", "wegui.option.direction.south.tooltip"),
            new Option("east", "wegui.option.direction.east.label", "wegui.option.direction.east.tooltip"),
            new Option("west", "wegui.option.direction.west.label", "wegui.option.direction.west.tooltip"),
            new Option("up", "wegui.option.direction.up.label", "wegui.option.direction.up.tooltip"),
            new Option("down", "wegui.option.direction.down.label", "wegui.option.direction.down.tooltip"),
            new Option("me", "wegui.option.direction.me.label", "wegui.option.direction.me.tooltip")
    );
    private static final Param DIRECTION = new Param("wegui.param.direction.name", ParamType.ENUM, DIRECTION_OPTIONS, "me", true);

    private static final List<Option> AXIS_XYZ_OPTIONS = List.of(
            new Option("x", "wegui.option.axis.x.label", "wegui.option.axis.x.tooltip"),
            new Option("y", "wegui.option.axis.y.label", "wegui.option.axis.y.tooltip"),
            new Option("z", "wegui.option.axis.z.label", "wegui.option.axis.z.tooltip")
    );
    private static final Param REVOLVE_AXIS = new Param("wegui.param.axis.name", ParamType.ENUM, AXIS_XYZ_OPTIONS, "y", true);

    private static final List<Option> HV_OPTIONS = List.of(
            new Option("", "wegui.option.hv..label", "wegui.option.hv..tooltip"),
            new Option("-h", "wegui.option.hv.-h.label", "wegui.option.hv.-h.tooltip"),
            new Option("-v", "wegui.option.hv.-v.label", "wegui.option.hv.-v.tooltip")
    );
    private static final Param HV_FLAG = new Param("wegui.param.hv_flag.name", ParamType.ENUM, HV_OPTIONS, "", true);

    private static final List<Option> UP_FG_OPTIONS = List.of(
            new Option("", "wegui.option.up_fg..label", "wegui.option.up_fg..tooltip"),
            new Option("-f", "wegui.option.up_fg.-f.label", "wegui.option.up_fg.-f.tooltip"),
            new Option("-g", "wegui.option.up_fg.-g.label", "wegui.option.up_fg.-g.tooltip")
    );
    private static final Param UP_FG = new Param("wegui.param.up_fg.name", ParamType.ENUM, UP_FG_OPTIONS, "", true);

    private static final List<Option> BIOMEINFO_PT_OPTIONS = List.of(
            new Option("", "wegui.option.biomeinfo_pt..label", "wegui.option.biomeinfo_pt..tooltip"),
            new Option("-p", "wegui.option.biomeinfo_pt.-p.label", "wegui.option.biomeinfo_pt.-p.tooltip"),
            new Option("-t", "wegui.option.biomeinfo_pt.-t.label", "wegui.option.biomeinfo_pt.-t.tooltip")
    );
    private static final Param BIOMEINFO_PT = new Param("wegui.param.biomeinfo_pt.name", ParamType.ENUM, BIOMEINFO_PT_OPTIONS, "", true);

    private static final List<Option> FORMAT_OPTIONS = List.of(
            new Option("sponge", "wegui.option.format.sponge.label", "wegui.option.format.sponge.tooltip"),
            new Option("schematic", "wegui.option.format.schematic.label", "wegui.option.format.schematic.tooltip")
    );
    private static final Param FORMAT = new Param("wegui.param.format.name", ParamType.ENUM, FORMAT_OPTIONS, "sponge", true);

    private static final List<Option> TREE_TYPE_OPTIONS = List.of(
            new Option("oak", "wegui.option.tree.oak.label", "wegui.option.tree.oak.tooltip"),
            new Option("oak_checked", "wegui.option.tree.oak_checked.label", "wegui.option.tree.oak_checked.tooltip"),
            new Option("oak_bees_002", "wegui.option.tree.oak_bees_002.label", "wegui.option.tree.oak_bees_002.tooltip"),
            new Option("fancy_oak_bees", "wegui.option.tree.fancy_oak_bees.label", "wegui.option.tree.fancy_oak_bees.tooltip"),
            new Option("fancy_oak_bees_002", "wegui.option.tree.fancy_oak_bees_002.label", "wegui.option.tree.fancy_oak_bees_002.tooltip"),
            new Option("fancy_oak_checked", "wegui.option.tree.fancy_oak_checked.label", "wegui.option.tree.fancy_oak_checked.tooltip"),
            new Option("birch_checked", "wegui.option.tree.birch_checked.label", "wegui.option.tree.birch_checked.tooltip"),
            new Option("birch_bees_0002", "wegui.option.tree.birch_bees_0002.label", "wegui.option.tree.birch_bees_0002.tooltip"),
            new Option("birch_bees_002", "wegui.option.tree.birch_bees_002.label", "wegui.option.tree.birch_bees_002.tooltip"),
            new Option("birch_tall", "wegui.option.tree.birch_tall.label", "wegui.option.tree.birch_tall.tooltip"),
            new Option("super_birch_bees", "wegui.option.tree.super_birch_bees.label", "wegui.option.tree.super_birch_bees.tooltip"),
            new Option("super_birch_bees_0002", "wegui.option.tree.super_birch_bees_0002.label", "wegui.option.tree.super_birch_bees_0002.tooltip"),
            new Option("spruce", "wegui.option.tree.spruce.label", "wegui.option.tree.spruce.tooltip"),
            new Option("spruce_checked", "wegui.option.tree.spruce_checked.label", "wegui.option.tree.spruce_checked.tooltip"),
            new Option("pine", "wegui.option.tree.pine.label", "wegui.option.tree.pine.tooltip"),
            new Option("pine_checked", "wegui.option.tree.pine_checked.label", "wegui.option.tree.pine_checked.tooltip"),
            new Option("mega_spruce_checked", "wegui.option.tree.mega_spruce_checked.label", "wegui.option.tree.mega_spruce_checked.tooltip"),
            new Option("mega_pine_checked", "wegui.option.tree.mega_pine_checked.label", "wegui.option.tree.mega_pine_checked.tooltip"),
            new Option("jungle_tree", "wegui.option.tree.jungle_tree.label", "wegui.option.tree.jungle_tree.tooltip"),
            new Option("jungle_bush", "wegui.option.tree.jungle_bush.label", "wegui.option.tree.jungle_bush.tooltip"),
            new Option("mega_jungle_tree_checked", "wegui.option.tree.mega_jungle_tree_checked.label", "wegui.option.tree.mega_jungle_tree_checked.tooltip"),
            new Option("dark_oak_checked", "wegui.option.tree.dark_oak_checked.label", "wegui.option.tree.dark_oak_checked.tooltip"),
            new Option("acacia", "wegui.option.tree.acacia.label", "wegui.option.tree.acacia.tooltip"),
            new Option("acacia_checked", "wegui.option.tree.acacia_checked.label", "wegui.option.tree.acacia_checked.tooltip"),
            new Option("cherry_bees_005", "wegui.option.tree.cherry_bees_005.label", "wegui.option.tree.cherry_bees_005.tooltip"),
            new Option("cherry_checked", "wegui.option.tree.cherry_checked.label", "wegui.option.tree.cherry_checked.tooltip"),
            new Option("mangrove_checked", "wegui.option.tree.mangrove_checked.label", "wegui.option.tree.mangrove_checked.tooltip"),
            new Option("tall_mangrove_checked", "wegui.option.tree.tall_mangrove_checked.label", "wegui.option.tree.tall_mangrove_checked.tooltip"),
            new Option("pale_oak_checked", "wegui.option.tree.pale_oak_checked.label", "wegui.option.tree.pale_oak_checked.tooltip"),
            new Option("pale_oak_creaking_checked", "wegui.option.tree.pale_oak_creaking_checked.label", "wegui.option.tree.pale_oak_creaking_checked.tooltip"),
            new Option("rooted_azalea_tree", "wegui.option.tree.rooted_azalea_tree.label", "wegui.option.tree.rooted_azalea_tree.tooltip"),
            new Option("chorus_plant", "wegui.option.tree.chorus_plant.label", "wegui.option.tree.chorus_plant.tooltip"),
            new Option("crimson_fungi", "wegui.option.tree.crimson_fungi.label", "wegui.option.tree.crimson_fungi.tooltip"),
            new Option("warped_fungi", "wegui.option.tree.warped_fungi.label", "wegui.option.tree.warped_fungi.tooltip"),
            new Option("fallen_oak_tree", "wegui.option.tree.fallen_oak_tree.label", "wegui.option.tree.fallen_oak_tree.tooltip"),
            new Option("fallen_birch_tree", "wegui.option.tree.fallen_birch_tree.label", "wegui.option.tree.fallen_birch_tree.tooltip"),
            new Option("fallen_spruce_tree", "wegui.option.tree.fallen_spruce_tree.label", "wegui.option.tree.fallen_spruce_tree.tooltip"),
            new Option("fallen_jungle_tree", "wegui.option.tree.fallen_jungle_tree.label", "wegui.option.tree.fallen_jungle_tree.tooltip"),
            new Option("fallen_super_birch_tree", "wegui.option.tree.fallen_super_birch_tree.label", "wegui.option.tree.fallen_super_birch_tree.tooltip")
    );
    private static final Param TREE_TYPE = new Param("wegui.param.tree_type.name", ParamType.ENUM, TREE_TYPE_OPTIONS, "oak", false);
    private static final Param FOREST_TYPE = new Param("wegui.param.forest_type.name", ParamType.ENUM, TREE_TYPE_OPTIONS, "oak", true);

    private static final Param AMOUNT = new Param("wegui.param.amount.name", ParamType.INTEGER, "5");
    private static final Param RADIUS = new Param("wegui.param.radius.name", ParamType.INTEGER, "5");
    private static final Param RANGE = new Param("wegui.param.range.name", ParamType.INTEGER, "5");
    private static final Param SIZE = new Param("wegui.param.size.name", ParamType.INTEGER, "10");
    private static final Param COUNT = new Param("wegui.param.count.name", ParamType.INTEGER, "3", true);
    private static final Param HEIGHT = new Param("wegui.param.height.name", ParamType.INTEGER, "5", true);
    private static final Param ITERATIONS = new Param("wegui.param.iterations.name", ParamType.INTEGER, "1", true);
    private static final Param DISTANCE = new Param("wegui.param.distance.name", ParamType.INTEGER, "5", true);
    private static final Param DEPTH = new Param("wegui.param.depth.name", ParamType.INTEGER, "0", true);
    private static final Param THICKNESS = new Param("wegui.param.thickness.name", ParamType.INTEGER, "1", true);
    private static final Param EXPRESSION = new Param("wegui.param.expression.name", ParamType.STRING, "", "wegui.param.expression.description");
    private static final Param FILENAME = new Param("wegui.param.filename.name", ParamType.FILENAME, "my_build");
    private static final Param PLAYER = new Param("wegui.param.player.name", ParamType.PLAYER, "", true);
    private static final Param SNAPSHOT_NAME = new Param("wegui.param.snapshot_name.name", ParamType.STRING, "2024-01-01-01");
    private static final Param DATE = new Param("wegui.param.date.name", ParamType.STRING, "2024-01-01");
    private static final Param SCRIPT = new Param("wegui.param.script.name", ParamType.STRING, "myscript");
    private static final Param QUERY = new Param("wegui.param.query.name", ParamType.STRING, "", "wegui.param.query.description");
    private static final Param STEPS = new Param("wegui.param.steps.name", ParamType.INTEGER, "1", true);
    private static final Param OFFSET = new Param("wegui.param.offset.name", ParamType.STRING, "", "wegui.param.offset.description", true);
    private static final Param BUTCHER_FLAGS = new Param("wegui.param.butcher_flags.name", ParamType.STRING, "", "wegui.param.butcher_flags.description", true);
    private static final Param DENSITY = new Param("wegui.param.density.name", ParamType.DECIMAL, "5", true);

    // ==================== 分类注册 ====================

    private static void initBiomeOptions() {
        if (BIOME_OPTIONS != null) return;

        List<Option> list = new ArrayList<>();
        try {
            RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            Registry<Biome> biomeRegistry = access.lookupOrThrow(Registries.BIOME);
            for (Biome biome : biomeRegistry) {
                String id = biomeRegistry.getKey(biome).toString();
                list.add(new Option(id, id, id));
            }
        } catch (Throwable ignored) {
            // 注册表尚未准备好时回退到空列表
        }
        list.sort(Comparator.comparing(Option::label));
        BIOME_OPTIONS = list;
    }

    private static void initEntityTypeOptions() {
        if (ENTITY_TYPE_OPTIONS != null) return;

        List<Option> list = new ArrayList<>();
        try {
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                list.add(new Option(id, id, id));
            }
        } catch (Throwable ignored) {
            // 注册表尚未准备好时回退到空列表
        }
        list.sort(Comparator.comparing(Option::label));
        ENTITY_TYPE_OPTIONS = list;
    }

    private static Param biomeParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, BIOME_OPTIONS, defaultValue, optional);
    }

    private static Param entityTypeParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, ENTITY_TYPE_OPTIONS, defaultValue, optional);
    }

    private static void registerGeneral() {
        currentCategory = Category.GENERAL;
        register("undo", "wegui.command.undo.name", "wegui.command.undo.description", param("undo", "/undo", "wegui.command.undo.usage.undo.template", "wegui.command.undo.usage.undo.description", STEPS, PLAYER));
        register("redo", "wegui.command.redo.name", "wegui.command.redo.description", param("redo", "/redo", "wegui.command.redo.usage.redo.template", "wegui.command.redo.usage.redo.description", STEPS, PLAYER));
        register("clearhistory", "wegui.command.clearhistory.name", "wegui.command.clearhistory.description", instant("clearhistory", "/clearhistory", "wegui.command.clearhistory.usage.clearhistory.description"));
        register("limit", "wegui.command.limit.name", "wegui.command.limit.description", param("limit", "//limit", "wegui.command.limit.usage.limit.template", "wegui.command.limit.usage.limit.description", AMOUNT));
        register("timeout", "wegui.command.timeout.name", "wegui.command.timeout.description", param("timeout", "//timeout", "wegui.command.timeout.usage.timeout.template", "wegui.command.timeout.usage.timeout.description", AMOUNT));
        register("perf", "wegui.command.perf.name", "wegui.command.perf.description", instant("perf", "//perf", "wegui.command.perf.usage.perf.description"));
        register("reorder", "wegui.command.reorder.name", "wegui.command.reorder.description",
                param("reorder", "//reorder", "wegui.command.reorder.usage.reorder.template", "wegui.command.reorder.usage.reorder.description",
                        new Param("wegui.command.reorder.param.0.name", ParamType.ENUM, List.of(
                                new Option("fast", "wegui.command.reorder.option.0.label", "wegui.command.reorder.option.0.tooltip"),
                                new Option("multi", "wegui.command.reorder.option.1.label", "wegui.command.reorder.option.1.tooltip"),
                                new Option("none", "wegui.command.reorder.option.2.label", "wegui.command.reorder.option.2.tooltip")
                        ), "fast", false)));
        register("drawsel", "wegui.command.drawsel.name", "wegui.command.drawsel.description", instant("drawsel", "//drawsel", "wegui.command.drawsel.usage.drawsel.description"));
        register("gmask", "wegui.command.gmask.name", "wegui.command.gmask.description",
                param("gmask_set", "/gmask", "wegui.command.gmask.usage.gmask_set.template", "wegui.command.gmask.usage.gmask_set.description", MASK),
                instant("gmask_clear", "/gmask", "wegui.command.gmask.usage.gmask_clear.description"));
        register("toggleplace", "wegui.command.toggleplace.name", "wegui.command.toggleplace.description", instant("toggleplace", "/toggleplace", "wegui.command.toggleplace.usage.toggleplace.description"));
        register("searchitem", "wegui.command.searchitem.name", "wegui.command.searchitem.description", param("searchitem", "/searchitem", "wegui.command.searchitem.usage.searchitem.template", "wegui.command.searchitem.usage.searchitem.description", QUERY));
        register("toggleeditwand", "wegui.command.toggleeditwand.name", "wegui.command.toggleeditwand.description", instant("toggleeditwand", "/toggleeditwand", "wegui.command.toggleeditwand.usage.toggleeditwand.description"));
        register("fast", "wegui.command.fast.name", "wegui.command.fast.description", instant("fast", "//fast", "wegui.command.fast.usage.fast.description"));
        register("we_reload", "wegui.command.we_reload.name", "wegui.command.we_reload.description", instant("we_reload", "//worldedit reload", "wegui.command.we_reload.usage.we_reload.description"));
        register("we_version", "wegui.command.we_version.name", "wegui.command.we_version.description", instant("we_version", "//worldedit version", "wegui.command.we_version.usage.we_version.description"));
        register("we_tz", "wegui.command.we_tz.name", "wegui.command.we_tz.description", param("we_tz", "//worldedit tz", "wegui.command.we_tz.usage.we_tz.template", "wegui.command.we_tz.usage.we_tz.description", new Param("wegui.command.we_tz.param.0.name", ParamType.STRING, "UTC")));
        register("we_help", "wegui.command.we_help.name", "wegui.command.we_help.description", param("we_help", "/worldedit help", "wegui.command.we_help.usage.we_help.template", "wegui.command.we_help.usage.we_help.description", new Param("wegui.command.we_help.param.0.name", ParamType.STRING, "set", true)));
    }

    private static void registerSelection() {
        currentCategory = Category.SELECTION;
        register("wand", "wegui.command.wand.name", "wegui.command.wand.description", instant("wand", "//wand", "wegui.command.wand.usage.wand.description"));
        register("toggleeditwand_sel", "wegui.command.toggleeditwand_sel.name", "wegui.command.toggleeditwand_sel.description", instant("toggleeditwand_sel", "/toggleeditwand", "wegui.command.toggleeditwand_sel.usage.toggleeditwand_sel.description"));
        register("sel", "wegui.command.sel.name", "wegui.command.sel.description",
                param("sel", "//sel", "wegui.command.sel.usage.sel.template", "wegui.command.sel.usage.sel.description",
                        new Param("wegui.command.sel.param.0.name", ParamType.ENUM, List.of(
                                new Option("cuboid", "wegui.command.sel.option.0.label", "wegui.command.sel.option.0.tooltip"),
                                new Option("extend", "wegui.command.sel.option.1.label", "wegui.command.sel.option.1.tooltip"),
                                new Option("poly", "wegui.command.sel.option.2.label", "wegui.command.sel.option.2.tooltip"),
                                new Option("ellipsoid", "wegui.command.sel.option.3.label", "wegui.command.sel.option.3.tooltip"),
                                new Option("sphere", "wegui.command.sel.option.4.label", "wegui.command.sel.option.4.tooltip"),
                                new Option("cyl", "wegui.command.sel.option.5.label", "wegui.command.sel.option.5.tooltip"),
                                new Option("convex", "wegui.command.sel.option.6.label", "wegui.command.sel.option.6.tooltip")
                        ), "cuboid", false)));
        register("desel", "wegui.command.desel.name", "wegui.command.desel.description", instant("desel", "//desel", "wegui.command.desel.usage.desel.description"));
        register("pos1", "wegui.command.pos1.name", "wegui.command.pos1.description", instant("pos1", "//pos1", "wegui.command.pos1.usage.pos1.description"));
        register("pos2", "wegui.command.pos2.name", "wegui.command.pos2.description", instant("pos2", "//pos2", "wegui.command.pos2.usage.pos2.description"));
        register("hpos1", "wegui.command.hpos1.name", "wegui.command.hpos1.description", instant("hpos1", "//hpos1", "wegui.command.hpos1.usage.hpos1.description"));
        register("hpos2", "wegui.command.hpos2.name", "wegui.command.hpos2.description", instant("hpos2", "//hpos2", "wegui.command.hpos2.usage.hpos2.description"));
        register("chunk", "wegui.command.chunk.name", "wegui.command.chunk.description", instant("chunk", "//chunk", "wegui.command.chunk.usage.chunk.description"));
        register("expand", "wegui.command.expand.name", "wegui.command.expand.description",
                param("expand_dir", "//expand", "wegui.command.expand.usage.expand_dir.template", "wegui.command.expand.usage.expand_dir.description", AMOUNT, DIRECTION),
                param("expand_vert", "//expand", "wegui.command.expand.usage.expand_vert.template", "wegui.command.expand.usage.expand_vert.description", AMOUNT));
        register("contract", "wegui.command.contract.name", "wegui.command.contract.description",
                param("contract_dir", "//contract", "wegui.command.contract.usage.contract_dir.template", "wegui.command.contract.usage.contract_dir.description", AMOUNT, DIRECTION),
                param("contract_vert", "//contract", "wegui.command.contract.usage.contract_vert.template", "wegui.command.contract.usage.contract_vert.description", AMOUNT));
        register("shift", "wegui.command.shift.name", "wegui.command.shift.description", param("shift", "//shift", "wegui.command.shift.usage.shift.template", "wegui.command.shift.usage.shift.description", AMOUNT, DIRECTION));
        register("outset", "wegui.command.outset.name", "wegui.command.outset.description", param("outset", "//outset", "wegui.command.outset.usage.outset.template", "wegui.command.outset.usage.outset.description", HV_FLAG, AMOUNT));
        register("inset", "wegui.command.inset.name", "wegui.command.inset.description", param("inset", "//inset", "wegui.command.inset.usage.inset.template", "wegui.command.inset.usage.inset.description", HV_FLAG, AMOUNT));
        register("size", "wegui.command.size.name", "wegui.command.size.description", instant("size", "//size", "wegui.command.size.usage.size.description"));
        register("count", "wegui.command.count.name", "wegui.command.count.description",
                param("count", "//count", "wegui.command.count.usage.count.template", "wegui.command.count.usage.count.description", new Param("wegui.command.count.param.0.name", ParamType.FLAG, "-d", true), MASK));
        register("distr", "wegui.command.distr.name", "wegui.command.distr.description",
                param("distr", "//distr", "//distr [-c]", "wegui.command.distr.usage.distr.description", new Param("wegui.command.distr.param.0.name", ParamType.FLAG, "-c", true)));
        register("seltoggle", "wegui.command.seltoggle.name", "wegui.command.seltoggle.description", instant("seltoggle", "//sel toggle", "wegui.command.seltoggle.usage.seltoggle.description"));
    }

    private static void registerRegion() {
        currentCategory = Category.REGION;
        register("set", "wegui.command.set.name", "wegui.command.set.description", param("set", "//set", "wegui.command.set.usage.set.template", "wegui.command.set.usage.set.description", PATTERN));
        register("replace", "wegui.command.replace.name", "wegui.command.replace.description",
                param("replace", "//replace", "wegui.command.replace.usage.replace.template", "wegui.command.replace.usage.replace.description", FROM_MASK, TO_PATTERN),
                param("replace_all", "//replace", "wegui.command.replace.usage.replace_all.template", "wegui.command.replace.usage.replace_all.description", TO_PATTERN));
        register("overlay", "wegui.command.overlay.name", "wegui.command.overlay.description", param("overlay", "//overlay", "wegui.command.overlay.usage.overlay.template", "wegui.command.overlay.usage.overlay.description", PATTERN));
        register("walls", "wegui.command.walls.name", "wegui.command.walls.description", param("walls", "//walls", "wegui.command.walls.usage.walls.template", "wegui.command.walls.usage.walls.description", PATTERN));
        register("outline", "wegui.command.outline.name", "wegui.command.outline.description", param("outline", "//outline", "wegui.command.outline.usage.outline.template", "wegui.command.outline.usage.outline.description", PATTERN));
        register("smooth", "wegui.command.smooth.name", "wegui.command.smooth.description", param("smooth", "//smooth", "wegui.command.smooth.usage.smooth.template", "wegui.command.smooth.usage.smooth.description", ITERATIONS));
        register("naturalize", "wegui.command.naturalize.name", "wegui.command.naturalize.description", instant("naturalize", "//naturalize", "wegui.command.naturalize.usage.naturalize.description"));
        register("move", "wegui.command.move.name", "wegui.command.move.description",
                param("move", "//move", "wegui.command.move.usage.move.template", "wegui.command.move.usage.move.description", AMOUNT, DIRECTION, new Param("wegui.command.move.param.0.name", ParamType.FLAG, "-s", true)));
        register("stack", "wegui.command.stack.name", "wegui.command.stack.description",
                param("stack", "//stack", "wegui.command.stack.usage.stack.template", "wegui.command.stack.usage.stack.description", COUNT, DIRECTION,
                        new Param("wegui.command.stack.param.0.name", ParamType.FLAG, "-s", true)));
        register("regen", "wegui.command.regen.name", "wegui.command.regen.description", instant("regen", "//regen", "wegui.command.regen.usage.regen.description"));
        register("deform", "wegui.command.deform.name", "wegui.command.deform.description", param("deform", "//deform", "wegui.command.deform.usage.deform.template", "wegui.command.deform.usage.deform.description", EXPRESSION));
        register("hollow", "wegui.command.hollow.name", "wegui.command.hollow.description",
                param("hollow", "//hollow", "wegui.command.hollow.usage.hollow.template", "wegui.command.hollow.usage.hollow.description", THICKNESS, new Param("wegui.command.hollow.param.0.name", ParamType.PATTERN, "air", true)));
        register("line", "wegui.command.line.name", "wegui.command.line.description",
                param("line", "//line", "wegui.command.line.usage.line.template", "wegui.command.line.usage.line.description", new Param("wegui.command.line.param.0.name", ParamType.FLAG, "-h", true), PATTERN, THICKNESS));
        register("curve", "wegui.command.curve.name", "wegui.command.curve.description",
                param("curve", "//curve", "wegui.command.curve.usage.curve.template", "wegui.command.curve.usage.curve.description", new Param("wegui.command.curve.param.0.name", ParamType.FLAG, "-h", true), PATTERN, THICKNESS));
    }

    private static void registerGeneration() {
        currentCategory = Category.GENERATION;
        register("generate", "wegui.command.generate.name", "wegui.command.generate.description",
                param("generate", "//generate", "wegui.command.generate.usage.generate.template", "wegui.command.generate.usage.generate.description",
                        new Param("wegui.command.generate.param.0.name", ParamType.STRING, "", "wegui.command.generate.param.0.description", true), PATTERN, EXPRESSION));
        register("cyl", "wegui.command.cyl.name", "wegui.command.cyl.description", param("cyl", "//cyl", "wegui.command.cyl.usage.cyl.template", "wegui.command.cyl.usage.cyl.description", new Param("wegui.command.cyl.param.0.name", ParamType.FLAG, "-h", true), PATTERN, RADIUS, HEIGHT));
        register("hcyl", "wegui.command.hcyl.name", "wegui.command.hcyl.description", param("hcyl", "//hcyl", "wegui.command.hcyl.usage.hcyl.template", "wegui.command.hcyl.usage.hcyl.description", PATTERN, RADIUS, HEIGHT));
        register("sphere", "wegui.command.sphere.name", "wegui.command.sphere.description",
                param("sphere", "//sphere", "wegui.command.sphere.usage.sphere.template", "wegui.command.sphere.usage.sphere.description",
                        new Param("wegui.command.sphere.param.0.name", ParamType.FLAG, "-r", true),
                        new Param("wegui.command.sphere.param.1.name", ParamType.FLAG, "-h", true), PATTERN, RADIUS));
        register("hsphere", "wegui.command.hsphere.name", "wegui.command.hsphere.description",
                param("hsphere", "//hsphere", "wegui.command.hsphere.usage.hsphere.template", "wegui.command.hsphere.usage.hsphere.description",
                        new Param("wegui.command.hsphere.param.0.name", ParamType.FLAG, "-r", true), PATTERN, RADIUS));
        register("pyramid", "wegui.command.pyramid.name", "wegui.command.pyramid.description", param("pyramid", "//pyramid", "wegui.command.pyramid.usage.pyramid.template", "wegui.command.pyramid.usage.pyramid.description", PATTERN, SIZE));
        register("hpyramid", "wegui.command.hpyramid.name", "wegui.command.hpyramid.description", param("hpyramid", "//hpyramid", "wegui.command.hpyramid.usage.hpyramid.template", "wegui.command.hpyramid.usage.hpyramid.description", PATTERN, SIZE));
        register("cone", "wegui.command.cone.name", "wegui.command.cone.description", param("cone", "//cone", "wegui.command.cone.usage.cone.template", "wegui.command.cone.usage.cone.description", PATTERN, RADIUS, HEIGHT));
        register("forest", "wegui.command.forest.name", "wegui.command.forest.description",
                param("forest", "//forest", "wegui.command.forest.usage.forest.template", "wegui.command.forest.usage.forest.description", FOREST_TYPE, DENSITY));
        register("pumpkins", "wegui.command.pumpkins.name", "wegui.command.pumpkins.description", instant("pumpkins", "//pumpkins", "wegui.command.pumpkins.usage.pumpkins.description"));
        register("flora", "wegui.command.flora.name", "wegui.command.flora.description", instant("flora", "//flora", "wegui.command.flora.usage.flora.description"));
        register("caves", "wegui.command.caves.name", "wegui.command.caves.description", instant("caves", "//caves", "wegui.command.caves.usage.caves.description"));
        register("ore", "wegui.command.ore.name", "wegui.command.ore.description", param("ore", "//ore", "wegui.command.ore.usage.ore.template", "wegui.command.ore.usage.ore.description", PATTERN, SIZE));
        register("hkaleidoscope", "wegui.command.hkaleidoscope.name", "wegui.command.hkaleidoscope.description", instant("hkaleidoscope", "//hkaleidoscope", "wegui.command.hkaleidoscope.usage.hkaleidoscope.description"));
    }

    private static void registerClipboard() {
        currentCategory = Category.CLIPBOARD;
        register("copy", "wegui.command.copy.name", "wegui.command.copy.description", instant("copy", "//copy", "wegui.command.copy.usage.copy.description"), instant("copy_entities", "//copy -e", "wegui.command.copy.usage.copy_entities.description"));
        register("cut", "wegui.command.cut.name", "wegui.command.cut.description", instant("cut", "//cut", "wegui.command.cut.usage.cut.description"), instant("cut_entities", "//cut -e", "wegui.command.cut.usage.cut_entities.description"));
        register("paste", "wegui.command.paste.name", "wegui.command.paste.description",
                param("paste", "//paste", "//paste [-a] [-s] [-o]", "wegui.command.paste.usage.paste.description",
                        new Param("wegui.command.paste.param.0.name", ParamType.FLAG, "-a", true),
                        new Param("wegui.command.paste.param.1.name", ParamType.FLAG, "-s", true),
                        new Param("wegui.command.paste.param.2.name", ParamType.FLAG, "-o", true)));
        register("rotate", "wegui.command.rotate.name", "wegui.command.rotate.description", param("rotate", "//rotate", "wegui.command.rotate.usage.rotate.template", "wegui.command.rotate.usage.rotate.description", new Param("wegui.command.rotate.param.0.name", ParamType.INTEGER, "90")));
        register("flip", "wegui.command.flip.name", "wegui.command.flip.description", param("flip", "//flip", "wegui.command.flip.usage.flip.template", "wegui.command.flip.usage.flip.description", DIRECTION));
        register("clearclipboard", "wegui.command.clearclipboard.name", "wegui.command.clearclipboard.description", instant("clearclipboard", "//clearclipboard", "wegui.command.clearclipboard.usage.clearclipboard.description"));
    }

    private static void registerSuperPickaxe() {
        currentCategory = Category.SUPER_PICKAXE;
        register("sp_single", "wegui.command.sp_single.name", "wegui.command.sp_single.description", instant("sp_single", "//sp single", "wegui.command.sp_single.usage.sp_single.description"));
        register("sp_area", "wegui.command.sp_area.name", "wegui.command.sp_area.description", param("sp_area", "//sp area", "wegui.command.sp_area.usage.sp_area.template", "wegui.command.sp_area.usage.sp_area.description", RANGE));
        register("sp_recursive", "wegui.command.sp_recursive.name", "wegui.command.sp_recursive.description", param("sp_recursive", "//sp recursive", "wegui.command.sp_recursive.usage.sp_recursive.template", "wegui.command.sp_recursive.usage.sp_recursive.description", RANGE));
        register("sp_none", "wegui.command.sp_none.name", "wegui.command.sp_none.description", instant("sp_none", "//sp none", "wegui.command.sp_none.usage.sp_none.description"));
    }

    private static void registerTool() {
        currentCategory = Category.TOOL;
        register("none", "wegui.command.none.name", "wegui.command.none.description", instant("none", "//none", "wegui.command.none.usage.none.description"));
        register("farwand", "wegui.command.farwand.name", "wegui.command.farwand.description", instant("farwand", "//farwand", "wegui.command.farwand.usage.farwand.description"));
        register("lrbuild", "wegui.command.lrbuild.name", "wegui.command.lrbuild.description", instant("lrbuild", "//lrbuild", "wegui.command.lrbuild.usage.lrbuild.description"));
        register("tree", "wegui.command.tree.name", "wegui.command.tree.description", bind("tree", "/tool tree", "wegui.command.tree.usage.tree.template", "wegui.command.tree.usage.tree.description", TREE_TYPE));
        register("deltree", "wegui.command.deltree.name", "wegui.command.deltree.description", bindNoParam("deltree", "/tool deltree", "wegui.command.deltree.usage.deltree.description"));
        register("repl", "wegui.command.repl.name", "wegui.command.repl.description", bind("repl", "/tool repl", "wegui.command.repl.usage.repl.template", "wegui.command.repl.usage.repl.description", TO_PATTERN));
        register("cycler", "wegui.command.cycler.name", "wegui.command.cycler.description", bindNoParam("cycler", "/tool cycler", "wegui.command.cycler.usage.cycler.description"));
        register("floodfill", "wegui.command.floodfill.name", "wegui.command.floodfill.description",
                bind("floodfill", "/tool floodfill", "wegui.command.floodfill.usage.floodfill.template", "wegui.command.floodfill.usage.floodfill.description", PATTERN, RANGE));
        register("info", "wegui.command.info.name", "wegui.command.info.description", bindNoParam("info", "/tool info", "wegui.command.info.usage.info.description"));
        register("selwand", "wegui.command.selwand.name", "wegui.command.selwand.description", bindNoParam("selwand", "/tool selwand", "wegui.command.selwand.usage.selwand.description"));
    }

    private static void registerBrush() {
        currentCategory = Category.BRUSH;
        register("brush_sphere", "wegui.command.brush_sphere.name", "wegui.command.brush_sphere.description",
                bind("brush_sphere", "/brush sphere", "wegui.command.brush_sphere.usage.brush_sphere.template", "wegui.command.brush_sphere.usage.brush_sphere.description", PATTERN, RADIUS));
        register("brush_cylinder", "wegui.command.brush_cylinder.name", "wegui.command.brush_cylinder.description",
                bind("brush_cylinder", "/brush cylinder", "wegui.command.brush_cylinder.usage.brush_cylinder.template", "wegui.command.brush_cylinder.usage.brush_cylinder.description", PATTERN, RADIUS, HEIGHT));
        register("brush_clipboard", "wegui.command.brush_clipboard.name", "wegui.command.brush_clipboard.description",
                bind("brush_clipboard", "/brush clipboard", "/brush clipboard [-a] [-p]", "wegui.command.brush_clipboard.usage.brush_clipboard.description",
                        new Param("wegui.command.brush_clipboard.param.0.name", ParamType.FLAG, "-a", true),
                        new Param("wegui.command.brush_clipboard.param.1.name", ParamType.FLAG, "-p", true)));
        register("brush_smooth", "wegui.command.brush_smooth.name", "wegui.command.brush_smooth.description",
                bind("brush_smooth", "/brush smooth", "wegui.command.brush_smooth.usage.brush_smooth.template", "wegui.command.brush_smooth.usage.brush_smooth.description", ITERATIONS, RADIUS));
        register("brush_gravity", "wegui.command.brush_gravity.name", "wegui.command.brush_gravity.description",
                bind("brush_gravity", "/brush gravity", "wegui.command.brush_gravity.usage.brush_gravity.template", "wegui.command.brush_gravity.usage.brush_gravity.description", RADIUS));
        register("brush_butcher", "wegui.command.brush_butcher.name", "wegui.command.brush_butcher.description",
                bind("brush_butcher", "/brush butcher", "wegui.command.brush_butcher.usage.brush_butcher.template", "wegui.command.brush_butcher.usage.brush_butcher.description", BUTCHER_FLAGS, RADIUS));
        register("brush_eraser", "wegui.command.brush_eraser.name", "wegui.command.brush_eraser.description", bindNoParam("brush_eraser", "/brush eraser", "wegui.command.brush_eraser.usage.brush_eraser.description"));
        register("brush_forest", "wegui.command.brush_forest.name", "wegui.command.brush_forest.description",
                bind("brush_forest", "/brush forest", "wegui.command.brush_forest.usage.brush_forest.template", "wegui.command.brush_forest.usage.brush_forest.description", FOREST_TYPE, RADIUS));
        register("brush_raise", "wegui.command.brush_raise.name", "wegui.command.brush_raise.description",
                bind("brush_raise", "/brush raise", "wegui.command.brush_raise.usage.brush_raise.template", "wegui.command.brush_raise.usage.brush_raise.description", RADIUS));
        register("brush_lower", "wegui.command.brush_lower.name", "wegui.command.brush_lower.description",
                bind("brush_lower", "/brush lower", "wegui.command.brush_lower.usage.brush_lower.template", "wegui.command.brush_lower.usage.brush_lower.description", RADIUS));
    }

    private static Usage bindNoParam(String id, String command, String description) {
        return new Usage(id, command, command, description, List.of(), Type.BIND);
    }

    private static void registerNavigation() {
        currentCategory = Category.NAVIGATION;
        register("unstuck", "wegui.command.unstuck.name", "wegui.command.unstuck.description", instant("unstuck", "/unstuck", "wegui.command.unstuck.usage.unstuck.description"));
        register("ascend", "wegui.command.ascend.name", "wegui.command.ascend.description", param("ascend", "/ascend", "wegui.command.ascend.usage.ascend.template", "wegui.command.ascend.usage.ascend.description", COUNT));
        register("descend", "wegui.command.descend.name", "wegui.command.descend.description", param("descend", "/descend", "wegui.command.descend.usage.descend.template", "wegui.command.descend.usage.descend.description", COUNT));
        register("ceil", "wegui.command.ceil.name", "wegui.command.ceil.description", param("ceil", "/ceil", "wegui.command.ceil.usage.ceil.template", "wegui.command.ceil.usage.ceil.description", new Param("wegui.command.ceil.param.0.name", ParamType.INTEGER, "0", true)));
        register("thru", "wegui.command.thru.name", "wegui.command.thru.description", instant("thru", "/thru", "wegui.command.thru.usage.thru.description"));
        register("jumpto", "wegui.command.jumpto.name", "wegui.command.jumpto.description", instant("jumpto", "/jumpto", "wegui.command.jumpto.usage.jumpto.description"));
        register("up", "wegui.command.up.name", "wegui.command.up.description",
                param("up", "/up", "wegui.command.up.usage.up.template", "wegui.command.up.usage.up.description", UP_FG, AMOUNT));
    }

    private static void registerBiome() {
        currentCategory = Category.BIOME;
        register("setbiome", "wegui.command.setbiome.name", "wegui.command.setbiome.description",
                param("setbiome", "//setbiome", "wegui.command.setbiome.usage.setbiome.template", "wegui.command.setbiome.usage.setbiome.description",
                        new Param("wegui.command.setbiome.param.1.name", ParamType.FLAG, "-p", true), biomeParam("wegui.command.setbiome.param.0.name", "minecraft:plains", false)));
        register("biomeinfo", "wegui.command.biomeinfo.name", "wegui.command.biomeinfo.description",
                param("biomeinfo", "/biomeinfo", "/biomeinfo [-pt]", "wegui.command.biomeinfo.usage.biomeinfo.description", BIOMEINFO_PT));
        register("replacebiome", "wegui.command.replacebiome.name", "wegui.command.replacebiome.description",
                param("replacebiome", "//replacebiome", "wegui.command.replacebiome.usage.replacebiome.template", "wegui.command.replacebiome.usage.replacebiome.description",
                        biomeParam("wegui.command.replacebiome.param.0.name", "minecraft:plains", false),
                        biomeParam("wegui.command.replacebiome.param.1.name", "minecraft:forest", false)));
    }

    private static void registerChunk() {
        currentCategory = Category.CHUNK;
        register("chunkinfo", "wegui.command.chunkinfo.name", "wegui.command.chunkinfo.description", instant("chunkinfo", "//chunkinfo", "wegui.command.chunkinfo.usage.chunkinfo.description"));
        register("listchunks", "wegui.command.listchunks.name", "wegui.command.listchunks.description", instant("listchunks", "//listchunks", "wegui.command.listchunks.usage.listchunks.description"));
        register("delchunks", "wegui.command.delchunks.name", "wegui.command.delchunks.description", instant("delchunks", "//delchunks", "wegui.command.delchunks.usage.delchunks.description"));
    }

    private static void registerSnapshot() {
        currentCategory = Category.SNAPSHOT;
        register("snapshot_use", "wegui.command.snapshot_use.name", "wegui.command.snapshot_use.description", param("snapshot_use", "//snapshot use", "wegui.command.snapshot_use.usage.snapshot_use.template", "wegui.command.snapshot_use.usage.snapshot_use.description", SNAPSHOT_NAME));
        register("snapshot_list", "wegui.command.snapshot_list.name", "wegui.command.snapshot_list.description", instant("snapshot_list", "//snapshot list", "wegui.command.snapshot_list.usage.snapshot_list.description"));
        register("snapshot_sel", "wegui.command.snapshot_sel.name", "wegui.command.snapshot_sel.description", param("snapshot_sel", "//snapshot sel", "wegui.command.snapshot_sel.usage.snapshot_sel.template", "wegui.command.snapshot_sel.usage.snapshot_sel.description", DATE));
        register("restore", "wegui.command.restore.name", "wegui.command.restore.description", instant("restore", "//restore", "wegui.command.restore.usage.restore.description"));
    }

    private static void registerScript() {
        currentCategory = Category.SCRIPT;
        register("cs", "wegui.command.cs.name", "wegui.command.cs.description", param("cs", "/cs", "wegui.command.cs.usage.cs.template", "wegui.command.cs.usage.cs.description", SCRIPT));
        register("load", "wegui.command.load.name", "wegui.command.load.description", param("load", "//load", "wegui.command.load.usage.load.template", "wegui.command.load.usage.load.description", SCRIPT));
    }

    private static void registerUtility() {
        currentCategory = Category.UTILITY;
        register("calc", "wegui.command.calc.name", "wegui.command.calc.description", param("calc", "//calc", "wegui.command.calc.usage.calc.template", "wegui.command.calc.usage.calc.description", EXPRESSION));
        register("remove", "wegui.command.remove.name", "wegui.command.remove.description", param("remove", "/remove", "wegui.command.remove.usage.remove.template", "wegui.command.remove.usage.remove.description", entityTypeParam("wegui.command.remove.param.0.name", "minecraft:pig", false), RANGE));
        register("butcher", "wegui.command.butcher.name", "wegui.command.butcher.description", param("butcher", "/butcher", "wegui.command.butcher.usage.butcher.template", "wegui.command.butcher.usage.butcher.description", BUTCHER_FLAGS, RADIUS));
        register("extinguish", "wegui.command.extinguish.name", "wegui.command.extinguish.description", param("extinguish", "/ex", "wegui.command.extinguish.usage.extinguish.template", "wegui.command.extinguish.usage.extinguish.description", RADIUS));
        register("green", "wegui.command.green.name", "wegui.command.green.description",
                param("green", "/green", "wegui.command.green.usage.green.template", "wegui.command.green.usage.green.description",
                        new Param("wegui.command.green.param.0.name", ParamType.FLAG, "-f", true), RADIUS));
        register("drain", "wegui.command.drain.name", "wegui.command.drain.description", param("drain", "/drain", "wegui.command.drain.usage.drain.template", "wegui.command.drain.usage.drain.description", RADIUS));
        register("replacenear", "wegui.command.replacenear.name", "wegui.command.replacenear.description",
                param("replacenear", "/replacenear", "wegui.command.replacenear.usage.replacenear.template", "wegui.command.replacenear.usage.replacenear.description",
                        new Param("wegui.command.replacenear.param.0.name", ParamType.FLAG, "-f", true), RANGE, FROM_MASK, TO_PATTERN));
        register("snow", "wegui.command.snow.name", "wegui.command.snow.description", param("snow", "/snow", "wegui.command.snow.usage.snow.template", "wegui.command.snow.usage.snow.description", RADIUS));
        register("thaw", "wegui.command.thaw.name", "wegui.command.thaw.description", param("thaw", "/thaw", "wegui.command.thaw.usage.thaw.template", "wegui.command.thaw.usage.thaw.description", RADIUS));
        register("fixwater", "wegui.command.fixwater.name", "wegui.command.fixwater.description", param("fixwater", "/fixwater", "wegui.command.fixwater.usage.fixwater.template", "wegui.command.fixwater.usage.fixwater.description", RADIUS));
        register("fixlava", "wegui.command.fixlava.name", "wegui.command.fixlava.description", param("fixlava", "/fixlava", "wegui.command.fixlava.usage.fixlava.template", "wegui.command.fixlava.usage.fixlava.description", RADIUS));
        register("fillr", "wegui.command.fillr.name", "wegui.command.fillr.description", param("fillr", "/fillr", "wegui.command.fillr.usage.fillr.template", "wegui.command.fillr.usage.fillr.description", PATTERN, RADIUS));
        register("fill", "wegui.command.fill.name", "wegui.command.fill.description", param("fill", "/fill", "wegui.command.fill.usage.fill.template", "wegui.command.fill.usage.fill.description", PATTERN, RADIUS, DEPTH));
        register("removeabove", "wegui.command.removeabove.name", "wegui.command.removeabove.description",
                param("removeabove", "/removeabove", "wegui.command.removeabove.usage.removeabove.template", "wegui.command.removeabove.usage.removeabove.description", SIZE, HEIGHT));
        register("removebelow", "wegui.command.removebelow.name", "wegui.command.removebelow.description",
                param("removebelow", "/removebelow", "wegui.command.removebelow.usage.removebelow.template", "wegui.command.removebelow.usage.removebelow.description", SIZE, HEIGHT));
        register("removenear", "wegui.command.removenear.name", "wegui.command.removenear.description",
                param("removenear", "/removenear", "wegui.command.removenear.usage.removenear.template", "wegui.command.removenear.usage.removenear.description", entityTypeParam("wegui.command.removenear.param.0.name", "minecraft:pig", false), RANGE));
        register("center", "wegui.command.center.name", "wegui.command.center.description", param("center", "//center", "wegui.command.center.usage.center.template", "wegui.command.center.usage.center.description", PATTERN));
        register("revolve", "wegui.command.revolve.name", "wegui.command.revolve.description",
                param("revolve", "//revolve", "wegui.command.revolve.usage.revolve.template", "wegui.command.revolve.usage.revolve.description",
                        new Param("wegui.command.revolve.param.0.name", ParamType.INTEGER, "90"), REVOLVE_AXIS, new Param("wegui.command.revolve.param.1.name", ParamType.INTEGER, "1", true)));
    }
}
