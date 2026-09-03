# RPG Menu Framework

[English](README.md) | [简体中文](README.zh-CN.md)

RPG Menu Framework 是一款运行于客户端和服务端的 Minecraft **1.21.1 / NeoForge 21.1.x / Java 21** 模组，提供响应迅速且可扩展的 RPG 玩家菜单。它是一个框架，而非商业游戏 UI 的复刻：内置的暗黑奇幻视觉风格为原创，仅使用绘制图元以及 Minecraft 自身渲染的物品和实体，不包含任何第三方游戏资产。

## 功能

- 可选地用 `E` 替换原版背包界面；`R` 始终可独立打开 RPG 菜单。
- 动态语义标签页、Java 注册 API、子页面、优先级、显示条件、角标，以及溢出/紧凑渲染。
- 服务端权威的背包查询协议：受限分页、每位玩家独立会话 ID、不透明条目键和存储版本号。
- 基于数据组件的物品聚合（`ItemStack.isSameItemSameComponents`）、long 数量与安全的紧凑数值格式。
- 自适应虚拟物品网格、分类、排序、`@modid` / 普通文本 / `#分类` 搜索与防抖。
- 完整的玩家渲染链：皮肤、盔甲、图层和渲染器替换均会生效；角色预览可拖拽旋转。
- 通过提供器展示原版装备和属性。
- 针对原版快捷栏的上下文快速装备，并可选支持 Epic Fight。
- 可选兼容 Curios、More Offhand Slots、Iron's Spells 'n Spellbooks、FTB Quests、Xaero's World Map、JourneyMap、Epic Fight 与 Epic Skills；只有提供器可用时才显示相应标签页。
- 内嵌地图视图；可指定首选地图提供器，并保留平移和缩放状态。
- 菜单不暂停游戏；正常情况下可通过可重映射的 WASD / Space / Shift 移动。焦点位于搜索框时不会将移动输入透传到游戏。
- 按服务器保存的收藏夹 UI 元数据。
- 支持资源重载的主题颜色、`en_us` / `zh_cn` 本地化、资源包 JSON 标签页及开发者布局编辑器。
- 主题映射的原生 UI 音效，支持主音量、单事件音量与音调、安全静音覆盖，以及焦点重复冷却。
- 按 `C` 可随时打开未修改的原版背包，以使用原版 2×2 合成和所有原生槽位操作。

## 安装

1. 安装适用于 Minecraft `1.21.1` 的 NeoForge `21.1.x`。
2. 将 `rpgmenuframework-0.1.0.jar` 放入客户端和服务端的 `mods` 目录。
3. 所有兼容模组均为软依赖；即使未安装它们，框架也能正常启动。请在相应一侧安装受支持的模组，以启用额外标签页或装备支持。

客户端配置文件会生成在 `config/rpgmenuframework-client.toml`。将 `replaceVanillaInventory=false` 可保留原版 `E` 键，并仅用 `R` 打开菜单。使用 `preferredMapProvider` 选择已安装的地图提供器，使用 `preserveMapView` 在再次打开菜单时保留地图中心和缩放级别。

### 可选兼容

| 模组 | 菜单支持 |
| --- | --- |
| Curios / More Offhand Slots | 安装后提供额外装备槽位和安全的装备操作。 |
| Iron's Spells 'n Spellbooks | 法术标签页和法术相关属性。 |
| FTB Quests | 任务标签页。 |
| Xaero's World Map / JourneyMap | 内嵌地图标签页；可在客户端配置中选择优先使用的已安装提供器。 |
| Epic Fight | 快速装备兼容。 |
| Epic Skills | 在同时安装 Epic Fight 时显示技能标签页。 |

所有兼容项均为可选。框架会在启动时检测可用性；缺失或不兼容的可选模组不会阻止框架加载。

## 开发者 API

公共入口为 `RpgMenuApi.get()`。核心扩展类型位于 `dev.rpgmenu.framework.api`，不依赖 `Screen`、`GuiGraphics`、`Font`、纹理或玩家渲染器类。详见 [API.md](docs/API.md)、[TABS.md](docs/TABS.md)、[THEMING.md](docs/THEMING.md)、[ARCHITECTURE.md](docs/ARCHITECTURE.md) 和 [COMPATIBILITY.md](docs/COMPATIBILITY.md)。

### 注册标签页

```java
RpgMenuApi.get().tabs().registerTab(RpgMenuTab.builder(
        ResourceLocation.fromNamespaceAndPath("example", "factions"),
        "tab.example.factions")
    .priority(500)
    .content(TabContentFactory.marker("placeholder"))
    .build());
```

### 注册 StatProvider

```java
RpgMenuApi.get().statProviders().register(MY_ID, new MyStatProvider());
```

### 注册 InventorySource

实现 `InventorySource.query`，让筛选、排序和分页在数据源侧完成。请勿返回没有边界的存储快照。变更操作仅可在服务端通过 `extract` / `insert` 和框架事务协调器执行。

## 布局编辑器

在客户端执行 `/rpgmenuframework editor`。拖动组件可移动位置，拖动右下角手柄可调整大小；按 `S` 保存/导出至 `config/rpgmenuframework/layout.json`，按 `R` 重置。

## 构建

```text
./gradlew build
./gradlew runClient
./gradlew runServer
```

仓库中的 Gradle Wrapper 目标版本为 8.14.2。构建基线为 ModDevGradle 2.0.137、NeoForge 21.1.244、带 Parchment 2024.11.17 补充映射的 Mojang mappings，以及 Java 21。

## UI 音效署名

部分 UI 音效来自 **Nathan Gibson / Cyrex Studios** 的 **Universal UI/Menu Soundpack**，采用 [Creative Commons Attribution 4.0 International license](https://creativecommons.org/licenses/by/4.0/) 授权。音效包[原始页面](https://cyrex-studios.itch.io/universal-ui-soundpack)及完整署名见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 0.1.0 已知限制

- Beyond Dimensions、RarityCore 和 Controlify 已预留公共扩展边界，但此版本尚未提供专用适配器。
- 对提供安全 `extract` / `insert` 的外部数据源，虚拟网格支持取出物品；原版背包条目仅支持查看/收藏，因为原生操作仍在原版合成背包中完成。
- 主题 JSON 颜色、布局覆盖和命名空间 UI 音效映射已经生效。尚未实现动态导入外部 PNG/TTF/OGG 包；音效资源仍使用标准 Minecraft 资源包资产。
- 布局编辑器目前可编辑位置和尺寸偏移；公共 schema 已表示锚点、透明度和 z-index，但编辑器尚未为全部字段提供控件。

这些限制的精确追踪见 [COMPATIBILITY.md](docs/COMPATIBILITY.md)。
