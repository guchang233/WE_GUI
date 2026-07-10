# WE 命令面板呈现方式与实现计划

## 1. 目标与信息架构

**最终目的**：打开面板 → 搜索/点分类 → 点命令 → （如需参数）填写 → 执行，全程尽量不手敲命令，降低出错。

**呈现方式（主面板）**：
- 采用**紧凑列表**：左侧分类/收藏/最近使用栏，顶部搜索框，主区域每行一个命令。
- 每行左侧为收藏星标 `★/☆`，右侧为 `中文名 §7命令模板`，一目了然。
- 单用法且无参数的 `INSTANT` 命令在列表中点击后直接执行，不进入参数页。

**多用法命令呈现**：
- 在参数输入页顶部使用**用法标签行**切换，例如 `//replace` 的“替换指定方块”与“替换所有方块”。
- 切换后表单即时重建，避免页面跳转。

**参数输入呈现**：
- 根据参数类型自动选择最合适的控件：
  - `PATTERN`/`MASK`：文本框 + “选”（背包物品）+ “常”（常用方块下拉）。
  - `ENUM`（群系/实体/方向等）：选项少时用按钮组，选项多时用可搜索下拉。
  - `FLAG`/`BOOLEAN`：切换按钮 `[√] / [ ]`。
  - `INTEGER`/`DECIMAL`：数字输入框 + 步进按钮。
  - `PLAYER`/`FILENAME`：可搜索下拉（玩家列表 / schematic 文件）。
- 底部实时显示生成的命令预览，非法时显示红色错误提示。

---

## 2. 当前状态分析（基于实际代码）

### 2.1 已完成

- `WeCommandScreen`：已继承 `GuiListBase`，有左侧分类栏、顶部搜索、底部按钮；`FilterEntry` 已替代旧的 `CategoryEntry`（`CategoryEntry.java` 已不存在）。
- `WidgetListCommands` / `WidgetCommandEntry`：已改为单命令每行，支持收藏星标和命令模板显示。
- `ParamInputScreen`：已实现用法选择按钮行、参数表单、实时预览、基础校验、`Tab` 切换焦点。
- `InventoryPickerScreen`：已支持背包 + 快捷栏、搜索、Tooltip、快捷栏高亮。
- `PickerControl`：已为 `PATTERN`/`MASK` 添加“选”和“常”两个按钮。
- `WeCommands`：已动态读取 Block/Item/Biome/EntityType 注册表生成选项。
- `Configs.CommandPanel` 与 `CommandHistory`：已实现收藏/最近使用的持久化。
- `ParamControlFactory`：`BOOLEAN` 已改为 `ToggleButton`。
- `OptionPickerScreen`：已支持搜索、返回、空结果提示，且已为选项按钮添加 Tooltip。
- 中英文语言键已补齐。

### 2.2 仍需处理

- `Configs.CommandPanel.COLUMNS` 默认值为 `3`，但当前实现是严格单列表，该配置未生效且与“紧凑列表”方向冲突，应移除。
- 左侧分类栏数量统计未随搜索词过滤实时更新（`buildFilterEntries()` 使用全量计数）。
- `WidgetCommandEntry` 未对命令模板文字做截断处理，长模板会溢出按钮。
- `WidgetCommandEntry` 对 `INSTANT` 命令未直接执行，仍打开参数页。
- `ParamInputScreen` 用法选择按钮未限制最大宽度，长描述可能换行混乱；未显示完整描述的 Tooltip。
- `ParamInputScreen` 错误提示位置固定，可能遮挡预览；预览过长时未截断。
- `ParamInputScreen` `Tab` 切换焦点时未确保控件在可视区域内。
- `WeCommands` 中 `//stack` 缺少 `-m <掩码>`、`-a`、`-b`、`-e`、`-r` 等标志；`//move` 缺少 `-b`、`-e`、`-a` 等标志。

---

## 3. 需要修改的文件与具体改动

### 3.1 `WeCommandScreen.java`

路径：`src/client/java/com/sow/wegui/client/screen/WeCommandScreen.java`

- 保持当前泛型声明与 `ISelectionListener<CommandRow>`。
- 修改 `buildFilterEntries()`：数量统计基于当前搜索词过滤后的结果计算（收藏/最近使用保持原逻辑，分类按 `matches` 过滤后计数）。
- 搜索框回调中除 `refreshList()` 外，调用 `updateCategoryButtons()` 以同步左侧数量。

### 3.2 `WidgetListCommands.java`

路径：`src/client/java/com/sow/wegui/client/screen/WidgetListCommands.java`

- 保持 `getAllEntries()` 过滤逻辑。
- 无需读取 `COLUMNS` 配置，保持紧凑单列表。

### 3.3 `WidgetCommandEntry.java`

路径：`src/client/java/com/sow/wegui/client/screen/WidgetCommandEntry.java`

- 命令模板截断：当 `displayName + template` 总宽度超过命令按钮宽度时，截断模板并加 `…`。
- 收藏星标与命令按钮间距保持 `STAR_WIDTH=18, GAP=2`，确保在不同 GUI 缩放下对齐。
- 点击命令按钮时：若命令只有一个 `INSTANT` 用法，直接调用 `CommandSender.send(baseCommand)` 并记录最近使用；否则打开 `ParamInputScreen`。

### 3.4 `ParamInputScreen.java`

路径：`src/client/java/com/sow/wegui/client/screen/ParamInputScreen.java`

- 用法选择按钮最大宽度限制为屏幕宽度的 40%（约 `width * 0.4`），过长标签截断并显示 Tooltip（Tooltip 使用完整 `usage.description()`）。
- 错误提示位置改为预览上方 14 像素，预览文字过长时截断并显示 Tooltip 展示完整命令。
- `Tab` 切换焦点时，调用 `setFocused(true)` 后，若焦点控件超出当前屏幕可视区域，调用 Malilib/Native 滚动或简单调整 `CONTENT_TOP` 偏移（当前屏幕无滚动条，优先保证焦点控件在表单区域内）。

### 3.5 `OptionPickerScreen.java`

路径：`src/client/java/com/sow/wegui/client/screen/OptionPickerScreen.java`

- 保持现状，选项 Tooltip 已存在，空结果提示已存在。

### 3.6 `InventoryPickerScreen.java`

路径：`src/client/java/com/sow/wegui/client/screen/InventoryPickerScreen.java`

- 保持现状。

### 3.7 `WeCommands.java`

路径：`src/main/java/com/sow/wegui/commands/WeCommands.java`

- 补全 `//stack`：
  - 模板：`//stack [次数] [方向] [-abers] [-m <掩码>]`
  - 参数：`COUNT`（可选）、`DIRECTION`（可选）、`-a`、`-b`、`-e`、`-r`、`-s`、`-m <掩码>`。
- 补全 `//move`：
  - 模板：`//move [数量] [方向] [-sbea] [图案]`
  - 参数：`AMOUNT`（可选）、`DIRECTION`（可选）、`-s`、`-b`、`-e`、`-a`、可选的 `PATTERN`。
- 检查 `brush` 系列：`brush_sphere`、`brush_cylinder`、`brush_clipboard`、`brush_smooth`、`brush_gravity`、`brush_butcher`、`brush_forest`、`brush_raise`、`brush_lower` 已覆盖半径/高度/迭代/标志等关键参数，保持现状。

### 3.8 `ParamControlFactory.java`

路径：`src/client/java/com/sow/wegui/client/screen/widget/ParamControlFactory.java`

- 保持 `BOOLEAN` 使用 `ToggleButton`。
- 保持 `PATTERN`/`MASK` 使用 `PickerControl`。
- 保持 `ENUM` 根据选项数量自动选择按钮组/下拉框/可搜索选择器。

### 3.9 `Configs.java`

路径：`src/client/java/com/sow/wegui/config/Configs.java`

- 移除 `CommandPanel.COLUMNS` 配置项及其在 `OPTIONS` 列表中的引用。

### 3.10 语言文件

路径：
- `src/main/resources/assets/wegui/lang/zh_cn.json`
- `src/main/resources/assets/wegui/lang/en_us.json`

- 移除 `commandPanelColumns` 相关语言键。
- 补充新增参数描述（如 `//stack` 的 `-m` 掩码等）如果需要新增独立语言键；否则直接以中文参数名展示。

---

## 4. 实现顺序

1. **代码清理与配置整理**：
   - 确认 `CategoryEntry.java` 已删除。
   - 从 `Configs.CommandPanel.OPTIONS` 移除 `COLUMNS` 并清理语言文件。
2. **命令覆盖补全**：在 `WeCommands.java` 中补充 `//stack` 与 `//move` 的完整标志参数。
3. **主面板 UX 打磨**：
   - `WeCommandScreen` 分类计数随搜索过滤实时更新。
   - `WidgetCommandEntry` 模板截断、`INSTANT` 命令直接执行。
4. **参数输入页打磨**：
   - `ParamInputScreen` 用法按钮最大宽度、Tooltip、错误提示与预览布局、`Tab` 焦点可见性。
5. **最终编译与运行验证**：运行 `./gradlew build` 与 `./gradlew runClient`，确认无编译错误且功能正常。

---

## 5. 验收标准

- `./gradlew build` 无编译错误。
- 打开 WE 面板：显示搜索框、左侧分类栏（数量正确）、紧凑命令列表。
- 搜索 `set`、`//replace`、`撤销` 等能正确过滤命令；左侧分类数量随搜索实时变化。
- 单用法 `INSTANT` 命令（如 `//wand`）点击后直接执行，不打开参数页。
- 点击星标后切换到“收藏”分类能看到该命令；取消收藏后消失。
- 执行任意命令后，“最近使用”分类按时间倒序显示。
- 多用法命令（如 `//replace`）进入参数页后顶部出现用法切换，切换后参数表单变化。
- `//set` 的 `PATTERN` 参数有“选”和“常”两个按钮，分别打开背包选择器和方块列表。
- `//stack` 的参数页中出现 `-m` 掩码、`-a`、`-b`、`-e`、`-r` 等可选标志。
- 非法数字/空必填参数点击执行时提示错误，不发送命令。
- 设置面板出现“命令面板”标签页，配置项正常读写；已无 `Columns` 配置项。

---

## 6. 假设与决策

- 继续基于 **Minecraft 1.21.11 + Fabric + malilib 0.27.x**。
- 命令数据仍采用手动注册，不解析 WorldEdit 命令树。
- 主面板最终采用**紧凑列表（每行一个命令）**；`COLUMNS` 配置直接移除。
- 多用法命令不再跳转额外页面，统一在 `ParamInputScreen` 内切换。
- 收藏/最近使用存储在本地 `wegui.json` 配置中。
- 命令图标、自定义排序、拖拽排序、物品分类过滤、键盘上下键导航不在本次范围内。
