# WorldEdit GUI (WeGui)

WorldEdit 可视化助手 —— 为 Minecraft Fabric 服务器提供快捷轮盘与分类 GUI 面板，让你无需记忆繁琐指令即可轻松使用 WorldEdit。

## 功能特性

- 🎯 **快捷轮盘** —— 按住快捷键呼出轮盘菜单，快速切换常用 WorldEdit 功能
- 📂 **分类 GUI 面板** —— 按功能分类展示所有可用指令，点选即用
- 📋 **参数输入面板** —— 直观的 UI 界面，无需记忆指令语法
- 👁 **粘贴预览** —— 粘贴前预览结构，避免误操作
- 🎨 **多语言支持** —— 支持中英文切换

## 演示

> 🎬 *功能演示 GIF — 待添加*
>
> ![演示](docs/demo.gif)

## 环境要求

- **Minecraft**: 1.21.11
- **Fabric Loader**: >= 0.18.4
- **Java**: >= 21
- **Fabric API**: 0.141.4+
- **WorldEdit**: 7.4.2+（建议安装，部分功能可用）

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/)
2. 下载 [Fabric API](https://modrinth.com/mod/fabric-api)
3. 下载本模组的 JAR 文件放入 `mods` 文件夹
4. （可选）安装 [WorldEdit](https://modrinth.com/mod/worldedit) 以使用完整功能
5. 启动游戏

## 构建

```bash
# 克隆仓库
git clone https://github.com/sow/wegui.git
cd wegui

# 构建 JAR
./gradlew build

# 构建产物位于 build/libs/
```

## 从 GitHub Actions 下载

每次推送 tag（如 `v0.1`）时，GitHub Actions 会自动构建并发布 JAR 文件到 Release 页面。

## 许可

[MIT](LICENSE)