# 动画已知问题

本页只记录当前实现可证实的动画缺口。正常职责和数据流见[动画架构](../../architecture/animation/README.md)。

## 求值与语义

- `isMoving` 当前有逻辑错误，会影响依赖该字段的 `CodedAnimationController`。
- 头部 yaw / pitch 输入已经计算，但 coded head-bone 应用仍被禁用；第一人称手臂也使用独立且未完成的动画运行时，未共享完整 controller 进度。
- 骨骼基准 snapshot 错误地以 `children_hidden` 初始化 `cubes_hidden`，两个可见性通道可能互相污染。
- Controller 的旋转混合仍保留不完整的历史行为，`blend_via_shortest_path` 未实际生效。
- `ysm.bone_absolute_pivot` 未实现；子级 Molang context 仍不完整，public / foreign 变量未接线。
- 模型音频与 sound keyframe 不受支持，controller-state sound effect 也在绑定时丢失；`ysm.play_sound` 播放 Minecraft `SoundEvent` 是仍可用的独立能力。
- 表达式解析错误可退化为零，但运行期异常缺少 controller / handler 级隔离，单个脚本错误可能中断整个实体更新。

## Context、线程与生命周期

- 异步求值没有不可变输入快照，会直接读取 live `Entity`、`level`、`Minecraft` 输入和可选模组 API；同次求值可能混入不同时间点数据，也可能违反外部 API 的线程限制。
- 副作用 emitting gate 未覆盖全部 controller 与 update-handler 路径，多 `RenderContext` 重算仍可能重复或漏发声音、粒子或 deferred action。
- Canonical pose 可以跨兼容 pass 复用，但当前逐次 draw metadata 可能随输出一起复用；例如同帧 shadow pass 可能观察到上一 `RenderContext`。
- 同一 render target 内热替换会保留动画状态并依赖烘焙兼容；兼容性门禁和第一人称独立运行时尚缺完整闭环。
- `AnimationProcessor` 原地修改共享 snapshot 与 attribute，没有事务 staging、异常回滚或模型 revision 二次校验，失败可能留下部分更新。

## Molang 与动作

- Molang 用户函数已有递归深度限制，但没有统一的指令、循环、耗时或外部动作预算；第三方模型仍可能造成长时间求值或高频副作用。
- Roaming 使用模型派生的 32-bit 短键分组，存在碰撞和零值哨兵问题；本地容量校验可能在拒绝报告前已经保存超限数据。

## 联动与验证

- 主线仍直接接线部分战斗、移动、载具和装备 adapter，大量目标未迁入；既有接线也未按统一输入边界完成线程审计与实机验收，不能视为正式支持。
- 缺少覆盖 coded / Bedrock / hybrid controller、Molang handler、Roaming、模型热切换、同帧多 `RenderContext` 和第一人称的 Minecraft 端到端测试。
- `BoneAttribute` 与 `BakedModel` preorder、可见性、locator 的跨语言契约缺少独立 golden 验证；布局重复定义仍有漂移风险。

模组恢复边界见[模组动画联动](../../future/mod-animation-integration.md)，渲染侧的 extract、locator 与重入缺口见[渲染已知问题](rendering.md)。
