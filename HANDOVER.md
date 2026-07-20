# 项目交接文档

## 项目概述

**WeGui** —— 一个 WorldEdit 的 Fabric GUI 增强模组，集成 Litematica 作为渲染后端。

- 仓库：`https://github.com/guchang233/WE_GUI.git`
- 本地路径：`E:\MOD_CODE\MIX`
- 双分支同步发布：main（MC 26.2）和 support/1.21.11
- 当前版本：v0.5.3

## 分支架构

| 分支 | MC 版本 | Java | loom | Fabric Loader | Fabric API | litematica | malilib |
|------|---------|------|------|---------------|------------|------------|---------|
| main | 26.2 | 25 | 1.17.14 | 0.19.3 | 0.154.2+26.2 | 0.28.4 | 0.29.2 |
| support/1.21.11 | 1.21.11 | 21 | 1.15.4 | 0.18.4 | 0.141.4+1.21.11 | 0.25.0 | 0.27.7 |

**Litematica 1.21.11 特殊处理**：Modrinth maven 未发布 0.25.0 的 POM/JAR，通过本地 jar 引入：
- 文件：`libs/litematica-fabric-1.21.11-0.25.0.jar`
- `.gitignore` 已加 `!libs/*.jar` 例外允许入库

## 关键差异（26.x vs 1.21.11）

| 概念 | 26.x | 1.21.11 |
|------|------|---------|
| IRenderer 方法名 | `onRenderWorldLast` | `onRenderWorldLastAdvanced` |
| Matrix4f 类型 | `Matrix4fc`（joml 接口） | `Matrix4f`（joml 类） |
| Camera 类型 | `CameraRenderState` | `Camera` |
| 额外参数 | `GpuBufferSlice, Vector4f` | 无 |
| 玩家消息 | `player.sendOverlayMessage / sendSystemMessage` | `player.displayClientMessage(msg, true/false)` |
| 屏幕访问 | `mc.gui.screen()` | `mc.screen` |
| 切换屏幕 | `mc.setScreenAndShow()` | `mc.setScreen()` |
| WE 适配器 | `FabricAdapter.get().toNativeBlockState() / fromNativeWorld()` | `FabricAdapter.adapt()` |
| OverlayRenderer.BoxType | 公开可用 | package-private，需用 RenderUtils 替代 |

**1.21.11 选区框实现**（替代 OverlayRenderer.renderSelectionBox）：
```java
RenderUtils.renderAreaOutline(pos1, pos2, 1.5f, COLOR_X, COLOR_Y, COLOR_Z);
RenderUtils.renderBlockOutline(pos1, 0.001f, 2.0f, COLOR_CORNER);
RenderUtils.renderBlockOutline(pos2, 0.001f, 2.0f, COLOR_CORNER);
```

## 核心模块

### LitematicaBridge（核心同步）
- 文件：`src/client/java/com/sow/wegui/client/LitematicaBridge.java`
- 职责：把 WE 剪贴板同步到 Litematica 的 `SchematicPlacementManager`，让 Litematica 渲染 ghost blocks 与 mismatch；同时渲染 WE 选区框
- 注册：被 `WeGuiClient.onInitializeClient()` 调用 `LitematicaBridge.register()`

**v0.5.3 性能优化**（重要）：
- 用 `ClipboardHolder` 引用比较（O(1)）代替 `getClipboardBlocks + hashCode`（O(n)）
- WE 每次 `//copy`/`//flip`/`//rotate` 创建新的 `ClipboardHolder` 实例
- origin 变化时调用 `placement.setOrigin(newPos, msg -> {})` O(1) 更新
- 只有引用变化时才 O(n) 重建 schematic

### WorldEditBridge（WE 状态查询）
- 文件：`src/client/java/com/sow\wegui\client\WorldEditBridge.java`
- 关键方法：
  - `getClipboardHolder(mc)`（v0.5.3 新增，O(1) 引用返回）
  - `getClipboardBlocks(mc)`（O(n)，仅在剪贴板变化时调用）
  - `getSelectionBounds(mc)` / `getPartialSelectionCorners(mc)`（选区框数据）
  - `getWandItem()` / `setWandItem(itemId)`（魔杖配置）
  - `AirOnlyExtent`（内部类，`PASTE_REPLACE_AIR_ONLY` 配置启用时跳过非空气方块）
- 异常缓存：`noClipboardUntilTick` 缓存无剪贴板状态 20 tick（1 秒），避免反复抛 `EmptyClipboardException`

### AxeModeHandler（木斧多模式）
- 文件：`src/client/java/com/sow\wegui\client\AxeModeHandler.java`
- 状态：
  - `fixedOrigin`（FIXED 模式下的固定 origin）
  - `lastMode`（监听 `PASTE_PLACEMENT_MODE` 配置变化）
- 方法：`getEffectiveOrigin(mc)` 返回当前模式实际 origin（FOLLOW_PLAYER → 玩家位置；FIXED → fixedOrigin）

### 原理图保存
- `SchematicExporter.java` —— 导出 .litematic / .nbt 到游戏目录
- `screen/SaveSchematicScreen.java` —— 保存界面（`mc.setScreen()` 1.21.11 / `mc.setScreenAndShow()` 26.x）

### 配置（Configs.java）
- 分类：Generic、StatusBar、PastePreview、ModeIndicator、Hotkeys、CommandPanel
- PastePreview 关键配置：
  - `PASTE_PLACEMENT_MODE`：FIXED（固定预览）/ FOLLOW_PLAYER（跟随玩家）
  - `PASTE_REPLACE_AIR_ONLY`：只替换空气方块
- **无 RenderStyles 分类**：v0.5.0 移除，渲染参数全部由 Litematica 维护
- 旧 RenderStyles 用户的迁移：删除对应 JSON 节点即可，配置会回退到默认值

### 其他重要文件
- `mixin/ButtonBaseMixin.java` —— 禁用 malilib 按钮的滚轮触发
- `screen/WeGuiConfigs.java` —— 配置界面，PASTE_PREVIEW tab 下有"保存原理图"和"WE 面板"按钮
- `WeGuiClient.java` —— 主初始化，调用 `LitematicaBridge.register()`

## 构建与发布流程

### 本地编译
```bash
.\gradlew.bat build -x test
```

### 发布新版本（重要！只需推送 tag）
```bash
# 1. 在对应分支修改 + 升级 gradle.properties 的 mod_version
# 2. 添加 LOGS/CHANGELOG-V<版本>.log
# 3. 提交
git add <files>
git commit -m "..."
# 4. 打 tag（格式必须为 v<版本>-26.x 或 v<版本>-1.21.11）
git tag -a v0.5.3-26.x -m "v0.5.3 for MC 26.2"
# 5. 推送分支和 tag
git push origin <branch>
git push origin v0.5.3-26.x
```

**CI 自动完成**（无需手动操作）：
1. 构建对应 MC 版本的 jar
2. 通过 `softprops/action-gh-release` 追加到共享 release（两个 tag 共用 `v0.5.3` release）
3. `Kir-Antipov/mc-publish` 发布到 Modrinth，按 MC 版本条件发布

### 关键文件：`.github/workflows/build.yml`
- 触发：`push tag v*-26.x / v*-1.21.11`
- Modrinth 依赖（两分支都必须完整四个）：
  ```yaml
  fabric-api(required){modrinth:P7dR8mSH}
  malilib@>=0.27.0 / >=0.29.2(required){modrinth:GcWjdA9I}
  litematica@>=0.25.0- / >=0.28.4-(required){modrinth:bEpr0Arc}
  worldedit(optional){modrinth:1u6JkXh5}
  ```
- **注意**：v0.5.2 曾因 1.21.11 分支漏写 litematica 而修复过一次，后续发布务必检查完整

### CHANGELOG 位置
- `LOGS/CHANGELOG-V<版本>.log`（如 `CHANGELOG-V0.5.3.log`）
- CI 自动读取此文件作为 release body 和 Modrinth changelog

## 版本历史（本次会话主线）

| 版本 | 主要变更 |
|------|----------|
| v0.5.0 | Litematica 渲染集成、原理图保存、移除 RenderStyles、新增 PASTE_REPLACE_AIR_ONLY、滚轮禁用、退出清理 |
| v0.5.1 | 移除同步节流（错误决策，导致大型建筑卡顿） |
| v0.5.2 | 修复 Modrinth 1.21.11 依赖缺失 litematica |
| v0.5.3 | 用 O(1) holder 引用比较 + setOrigin 优化，修复大型建筑卡顿 |

## 当前 Git 状态

- main 分支：`4343898`（v0.5.3-26.x 已推送）
- support/1.21.11 分支：`b0352c5`（v0.5.3-1.21.11 已推送）
- 两个 tag 都在远程

## 已知注意事项

1. **不要回到节流方案**：v0.5.3 用 O(1) 引用比较 + setOrigin 是正确方案，不要再加 `SYNC_INTERVAL_TICKS`
2. **SchematicPlacement.setOrigin 签名**：`placement.setOrigin(BlockPos, IStringConsumer)`，26.x 和 1.21.11 都有，用 `msg -> {}` 丢弃回调
3. **Litematica 1.21.11 maven 不可用**：不要尝试改回 `modImplementation "maven.modrinth:bEpr0Arc:DoYx2QST"`，必须用本地 jar
4. **OverlayRenderer.BoxType**：1.21.11 是 package-private，跨包调用改用 `RenderUtils.renderAreaOutline` + `RenderUtils.renderBlockOutline`
5. **WE 适配器方法**：1.21.11 用 `FabricAdapter.adapt(level)` 和 `FabricAdapter.adapt(transformedBase.toImmutableState())`，26.x 才用 `FabricAdapter.get().fromNativeWorld()` 等
6. **配置迁移**：移除 RenderStyles 后，旧用户配置 JSON 会有遗留 RenderStyles 节点，malilib 会忽略未知节点，自动回退到默认值，无需手动迁移代码
7. **开发环境冲突**：若报 "incompatible mods"，检查 `run\mods/` 是否有重复的 malilib/litematica jar（应只放 worldedit 等 loom 不会自动引入的依赖）

## 用户对话要点（已采纳的判断）

- v0.5.1 移除节流是错误判断 → 已修正为 v0.5.3 O(1) 优化
- v0.5.0 1.21.11 漏功能（jar 是 v0.4.9）→ 已通过 v0.5.0 移植全部功能解决
- v0.5.0 选区框渲染参数（0.001f / 2.0f / 1.5f）语义验证：经字节码反编译确认不是 partialTicks
- Modrinth 1.21.11 必须包含完整四个依赖（v0.5.2 修复）

## 移交清单

下一个 agent 需要：
1. 读取本文件了解项目背景
2. 查看 `LOGS/CHANGELOG-V0.5.3.log` 了解最新变更
3. 查看 `src/client/java/com/sow/wegui/client/LitematicaBridge.java` 了解核心同步逻辑
4. 查看 `.github/workflows/build.yml` 了解发布流程
5. 主要工作：按 `mod_version` + tag 格式发布新版本
