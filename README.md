# WorldEdit GUI (WeGui)

WorldEdit 可视化助手 —— 为 Minecraft Fabric 服务器提供快捷轮盘与分类 GUI 面板，让你无需记忆繁琐指令即可轻松使用 WorldEdit。

## 功能特性

- 🎯 **快捷轮盘** —— 按住快捷键呼出轮盘菜单，快速切换常用 WorldEdit 功能
- 📂 **分类 GUI 面板** —— 按功能分类展示所有可用指令，点选即用
- 📋 **参数输入面板** —— 直观的 UI 界面，无需记忆指令语法
- 👁 **粘贴预览** —— 粘贴前预览结构，避免误操作
- 🎨 **多语言支持** —— 支持中英文切换

## 演示

> 🎬 *功能演示 GIF *

<img width="600" height="450" alt="QQ20260709-221355" src="https://github.com/user-attachments/assets/e99900b4-fd8b-4af3-a398-b414655f8fcc" />

>渲染copy选区（旧）

<img width="854" height="510" alt="1" src="https://github.com/user-attachments/assets/ca23b138-f949-4641-ab72-a93caaa21d7a" />

>渲染copy选区（新）

<img width="600" height="450" alt="QQ20260710-143734" src="https://github.com/user-attachments/assets/b6bd67a1-0ad9-46be-ae4a-434ae58fa134" />

>图形GUI界面
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
git clone https://github.com/guchang233/WE_GUI.git
cd WE_GUI

# 构建 JAR
./gradlew build

# 构建产物位于 build/libs/
```

## 从 release 下载

每次推送时会自动构建并发布JAR文件到[Release](https://github.com/guchang233/WE_GUI/releases) 页面。

## 许可

[MIT](LICENSE)
