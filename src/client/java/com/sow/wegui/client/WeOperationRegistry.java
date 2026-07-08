package com.sow.wegui.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令用法 ID 到直接 API 操作的映射。
 * 所有注册操作均直接调用 WorldEdit API，不经过命令字符串解析。
 */
public final class WeOperationRegistry {
    private static final Map<String, WeOperation> OPERATIONS = new HashMap<>();

    static {
        // 通用
        register("undo", new UndoOperation());
        register("redo", new RedoOperation());
        register("clearhistory", new ClearHistoryOperation());

        // 选区
        register("sel", new SelOperation());
        register("desel", new DeselOperation());
        register("pos1", new Pos1Operation());
        register("pos2", new Pos2Operation());
        register("hpos1", new HPos1Operation());
        register("hpos2", new HPos2Operation());
        register("chunk_sel", new ChunkSelOperation());
        register("chunk_all", new ChunkSelOperation());
        register("expand", new ExpandOperation());
        register("expand_rev", new ExpandOperation());
        register("expand_vert", new ExpandOperation());
        register("contract", new ContractOperation());
        register("contract_rev", new ContractOperation());
        register("shift", new ShiftOperation());
        register("outset", new OutsetOperation());
        register("inset", new InsetOperation());
        register("size", new SizeOperation());
        register("count", new CountOperation());
        register("distr", new DistrOperation());
        register("distr_c", new DistrOperation());

        // 区域操作
        register("set", new SetOperation());
        register("replace_to", new ReplaceOperation());
        register("replace_from_to", new ReplaceOperation());
        register("overlay", new OverlayOperation());
        register("center", new CenterOperation());
        register("walls", new WallsOperation());
        register("outline", new OutlineOperation());
        register("hollow", new HollowOperation());
        register("smooth", new SmoothOperation());
        register("naturalize", new NaturalizeOperation());
        register("move", new MoveOperation());
        register("stack", new StackOperation());

        // 生成
        register("sphere", new SphereOperation());
        register("hsphere", new HSphereOperation());
        register("cyl", new CylOperation());
        register("hcyl", new HCylOperation());
        register("cone", new ConeOperation());
        register("pyramid", new PyramidOperation());
        register("hpyramid", new HPyramidOperation());
        register("generate", new GenerateOperation());

        // 剪贴板
        register("copy", new CopyOperation());
        register("copy_e", new CopyOperation());
        register("cut", new CutOperation());
        register("cut_e", new CutOperation());
        register("paste", new PasteOperation());
        register("rotate", new RotateOperation());
        register("flip", new FlipOperation());
        register("clearclipboard", new ClearClipboardOperation());
        register("revolve", new RevolveOperation());
        register("schem_load", new SchemLoadOperation());
        register("schem_save", new SchemSaveOperation());
        register("schem_list", new SchemListOperation());
        register("schem_delete", new SchemDeleteOperation());
        register("schem_formats", new SchemFormatsOperation());
        register("schem_unload", new SchemUnloadOperation());

        // 实用工具
        register("fill", new FillOperation());
        register("fillr", new FillrOperation());
        register("drain", new DrainOperation());
        register("fixwater", new FixWaterOperation());
        register("fixlava", new FixLavaOperation());
        register("removeabove", new RemoveAboveOperation());
        register("removebelow", new RemoveBelowOperation());
        register("removenear", new RemoveNearOperation());
        register("replacenear", new ReplaceNearOperation());
        register("snow", new SnowOperation());
        register("thaw", new ThawOperation());
        register("green", new GreenOperation());
        register("extinguish", new ExtinguishOperation());
        register("butcher", new ButcherOperation());
        register("remove", new RemoveOperation());
        register("calc", new CalcOperation());

        // 导航
        register("unstuck", new UnstuckOperation());
        register("ascend", new AscendOperation());
        register("descend", new DescendOperation());
        register("ceil", new CeilOperation());
        register("thru", new ThruOperation());
        register("jumpto", new JumptoOperation());
        register("up", new UpOperation());

        // 生物群系
        register("setbiome", new SetBiomeOperation());
        register("replacebiome", new ReplaceBiomeOperation());
        register("biomeinfo", new BiomeInfoOperation());
    }

    private WeOperationRegistry() {}

    private static void register(String usageId, WeOperation operation) {
        OPERATIONS.put(usageId, operation);
    }

    public static WeOperation get(String usageId) {
        return OPERATIONS.get(usageId);
    }
}
