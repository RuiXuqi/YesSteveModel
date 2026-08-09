# 渲染架构

本主题描述当前 CPU renderer 的逻辑结构。顶层目标见[渲染系统](../../concepts/rendering.md)；公开模型数据以 [Model Schema](../../standards/model-schema/README.md) 为准。这里的 YSM native `BakedModel`、`ModelState`、`RenderSchedule`、`RenderTask` 和 `VertexKind` 都是内部实现契约，不反向定义模型格式。

## 责任边界

| 参与方 | 当前所有权与职责 | 不拥有 |
|---|---|---|
| Java 模型与渲染接入 | render target owner、`GeoModelState`、纹理、`entity` / `level`、`PoseStack` 和 `RenderContext`；选择 `RenderType`，取得 `VertexConsumer`，发起 bake / extract / render | native 热数据布局、顶点计算、GPU draw |
| native renderer | `BakedModel`、`ModelState`、`RenderSchedule` / `RenderTask`；静态烘焙、骨骼层级、逐帧状态提取、CPU 调度、变换、面剔除、透明排序和顶点生成 | 纹理对象、Minecraft render state、GPU resource |
| Minecraft / Iris | `RenderType`、`MultiBufferSource`、`VertexConsumer`、纹理注册、批处理、上传和 draw | 模型来源、`BakedModel`、动画状态 |

Render target owner 在绘制期间保活资源；`AnimatedGeoModel` 持有 `entity` 级 `BoneAttribute`，每个 `GeoModelState` 输出槽持有自己的 native `ModelState` 与 locator mapping，并借用该 `ModelState` 持有的 `BonePose`。Native 不持有 animation、texture 或 `VertexConsumer`。

```mermaid
flowchart TB
    RT["render target owner"] --> BM["immutable BakedModel"]
    RT --> TX["Minecraft / Iris textures"]
    RT --> AN["animation resources"]
    LE["entity or GUI owner"] -. "keeps alive" .-> RT
    EN["entity-owned mutable state"] --> AM["AnimatedGeoModel"]
    AM --> BA["BoneAttribute array"]
    EN --> GS["GeoModelState output slots"]
    GS --> FS["native ModelState"]
    FS --> PS["native-owned BonePose array"]
    GS -. "borrows BonePoseView" .-> PS
    BM --> FS
    BA -. "temporary input" .-> EX["ModelState::Extract"]
    EX --> FS
    EX --> PS
    VA["NativeRenderAdapter"] --> RC["renderer::Render"]
    FS --> RC
    RC --> OR["direct / VertexConsumer fallback result"]
    TX --> MC["Minecraft draw"]
    OR -->|"adapter commits"| VC["VertexConsumer"]
    VC --> MC
```

## 三阶段契约

| 阶段 | 频率与执行位置 | 输入 | 输出 |
|---|---|---|---|
| bake | 模型或影响烘焙的资源变化时；后台构建路径 | 几何、基础纹理 alpha、UV 约定和 `BakeModelOptions` | 不可变 `BakedModel`；可选 serialized baked cache |
| extract | 每个需要新动画结果的 `entity` 与 `RenderContext`；`level` entity 主路径可在 Java worker，部分同步路径仍在渲染线程 | `BakedModel` 与 `BoneAttribute` | 有效 `ModelState`、其持有的 `BonePose`、借用的 `BonePoseView`、可见骨骼、locator indices 和 `RenderSchedule` |
| render | Minecraft 渲染线程发起；调用线程参与 native 执行 | `ModelState`、`RenderParameters`、`VertexKind` 与目标输出区间 | 成功后提交到 `VertexConsumer`，再由 Minecraft 上传和 draw |

阶段名描述数据依赖，不保证固定线程。Native 渲染 worker 不执行动画求值或 extract；具体线程与同步边界见[逐帧状态与调度](frame-execution.md)。

## 核心不变量

- Bake 只产生可重建的内部派生物；CPU 数据布局、serialized cache 和逐帧调度均不是公开 ABI。
- Extract 成功前 `GeoModelState` 不可发布；Render 只能消费当前 valid 的 `ModelState`，Java 只能在下次 Extract 或 close 前借用对应 `BonePoseView`，失败时不回退到旧状态。
- Position 与 normal 使用独立矩阵通道；Native 只生成顶点，不持有纹理或 GPU 对象。
- `RenderType`、Java `RenderContext` 的 native 投影与 `VertexKind` 是三个独立维度，不能互相替代。

## 子主题

- [Bake、分区与 cache](bake-and-partition.md)：`BakedModel`、切线烘焙、四逻辑分区和 AoSoA。
- [逐帧状态与调度](frame-execution.md)：extract、可见性、附着点、任务拆分与低延迟同步。
- [CPU render 与顶点输出](vertex-output.md)：矩阵、剔除、normal / tangent、`RenderType`、`VertexConsumer`、输出区间、透明排序和 SIMD。
- [GPU Compute Renderer](../../future/gpu-compute-renderer.md)：尚未实现的 GPU 资源与调度方向。
- [渲染已知问题](../../status/known-issues/rendering.md)：当前可证实的视觉、失败处理、验证和重入缺口。
