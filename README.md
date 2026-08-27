# ♟️ Chess H4ck for Android (国际象棋悬浮辅助移动版)

<div align="center">

  <img src="app/src/main/res/drawable/app_logo.png" alt="Chess H4ck Logo" width="128" height="128" />

  <h3>🌟 全功能国际象棋悬浮辅助 · Stockfish 18 智能引擎 · 毛玻璃极简美学 🌟</h3>

  <p>
    <b>专为 Android 移动设备与平板设计，支持对局分析、自由沙盒排局、AI 自动推演与局势评分</b>
  </p>

  <p>
    <a href="https://github.com/LinHouYu/chess_H4ck_Android"><img src="https://img.shields.io/badge/Platform-Android_7.0%2B-38BDF8?logo=android&logoColor=white" alt="Platform" /></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
    <a href="https://stockfishchess.org/"><img src="https://img.shields.io/badge/Engine-Stockfish_18-22C55E?logo=lichess&logoColor=white" alt="Stockfish" /></a>
    <a href="#-许可证与免责声明-license--disclaimer"><img src="https://img.shields.io/badge/License-CC_BY--NC_4.0-E11D48?logo=creative-commons&logoColor=white" alt="License" /></a>
    <a href="#-许可证与免责声明-license--disclaimer"><img src="https://img.shields.io/badge/Usage-Non--Commercial_Only-F59E0B" alt="Non-Commercial" /></a>
  </p>

  <p>
    <a href="#-多平台版本导航-cross-platform">🌐 多平台版本</a> •
    <a href="#-核心亮点与特性">✨ 核心特性</a> •
    <a href="#-界面与动效美学">🎨 界面美学</a> •
    <a href="#-快捷控制面板一览">🎛️ 快捷功能</a> •
    <a href="#-安装与使用教程">📖 使用教程</a> •
    <a href="#-源码编译教程-开发者">🛠️ 编译教程</a> •
    <a href="#-项目结构说明">📂 项目结构</a> •
    <a href="#-许可证与免责声明-license--disclaimer">⚖️ 免责声明</a>
  </p>

</div>

---

## 🌐 多平台版本导航 (Cross-Platform)

本项目拥有 **Windows 桌面端** 与 **Android 移动端** 两个独立原生版本，针对不同设备场景进行了深度适配与优化：

| 平台版本 | 官方仓库地址 | 核心特色与技术栈 |
| :--- | :--- | :--- |
| 📱 **Android 移动版 (当前项目)** | 🔗 **[chess_H4ck_Android](https://github.com/LinHouYu/chess_H4ck_Android)** | **免无障碍权限**、三层独立悬浮窗、自由拖拽拉伸、**毛玻璃 UI + 象棋 Emoji 唯美飘雪背景**（Kotlin + Stockfish 18） |
| 💻 **Windows 桌面版** | 🔗 **[chess_H4ck_Windows](https://github.com/LinHouYu/chess_H4ck_Windows/tree/main)** | 原生 Win32 穿透自动化、多线程非阻塞深度计算、无级缩放与全键盘快捷键操控（Python + Tkinter + Stockfish） |

---

## ✨ 核心亮点与特性

- 🪟 **三层独立悬浮架构**：
  - **顶部固定状态栏**：紧凑固定于屏幕最顶部，不可移动，实时清晰呈现当前执棋方、模式、最佳走法（如 `e2 -> e4`）与局势评分（如 `+1.85` / `杀棋 #2`）。
  - **自由拖拽拉伸棋盘**：半透明背景不遮挡游戏画面，按住顶部把手可在游戏上方自由拖动对齐，按住右下角手柄可随意无级拉伸放大与缩小。
  - **可折叠悬浮快捷球**：折叠时仅占用 54dp 边缘空间，展开即呈现 10 大功能的双行面板，展开状态下**按住标题栏仍可自由拖动全屏停靠**。
- ⚡ **纯净免权限架构（彻底移除无障碍）**：
  - 无需开启繁琐且容易被系统拦截的“无障碍服务”。
  - 【AI走子】与【自动代走】直接在悬浮棋盘内部完成走子与推演，秒级响应，零权限负担。
- 🧠 **Stockfish 18 智能引擎深度分析**：
  - 异步协程后台深度计算，实时高亮推荐走法起点（绿色框）、目标点（蓝色框）及战术进攻方向箭头。
- 🔄 **第一人称代打原则（黑白视角翻转自适应）**：
  - **AI 永远只操控屏幕下方的我方棋子**。
  - 默认视角（白在下）：AI 执白棋并走下方白棋；点击【翻转】后（黑在下），AI 自动转为**执黑棋**并走下方黑棋！
- 🎨 **双模式交互与沙盒排局**：
  - 支持 **“点击选子 $\to$ 点击目标格落子/吃子”** 与 **“按住棋子自由拖动落子”** 两种交互。
  - 开启沙盒模式后，支持跨规则任意挪子、任意吃子，方便极速摆谱复盘。
- 👁️ **一键收起/显示棋盘**：
  - 快捷面板提供专属【隐藏棋盘】按钮，随时收起棋盘悬浮窗以全面观察底层界面，再次点击原位复原。

---
## 视频实例



https://github.com/user-attachments/assets/d4b2c197-e69d-4169-afea-6bf6c768f2a2



---

## 🎨 界面与动效美学

- ❄️ **象棋 Emoji 动态下雪背景 (`ChessSnowFallingView`)**：
  - 应用主界面背景动态飘落包含 `♔ ♕ ♖ ♗ ♘ ♙ ♚ ♛ ♜ ♝ ♞ ♟` 等各类国际象棋符号粒子。
  - 采用物理学正弦摆动与微小旋转动效，搭配纯净白与淡蓝层次色彩（`#FFFFFF`、`#E0F2FE`、`#BAE6FD`、`#38BDF8`）。
- 💎 **淡蓝浅白 + 磨砂毛玻璃 (Glassmorphism) 主题**：
  - 告别压抑的纯黑界面，整体采用清新优雅的冰雪白与浅蓝色调。
  - 各类功能卡片均采用半透明磨砂质感（`bg_glass_card.xml`）与精致浅蓝高光描边。

---

## 🎛️ 快捷控制面板一览

展开悬浮菜单后，清晰呈现 10 个专属功能快捷键：

| 图标 | 功能名称 | 说明与效果 |
| :---: | :--- | :--- |
| 🔄 | **重置开局** | 将棋盘一键复位至标准 8x8 开局状态 |
| ✏️ | **自由沙盒** | 开启/关闭自由摆子模式（支持任意跨规则挪子与吃子） |
| ⇄ | **切换回合** | 强制切换走棋方（白棋回合 $\leftrightarrow$ 黑棋回合） |
| 🔃 | **翻转视角** | 黑白视角上下对调（执黑棋时使用，AI 自动接管黑棋） |
| ↩️ | **撤销走子** | 回退上一步落子记录并恢复历史局面 |
| ⚡ | **AI走子** | 触发 Stockfish 18 计算并立即在棋盘上执行推荐走法 |
| 🤖 | **自动代走** | 开启连续托管循环，轮到我方回合时自动计算并持续走子 |
| 👁️ | **隐藏棋盘** | 一键收起半透明棋盘悬浮窗，再次点击即刻原位复原 |
| 🌐 | **穿透模式** | 切换触摸穿透，方便直接触摸操作底层的其他 App |
| ❌ | **退出辅助** | 优雅停止前台悬浮窗服务并退出 |

---

## 📖 安装与使用教程

### 1. 下载与安装
- 从 [Releases](https://github.com/LinHouYu/chess_H4ck_Android/releases) 页面下载最新的 `app-debug.apk` 并安装到 Android 设备中。
- 首次打开应用时，点击 **【去授权】** 并允许 **悬浮窗权限**（在其他应用上层显示）。

### 2. 启动悬浮辅助
- 点击主界面正中的 **【♟ 启动悬浮辅助】** 按钮。
- 此时屏幕上方将出现 **顶部状态栏**，中间出现 **半透明棋盘**，右侧出现 **悬浮菜单球**。

### 3. 匹配对齐底层对局软件
1. 打开你需要辅助分析的国际象棋应用（如 Chess.com、Lichess、网页版棋盘等）。
2. 按住悬浮棋盘 **顶部横条**，将悬浮棋盘拖动到游戏棋盘上方。
3. 按住悬浮棋盘 **右下角的拉伸手柄**，自由放大或缩小，直到悬浮格子与底层棋盘格子完全重合。

### 4. 辅助走子与分析
- 在悬浮棋盘上正常挪子即可，顶部状态栏会实时给出最佳走法与局势评分。
- 点击悬浮球展开菜单，点击 **【AI走子】** 或开启 **【自动代走】**，即可享受 AI 自动推演辅助！

---

## 🛠️ 源码编译教程 (开发者)

### 环境要求
- **Android Studio** Ladybug (2024.2+) 或更高版本
- **JDK**：OpenJDK 17 / 21
- **Gradle**：8.10+
- **Android SDK**：`compileSdk = 35`, `minSdk = 24` (Android 7.0+)

### 命令行编译 APK
```bash
# 1. 克隆本仓库
git clone https://github.com/LinHouYu/chess_H4ck_Android.git
cd chess_H4ck_Android

# 2. 运行单元测试
./gradlew testDebugUnitTest

# 3. 打包编译 Debug APK
./gradlew assembleDebug
```
编译生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`。

---

## 📂 项目结构说明

```text
chess_H4ck_Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/linhouyu/chess_h4ck/
│   │   │   ├── core/
│   │   │   │   ├── engine/          # Stockfish 18 高并发计算与 PST 剪枝引擎
│   │   │   │   ├── model/           # 棋子、棋盘格子与移动模型
│   │   │   │   └── state/           # 棋盘状态机 (FEN 解析、沙盒、历史撤销)
│   │   │   ├── service/
│   │   │   │   └── OverlayService.kt # 前台悬浮窗协调服务
│   │   │   ├── ui/
│   │   │   │   ├── overlay/         # 三大悬浮窗组件 (StatusBar, Chessboard, FloatingMenu)
│   │   │   │   ├── view/            # 自定义动画 View (ChessSnowFallingView 飘雪粒子)
│   │   │   │   └── MainActivity.kt  # 启动主界面
│   │   │   └── util/                # 屏幕与格子坐标几何换算工具
│   │   ├── res/
│   │   │   ├── drawable/            # 毛玻璃背景、专属 Vector SVG 图标、原版 App Logo
│   │   │   ├── layout/              # 主界面及各大悬浮窗布局 XML
│   │   │   └── mipmap-*/            # 全套多分辨率 App Launcher 图标
│   │   └── AndroidManifest.xml      # 清单配置文件 (悬浮窗与前台服务权限)
│   └── build.gradle.kts             # 模块构建脚本
├── README.md                        # 本项目文档说明
└── build.gradle.kts                 # 根项目配置
```

---

## ⚖️ 许可证与免责声明 (License & Disclaimer)

本项目遵循 **[Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](https://creativecommons.org/licenses/by-nc/4.0/)** 非商业授权协议。

### 🚫 禁止商用声明 (Non-Commercial Strictly Prohibited)
- **本项目完全开源免费，仅供计算机图形学、算法分析与个人离线教育学习交流使用。**
- **严禁将本项目的任何源代码、编译产物（APK）或衍生作品用于任何商业营利、付费分发、商业服务集成、广告变现或引流转售行为！**
- **严禁在任何线上未经许可的多人竞技对战平台（如 Chess.com、Lichess 等排位匹配中）进行不当利用与作弊行为。使用者须自行承担因违规使用产生的一切法律与账号风险。**

---

<div align="center">
  <sub>Designed & Developed with ❤️ by LinHouYu · Powered by Stockfish 18 Engine</sub><br />
  <sub>📱 Android Version: <a href="https://github.com/LinHouYu/chess_H4ck_Android">chess_H4ck_Android</a> · 💻 Windows Version: <a href="https://github.com/LinHouYu/chess_H4ck_Windows/tree/main">chess_H4ck_Windows</a></sub>
</div>
