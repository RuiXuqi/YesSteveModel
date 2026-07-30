# 术语表

| 术语 | 含义 |
|---|---|
| Asset Container | 与资产类型无关的独立二进制容器标准。 |
| Model Schema | 建立在 Asset Container 之上的独立模型数据标准。 |
| stored representation | chunk 在容器中保存的字节表示。 |
| logical content | 完成容器级解码后交给上层 schema 的内容。 |
| verified preamble | 从容器起点到 verification payload 结束的 metadata 前缀。 |
| modelHash | producer 定义的 32-byte opaque 模型身份。 |
| chunk hash | 单个 logical content 的完整性摘要。 |
| raw model | 转换前的模型资源集合。 |
| historical `.ysm` | 旧版私有文件后缀；必须由文件头区分 V1/V2 raw archive、V3 或无效输入。 |
| `.mxc` | 当前 `0.1.0-unstable` Asset Container 文件后缀。 |
| YSM `BakedModel` / `CubeGroup` | Native CPU renderer 的不可变烘焙结果及其 SIMD 几何和调度单元，与 Minecraft 同名类型无关。 |
| serialized baked cache | `BakedModel` 的内部派生缓存表示，不是公开模型格式。 |
| `EntityModelBinding` / `AnimatableEntity` / `AnimatedGeoModel` | 分别负责 render-target 绑定、`entity` 动画生命周期和 `BoneAttribute` 所有权。 |
| `BoneAttribute` | Native 读取的逐骨骼记录；Java 持有布局等价的连续 attribute 数组。 |
| `GeoModelState` / `GeoRenderData` | 前者拥有逐帧 native 状态和 pose buffer，后者组合借用视图与本次 draw metadata。 |
| `RenderSchedule` / `RenderTask` | 按可见骨骼和几何分区形成的只读 CPU 工作及固定输出区间计划。 |
| `VertexKind` | Native 顶点输出布局选择，区分 direct 布局与 `VertexConsumer` fallback。 |
| bake / extract / render | 构建不可变几何、形成逐帧状态、输出顶点的三个阶段。 |
