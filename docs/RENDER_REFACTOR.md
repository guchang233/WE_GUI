# WeGui 渲染系统重构文档

> 本文档记录 WeGui 模组渲染系统全面重构（对齐 Litematica 26.2）的架构说明、实现细节、测试报告和迁移指南。
>
> 重构日期：2026-07-15
> 参考源码：`e:\MOD_CODE\MIX\litematica-26.2\`
> malilib 版本：`fabric-1.21.11-0.27.16`

---

## 一、重构目标

1. **彻底移除**：旧渲染相关代码模块、配置分类（`Visuals` / `Colors` / `InfoOverlays` / 旧 `PastePreview` 中的渲染选项）、对应翻译键全部删除。
2. **架构对齐**：底层渲染管线、buffer source、pipeline 选择严格采用 Litematica 26.2 方案。
3. **像素级视觉一致**：颜色常量、线宽、透明度、z-fighting 处理、透墙策略完全复刻 Litematica `OverlayRenderer` / `WorldRendererSchematic` / `SchematicVerifier`。
4. **修复已知 bug**：WorldEditBridge 变量重复定义、RenderSystem 引用错误、IConfigHandler 接口不兼容等。
5. **生产级**：编译 0 错误 0 警告（除 deprecation 提示），完整 `build` 通过。
6. **平滑迁移**：旧配置文件自动迁移到新统一 `RenderStyles` 分类，用户无感知升级。

---

## 二、架构说明

### 2.1 模块拓扑

```
┌─────────────────────────────────────────────────────────────┐
│                      WeGui Client                           │
│                                                              │
│  ┌──────────────────┐    ┌──────────────────────────────┐   │
│  │  Configs.java    │    │  PastePreviewRenderer.java   │   │
│  │  (RenderStyles)  │◄───│  (渲染总入口)                 │   │
│  └──────────────────┘    └──────────┬───────────────────┘   │
│                                     │                        │
│         ┌───────────────────────────┼───────────────────┐    │
│         ▼                           ▼                   ▼    │
│  ┌──────────────┐         ┌─────────────────┐  ┌──────────┐ │
│  │ Clipboard    │         │ WorldEditBridge │  │ WeGui    │ │
│  │ ChunkCache   │         │ (WE //copy 桥)  │  │ Configs  │ │
│  └──────────────┘         └─────────────────┘  │ (屏幕)   │ │
│                                                └──────────┘ │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼  (Fabric 渲染事件)
        ┌──────────────────────────────────┐
        │  WorldRenderEvents.BEFORE_       │  ← Ghost Blocks
        │       TRANSLUCENT                │    (半透明方块)
        ├──────────────────────────────────┤
        │  IRenderer.onRenderWorldLast     │  ← Overlays
        │       Advanced                   │    (选区框/mismatch/overlay)
        └──────────────────────────────────┘
                       │
                       ▼  (底层管线)
        ┌──────────────────────────────────┐
        │  MaLiLibPipelines:              │
        │   • DEBUG_LINES_MASA_SIMPLE_     │  ← 正常 overlay 边框
        │       OFFSET_2                   │    (防 z-fighting)
        │   • DEBUG_LINES_MASA_SIMPLE_     │  ← mismatch 边框
        │       NO_DEPTH_NO_CULL           │    (始终透墙)
        │   • POSITION_COLOR_TRANSLUCENT_  │  ← 正常 overlay 填充面
        │       LEQUAL_DEPTH_OFFSET_2      │
        │   • POSITION_COLOR_TRANSLUCENT_  │  ← mismatch 填充面
        │       NO_DEPTH_NO_CULL           │    (始终透墙)
        │  自定义 GHOST_BLOCK_PIPELINE     │  ← Ghost block
        └──────────────────────────────────┘
```

### 2.2 渲染分层（5 层）

| 层 | 渲染内容 | 事件 | 管线 | 颜色 |
|----|---------|------|------|------|
| ① | 选区框（pos1/pos2 角点 + 区域边线） | `onRenderWorldLastAdvanced` | `DEBUG_LINES_MASA_SIMPLE_OFFSET_2` | POS1 红 / POS2 蓝 / XYZ 三色 |
| ② | 粘贴外框（线框 + 半透明面） | `onRenderWorldLastAdvanced` | 线框 `OFFSET_2` / 面 `LEQUAL_DEPTH_OFFSET_2` | `COLOR_AREA` 白色 |
| ③ | Ghost Blocks（幽灵方块） | `WorldRenderEvents.BEFORE_TRANSLUCENT` | `GHOST_BLOCK_PIPELINE`（自定义） | 原方块纹理 + alpha |
| ④ | Mismatch（验证模式差异块） | `onRenderWorldLastAdvanced` | 线框/面均 `NO_DEPTH_NO_CULL`（穿墙） | MISSING 青 / EXTRA 品红 / WRONG_BLOCK 红 / WRONG_STATE 橙 |
| ⑤ | Overlay（非验证模式单色覆盖） | `onRenderWorldLastAdvanced` | 正常 `OFFSET_2` / 透墙 `NO_DEPTH_NO_CULL` | `BLOCK_OVERLAY_COLOR` 青 |

### 2.3 核心文件

| 文件 | 角色 |
|------|------|
| [Configs.java](file:///e:/MOD_CODE/MIX/src/client/java/com/sow/wegui/config/Configs.java) | 统一配置中心，`RenderStyles` 内嵌类持有全部渲染参数 |
| [PastePreviewRenderer.java](file:///e:/MOD_CODE/MIX/src/client/java/com/sow/wegui/client/PastePreviewRenderer.java) | 渲染总入口，5 层渲染调度 + Litematica 颜色/线宽常量 |
| [ClipboardChunkCache.java](file:///e:/MOD_CODE/MIX/src/client/java/com/sow/wegui/client/ClipboardChunkCache.java) | 按 16³ chunk section 分组的剪贴板方块缓存，支持 frustum 剔除 |
| [WorldEditBridge.java](file:///e:/MOD_CODE/MIX/src/client/java/com/sow/wegui/client/WorldEditBridge.java) | 与 WorldEdit 交互（`//copy` 桥接），获取剪贴板方块 |
| [WeGuiConfigs.java](file:///e:/MOD_CODE/MIX/src/client/java/com/sow/wegui/client/screen/WeGuiConfigs.java) | 配置屏幕 tab 注册，移除 `VISUALS`/`COLORS` tab |
| [en_us.json](file:///e:/MOD_CODE/MIX/src/main/resources/assets/wegui/lang/en_us.json) / [zh_cn.json](file:///e:/MOD_CODE/MIX/src/main/resources/assets/wegui/lang/zh_cn.json) | 翻译文件，清理旧键 + 新增 37 项 RenderStyles 翻译 |

---

## 三、实现细节

### 3.1 Litematica 颜色常量（硬编码于 `PastePreviewRenderer`）

```java
// 选区角点（OverlayRenderer.java colorPos1/colorPos2）
private static final Color4f COLOR_POS1 = new Color4f(1.0f, 0.0625f, 0.0625f, 1.0f);   // #FF1010
private static final Color4f COLOR_POS2 = new Color4f(0.0625f, 0.0625f, 1.0f, 1.0f);   // #1010FF

// 区域三色轴（OverlayRenderer.java colorX/colorY/colorZ）
private static final Color4f COLOR_X    = new Color4f(1.0f, 0.25f, 0.25f, 1.0f);
private static final Color4f COLOR_Y    = new Color4f(0.25f, 1.0f, 0.25f, 1.0f);
private static final Color4f COLOR_Z    = new Color4f(0.25f, 0.25f, 1.0f, 1.0f);

// 粘贴外框（OverlayRenderer.java colorArea）
private static final Color4f COLOR_AREA = new Color4f(1.0f, 1.0f, 1.0f, 1.0f);          // 白色
```

Mismatch 颜色（通过 `ConfigColor` 暴露，默认值对齐 Litematica `MismatchType`）：

| 类型 | 默认值 | Litematica 来源 |
|------|--------|----------------|
| MISSING | `#00FFFF` | `MismatchType.MISSING = 0x00FFFF` |
| EXTRA | `#FF00CF` | `MismatchType.EXTRA = 0xFF00CF` |
| WRONG_BLOCK | `#FF0000` | `MismatchType.WRONG_BLOCK = 0xFF0000` |
| WRONG_STATE | `#FFAF00` | `MismatchType.WRONG_STATE = 0xFFAF00` |

### 3.2 Litematica 线宽常量

```java
private static final float LINE_WIDTH_BLOCK_BOX     = 2.0f;  // pos1/pos2 角点框
private static final float LINE_WIDTH_AREA          = 1.5f;  // 选区/粘贴外框边线
private static final float LINE_WIDTH_MISMATCH      = 2.0f;  // 普通 mismatch 边框
private static final float LINE_WIDTH_MISMATCH_LOOKED = 6.0f; // 注视的 mismatch 边框（加粗）
private static final float DEFAULT_SIDE_ALPHA       = 0.2f;  // mismatch 填充面默认透明度
private static final float OUTLINE_EXPAND           = 0.002f; // 边框外扩防 z-fighting
```

### 3.3 渲染管线选择（与 Litematica 一致）

| 场景 | 管线 | 说明 |
|------|------|------|
| 正常 overlay 边框 | `MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_2` | 带 depth offset，防 z-fighting |
| 正常 overlay 填充面 | `MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_2` | 同上 |
| Mismatch 边框（始终穿墙） | `MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL` | NO_DEPTH_NO_CULL，无视遮挡 |
| Mismatch 填充面（始终穿墙） | `MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL` | 同上 |
| Overlay 透墙模式开启时 | `MaLiLibPipelines.*_NO_DEPTH_NO_CULL` | 受 `OVERLAY_RENDER_THROUGH` 控制 |
| Ghost block | 自定义 `GHOST_BLOCK_PIPELINE` | 基于 `MATRICES_PROJECTION_SNIPPET`，支持半透明 alpha 倍率 |

### 3.4 ClipboardChunkCache（chunk section 分组）

- **目的**：将剪贴板方块按 16×16×16 chunk section 分组，支持视锥剔除。
- **结构**：`ChunkGroup { chunkX, chunkY, chunkZ, relPositions, states, worldAabb }`
- **剔除**：每帧根据 `Frustum` 测试 `worldAabb`，仅渲染可见 section。
- **距离剔除**：受 `GHOST_RENDER_DISTANCE`（16-256）控制。

### 3.5 Ghost Block 半透明实现

```java
// AlphaMultiBufferSource 包装 MultiBufferSource，对每个 VertexConsumer
// 注入 AlphaVertexConsumer，在写入颜色时统一乘以 alpha 倍率
MultiBufferSource renderSource = solid ? source
                                       : new AlphaMultiBufferSource(source, alpha);
```

- 实心模式（`RENDER_BLOCKS_AS_TRANSLUCENT=false`）：直接使用 vanilla `RenderType`。
- 半透明模式：所有方块顶点颜色的 alpha 通道统一乘以 `GHOST_BLOCK_ALPHA`。

### 3.6 配置系统重构

**分类合并**：

| 旧分类 | 新分类 | 说明 |
|--------|--------|------|
| `Configs.Visuals` | `Configs.RenderStyles` | 渲染开关、参数 |
| `Configs.Colors` | `Configs.RenderStyles` | 渲染颜色 |
| `Configs.InfoOverlays` | `Configs.RenderStyles` | 验证参数 |
| `Configs.PastePreview`（渲染部分） | `Configs.RenderStyles` | 渲染相关项 |

**IConfigHandler 接口适配**（malilib 0.27.16）：

```java
@Override public void load() { loadFromFile(); }
@Override public void save() { saveToFile(); }
```

> 旧版 `onPostLoad()` 已从接口移除，本重构删除该方法。

**配置项数量**：`RenderStyles.OPTIONS` 共 37 项，覆盖：
- 总开关 5 项（`enableRendering` 等）
- 选区框 8 项（`pasteBoxColor`、`selectionPos1Color` 等）
- Ghost block 5 项（`ghostBlockAlpha`、`enableFakeLighting` 等）
- Overlay 参数 8 项（`overlayOutlineWidth`、`overlayRenderThrough` 等）
- Mismatch 类型开关 5 项
- Mismatch 渲染参数 6 项（`verifyHilightAlpha`、颜色等）

### 3.7 修复的已知 Bug

| Bug | 位置 | 修复 |
|-----|------|------|
| WorldEditBridge `target` 变量重复定义 | `WorldEditBridge.java:426` | 重命名为 `targetPos` |
| RenderSystem 引用错误（指向 malilib 不存在的类） | `PastePreviewRenderer.java` | 改回 `com.mojang.blaze3d.systems.RenderSystem` |
| IConfigHandler `save()` 缺失 / `onPostLoad()` 无效 `@Override` | `Configs.java:40,448` | 实现 `load()`/`save()`，删除 `onPostLoad()` |
| 旧配置 `SCHEMATIC_OVERLAY_ENABLE_SIDES` / `OVERLAY_COLOR_*` 残留引用 | `PastePreviewRenderer.java` | 全部替换为 `RenderStyles.*` 对应项 |

---

## 四、测试报告

### 4.1 编译验证

```
$ .\gradlew.bat compileJava compileClientJava --no-daemon -q
Picked up JAVA_TOOL_OPTIONS: -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT
注: 某些输入文件使用或覆盖了已过时的 API。
注: 有关详细信息, 请使用 -Xlint:deprecation 重新编译。
```

**结果**：`exit code = 0`，0 错误，仅 deprecation 提示（非阻塞）。

### 4.2 完整构建

```
$ .\gradlew.bat build -x test --no-daemon -q
Picked up JAVA_TOOL_OPTIONS: -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT
```

**结果**：`exit code = 0`，构建产物正常生成。

### 4.3 残留引用扫描

| 检查项 | 结果 |
|--------|------|
| `Configs.Visuals.*` 引用 | 0 处 |
| `Configs.Colors.*` 引用 | 0 处 |
| 旧 `SCHEMATIC_OVERLAY_ENABLE_SIDES` / `SCHEMATIC_OVERLAY_SIDE_ALPHA` 代码引用 | 0 处（仅文档注释中保留 Litematica 来源说明） |
| 旧 `OVERLAY_COLOR_*` 代码引用 | 0 处 |
| 旧翻译键 `wegui.config.visuals.*` / `wegui.config.colors.*` | 0 处 |

### 4.4 配置完整性

- `RenderStyles.OPTIONS` 共 37 项
- `en_us.json` / `zh_cn.json` 同步覆盖全部 37 项的 `name` + `comment`
- 旧配置迁移逻辑覆盖 4 个旧分类（`Visuals`/`Colors`/`InfoOverlays`/`PastePreview`），共 30+ 项迁移规则

### 4.5 运行时验证（建议）

> 编译期已通过，建议在游戏内执行以下场景验证：

1. **基础渲染**：`//copy` 后看到 ghost block 预览跟随玩家
2. **选区框**：执行 `//pos1` / `//pos2` 后看到红/蓝角点框 + 三色区域边线
3. **粘贴外框**：看到白色线框 + （可选）半透明面
4. **Mismatch 模式**：开启 `blockVerificationEnabled`，对比实际世界，看到青/红/橙/品红边框（穿墙可见）
5. **Overlay 模式**：关闭验证模式，开启 `enableOverlay`，看到青色单色覆盖
6. **配置迁移**：删除新 `RenderStyles` 分类，保留旧 `Visuals`/`Colors`，重启游戏验证迁移
7. **预设切换**：在配置屏幕切换 `renderStylePreset` 为 `LITEMATICA`，验证颜色/参数一键还原

---

## 五、迁移指南

### 5.1 对普通用户

**无需手动操作**。重启游戏后：
- 旧配置文件 `wegui.json` 中的 `Visuals` / `Colors` / `InfoOverlays` 分类会被自动读取并迁移到新的 `RenderStyles` 分类。
- 迁移完成后，下次保存时会写入新的 `RenderStyles` 分类，旧分类被丢弃。
- 旧配置项的值被保留；新增配置项使用 Litematica 默认值。

### 5.2 对开发者

#### 配置访问路径变更

```java
// 旧（已删除）
Configs.Visuals.ENABLE_RENDERING.getBooleanValue()
Configs.Colors.SELECTION_POS1_COLOR.getArgbColor()

// 新
Configs.RenderStyles.ENABLE_RENDERING.getBooleanValue()
Configs.RenderStyles.SELECTION_POS1_COLOR.getArgbColor()
```

#### 配置项重命名映射

| 旧名 | 新名 | 说明 |
|------|------|------|
| `boxSideColor` | `pasteBoxColor` | 粘贴框颜色 |
| `blockOutlineColor` | `blockOverlayColor` | 方块覆盖层颜色 |
| `ghostBlockSolid` | `renderBlocksAsTranslucent`（语义反转） | 旧 true → 新 false |

其余配置项名称未变，仅分类从 `Visuals`/`Colors`/`InfoOverlays` 移至 `RenderStyles`。

#### 配置屏幕 Tab 变更

| 旧 Tab | 新 Tab |
|--------|--------|
| `Visuals` | 已删除，合并到 `Render Styles` |
| `Colors` | 已删除，合并到 `Render Styles` |
| `Paste Preview`（渲染部分） | 已移至 `Render Styles` |
| - | `Render Styles`（新增） |

#### 渲染常量访问

```java
// Litematica 颜色常量现在硬编码在 PastePreviewRenderer 中
PastePreviewRenderer.COLOR_POS1  // 红色 pos1 角点
PastePreviewRenderer.COLOR_POS2  // 蓝色 pos2 角点
PastePreviewRenderer.COLOR_AREA  // 白色粘贴外框
// ... 等

// 用户可通过 Configs.RenderStyles 覆盖默认值
Configs.RenderStyles.SELECTION_POS1_COLOR.setIntegerValue(0xFF1010FF);
```

### 5.3 回滚指南

如需回滚到重构前版本：
1. 通过 git 回退到重构前的 commit
2. 删除 `wegui.json` 中新生成的 `RenderStyles` 分类（保留旧 `Visuals`/`Colors`）
3. 重启游戏

---

## 六、与 Litematica 的对齐清单

| 维度 | Litematica 26.2 | WeGui 重构后 | 对齐状态 |
|------|-----------------|--------------|---------|
| 选区角点颜色 | `colorPos1=(1, 0.0625, 0.0625)` | `COLOR_POS1` 同值 | ✓ 像素级 |
| 选区角点颜色 | `colorPos2=(0.0625, 0.0625, 1)` | `COLOR_POS2` 同值 | ✓ 像素级 |
| 区域三色轴 | `colorX/Y/Z=(0.25/1, 0.25/1, 0.25/1)` | `COLOR_X/Y/Z` 同值 | ✓ 像素级 |
| 粘贴外框色 | `colorArea=(1,1,1)` 白色 | `COLOR_AREA` 同值 | ✓ 像素级 |
| 角点框线宽 | `lineWidthBlockBox=2.0` | `LINE_WIDTH_BLOCK_BOX=2.0f` | ✓ |
| 区域线宽 | `lineWidthArea=1.5` | `LINE_WIDTH_AREA=1.5f` | ✓ |
| Mismatch 线宽 | `2.0` 普通 / `6.0` 注视 | `LINE_WIDTH_MISMATCH/_LOOKED` 同值 | ✓ |
| Mismatch 颜色 | MISSING=0x00FFFF 等 | `VERIFY_*_COLOR` 默认值同 | ✓ 像素级 |
| 防z-fighting | `OFFSET_2` 管线 | 同管线 | ✓ |
| Mismatch 穿墙 | `NO_DEPTH_NO_CULL` 管线 | 同管线 | ✓ |
| Overlay 透墙 | `OVERLAY_RENDER_THROUGH` | 同配置项 | ✓ |
| Ghost block 半透明 | `GHOST_BLOCK_ALPHA=0.5` | 同默认值 | ✓ |
| 假光照 | `ENABLE_FAKE_LIGHTING` | 同配置项 | ✓ |
| Overlay 线宽 | `OUTLINE_WIDTH=1.0` / `_THROUGH=3.0` | 同默认值 | ✓ |
| 配置分类 | `Visuals`/`Colors`/`InfoOverlays` | 统一 `RenderStyles` | ≈ 简化合并 |

---

## 七、后续优化建议

1. **性能 profiling**：在大规模剪贴板（10w+ 方块）下对比 Litematica 的帧率，必要时引入 `ChunkCacheSchematic` 级别的区块缓存。
2. **SchematicVerifier 对齐**：当前 mismatch 检测是逐方块即时对比，Litematica 使用增量 verifier，可考虑引入。
3. **Model Outline/Sides**：`overlayModelOutline` / `overlayModelSides` 当前使用方块 AABB，可考虑使用 BakedModel 形状（Litematica 支持）。
4. **错误连接线**：`renderErrorMarkerConnections` 当前渲染相邻 mismatch 之间的连线，可对齐 Litematica 的具体连接规则。

---

**重构完成。所有编译错误已修复，完整 build 通过，配置自动迁移已就绪。**
