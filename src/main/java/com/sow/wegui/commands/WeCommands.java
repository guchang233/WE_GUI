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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

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
    private static List<Option> BLOCK_OPTIONS;
    private static List<Option> ITEM_OPTIONS;

    public static List<Option> biomeOptions() {
        initBiomeOptions();
        return BIOME_OPTIONS;
    }

    public static List<Option> entityTypeOptions() {
        initEntityTypeOptions();
        return ENTITY_TYPE_OPTIONS;
    }

    public static List<Option> blockOptions() {
        initBlockOptions();
        return BLOCK_OPTIONS;
    }

    public static List<Option> itemOptions() {
        initItemOptions();
        return ITEM_OPTIONS;
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        initBiomeOptions();
        initEntityTypeOptions();
        initBlockOptions();
        initItemOptions();

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

    /**
     * 参数控件显示偏好。
     */
    public enum ParamControlType {
        DEFAULT,
        BUTTON_ROW,
        SEARCHABLE_DROPDOWN,
        NUMBER_SPINNER
    }

    /**
     * 参数额外元数据：建议、校验、控件偏好。
     */
    public record ParamMeta(
            boolean supportsSuggestions,
            String validatorRegex,
            int min,
            int max,
            ParamControlType controlType
    ) {
        public static final ParamMeta DEFAULT = new ParamMeta(false, null, Integer.MIN_VALUE, Integer.MAX_VALUE, ParamControlType.DEFAULT);
    }

    public record Command(
            String id,
            String displayName,
            String description,
            Category category,
            List<Usage> usages,
            String[] aliases,
            String icon
    ) {
        public Command(String id, String displayName, String description, Category category, List<Usage> usages) {
            this(id, displayName, description, category, usages, new String[0], null);
        }
    }

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
            List<Option> options,
            ParamMeta meta
    ) {
        public Param(String name, ParamType paramType, String defaultValue) {
            this(name, paramType, false, defaultValue, null, "", List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, String defaultValue, String description) {
            this(name, paramType, false, defaultValue, null, description, List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, String defaultValue, boolean optional, String description) {
            this(name, paramType, optional, defaultValue, null, description, List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, List<Option> options, String defaultValue, boolean optional) {
            this(name, paramType, optional, defaultValue, null, "", List.copyOf(options), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, boolean optional, String defaultValue, String hint) {
            this(name, paramType, optional, defaultValue, hint, "", List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, String defaultValue, boolean optional) {
            this(name, paramType, optional, defaultValue, null, "", List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, String defaultValue, String hint, boolean optional) {
            this(name, paramType, optional, defaultValue, hint, "", List.of(), ParamMeta.DEFAULT);
        }

        public Param(String name, ParamType paramType, List<Option> options, String defaultValue, boolean optional, ParamMeta meta) {
            this(name, paramType, optional, defaultValue, null, "", List.copyOf(options), meta);
        }

        public Param(String name, ParamType paramType, String defaultValue, String description, ParamMeta meta) {
            this(name, paramType, false, defaultValue, null, description, List.of(), meta);
        }

        public Param withMeta(ParamMeta meta) {
            return new Param(name, paramType, optional, defaultValue, hint, description, options, meta);
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
        register(id, displayName, description, new String[0], null, usages);
    }

    private static void register(String id, String displayName, String description, String[] aliases, String icon, Usage... usages) {
        Command cmd = new Command(id, displayName, description, currentCategory, List.of(usages), aliases == null ? new String[0] : aliases, icon);
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

    private static final Param PATTERN = new Param("图案", ParamType.PATTERN, "minecraft:stone", "如 minecraft:stone、50%stone,50%cobblestone、#clipboard");
    private static final Param MASK = new Param("掩码", ParamType.MASK, "", "如 !air、stone、>water");
    private static final Param FROM_MASK = new Param("被替换方块", ParamType.MASK, "", "如 stone");
    private static final Param TO_PATTERN = new Param("目标图案", ParamType.PATTERN, "", "如 minecraft:grass_block");

    private static final List<Option> DIRECTION_OPTIONS = List.of(
            new Option("north", "北", "朝向北方（-Z 方向）"),
            new Option("south", "南", "朝向南方（+Z 方向）"),
            new Option("east", "东", "朝向东方（+X 方向）"),
            new Option("west", "西", "朝向西方（-X 方向）"),
            new Option("up", "上", "朝上（+Y 方向）"),
            new Option("down", "下", "朝下（-Y 方向）"),
            new Option("me", "玩家朝向", "使用玩家当前朝向")
    );
    private static final Param DIRECTION = new Param("方向", ParamType.ENUM, DIRECTION_OPTIONS, "me", true);

    private static final List<Option> AXIS_XYZ_OPTIONS = List.of(
            new Option("x", "X 轴", "绕 X 轴旋转/翻转"),
            new Option("y", "Y 轴", "绕 Y 轴旋转/翻转（默认）"),
            new Option("z", "Z 轴", "绕 Z 轴旋转/翻转")
    );
    private static final Param REVOLVE_AXIS = new Param("轴", ParamType.ENUM, AXIS_XYZ_OPTIONS, "y", true);

    private static final List<Option> HV_OPTIONS = List.of(
            new Option("", "全部方向", "向所有方向扩展/收缩"),
            new Option("-h", "仅水平", "只在水平方向生效"),
            new Option("-v", "仅竖直", "只在竖直方向生效")
    );
    private static final Param HV_FLAG = new Param("方向范围", ParamType.ENUM, HV_OPTIONS, "", true);

    private static final List<Option> UP_FG_OPTIONS = List.of(
            new Option("", "默认", "正常向上传送"),
            new Option("-f", "强制", "强制放置，忽略脚下方块"),
            new Option("-g", "玻璃", "在脚下生成玻璃平台")
    );
    private static final Param UP_FG = new Param("模式", ParamType.ENUM, UP_FG_OPTIONS, "", true);

    private static final List<Option> BIOMEINFO_PT_OPTIONS = List.of(
            new Option("", "默认", "显示当前位置群系"),
            new Option("-p", "玩家位置", "使用玩家脚下方块位置"),
            new Option("-t", "准星位置", "使用准星指向的方块位置")
    );
    private static final Param BIOMEINFO_PT = new Param("位置模式", ParamType.ENUM, BIOMEINFO_PT_OPTIONS, "", true);

    private static final List<Option> FORMAT_OPTIONS = List.of(
            new Option("sponge", "Sponge", "Sponge 格式（.schem）"),
            new Option("schematic", "Schematic", "传统 Schematic 格式")
    );
    private static final Param FORMAT = new Param("格式", ParamType.ENUM, FORMAT_OPTIONS, "sponge", true);

    private static final List<Option> TREE_TYPE_OPTIONS = List.of(
            new Option("oak", "橡树", "普通橡树"),
            new Option("oak_checked", "橡树（检查型）", "仅在可种植方块上生成"),
            new Option("oak_bees_002", "橡树（0.2%蜂巢）", "有 0.2% 概率带蜂巢"),
            new Option("fancy_oak_bees", "大型橡树（蜂巢）", "枝叶更繁茂的大型橡树"),
            new Option("fancy_oak_bees_002", "大型橡树（0.2%蜂巢）", "大型橡树，0.2% 概率带蜂巢"),
            new Option("fancy_oak_checked", "大型橡树（检查型）", "大型橡树，检查土壤"),
            new Option("birch_checked", "白桦树（检查型）", "仅在可种植方块上生成"),
            new Option("birch_bees_0002", "白桦树（0.02%蜂巢）", "有 0.02% 概率带蜂巢"),
            new Option("birch_bees_002", "白桦树（0.2%蜂巢）", "有 0.2% 概率带蜂巢"),
            new Option("birch_tall", "高白桦树", "比普通白桦更高的变体"),
            new Option("super_birch_bees", "超高白桦树（蜂巢）", "极高的白桦树"),
            new Option("super_birch_bees_0002", "超高白桦树（0.02%蜂巢）", "极高的白桦树，0.02% 蜂巢"),
            new Option("spruce", "云杉", "普通云杉"),
            new Option("spruce_checked", "云杉（检查型）", "仅在可种植方块上生成"),
            new Option("pine", "松树", "普通松树"),
            new Option("pine_checked", "松树（检查型）", "仅在可种植方块上生成"),
            new Option("mega_spruce_checked", "大型云杉（检查型）", "巨型云杉树"),
            new Option("mega_pine_checked", "大型松树（检查型）", "巨型松树"),
            new Option("jungle_tree", "丛林树", "普通丛林树"),
            new Option("jungle_bush", "丛林灌木", "低矮的丛林灌木"),
            new Option("mega_jungle_tree_checked", "大型丛林树（检查型）", "巨型丛林树"),
            new Option("dark_oak_checked", "深色橡树（检查型）", "黑森林中的深色橡树"),
            new Option("acacia", "金合欢树", "热带草原金合欢树"),
            new Option("acacia_checked", "金合欢树（检查型）", "仅在可种植方块上生成"),
            new Option("cherry_bees_005", "樱花树（0.5%蜂巢）", "粉红色樱花树，0.5% 蜂巢"),
            new Option("cherry_checked", "樱花树（检查型）", "粉红色樱花树"),
            new Option("mangrove_checked", "红树（检查型）", "沼泽红树"),
            new Option("tall_mangrove_checked", "高红树（检查型）", "较高的红树"),
            new Option("pale_oak_checked", "苍白橡树（检查型）", "苍白园林中的橡树"),
            new Option("pale_oak_creaking_checked", "苍白橡树（嘎枝怪，检查型）", "可能生成嘎枝怪的苍白橡树"),
            new Option("rooted_azalea_tree", "生根杜鹃树", "带有根系的杜鹃树"),
            new Option("chorus_plant", "Chorus 植物", "末地 Chorus 植物"),
            new Option("crimson_fungi", "绯红巨型菌", "下界绯红森林巨型菌"),
            new Option("warped_fungi", "诡异巨型菌", "下界诡异森林巨型菌"),
            new Option("fallen_oak_tree", "倒下的橡树", "横倒的橡树树干"),
            new Option("fallen_birch_tree", "倒下的白桦树", "横倒的白桦树干"),
            new Option("fallen_spruce_tree", "倒下的云杉", "横倒的云杉树干"),
            new Option("fallen_jungle_tree", "倒下的丛林树", "横倒的丛林树干"),
            new Option("fallen_super_birch_tree", "倒下的超高白桦树", "横倒的超高白桦树干")
    );
    private static final Param TREE_TYPE = new Param("树类型", ParamType.ENUM, TREE_TYPE_OPTIONS, "oak", false);
    private static final Param FOREST_TYPE = new Param("森林类型", ParamType.ENUM, TREE_TYPE_OPTIONS, "oak", true);

    private static final Param AMOUNT = new Param("数量", ParamType.INTEGER, "5");
    private static final Param RADIUS = new Param("半径", ParamType.INTEGER, "5");
    private static final Param RANGE = new Param("范围", ParamType.INTEGER, "5");
    private static final Param SIZE = new Param("尺寸", ParamType.INTEGER, "10");
    private static final Param COUNT = new Param("次数", ParamType.INTEGER, "3", true);
    private static final Param HEIGHT = new Param("高度", ParamType.INTEGER, "5", true);
    private static final Param ITERATIONS = new Param("迭代次数", ParamType.INTEGER, "1", true);
    private static final Param DISTANCE = new Param("距离", ParamType.INTEGER, "5", true);
    private static final Param DEPTH = new Param("深度", ParamType.INTEGER, "0", true);
    private static final Param THICKNESS = new Param("粗细", ParamType.INTEGER, "1", true);
    private static final Param EXPRESSION = new Param("表达式", ParamType.STRING, "", "如 x^2+y^2+z^2<100");
    private static final Param FILENAME = new Param("文件名", ParamType.FILENAME, "my_build");
    private static final Param PLAYER = new Param("玩家", ParamType.PLAYER, "", true);
    private static final Param SNAPSHOT_NAME = new Param("快照名", ParamType.STRING, "2024-01-01-01");
    private static final Param DATE = new Param("日期", ParamType.STRING, "2024-01-01");
    private static final Param SCRIPT = new Param("脚本名", ParamType.STRING, "myscript");
    private static final Param QUERY = new Param("关键词", ParamType.STRING, "", "如 stone");
    private static final Param STEPS = new Param("步数", ParamType.INTEGER, "1", true);
    private static final Param OFFSET = new Param("偏移", ParamType.STRING, "", "如 north 或 0,1,0", true);
    private static final Param BUTCHER_FLAGS = new Param("标志", ParamType.STRING, "", "如 -plangbtfr", true);
    private static final Param DENSITY = new Param("密度", ParamType.DECIMAL, "5", true);

    // ==================== 分类注册 ====================

    private static void initBiomeOptions() {
        if (BIOME_OPTIONS != null) return;

        List<Option> list = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            Registry<Biome> biomeRegistry = (Registry<Biome>) BuiltInRegistries.REGISTRY.get(Registries.BIOME.location());
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

    private static void initBlockOptions() {
        if (BLOCK_OPTIONS != null) return;

        List<Option> list = new ArrayList<>();
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                list.add(new Option(id, id, id));
            }
        } catch (Throwable ignored) {
            // 注册表尚未准备好时回退到空列表
        }
        list.sort(Comparator.comparing(Option::label));
        BLOCK_OPTIONS = list;
    }

    private static void initItemOptions() {
        if (ITEM_OPTIONS != null) return;

        List<Option> list = new ArrayList<>();
        try {
            for (Item item : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                list.add(new Option(id, id, id));
            }
        } catch (Throwable ignored) {
            // 注册表尚未准备好时回退到空列表
        }
        list.sort(Comparator.comparing(Option::label));
        ITEM_OPTIONS = list;
    }

    private static Param biomeParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, BIOME_OPTIONS, defaultValue, optional,
                new ParamMeta(true, null, Integer.MIN_VALUE, Integer.MAX_VALUE, ParamControlType.SEARCHABLE_DROPDOWN));
    }

    private static Param entityTypeParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, ENTITY_TYPE_OPTIONS, defaultValue, optional,
                new ParamMeta(true, null, Integer.MIN_VALUE, Integer.MAX_VALUE, ParamControlType.SEARCHABLE_DROPDOWN));
    }

    private static Param blockParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, BLOCK_OPTIONS, defaultValue, optional,
                new ParamMeta(true, null, Integer.MIN_VALUE, Integer.MAX_VALUE, ParamControlType.SEARCHABLE_DROPDOWN));
    }

    private static Param itemParam(String name, String defaultValue, boolean optional) {
        return new Param(name, ParamType.ENUM, ITEM_OPTIONS, defaultValue, optional,
                new ParamMeta(true, null, Integer.MIN_VALUE, Integer.MAX_VALUE, ParamControlType.SEARCHABLE_DROPDOWN));
    }

    private static void registerGeneral() {
        currentCategory = Category.GENERAL;
        register("undo", "撤销", "撤销上一步或多步 WorldEdit 操作", param("undo", "/undo", "/undo [步数] [玩家]", "撤销指定步数", STEPS, PLAYER));
        register("redo", "重做", "重做上一步或多步撤销的操作", param("redo", "/redo", "/redo [步数] [玩家]", "重做指定步数", STEPS, PLAYER));
        register("clearhistory", "清空历史", "清空所有撤销/重做历史", instant("clearhistory", "/clearhistory", "清空操作历史记录"));
        register("limit", "修改上限", "设置每次操作可修改方块的最大数量", param("limit", "//limit", "//limit <数量|-1>", "设置上限，-1 为无限制", AMOUNT));
        register("timeout", "超时设置", "设置表达式求值超时时间", param("timeout", "//timeout", "//timeout <毫秒>", "设置超时时间", AMOUNT));
        register("perf", "性能模式", "切换副作用/性能模式", instant("perf", "//perf", "切换性能模式"));
        register("reorder", "重排模式", "设置方块重排模式",
                param("reorder", "//reorder", "//reorder <模式>", "设置重排模式",
                        new Param("模式", ParamType.ENUM, List.of(
                                new Option("fast", "快速", "最快但可能产生光照问题"),
                                new Option("multi", "多阶段", "分多阶段处理，更稳定"),
                                new Option("none", "无", "不重排")
                        ), "fast", false)));
        register("drawsel", "显示选区", "切换选区边框显示", instant("drawsel", "//drawsel", "切换选区边框显示"));
        register("gmask", "全局掩码", "设置作用于所有命令的全局掩码",
                param("gmask_set", "/gmask", "/gmask <掩码>", "设置全局掩码", MASK),
                instant("gmask_clear", "/gmask", "清除全局掩码"));
        register("toggleplace", "切换放置点", "在玩家位置与 pos1 之间切换放置点", instant("toggleplace", "/toggleplace", "切换放置点"));
        register("searchitem", "搜索物品", "搜索方块或物品 ID", param("searchitem", "/searchitem", "/searchitem <关键词>", "搜索物品/方块", QUERY));
        register("toggleeditwand", "切换魔杖", "启用/关闭选区魔杖功能", instant("toggleeditwand", "/toggleeditwand", "切换魔杖功能"));
        register("fast", "快速模式", "切换快速执行模式", instant("fast", "//fast", "切换快速模式"));
        register("we_reload", "重载配置", "重新加载 WorldEdit 配置", instant("we_reload", "//worldedit reload", "重载 WE 配置"));
        register("we_version", "WE 版本", "显示 WorldEdit 版本", instant("we_version", "//worldedit version", "显示版本信息"));
        register("we_tz", "设置时区", "设置 WorldEdit 时区", param("we_tz", "//worldedit tz", "//worldedit tz <时区>", "设置时区", new Param("时区", ParamType.STRING, "UTC")));
        register("we_help", "WE 帮助", "显示 WorldEdit 帮助", param("we_help", "/worldedit help", "/worldedit help [命令]", "查看帮助", new Param("命令", ParamType.STRING, "set", true)));
    }

    private static void registerSelection() {
        currentCategory = Category.SELECTION;
        register("wand", "获取魔杖", "获取选区魔杖（木斧）", instant("wand", "//wand", "获取选区魔杖"));
        register("toggleeditwand_sel", "切换魔杖", "切换选区魔杖功能", instant("toggleeditwand_sel", "/toggleeditwand", "切换魔杖功能"));
        register("sel", "选区模式", "切换选区模式",
                param("sel", "//sel", "//sel <模式>", "切换选区模式",
                        new Param("模式", ParamType.ENUM, List.of(
                                new Option("cuboid", "长方体", "两点对角线选区"),
                                new Option("extend", "扩展", "从 pos1 延伸到点击位置"),
                                new Option("poly", "多边形", "多边形选区"),
                                new Option("ellipsoid", "椭球体", "中心加半径选区"),
                                new Option("sphere", "球体", "球形选区"),
                                new Option("cyl", "圆柱", "圆柱形选区"),
                                new Option("convex", "凸包", "凸包选区")
                        ), "cuboid", false)));
        register("desel", "取消选区", "取消当前选区", instant("desel", "//desel", "取消选区"));
        register("pos1", "设点 1", "将 pos1 设为当前位置", instant("pos1", "//pos1", "设 pos1 为玩家位置"));
        register("pos2", "设点 2", "将 pos2 设为当前位置", instant("pos2", "//pos2", "设 pos2 为玩家位置"));
        register("hpos1", "准星设点 1", "将 pos1 设为准星瞄准的方块", instant("hpos1", "//hpos1", "准星设 pos1"));
        register("hpos2", "准星设点 2", "将 pos2 设为准星瞄准的方块", instant("hpos2", "//hpos2", "准星设 pos2"));
        register("chunk", "选区设到区块", "将选区设为当前或指定区块", instant("chunk", "//chunk", "选区设为当前区块"));
        register("expand", "扩展选区", "向指定方向扩展选区",
                param("expand_dir", "//expand", "//expand <数量> [方向]", "按方向扩展", AMOUNT, DIRECTION),
                param("expand_vert", "//expand", "//expand <数量> vert", "向上向下扩展", AMOUNT));
        register("contract", "收缩选区", "向指定方向收缩选区",
                param("contract_dir", "//contract", "//contract <数量> [方向]", "按方向收缩", AMOUNT, DIRECTION),
                param("contract_vert", "//contract", "//contract <数量> vert", "向上向下收缩", AMOUNT));
        register("shift", "平移选区", "平移选区而不移动方块", param("shift", "//shift", "//shift <数量> [方向]", "平移选区", AMOUNT, DIRECTION));
        register("outset", "外扩选区", "向所有方向外扩选区", param("outset", "//outset", "//outset [-hv] <数量>", "向外扩展", HV_FLAG, AMOUNT));
        register("inset", "内缩选区", "向所有方向内缩选区", param("inset", "//inset", "//inset [-hv] <数量>", "向内收缩", HV_FLAG, AMOUNT));
        register("size", "选区尺寸", "显示选区尺寸与方块数", instant("size", "//size", "显示选区信息"));
        register("count", "统计方块", "统计选区中匹配掩码的方块数量",
                param("count", "//count", "//count [-d] <掩码>", "统计方块", new Param("-d 不同方块", ParamType.FLAG, "-d", true), MASK));
        register("distr", "方块分布", "显示选区中各方块比例",
                param("distr", "//distr", "//distr [-c]", "显示方块分布", new Param("-c 精确计数", ParamType.FLAG, "-c", true)));
        register("seltoggle", "切换选区显示", "切换选区粒子/边框显示", instant("seltoggle", "//sel toggle", "切换选区显示"));
    }

    private static void registerRegion() {
        currentCategory = Category.REGION;
        register("set", "设置方块", "将选区内所有方块替换为指定图案", param("set", "//set", "//set <图案>", "替换选区方块", PATTERN));
        register("replace", "替换方块", "将选区中匹配掩码的方块替换为目标图案",
                param("replace", "//replace", "//replace <被替换方块> <目标图案>", "替换指定方块", FROM_MASK, TO_PATTERN),
                param("replace_all", "//replace", "//replace <目标图案>", "替换所有方块", TO_PATTERN));
        register("overlay", "表层覆盖", "在选区最外层方块上覆盖图案", param("overlay", "//overlay", "//overlay <图案>", "表层覆盖", PATTERN));
        register("walls", "造墙", "以选区边界生成墙壁", param("walls", "//walls", "//walls <图案>", "生成墙壁", PATTERN));
        register("outline", "包边", "用图案包裹选区外表面", param("outline", "//outline", "//outline <图案>", "选区包边", PATTERN));
        register("smooth", "平滑", "平滑地形", param("smooth", "//smooth", "//smooth [迭代次数]", "平滑地形", ITERATIONS));
        register("naturalize", "自然化", "将石头与泥土自然过渡", instant("naturalize", "//naturalize", "自然化地形"));
        register("move", "移动", "移动选区内容",
                param("move", "//move", "//move [数量] [方向] [-sbea] [图案]", "移动选区内容",
                        AMOUNT, DIRECTION,
                        new Param("-s 选择新区域", ParamType.FLAG, "-s", true),
                        new Param("-b 复制空气", ParamType.FLAG, "-b", true),
                        new Param("-e 移动实体", ParamType.FLAG, "-e", true),
                        new Param("-a 忽略空气", ParamType.FLAG, "-a", true),
                        new Param("填充图案", ParamType.PATTERN, "air", true)));
        register("stack", "堆叠", "沿方向重复堆叠选区",
                param("stack", "//stack", "//stack [次数] [方向] [-abers] [-m <掩码>]", "堆叠选区",
                        COUNT, DIRECTION,
                        new Param("-a 忽略空气", ParamType.FLAG, "-a", true),
                        new Param("-b 复制空气", ParamType.FLAG, "-b", true),
                        new Param("-e 复制实体", ParamType.FLAG, "-e", true),
                        new Param("-r 不复制空气", ParamType.FLAG, "-r", true),
                        new Param("-s 选择新区域", ParamType.FLAG, "-s", true),
                        new Param("-m 掩码", ParamType.MASK, "", true)));
        register("regen", "重新生成", "按当前种子重新生成选区", instant("regen", "//regen", "重新生成选区"));
        register("deform", "变形", "按表达式变形选区", param("deform", "//deform", "//deform <表达式>", "按表达式变形", EXPRESSION));
        register("hollow", "镂空", "将选区内部挖空只留外壳",
                param("hollow", "//hollow", "//hollow [厚度] [图案]", "镂空选区", THICKNESS, new Param("填充图案", ParamType.PATTERN, "air", true)));
        register("line", "连线", "在选中的两点间画直线",
                param("line", "//line", "//line [-h] <图案> [粗细]", "两点连线", new Param("-h 空心", ParamType.FLAG, "-h", true), PATTERN, THICKNESS));
        register("curve", "曲线", "在 convex 选区的多点间画样条曲线",
                param("curve", "//curve", "//curve [-h] <图案> [粗细]", "多点曲线", new Param("-h 空心", ParamType.FLAG, "-h", true), PATTERN, THICKNESS));
    }

    private static void registerGeneration() {
        currentCategory = Category.GENERATION;
        register("generate", "生成结构", "按表达式在选区内生成图案",
                param("generate", "//generate", "//generate [-hroc] <图案> <表达式>", "按表达式生成",
                        new Param("-hroc 标志", ParamType.STRING, "", "如 -h", true), PATTERN, EXPRESSION));
        register("cyl", "实心圆柱", "生成实心圆柱", param("cyl", "//cyl", "//cyl [-h] <图案> <半径> [高度]", "实心圆柱", new Param("-h 空心", ParamType.FLAG, "-h", true), PATTERN, RADIUS, HEIGHT));
        register("hcyl", "空心圆柱", "生成空心圆柱", param("hcyl", "//hcyl", "//hcyl <图案> <半径> [高度]", "空心圆柱", PATTERN, RADIUS, HEIGHT));
        register("sphere", "实心球", "生成实心球体",
                param("sphere", "//sphere", "//sphere [-r] [-h] <图案> <半径>", "实心球体",
                        new Param("-r 抬高底部", ParamType.FLAG, "-r", true),
                        new Param("-h 空心", ParamType.FLAG, "-h", true), PATTERN, RADIUS));
        register("hsphere", "空心球", "生成空心球体",
                param("hsphere", "//hsphere", "//hsphere [-r] <图案> <半径>", "空心球体",
                        new Param("-r 抬高底部", ParamType.FLAG, "-r", true), PATTERN, RADIUS));
        register("pyramid", "金字塔", "生成金字塔", param("pyramid", "//pyramid", "//pyramid <图案> <尺寸>", "金字塔", PATTERN, SIZE));
        register("hpyramid", "空心金字塔", "生成空心金字塔", param("hpyramid", "//hpyramid", "//hpyramid <图案> <尺寸>", "空心金字塔", PATTERN, SIZE));
        register("cone", "圆锥", "生成圆锥", param("cone", "//cone", "//cone <图案> <半径> <高度>", "圆锥", PATTERN, RADIUS, HEIGHT));
        register("forest", "森林", "在选区内生成森林",
                param("forest", "//forest", "//forest [森林类型] [密度]", "生成森林", FOREST_TYPE, DENSITY));
        register("pumpkins", "南瓜灯", "生成南瓜灯", instant("pumpkins", "//pumpkins", "生成南瓜灯"));
        register("flora", "植物", "生成草、花等植物", instant("flora", "//flora", "生成植物"));
        register("caves", "洞穴", "生成洞穴", instant("caves", "//caves", "生成洞穴"));
        register("ore", "矿脉", "生成矿脉", param("ore", "//ore", "//ore <图案> <尺寸>", "生成矿脉", PATTERN, SIZE));
        register("hkaleidoscope", "万花筒", "万花筒镜像生成", instant("hkaleidoscope", "//hkaleidoscope", "万花筒效果"));
    }

    private static void registerClipboard() {
        currentCategory = Category.CLIPBOARD;
        register("copy", "复制", "复制选区到剪贴板", instant("copy", "//copy", "复制选区"), instant("copy_entities", "//copy -e", "同时复制实体"));
        register("cut", "剪切", "剪切选区到剪贴板", instant("cut", "//cut", "剪切选区"), instant("cut_entities", "//cut -e", "同时剪切实体"));
        register("paste", "粘贴", "粘贴剪贴板内容",
                param("paste", "//paste", "//paste [-a] [-s] [-o]", "粘贴",
                        new Param("-a 忽略空气", ParamType.FLAG, "-a", true),
                        new Param("-s 选择新区域", ParamType.FLAG, "-s", true),
                        new Param("-o 粘贴到原点", ParamType.FLAG, "-o", true)));
        register("rotate", "旋转", "旋转剪贴板内容", param("rotate", "//rotate", "//rotate <角度>", "旋转剪贴板", new Param("角度", ParamType.INTEGER, "90")));
        register("flip", "翻转", "翻转剪贴板内容", param("flip", "//flip", "//flip [方向]", "翻转剪贴板", DIRECTION));
        register("clearclipboard", "清空剪贴板", "清空剪贴板", instant("clearclipboard", "/clearclipboard", "清空剪贴板"));
    }

    private static void registerSuperPickaxe() {
        currentCategory = Category.SUPER_PICKAXE;
        register("sp_single", "单方块超级镐", "启用单击破坏单个方块", instant("sp_single", "//sp single", "单方块模式"));
        register("sp_area", "范围超级镐", "启用破坏范围方块", param("sp_area", "//sp area", "//sp area <范围>", "范围模式", RANGE));
        register("sp_recursive", "递归超级镐", "启用递归破坏相同方块", param("sp_recursive", "//sp recursive", "//sp recursive <范围>", "递归模式", RANGE));
        register("sp_none", "关闭超级镐", "关闭超级镐", instant("sp_none", "//sp none", "关闭超级镐"));
    }

    private static void registerTool() {
        currentCategory = Category.TOOL;
        register("none", "解除工具", "解除当前手持物品的 WorldEdit 工具绑定", instant("none", "//none", "解除工具绑定"));
        register("farwand", "远程魔杖", "绑定远程选区魔杖", instant("farwand", "//farwand", "绑定远程魔杖"));
        register("lrbuild", "左右键建造", "绑定左右键快速建造工具", instant("lrbuild", "//lrbuild", "绑定左右键建造"));
        register("tree", "树工具", "绑定种树工具", bind("tree", "/tool tree", "/tool tree [树类型]", "绑定树工具", TREE_TYPE));
        register("deltree", "删树工具", "绑定删除树工具", bindNoParam("deltree", "/tool deltree", "绑定删树工具"));
        register("repl", "替换工具", "绑定替换工具", bind("repl", "/tool repl", "/tool repl <目标图案>", "绑定替换工具", TO_PATTERN));
        register("cycler", "循环工具", "绑定方块状态循环工具", bindNoParam("cycler", "/tool cycler", "绑定循环工具"));
        register("floodfill", "flood fill", "绑定 flood fill 工具",
                bind("floodfill", "/tool floodfill", "/tool floodfill <图案> <范围>", "绑定 flood fill", PATTERN, RANGE));
        register("info", "信息工具", "绑定方块信息工具", bindNoParam("info", "/tool info", "绑定信息工具"));
        register("selwand", "选区魔杖", "绑定普通选区魔杖", bindNoParam("selwand", "/tool selwand", "绑定选区魔杖"));
    }

    private static void registerBrush() {
        currentCategory = Category.BRUSH;
        register("brush_sphere", "球体笔刷", "绑定球体笔刷",
                bind("brush_sphere", "/brush sphere", "/brush sphere <图案> <半径>", "球体笔刷", PATTERN, RADIUS));
        register("brush_cylinder", "圆柱笔刷", "绑定圆柱笔刷",
                bind("brush_cylinder", "/brush cylinder", "/brush cylinder <图案> <半径> [高度]", "圆柱笔刷", PATTERN, RADIUS, HEIGHT));
        register("brush_clipboard", "剪贴板笔刷", "绑定剪贴板粘贴笔刷",
                bind("brush_clipboard", "/brush clipboard", "/brush clipboard [-a] [-p]", "剪贴板笔刷",
                        new Param("-a 忽略空气", ParamType.FLAG, "-a", true),
                        new Param("-p 粘贴于原点", ParamType.FLAG, "-p", true)));
        register("brush_smooth", "平滑笔刷", "绑定平滑地形笔刷",
                bind("brush_smooth", "/brush smooth", "/brush smooth [迭代次数] [半径]", "平滑笔刷", ITERATIONS, RADIUS));
        register("brush_gravity", "重力笔刷", "绑定重力笔刷",
                bind("brush_gravity", "/brush gravity", "/brush gravity [半径]", "重力笔刷", RADIUS));
        register("brush_butcher", "清怪笔刷", "绑定清怪笔刷",
                bind("brush_butcher", "/brush butcher", "/brush butcher [标志] [半径]", "清怪笔刷", BUTCHER_FLAGS, RADIUS));
        register("brush_eraser", "橡皮擦", "绑定橡皮擦笔刷", bindNoParam("brush_eraser", "/brush eraser", "橡皮擦"));
        register("brush_forest", "森林笔刷", "绑定森林生成笔刷",
                bind("brush_forest", "/brush forest", "/brush forest [森林类型] [半径]", "森林笔刷", FOREST_TYPE, RADIUS));
        register("brush_raise", "抬升笔刷", "绑定抬升地形笔刷",
                bind("brush_raise", "/brush raise", "/brush raise [半径]", "抬升笔刷", RADIUS));
        register("brush_lower", "下陷笔刷", "绑定下陷地形笔刷",
                bind("brush_lower", "/brush lower", "/brush lower [半径]", "下陷笔刷", RADIUS));
    }

    private static Usage bindNoParam(String id, String command, String description) {
        return new Usage(id, command, command, description, List.of(), Type.BIND);
    }

    private static void registerNavigation() {
        currentCategory = Category.NAVIGATION;
        register("unstuck", "脱离卡死", "传送到上方安全位置", instant("unstuck", "/unstuck", "脱离卡死"));
        register("ascend", "向上穿层", "向上穿过天花板", param("ascend", "/ascend", "/ascend [层数]", "向上穿层", COUNT));
        register("descend", "向下穿层", "向下穿过地板", param("descend", "/descend", "/descend [层数]", "向下穿层", COUNT));
        register("ceil", "跳到天花板", "跳到当前空间顶部", param("ceil", "/ceil", "/ceil [间隙]", "跳到天花板", new Param("间隙", ParamType.INTEGER, "0", true)));
        register("thru", "穿墙", "向前穿过墙壁", instant("thru", "/thru", "穿墙"));
        register("jumpto", "跳到准星", "传送到准星指向的方块", instant("jumpto", "/jumpto", "跳到准星位置"));
        register("up", "向上", "向上垂直移动",
                param("up", "/up", "/up [-fg] <距离>", "向上传送", UP_FG, AMOUNT));
    }

    private static void registerBiome() {
        currentCategory = Category.BIOME;
        register("setbiome", "设置群系", "将选区内方块设置为指定群系",
                param("setbiome", "//setbiome", "//setbiome [-p] <群系>", "设置群系",
                        new Param("-p 玩家列", ParamType.FLAG, "-p", true), biomeParam("群系", "minecraft:plains", false)));
        register("biomeinfo", "群系信息", "查看准星或当前位置群系",
                param("biomeinfo", "/biomeinfo", "/biomeinfo [-pt]", "群系信息", BIOMEINFO_PT));
        register("replacebiome", "替换群系", "替换选区内的群系",
                param("replacebiome", "//replacebiome", "//replacebiome <原群系> <目标群系>", "替换群系",
                        biomeParam("原群系", "minecraft:plains", false),
                        biomeParam("目标群系", "minecraft:forest", false)));
    }

    private static void registerChunk() {
        currentCategory = Category.CHUNK;
        register("chunkinfo", "区块信息", "显示当前区块信息", instant("chunkinfo", "//chunkinfo", "显示区块信息"));
        register("listchunks", "列出区块", "列出选区涉及的所有区块", instant("listchunks", "//listchunks", "列出选区区块"));
        register("delchunks", "删除区块", "删除选区涉及的所有区块", instant("delchunks", "//delchunks", "删除选区区块"));
    }

    private static void registerSnapshot() {
        currentCategory = Category.SNAPSHOT;
        register("snapshot_use", "使用快照", "加载指定快照世界", param("snapshot_use", "//snapshot use", "//snapshot use <快照名>", "使用快照", SNAPSHOT_NAME));
        register("snapshot_list", "快照列表", "列出可用快照", instant("snapshot_list", "//snapshot list", "列出快照"));
        register("snapshot_sel", "选择快照日期", "选择快照日期", param("snapshot_sel", "//snapshot sel", "//snapshot sel <日期>", "选择快照日期", DATE));
        register("restore", "恢复快照", "从快照恢复选区", instant("restore", "//restore", "恢复选区"));
    }

    private static void registerScript() {
        currentCategory = Category.SCRIPT;
        register("cs", "执行脚本", "执行 WorldEdit 脚本", param("cs", "/cs", "/cs <脚本名> [参数...]", "执行脚本", SCRIPT));
        register("load", "加载脚本", "加载 WorldEdit 脚本文件", param("load", "//load", "//load <脚本名>", "加载脚本", SCRIPT));
    }

    private static void registerUtility() {
        currentCategory = Category.UTILITY;
        register("calc", "计算器", "计算数学表达式", param("calc", "//calc", "//calc <表达式>", "计算表达式", EXPRESSION));
        register("remove", "移除实体", "移除指定类型的实体", param("remove", "/remove", "/remove <类型> <半径>", "移除实体", entityTypeParam("实体类型", "minecraft:pig", false), RANGE));
        register("butcher", "击杀生物", "击杀指定半径内的生物", param("butcher", "/butcher", "/butcher [标志] [半径]", "击杀生物", BUTCHER_FLAGS, RADIUS));
        register("extinguish", "灭火", "熄灭指定半径内的火焰", param("extinguish", "/ex", "/ex [半径]", "熄灭火焰", RADIUS));
        register("green", "草化", "将泥土转为草方块",
                param("green", "/green", "/green [-f] [半径]", "草化",
                        new Param("-f 包括深板岩", ParamType.FLAG, "-f", true), RADIUS));
        register("drain", "排水", "吸干指定半径内的流体", param("drain", "/drain", "/drain [半径]", "排水", RADIUS));
        register("replacenear", "替换附近", "替换附近指定半径内的方块",
                param("replacenear", "/replacenear", "/replacenear [-f] <范围> <被替换方块> <目标图案>", "替换附近",
                        new Param("-f 强制", ParamType.FLAG, "-f", true), RANGE, FROM_MASK, TO_PATTERN));
        register("snow", "降雪", "生成雪", param("snow", "/snow", "/snow [半径]", "降雪", RADIUS));
        register("thaw", "融雪", "融化雪与冰", param("thaw", "/thaw", "/thaw [半径]", "融雪", RADIUS));
        register("fixwater", "修复水", "修复流动的水", param("fixwater", "/fixwater", "/fixwater [半径]", "修复水", RADIUS));
        register("fixlava", "修复岩浆", "修复流动的岩浆", param("fixlava", "/fixlava", "/fixlava [半径]", "修复岩浆", RADIUS));
        register("fillr", "径向填充", "径向填充方块", param("fillr", "/fillr", "/fillr <图案> <半径>", "径向填充", PATTERN, RADIUS));
        register("fill", "填充", "填充区域", param("fill", "/fill", "/fill <图案> <半径> [深度]", "填充", PATTERN, RADIUS, DEPTH));
        register("removeabove", "移除上方", "移除玩家上方指定范围的方块",
                param("removeabove", "/removeabove", "/removeabove [尺寸] [高度]", "移除上方", SIZE, HEIGHT));
        register("removebelow", "移除下方", "移除玩家下方指定范围的方块",
                param("removebelow", "/removebelow", "/removebelow [尺寸] [高度]", "移除下方", SIZE, HEIGHT));
        register("removenear", "移除附近", "移除玩家附近指定类型的方块",
                param("removenear", "/removenear", "/removenear <类型> <半径>", "移除附近", entityTypeParam("实体类型", "minecraft:pig", false), RANGE));
        register("center", "中心点", "在选区中心放置方块", param("center", "//center", "//center <图案>", "中心方块", PATTERN));
        register("revolve", "旋转复制", "绕轴旋转复制选区",
                param("revolve", "//revolve", "//revolve <角度> [轴] [复制份数]", "旋转复制",
                        new Param("角度", ParamType.INTEGER, "90"), REVOLVE_AXIS, new Param("份数", ParamType.INTEGER, "1", true)));
    }
}
