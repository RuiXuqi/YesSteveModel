# 渲染已知问题

本页只记录当前实现可证实的渲染缺口。目标与正常语义见[渲染架构](../../architecture/rendering/README.md)。

## 视觉与接入

- Java 主体渲染目前固定使用 `RenderType.entityCutoutNoCull`，没有按模型透明语义选择 translucent `RenderType`。Native 虽会排序透明面，最终混合仍可能与 Blockbench 不一致。
- 透明排序只覆盖单次模型 draw；跨实体、跨模型和跨 draw 的次序仍由上层决定。Iris shadow 不执行透明排序。
- PBR 效果依赖 Iris 版本、shader pack 和 companion texture 接入，不能仅凭 baked tangent 存在保证一致。
- `VertexConsumer` fallback 当前错误解释 packed normal 的分量顺序与 SNORM 缩放，光照方向不能视为与 Vanilla / Iris direct 输出等价。
- 当前实际接通的骨骼附着点 layer 只有右手持物；副手、头部、鞘翅、肩部和背包等仍未进入完整主链，第一人称手臂与背景入口也未迁移完成。

## 正确性与失败处理

- `ModelState::Extract` 会先使旧状态失效；失败后 `GeoModelState` 没有完整失败分支，同一逻辑帧可能不再重试。Native render 失败时不会改用其他输出路径或 `VertexConsumer` fallback，本次模型直接无顶点。
- Serialized baked cache payload 不能独立证明 SIMD capability 匹配；读取虽校验结构、层级、索引和计数，却未重新验证几何浮点值的有限性及语义域。Bake 对极端有限输入派生的 plane / tangent 也缺少完整结果域验证。
- 上层必须提供与 position matrix 匹配的 normal matrix；native 只校验数值有限，不验证二者一致，该组合目前也没有端到端验证。
- `BakeModelOptions.force_translucent` 可能让原本 opaque 的骨骼进入透明分区，却未同步其透明深度准备条件。当前 Java 主加载路径不启用该选项，启用前需补齐最终排序验证。

## 并发与性能

- `ParallelExecutor`、translucent scratch、`VertexConsumer` fallback 与 `NativeRenderAdapter` 的共享 matrix scratch 都不可重入，多个 `renderer::Render` 必须全局串行。`ModelState` 还借用 `GeoModelState` 的 pose buffer，因此同一输出槽的 Extract、Render、换模与释放必须串行。
- 调度按不可拆分 `CubeGroup` 数而非实际 quad、PBR 或剔除成本分配任务，复杂模型可能出现 worker 尾部不均衡。
- 剔除分区按最大可见容量预留，并以零值填充未使用槽位，这是固定 offset 的当前代价。

## 待验证场景

Native renderer 尚未形成可作为支持声明依据的自动化回归与视觉验收闭环。

在声明支持前，Minecraft 运行验证至少应覆盖：`level` entity 同帧多 pass、`inventory` / `paperDoll` context 的 mutable 输出、本地第一人称 `irisShadow`、模型热切换、`VertexConsumer` fallback、透明与 PBR、非均匀缩放及各 locator layer。还应验证 locator mapping 始终引用对应 `GeoModelState` 的 pose buffer。视觉验收应比较 Vanilla、Iris 与 Blockbench 基准，并区分几何语义偏差和 shader / 光照环境差异。
