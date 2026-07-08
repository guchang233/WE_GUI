package com.sow.wegui;

import java.util.*;

/**
 * WorldEdit 命令注册表 — 完整收录所有 WE 命令，并为每个命令提供详细的用法变体。
 */
public final class WeCommandRegistry {
    private static final Map<String, WeCommand> BY_ID = new LinkedHashMap<>();
    private static final Map<WeCommandCategory, List<WeCommand>> BY_CAT = new EnumMap<>(WeCommandCategory.class);
    private static boolean initialized = false;
    private static WeCommandCategory currentCat;

    private WeCommandRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        registerGeneral();
        registerSelection();
        registerRegion();
        registerGeneration();
        registerClipboard();
        registerSuperPick();
        registerTool();
        registerBrush();
        registerNavigation();
        registerBiome();
        registerChunk();
        registerSnapshot();
        registerScript();
        registerUtility();
        for (WeCommandCategory c : WeCommandCategory.values()) BY_CAT.putIfAbsent(c, List.of());
    }

    // ── 常用参数定义（可复用）──
    private static final WeCommandParam PATTERN = new WeCommandParam("图案", WeParamType.PATTERN, false, null, "如 minecraft:stone、50%stone,50%cobblestone、#clipboard");
    private static final WeCommandParam MASK = new WeCommandParam("掩码", WeParamType.MASK, false, null, "如 !air、stone、>water");
    private static final WeCommandParam FROM_MASK = new WeCommandParam("被替换方块", WeParamType.MASK, false, null, "如 stone");
    private static final WeCommandParam TO_PATTERN = new WeCommandParam("目标图案", WeParamType.PATTERN, false, null, "如 minecraft:grass_block");
    private static final WeCommandParam DIRECTION = new WeCommandParam("方向", WeParamType.DIRECTION, List.of("north", "south", "east", "west", "up", "down", "me"));
    private static final WeCommandParam REL_DIR = new WeCommandParam("相对方向", WeParamType.RELATIVE_DIRECTION, List.of("forward", "back", "left", "right"));
    private static final WeCommandParam AXIS = new WeCommandParam("轴向", WeParamType.AXIS, "north/up/forward/0,1,0", true);
    private static final WeCommandParam AMOUNT = new WeCommandParam("数量", WeParamType.INTEGER, "5", false);
    private static final WeCommandParam RADIUS = new WeCommandParam("半径", WeParamType.INTEGER, "5", false);
    private static final WeCommandParam RANGE = new WeCommandParam("范围", WeParamType.INTEGER, "5", false);
    private static final WeCommandParam SIZE = new WeCommandParam("尺寸", WeParamType.INTEGER, "10", false);
    private static final WeCommandParam COUNT = new WeCommandParam("次数", WeParamType.INTEGER, "3", false);
    private static final WeCommandParam HEIGHT = new WeCommandParam("高度", WeParamType.INTEGER, "5", true);
    private static final WeCommandParam ITERATIONS = new WeCommandParam("迭代次数", WeParamType.INTEGER, "1", true);
    private static final WeCommandParam DISTANCE = new WeCommandParam("距离", WeParamType.INTEGER, "5", true);
    private static final WeCommandParam DEPTH = new WeCommandParam("深度", WeParamType.INTEGER, "0", true);
    private static final WeCommandParam THICKNESS = new WeCommandParam("厚度", WeParamType.INTEGER, "1", true);
    private static final WeCommandParam EXPRESSION = new WeCommandParam("表达式", WeParamType.STRING, false, null, "如 x^2+y^2+z^2<100");
    private static final WeCommandParam FILENAME = new WeCommandParam("文件名", WeParamType.FILENAME, "my_build", false);
    private static final WeCommandParam FORMAT = new WeCommandParam("格式", WeParamType.STRING, "sponge 或 schematic", true);
    private static final WeCommandParam PLAYER = new WeCommandParam("玩家", WeParamType.PLAYER, "Steve", true);
    private static final WeCommandParam BIOME = new WeCommandParam("群系", WeParamType.STRING, false, null, "如 minecraft:plains");
    private static final WeCommandParam SNAPSHOT_NAME = new WeCommandParam("快照名", WeParamType.STRING, "2024-01-01-01", false);
    private static final WeCommandParam DATE = new WeCommandParam("日期", WeParamType.STRING, "2024-01-01", false);
    private static final WeCommandParam SCRIPT = new WeCommandParam("脚本名", WeParamType.STRING, "myscript", false);
    private static final WeCommandParam QUERY = new WeCommandParam("关键词", WeParamType.STRING, false, null, "stone");
    private static final WeCommandParam STEPS = new WeCommandParam("步数", WeParamType.INTEGER, "1", true);
    private static final WeCommandParam OFFSET = new WeCommandParam("偏移", WeParamType.STRING, "north 或 0,1,0", true);
    private static final WeCommandParam BLOCK_TYPE = new WeCommandParam("方块类型", WeParamType.STRING, false, null, "如 minecraft:pig");
    private static final WeCommandParam BUTCHER_FLAGS = new WeCommandParam("标志", WeParamType.STRING, "如 -plangbtfr", true);
    private static final WeCommandParam TREE_TYPE = new WeCommandParam("树类型", WeParamType.STRING, List.of("tree", "bigtree", "redwood", "tallredwood", "birch", "jungle", "junglebush", "swamp", "acacia", "darkoak"));
    private static final WeCommandParam FOREST_TYPE = new WeCommandParam("森林类型", WeParamType.STRING, List.of("rainforest", "seasonalforest", "forest", "pinetree", "birchforest", "swamp"));

    // ── Builder 辅助 ──
    private static void register(String id, String displayName, String description, WeCommandUsage... usages) {
        List<WeCommandUsage> list = List.of(usages);
        WeCommand cmd = new WeCommand(id, displayName, description, currentCat, list);
        BY_ID.put(id, cmd);
        BY_CAT.computeIfAbsent(currentCat, k -> new ArrayList<>()).add(cmd);
    }

    private static WeCommandUsage instant(String id, String command, String description) {
        return new WeCommandUsage(id, command, command, description, List.of(), WeCommandType.INSTANT);
    }

    private static WeCommandUsage param(String id, String base, String template, String description, WeCommandParam... params) {
        return new WeCommandUsage(id, template, base, description, List.of(params), WeCommandType.PARAMETRIC);
    }

    private static WeCommandUsage bind(String id, String base, String template, String description, WeCommandParam... params) {
        return new WeCommandUsage(id, template, base, description, List.of(params), WeCommandType.BIND);
    }

    private static WeCommandUsage bindNoParam(String id, String command, String description) {
        return new WeCommandUsage(id, command, command, description, List.of(), WeCommandType.BIND);
    }

    // ── 通用 ──
    private static void registerGeneral() {
        currentCat = WeCommandCategory.GENERAL;
        register("undo", "撤销", "撤销上一步或多步 WorldEdit 操作",
                param("undo", "/undo", "/undo [步数] [玩家]", "撤销指定步数", STEPS, PLAYER));
        register("redo", "重做", "重做上一步或多步撤销的操作",
                param("redo", "/redo", "/redo [步数] [玩家]", "重做指定步数", STEPS, PLAYER));
        register("clearhistory", "清空历史", "清空所有撤销/重做历史",
                instant("clearhistory", "/clearhistory", "清空操作历史记录"));
        register("limit", "修改上限", "设置每次操作可修改方块的最大数量",
                param("limit", "//limit", "//limit <数量|-1>", "设置上限，-1 为无限制", AMOUNT));
        register("timeout", "超时设置", "设置表达式求值超时时间",
                param("timeout", "//timeout", "//timeout <毫秒>", "设置超时时间", AMOUNT));
        register("perf", "性能模式", "切换副作用/性能模式",
                instant("perf", "//perf", "切换性能模式"));
        register("reorder", "重排模式", "设置方块重排模式",
                param("reorder", "//reorder", "//reorder <模式>", "设置重排模式", new WeCommandParam("模式", WeParamType.ENUM, List.of("fast", "multi", "none"))));
        register("drawsel", "显示选区", "切换选区边框显示",
                instant("drawsel", "//drawsel", "切换选区边框显示"));
        register("gmask", "全局掩码", "设置作用于所有命令的全局掩码",
                param("gmask_set", "/gmask", "/gmask <掩码>", "设置全局掩码", MASK),
                instant("gmask_clear", "/gmask", "清除全局掩码"));
        register("toggleplace", "切换放置点", "在玩家位置与 pos1 之间切换放置点",
                instant("toggleplace", "/toggleplace", "切换放置点"));
        register("searchitem", "搜索物品", "搜索方块或物品 ID",
                param("searchitem", "/searchitem", "/searchitem <关键词>", "搜索物品/方块", QUERY));
        register("toggleeditwand", "切换魔杖", "启用/关闭选区魔杖功能",
                instant("toggleeditwand", "/toggleeditwand", "切换魔杖功能"));
        register("fast", "快速模式", "切换快速执行模式",
                instant("fast", "//fast", "切换快速模式"));
        register("we_reload", "重载配置", "重新加载 WorldEdit 配置",
                instant("we_reload", "//worldedit reload", "重载 WE 配置"));
        register("we_version", "WE 版本", "显示 WorldEdit 版本",
                instant("we_version", "//worldedit version", "显示版本信息"));
        register("we_tz", "设置时区", "设置 WorldEdit 时区",
                param("we_tz", "//worldedit tz", "//worldedit tz <时区>", "设置时区", new WeCommandParam("时区", WeParamType.STRING, "UTC")));
        register("we_help", "WE 帮助", "显示 WorldEdit 帮助",
                param("we_help", "/worldedit help", "/worldedit help [命令]", "查看帮助", new WeCommandParam("命令", WeParamType.STRING, "set", true)));
    }

    // ── 选区 ──
    private static void registerSelection() {
        currentCat = WeCommandCategory.SELECTION;
        register("wand", "获取魔杖", "获取选区魔杖（木斧）",
                instant("wand", "//wand", "获取选区魔杖"));
        register("toggleeditwand_sel", "切换魔杖", "切换选区魔杖功能",
                instant("toggleeditwand_sel", "/toggleeditwand", "切换魔杖功能"));
        register("sel", "选区模式", "切换选区模式",
                param("sel", "//sel", "//sel <模式>", "切换选区模式", new WeCommandParam("模式", WeParamType.ENUM, List.of("cuboid", "extend", "poly", "ellipsoid", "sphere", "cyl", "convex"))));
        register("desel", "取消选区", "取消当前选区",
                instant("desel", "//desel", "取消选区"));
        register("pos1", "设点 1", "将 pos1 设为当前位置",
                instant("pos1", "//pos1", "设 pos1 为玩家位置"));
        register("pos2", "设点 2", "将 pos2 设为当前位置",
                instant("pos2", "//pos2", "设 pos2 为玩家位置"));
        register("hpos1", "准星设点 1", "将 pos1 设为准星瞄准的方块",
                instant("hpos1", "//hpos1", "设 pos1 为准星方块"));
        register("hpos2", "准星设点 2", "将 pos2 设为准星瞄准的方块",
                instant("hpos2", "//hpos2", "设 pos2 为准星方块"));
        register("chunk_sel", "选择区块", "选择当前所在区块",
                instant("chunk_sel", "//chunk", "选择当前区块"),
                instant("chunk_all", "//chunk all", "选择所有相邻区块"));
        register("expand", "扩展选区", "向指定方向扩展选区",
                param("expand", "//expand", "//expand <数量> [方向]", "向方向扩展", AMOUNT, DIRECTION),
                param("expand_rev", "//expand", "//expand <数量> <反向数量> [方向]", "双向扩展", AMOUNT, AMOUNT, DIRECTION));
        register("expand_vert", "垂直扩展", "选区垂直扩展到世界高度",
                instant("expand_vert", "//expand vert", "垂直扩展到世界高度"));
        register("contract", "收缩选区", "向指定方向收缩选区",
                param("contract", "//contract", "//contract <数量> [方向]", "向方向收缩", AMOUNT, DIRECTION),
                param("contract_rev", "//contract", "//contract <数量> <反向数量> [方向]", "双向收缩", AMOUNT, AMOUNT, DIRECTION));
        register("shift", "平移选区", "平移选区位置",
                param("shift", "//shift", "//shift <数量> [方向]", "平移选区", AMOUNT, DIRECTION));
        register("outset", "外扩选区", "向所有方向外扩选区",
                param("outset", "//outset", "//outset [-hv] <数量>", "向外扩展", new WeCommandParam("竖直/水平", WeParamType.STRING, "-h", true), AMOUNT));
        register("inset", "内缩选区", "向所有方向内缩选区",
                param("inset", "//inset", "//inset [-hv] <数量>", "向内收缩", new WeCommandParam("竖直/水平", WeParamType.STRING, "-h", true), AMOUNT));
        register("size", "选区信息", "查看选区尺寸信息",
                instant("size", "//size", "显示选区信息"));
        register("count", "方块统计", "统计选区内指定方块数量",
                param("count", "//count", "//count [-d] <方块>", "统计方块", new WeCommandParam("-d", WeParamType.STRING, "-d", true), new WeCommandParam("方块", WeParamType.MASK, "stone")));
        register("distr", "分布统计", "查看选区内方块分布",
                instant("distr", "//distr", "显示方块分布"),
                param("distr_c", "//distr", "//distr -c", "显示剪贴板分布", new WeCommandParam("-c", WeParamType.FIXED, "-c")));
    }

    // ── 区域操作 ──
    private static void registerRegion() {
        currentCat = WeCommandCategory.REGION;
        register("set", "填充", "用指定方块填充整个选区",
                param("set", "//set", "//set <图案>", "填充选区", PATTERN));
        register("replace", "替换", "替换选区内的方块",
                param("replace_to", "//replace", "//replace <目标图案>", "替换所有非空气方块", TO_PATTERN),
                param("replace_from_to", "//replace", "//replace <被替换方块> <目标图案>", "按条件替换", FROM_MASK, TO_PATTERN));
        register("overlay", "覆盖", "在选区顶部表面覆盖一层方块",
                param("overlay", "//overlay", "//overlay <图案>", "顶部覆盖", PATTERN));
        register("center", "设中心", "将选区中心设为指定方块",
                param("center", "//center", "//center <图案>", "设置中心方块", PATTERN));
        register("walls", "四面墙", "在选区边界建造四面墙",
                param("walls", "//walls", "//walls <图案>", "建造四面墙", PATTERN));
        register("outline", "外壳", "建造选区外壳（墙+屋顶+地板）",
                param("outline", "//outline", "//outline <图案>", "建造外壳", PATTERN));
        register("hollow", "掏空", "将选区内部掏空，仅留外壳",
                param("hollow", "//hollow", "//hollow [厚度] [图案]", "掏空内部", THICKNESS, PATTERN));
        register("smooth", "平滑", "平滑选区地形表面",
                param("smooth", "//smooth", "//smooth [迭代次数]", "平滑地形", ITERATIONS));
        register("naturalize", "自然化", "表层 3 格泥土，下方石头",
                instant("naturalize", "//naturalize", "自然化地形"));
        register("line", "连线", "在选中的两点间画直线",
                param("line", "//line", "//line [-h] <图案> [粗细]", "两点连线", new WeCommandParam("空心", WeParamType.STRING, "-h", true), PATTERN, THICKNESS));
        register("curve", "曲线", "在 convex 选区的多点间画样条曲线",
                param("curve", "//curve", "//curve [-h] <图案> [粗细]", "多点曲线", new WeCommandParam("空心", WeParamType.STRING, "-h", true), PATTERN, THICKNESS));
        register("move", "移动", "移动选区内容",
                param("move", "//move", "//move [距离] [方向] [填充图案]", "移动选区", DISTANCE, DIRECTION, PATTERN));
        register("stack", "堆叠", "向指定方向重复堆叠选区",
                param("stack", "//stack", "//stack [次数] [方向/偏移]", "堆叠选区", COUNT, OFFSET),
                param("stack_full", "//stack", "//stack [-abers] [次数] [方向] [-m <掩码>]", "完整堆叠", new WeCommandParam("标志", WeParamType.STRING, "-a", true), COUNT, OFFSET, new WeCommandParam("掩码", WeParamType.MASK, "", true)));
        register("regen", "重新生成", "重新生成选区内地形",
                param("regen", "//regen", "//regen [生物群系] [种子]", "重新生成地形", BIOME, new WeCommandParam("种子", WeParamType.STRING, "", true)));
        register("deform", "变形", "用数学表达式变形选区",
                param("deform", "//deform", "//deform <表达式>", "表达式变形", EXPRESSION));
        register("forest", "生成森林", "在选区内生成森林",
                param("forest", "//forest", "//forest [类型] [密度]", "生成森林", FOREST_TYPE, new WeCommandParam("密度", WeParamType.INTEGER, "5", true)));
        register("flora", "生成植被", "在选区内随机生成花/草植被",
                param("flora", "//flora", "//flora [密度]", "生成植被", new WeCommandParam("密度", WeParamType.INTEGER, "5", true)));
    }

    // ── 生成 ──
    private static void registerGeneration() {
        currentCat = WeCommandCategory.GENERATION;
        register("sphere", "实心球", "生成实心球体",
                param("sphere", "//sphere", "//sphere <图案> <半径> [是否抬高]", "实心球体", PATTERN, RADIUS, new WeCommandParam("抬高", WeParamType.STRING, "true", true)));
        register("hsphere", "空心球", "生成空心球体",
                param("hsphere", "//hsphere", "//hsphere <图案> <半径> [是否抬高]", "空心球体", PATTERN, RADIUS, new WeCommandParam("抬高", WeParamType.STRING, "true", true)));
        register("cyl", "实心圆柱", "生成实心垂直圆柱",
                param("cyl", "//cyl", "//cyl <图案> <半径> [高度]", "实心圆柱", PATTERN, RADIUS, HEIGHT));
        register("hcyl", "空心圆柱", "生成空心垂直圆柱",
                param("hcyl", "//hcyl", "//hcyl <图案> <半径> [高度]", "空心圆柱", PATTERN, RADIUS, HEIGHT));
        register("cone", "圆锥", "生成圆锥体",
                param("cone", "//cone", "//cone <图案> <半径> <高度>", "生成圆锥", PATTERN, RADIUS, HEIGHT));
        register("pyramid", "实心金字塔", "生成实心金字塔",
                param("pyramid", "//pyramid", "//pyramid <图案> <尺寸>", "实心金字塔", PATTERN, SIZE));
        register("hpyramid", "空心金字塔", "生成空心金字塔",
                param("hpyramid", "//hpyramid", "//hpyramid <图案> <尺寸>", "空心金字塔", PATTERN, SIZE));
        register("generate", "表达式生成", "按数学公式生成形状",
                param("generate", "//generate", "//generate [-hroc] <图案> <表达式>", "表达式生成", new WeCommandParam("标志", WeParamType.STRING, "-h", true), PATTERN, EXPRESSION));
        register("feature", "地物生成", "生成 Minecraft 地物",
                param("feature", "//feature", "//feature <地物>", "生成地物", new WeCommandParam("地物", WeParamType.STRING, "minecraft:tree")));
        register("structure", "结构生成", "生成 Minecraft 结构",
                param("structure", "//structure", "//structure <结构>", "生成结构", new WeCommandParam("结构", WeParamType.STRING, "minecraft:village")));
        register("forestgen", "森林生成", "在周围生成森林",
                param("forestgen", "/forestgen", "/forestgen [尺寸] [类型] [密度]", "周围生成森林", SIZE, FOREST_TYPE, new WeCommandParam("密度", WeParamType.INTEGER, "5", true)));
        register("pumpkins", "南瓜群", "在周围生成南瓜群",
                param("pumpkins", "/pumpkins", "/pumpkins [尺寸]", "生成南瓜群", SIZE));
    }

    // ── 剪贴板 ──
    private static void registerClipboard() {
        currentCat = WeCommandCategory.CLIPBOARD;
        register("copy", "复制", "复制选区到剪贴板",
                instant("copy", "//copy", "复制选区"),
                param("copy_e", "//copy", "//copy -e", "复制包含实体", new WeCommandParam("-e", WeParamType.FIXED, "-e")));
        register("cut", "剪切", "剪切选区到剪贴板",
                instant("cut", "//cut", "剪切选区"),
                param("cut_e", "//cut", "//cut -e", "剪切包含实体", new WeCommandParam("-e", WeParamType.FIXED, "-e")));
        register("paste", "粘贴", "粘贴剪贴板内容",
                param("paste", "//paste", "//paste [-a] [-s] [-o]", "粘贴", new WeCommandParam("-a 忽略空气", WeParamType.STRING, "-a", true), new WeCommandParam("-s 选择新区域", WeParamType.STRING, "-s", true), new WeCommandParam("-o 粘贴到原点", WeParamType.STRING, "-o", true)));
        register("rotate", "旋转", "旋转剪贴板内容",
                param("rotate", "//rotate", "//rotate <Y轴角度> [X轴] [Z轴]", "旋转剪贴板", new WeCommandParam("Y角度", WeParamType.INTEGER, "90"), new WeCommandParam("X角度", WeParamType.INTEGER, "0", true), new WeCommandParam("Z角度", WeParamType.INTEGER, "0", true)));
        register("flip", "翻转", "翻转剪贴板内容",
                param("flip", "//flip", "//flip [轴向]", "沿轴向翻转", AXIS));
        register("clearclipboard", "清空剪贴板", "清空剪贴板内容",
                instant("clearclipboard", "/clearclipboard", "清空剪贴板"));
        register("revolve", "旋转复制", "绕轴旋转复制选区",
                param("revolve", "//revolve", "//revolve <角度> [轴] [复制份数]", "旋转复制", new WeCommandParam("角度", WeParamType.INTEGER, "90"), new WeCommandParam("轴", WeParamType.STRING, "y", true), new WeCommandParam("份数", WeParamType.INTEGER, "1", true)));
        register("schem_load", "加载蓝图", "加载 schematic 到剪贴板",
                param("schem_load", "/schematic load", "/schematic load [格式] <文件名>", "加载蓝图", FORMAT, FILENAME));
        register("schem_save", "保存蓝图", "将剪贴板保存为 schematic",
                param("schem_save", "/schematic save", "/schematic save [格式] <文件名>", "保存蓝图", FORMAT, FILENAME));
        register("schem_list", "蓝图列表", "列出 schematic 文件",
                param("schem_list", "/schematic list", "/schematic list [页码]", "列出蓝图", new WeCommandParam("页码", WeParamType.INTEGER, "1", true)));
        register("schem_delete", "删除蓝图", "删除 schematic 文件",
                param("schem_delete", "/schematic delete", "/schematic delete <文件名>", "删除蓝图", FILENAME));
        register("schem_formats", "蓝图格式", "列出所有可用 schematic 格式",
                instant("schem_formats", "/schematic formats", "列出格式"));
        register("schem_unload", "卸载蓝图", "卸载当前加载的 schematic",
                instant("schem_unload", "/schematic unload", "卸载蓝图"));
    }

    // ── 超级镐 ──
    private static void registerSuperPick() {
        currentCat = WeCommandCategory.SUPER_PICK;
        register("superpickaxe", "切换超级镐", "切换超级镐模式开关",
                instant("superpickaxe", "//", "切换超级镐"));
        register("sp_single", "单块模式", "超级镐单块挖掘模式",
                instant("sp_single", "/sp single", "单块模式"));
        register("sp_area", "区域模式", "超级镐区域挖掘模式",
                param("sp_area", "/sp area", "/sp area <范围>", "区域模式", RANGE));
        register("sp_recur", "递归模式", "超级镐递归挖掘模式",
                param("sp_recur", "/sp recur", "/sp recur <范围>", "递归模式", RANGE));
    }

    // ── 工具绑定 ──
    private static void registerTool() {
        currentCat = WeCommandCategory.TOOL;
        register("tool_selwand", "选区魔杖", "绑定选区魔杖",
                bindNoParam("tool_selwand", "/tool selwand", "绑定选区魔杖"));
        register("tool_navwand", "导航魔杖", "绑定导航魔杖",
                bindNoParam("tool_navwand", "/tool navwand", "绑定导航魔杖"));
        register("tool_repl", "替换工具", "绑定方块替换工具",
                bind("tool_repl", "/tool repl", "/tool repl <图案>", "绑定替换工具", PATTERN));
        register("tool_floodfill", "洪水填充", "绑定洪水填充工具",
                bind("tool_floodfill", "/tool floodfill", "/tool floodfill <图案> <范围>", "绑定洪水填充", PATTERN, RANGE));
        register("tool_tree", "树木工具", "绑定树木生成工具",
                bind("tool_tree", "/tool tree", "/tool tree <树类型>", "绑定树木工具", TREE_TYPE));
        register("tool_stacker", "堆叠工具", "绑定方块堆叠工具",
                bind("tool_stacker", "/tool stacker", "/tool stacker <范围> [增量]", "绑定堆叠工具", RANGE, new WeCommandParam("增量", WeParamType.INTEGER, "1", true)));
        register("tool_lrbuild", "远程建造", "绑定远程建造工具",
                bind("tool_lrbuild", "/tool lrbuild", "/tool lrbuild <左键方块> <右键方块>", "绑定远程建造", PATTERN, PATTERN));
        register("tool_farwand", "远程魔杖", "绑定远程选区魔杖",
                bindNoParam("tool_farwand", "/tool farwand", "绑定远程魔杖"));
        register("tool_info", "信息工具", "绑定方块信息查询工具",
                bindNoParam("tool_info", "/tool info", "绑定信息工具"));
        register("tool_deltree", "浮空树移除", "绑定浮空树移除工具",
                bindNoParam("tool_deltree", "/tool deltree", "绑定浮空树移除"));
        register("tool_cycler", "数据值切换", "绑定数据值循环工具",
                bindNoParam("tool_cycler", "/tool cycler", "绑定数据值切换"));
        register("tool_brush", "笔刷工具", "绑定笔刷工具",
                bindNoParam("tool_brush", "/tool brush", "绑定笔刷工具"));
        register("tool_none", "解除绑定", "解除手持物品的工具绑定",
                bindNoParam("tool_none", "/tool none", "解除工具绑定"));
        register("repl", "替换工具", "快捷绑定替换工具",
                bind("repl", "/repl", "/repl <图案>", "绑定替换工具", PATTERN));
        register("cycler", "数据值切换", "快捷绑定数据值循环工具",
                bindNoParam("cycler", "/cycler", "绑定数据值切换"));
        register("flood", "洪水填充", "快捷绑定洪水填充工具",
                bind("flood", "/flood", "/flood <图案> <范围>", "绑定洪水填充", PATTERN, RANGE));
        register("deltree", "浮空树移除", "快捷绑定浮空树移除工具",
                bindNoParam("deltree", "/deltree", "绑定浮空树移除"));
        register("farwand", "远程魔杖", "启用远程选区魔杖",
                bindNoParam("farwand", "/farwand", "绑定远程魔杖"));
        register("lrbuild", "远程建造", "快捷绑定远程建造工具",
                bind("lrbuild", "/lrbuild", "/lrbuild <左键方块> <右键方块>", "绑定远程建造", PATTERN, PATTERN));
        register("tree", "树木工具", "快捷绑定树木生成工具",
                bind("tree", "/tree", "/tree <树类型>", "绑定树木工具", TREE_TYPE));
        register("info", "工具信息", "查看当前手持物品绑定的工具信息",
                bindNoParam("info", "/info", "查看工具信息"));
        register("none", "解除绑定", "解除手持物品绑定",
                bindNoParam("none", "/none", "解除绑定"));
        register("mask", "笔刷掩码", "设置或清除笔刷掩码",
                param("mask_clear", "/mask", "/mask", "清除掩码"),
                param("mask_set", "/mask", "/mask <掩码>", "设置掩码", MASK));
        register("range", "笔刷范围", "设置笔刷最大作用范围",
                param("range", "/range", "/range <范围>", "设置笔刷范围", RANGE));
        register("size_tool", "笔刷大小", "设置笔刷大小/半径",
                param("size_tool", "/size", "/size <半径>", "设置笔刷大小", RADIUS));
        register("material", "笔刷材料", "设置笔刷材料",
                param("material", "/material", "/material <图案>", "设置笔刷材料", PATTERN));
        register("gmask_tool", "全局掩码", "设置所有笔刷的全局掩码",
                param("gmask_clear", "//gmask", "//gmask", "清除全局掩码"),
                param("gmask_set", "//gmask", "//gmask <掩码>", "设置全局掩码", MASK));
    }

    // ── 笔刷 ──
    private static void registerBrush() {
        currentCat = WeCommandCategory.BRUSH;
        register("brush_sphere", "球形笔刷", "绑定球形笔刷",
                bind("brush_sphere", "/brush sphere", "/brush sphere [-h] <图案> <半径>", "绑定球形笔刷", new WeCommandParam("空心", WeParamType.STRING, "-h", true), PATTERN, RADIUS));
        register("brush_cylinder", "圆柱笔刷", "绑定圆柱笔刷",
                bind("brush_cylinder", "/brush cylinder", "/brush cylinder [-h] <图案> <半径> [高度]", "绑定圆柱笔刷", new WeCommandParam("空心", WeParamType.STRING, "-h", true), PATTERN, RADIUS, HEIGHT));
        register("brush_set", "填充笔刷", "绑定方块填充笔刷",
                bind("brush_set", "/brush set", "/brush set <图案>", "绑定填充笔刷", PATTERN));
        register("brush_smooth", "平滑笔刷", "绑定地形平滑笔刷",
                bind("brush_smooth", "/brush smooth", "/brush smooth [迭代] [半径]", "绑定平滑笔刷", ITERATIONS, RADIUS));
        register("brush_clipboard", "剪贴板笔刷", "绑定剪贴板粘贴笔刷",
                bind("brush_clipboard", "/brush clipboard", "/brush clipboard [-a] [-p]", "绑定剪贴板笔刷", new WeCommandParam("-a 忽略空气", WeParamType.STRING, "-a", true), new WeCommandParam("-p 粘贴于原点", WeParamType.STRING, "-p", true)));
        register("brush_deform", "变形笔刷", "绑定表达式变形笔刷",
                bind("brush_deform", "/brush deform", "/brush deform <表达式> [半径]", "绑定变形笔刷", EXPRESSION, RADIUS));
        register("brush_gravity", "重力笔刷", "绑定重力模拟笔刷",
                bind("brush_gravity", "/brush gravity", "/brush gravity [半径]", "绑定重力笔刷", RADIUS));
        register("brush_raise", "升高笔刷", "绑定地形升高笔刷",
                bind("brush_raise", "/brush raise", "/brush raise [半径]", "绑定升高笔刷", RADIUS));
        register("brush_lower", "降低笔刷", "绑定地形降低笔刷",
                bind("brush_lower", "/brush lower", "/brush lower [半径]", "绑定降低笔刷", RADIUS));
        register("brush_erode", "侵蚀笔刷", "绑定地形侵蚀笔刷",
                bind("brush_erode", "/brush erode", "/brush erode [半径]", "绑定侵蚀笔刷", RADIUS));
        register("brush_dilate", "膨胀笔刷", "绑定地形膨胀笔刷",
                bind("brush_dilate", "/brush dilate", "/brush dilate [半径]", "绑定膨胀笔刷", RADIUS));
        register("brush_biome", "群系笔刷", "绑定生物群系笔刷",
                bind("brush_biome", "/brush biome", "/brush biome <群系> [半径]", "绑定群系笔刷", BIOME, RADIUS));
        register("brush_forest", "森林笔刷", "绑定森林生成笔刷",
                bind("brush_forest", "/brush forest", "/brush forest [类型] [密度] [半径]", "绑定森林笔刷", FOREST_TYPE, new WeCommandParam("密度", WeParamType.INTEGER, "5", true), RADIUS));
        register("brush_butcher", "击杀笔刷", "绑定生物击杀笔刷",
                bind("brush_butcher", "/brush butcher", "/brush butcher [标志] [半径]", "绑定击杀笔刷", BUTCHER_FLAGS, RADIUS));
        register("brush_extinguish", "灭火笔刷", "绑定灭火笔刷",
                bind("brush_extinguish", "/brush extinguish", "/brush extinguish [半径]", "绑定灭火笔刷", RADIUS));
        register("brush_snow", "积雪笔刷", "绑定积雪笔刷",
                bind("brush_snow", "/brush snow", "/brush snow [半径]", "绑定积雪笔刷", RADIUS));
        register("brush_snowsmooth", "雪平滑笔刷", "绑定雪层平滑笔刷",
                bind("brush_snowsmooth", "/brush snowsmooth", "/brush snowsmooth [半径]", "绑定雪平滑笔刷", RADIUS));
        register("brush_none", "解除笔刷", "解除手持物品的笔刷绑定",
                bindNoParam("brush_none", "/brush none", "解除笔刷绑定"));
        register("brush_mat", "笔刷材料", "替换当前笔刷使用的图案",
                param("brush_mat", "/mat", "/mat <图案>", "设置笔刷材料", PATTERN));
        register("brush_range", "笔刷范围", "设置当前笔刷范围",
                param("brush_range", "/range", "/range <范围>", "设置笔刷范围", RANGE));
        register("brush_size", "笔刷大小", "设置当前笔刷大小",
                param("brush_size", "/size", "/size <半径>", "设置笔刷大小", RADIUS));
        register("brush_mask", "笔刷掩码", "设置或清除当前笔刷掩码",
                param("brush_mask_clear", "/mask", "/mask", "清除笔刷掩码"),
                param("brush_mask_set", "/mask", "/mask <掩码>", "设置笔刷掩码", MASK));
        register("brush_gmask", "全局掩码", "设置所有笔刷的全局掩码",
                param("brush_gmask_clear", "//gmask", "//gmask", "清除全局掩码"),
                param("brush_gmask_set", "//gmask", "//gmask <掩码>", "设置全局掩码", MASK));
    }

    // ── 导航 ──
    private static void registerNavigation() {
        currentCat = WeCommandCategory.NAVIGATION;
        register("unstuck", "脱困", "从卡住的方块中脱离",
                instant("unstuck", "/unstuck", "脱困"));
        register("ascend", "向上一层", "向上穿过一层天花板",
                param("ascend", "/ascend", "/ascend [层数]", "向上穿越", new WeCommandParam("层数", WeParamType.INTEGER, "1", true)));
        register("descend", "向下一层", "向下穿过一层地板",
                param("descend", "/descend", "/descend [层数]", "向下穿越", new WeCommandParam("层数", WeParamType.INTEGER, "1", true)));
        register("ceil", "到天花板", "传送到头顶最近的天花板下方",
                param("ceil", "/ceil", "/ceil [空隙]", "到天花板", new WeCommandParam("空隙", WeParamType.INTEGER, "3", true)));
        register("thru", "穿墙", "穿过面前瞄准的墙",
                instant("thru", "/thru", "穿墙"));
        register("jumpto", "跳转", "传送到准星瞄准的方块位置",
                instant("jumpto", "/jumpto", "跳转"));
        register("up", "向上", "向上垂直移动",
                param("up", "/up", "/up [-fg] <距离>", "向上传送", new WeCommandParam("-f/-g", WeParamType.STRING, "-f", true), AMOUNT));
    }

    // ── 生物群系 ──
    private static void registerBiome() {
        currentCat = WeCommandCategory.BIOME;
        register("biome", "当前群系", "显示当前所在位置的生物群系",
                instant("biome", "/biome", "显示当前群系"));
        register("biomelist", "群系列表", "列出所有可用的生物群系",
                param("biomelist", "/biomelist", "/biomelist [页码]", "群系列表", new WeCommandParam("页码", WeParamType.INTEGER, "1", true)));
        register("biomeinfo", "群系信息", "查看准星或当前位置群系",
                param("biomeinfo", "/biomeinfo", "/biomeinfo [-pt]", "群系信息", new WeCommandParam("-p/-t", WeParamType.STRING, "-p", true)));
        register("setbiome", "设置群系", "将选区内方块设置为指定群系",
                param("setbiome", "//setbiome", "//setbiome [-p] <群系>", "设置群系", new WeCommandParam("-p 玩家列", WeParamType.STRING, "-p", true), BIOME));
        register("replacebiome", "替换群系", "按条件替换选区内群系",
                param("replacebiome", "//replacebiome", "//replacebiome <原群系> <目标群系>", "替换群系", BIOME, BIOME));
        register("generatebiome", "生成群系", "用指定群系覆盖选区地形",
                param("generatebiome", "//generatebiome", "//generatebiome [-hroc] <群系> <表达式>", "生成群系", new WeCommandParam("标志", WeParamType.STRING, "-h", true), BIOME, EXPRESSION));
    }

    // ── 区块 ──
    private static void registerChunk() {
        currentCat = WeCommandCategory.CHUNK;
        register("chunkinfo", "区块信息", "查看当前所在区块的信息",
                instant("chunkinfo", "/chunkinfo", "区块信息"));
        register("listchunks", "区块列表", "列出选区包含的所有区块",
                instant("listchunks", "/listchunks", "列出区块"));
        register("delchunks", "删除区块", "删除选区内的区块数据",
                instant("delchunks", "/delchunks", "删除区块"));
    }

    // ── 快照 ──
    private static void registerSnapshot() {
        currentCat = WeCommandCategory.SNAPSHOT;
        register("restore", "恢复快照", "从指定快照恢复选区地形",
                param("restore", "//restore", "//restore [快照名]", "恢复快照", SNAPSHOT_NAME));
        register("snapshot_use", "使用快照", "选择使用指定快照",
                param("snapshot_use", "/snapshot use", "/snapshot use <快照名>", "使用快照", SNAPSHOT_NAME));
        register("snapshot_sel", "选择快照", "从列表中选择快照",
                param("snapshot_sel", "/snapshot sel", "/snapshot sel <索引>", "选择快照", new WeCommandParam("索引", WeParamType.INTEGER, "1")));
        register("snapshot_list", "快照列表", "列出可用快照",
                param("snapshot_list", "/snapshot list", "/snapshot list [页码]", "快照列表", new WeCommandParam("页码", WeParamType.INTEGER, "1", true)));
        register("snapshot_before", "之前快照", "选择指定日期之前最近的快照",
                param("snapshot_before", "/snapshot before", "/snapshot before <日期>", "之前快照", DATE));
        register("snapshot_after", "之后快照", "选择指定日期之后最近的快照",
                param("snapshot_after", "/snapshot after", "/snapshot after <日期>", "之后快照", DATE));
    }

    // ── 脚本 ──
    private static void registerScript() {
        currentCat = WeCommandCategory.SCRIPT;
        register("cs", "执行脚本", "执行 CraftScript 脚本文件",
                param("cs", "/cs", "/cs <脚本> [参数...]", "执行脚本", SCRIPT, new WeCommandParam("参数", WeParamType.STRING, "", true)));
        register("cs_repeat", "重复脚本", "以上次参数重新执行脚本",
                param("cs_repeat", "/.s", "/.s [参数...]", "重复执行脚本", new WeCommandParam("参数", WeParamType.STRING, "", true)));
        register("cs_js", "执行 JS", "执行 JavaScript 脚本文件",
                param("cs_js", "/.js", "/.js <脚本> [参数...]", "执行 JS 脚本", SCRIPT, new WeCommandParam("参数", WeParamType.STRING, "", true)));
    }

    // ── 实用工具 ──
    private static void registerUtility() {
        currentCat = WeCommandCategory.UTILITY;
        register("fill", "填充洞穴", "填充指定半径的洞/坑",
                param("fill", "//fill", "//fill <图案> <半径> [深度]", "填充洞穴", PATTERN, RADIUS, DEPTH));
        register("fillr", "递归填充", "递归填充指定半径的洞/坑",
                param("fillr", "//fillr", "//fillr <图案> <半径> [深度]", "递归填充洞穴", PATTERN, RADIUS, DEPTH));
        register("drain", "抽干", "抽干指定半径内的水/岩浆",
                param("drain", "//drain", "//drain <半径>", "抽干液体", RADIUS));
        register("fixwater", "修复水", "修复指定半径内流动的水",
                param("fixwater", "/fixwater", "/fixwater <半径>", "修复水", RADIUS));
        register("fixlava", "修复岩浆", "修复指定半径内流动的岩浆",
                param("fixlava", "/fixlava", "/fixlava <半径>", "修复岩浆", RADIUS));
        register("removeabove", "移除上方", "移除头顶上方的方块",
                param("removeabove", "/removeabove", "/removeabove [尺寸] [高度]", "移除上方", SIZE, HEIGHT));
        register("removebelow", "移除下方", "移除脚下方的方块",
                param("removebelow", "/removebelow", "/removebelow [尺寸] [高度]", "移除下方", SIZE, HEIGHT));
        register("removenear", "移除附近", "移除附近指定半径内的指定方块",
                param("removenear", "/removenear", "/removenear <方块> <范围>", "移除附近方块", new WeCommandParam("方块", WeParamType.MASK, "stone"), RANGE));
        register("replacenear", "替换附近", "替换附近指定半径内的方块",
                param("replacenear", "/replacenear", "/replacenear [-f] <范围> <被替换方块> <目标图案>", "替换附近", new WeCommandParam("-f", WeParamType.STRING, "-f", true), RANGE, FROM_MASK, TO_PATTERN));
        register("snow", "积雪", "模拟积雪覆盖周围地形",
                param("snow", "/snow", "/snow [半径]", "积雪覆盖", RADIUS));
        register("thaw", "融化", "融化指定半径内的雪/冰",
                param("thaw", "/thaw", "/thaw [半径]", "融化冰雪", RADIUS));
        register("green", "草化", "将泥土转为草方块",
                param("green", "/green", "/green [-f] [半径]", "草化", new WeCommandParam("-f", WeParamType.STRING, "-f", true), RADIUS));
        register("extinguish", "灭火", "熄灭指定半径内的火焰",
                param("extinguish", "/ex", "/ex [半径]", "熄灭火焰", RADIUS));
        register("butcher", "击杀生物", "击杀指定半径内的生物",
                param("butcher", "/butcher", "/butcher [标志] [半径]", "击杀生物", BUTCHER_FLAGS, RADIUS));
        register("remove", "移除实体", "移除指定类型的实体",
                param("remove", "/remove", "/remove <类型> <半径>", "移除实体", BLOCK_TYPE, RANGE));
        register("calc", "计算器", "计算数学表达式",
                param("calc", "//calc", "//calc <表达式>", "计算表达式", EXPRESSION));
    }

    // ── 查询 API ──
    public static List<WeCommand> getAll() { return List.copyOf(BY_ID.values()); }

    public static Optional<WeCommand> getById(String id) { return Optional.ofNullable(BY_ID.get(id)); }

    public static List<WeCommand> getByCategory(WeCommandCategory cat) {
        return Collections.unmodifiableList(BY_CAT.getOrDefault(cat, List.of()));
    }

    public static Map<WeCommandCategory, List<WeCommand>> getCategoryMap() {
        Map<WeCommandCategory, List<WeCommand>> copy = new EnumMap<>(WeCommandCategory.class);
        BY_CAT.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return copy;
    }
}
