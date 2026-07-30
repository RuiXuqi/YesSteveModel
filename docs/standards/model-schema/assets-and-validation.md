<!-- SPDX-License-Identifier: CC0-1.0 -->

# 资产与验证 (unstable)

本页定义模型 payload、跨 chunk 引用和验证规则。Wire 布局以 [ModelData Proto](proto/asset/model/ModelData.proto)、[StringData Proto](proto/asset/strings/StringData.proto) 和它们的 import closure 为准。

## Chunk namespace 与引用

| Chunk type | Payload | 要求 |
|---|---|---|
| `manifest` | `Manifest` | 必需、非空 |
| `blob-N` | 由引用处确定 | `N` 为从 1 开始的十进制正整数 |
| `thumb-button` | 命名图像 | 由 thumbnail source 决定 presence |
| `thumb-icon` | 命名图像 | 可选 |
| `stream-N` | 保留 stream payload | 当前 profile 禁止生产和依赖 |

Blob ID 0 表示未设置。正 ID 必须唯一对应 `blob-N`。一个 blob 可以被多处引用，但所有引用必须对 payload 类型、图像 metadata 和用途相容。

Canonical producer 直接存储 Manifest；definition 与 string data 可以使用 zstd；媒体图像直接存储其格式 payload。本 schema 未分配 flags bit，所有 chunk 的 flags 必须为 0。Alignment 完全遵循容器标准，不增加模型专属限制。

## Image 与 PBR texture

[Image Proto](proto/common/image.proto) 中的 descriptor 引用 blob，并声明格式、宽、高和帧数。宽高必须非零；当前 portable profile 只定义静态图像，`frame_count` 必须为 1。`frame_count = 0` 没有冻结语义。

Blob image 的格式和尺寸来自 descriptor。其 chunk encoding 可以为空；非空时必须与 descriptor format 大小写不敏感地一致。命名图像的 format 来自 chunk encoding，宽高编码在容器 `decodeSize` 的高、低 16 位，且都必须非零。

Portable profile 定义以下格式：

| Token | Payload 语义 |
|---|---|
| `RGBA` | RGBA8；每通道 8 bit，像素为 R/G/B/A 顺序，无行填充；从左到右、从上到下，首像素位于左上；长度必须精确为 checked `width * height * 4` |
| `PNG` | 一个符合 [PNG Specification, Third Edition, W3C Recommendation 2025-06-24](https://www.w3.org/TR/2025/REC-png-3-20250624/) 的静态 PNG datastream |
| `JPEG` | 一个符合 [ISO/IEC 10918-1:1994, Edition 1](https://www.iso.org/standard/18902.html) 的 JPEG coded image；JFIF/Exif 包装与 metadata 子集尚未冻结 |
| `WEBP` | 一个符合 [WebP Container Specification](https://developers.google.com/speed/webp/docs/riff_container) 的完整 RIFF WebP file；只允许一个静态 VP8 或 VP8L image |
| `AVIF` | 一个符合 [AV1 Image File Format v1.2.0](https://aomediacodec.github.io/av1-avif/v1.2.0.html) 的完整 AVIF file；只允许一个 primary static image |

外部格式 payload 解码后必须产生与 descriptor 宽高一致的 RGBA8 图像。Consumer 不得用文件扩展名替代 token，也不得猜测未知格式。颜色空间、ICC、方向 metadata、alpha 转换和尾随媒体字节策略尚未形成跨 codec 的稳定 portable profile；producer 应输出已应用方向、无需外部颜色状态即可消费的静态图像，跨实现一致性测试必须固定最终 RGBA8 结果。

`ZTX` 是当前实现可见的私有扩展 token，但端序、颜色、严格长度、版本升级和 fixtures 尚未冻结，因此不能用于 M2。实现可以显式接受它并报告 vendor extension 状态，但不得把结果标为 M2。

PBR texture set 的 `uv` 必须存在，`normal` 和 `specular` 可缺省。三者分别是 base texture、法线扩展和高光扩展的资源角色；通道编码与 shader 解释尚未冻结，不能仅凭字段名承诺跨 renderer 的逐像素等价。

## ModelData 与引用闭包

每个 target 的 definition blob 解析为 `ModelData`。Player 至少包含名为 `main` 和 `arm` 的 GeoModel；projectile 和 vehicle 至少包含 `main`。所有 map key 必须非空。

GeoModel 作为 ModelData map value 中的 bytes 二次编码；GeoModel 的 `cubes` 又是序列化的 `Cubes`。Consumer 必须逐层解析，不能把两层 bytes 当作任意 opaque data。`GeoModelIndex` 与 `EventKeyFrame` 当前不在 ModelData 的可达引用图中，不产生额外 chunk 或运行时能力。

## Geometry

对应 wire 见 [GeoModel Proto](proto/asset/model/data/geo_model.proto)。必须满足：

- Bone 数量不超过 65536；name 非空且全模型唯一。
- 空 parent 表示 root；非空 parent 必须存在。parent graph 无环，且每个 bone 可到达某个 root。
- pivot 与 rotate 各有三个有限数。Pivot 使用模型像素单位，consumer 在应用平移时换算为 1/16 模型单位；rotate 使用弧度。
- Bone 按列表顺序连续拥有 `cube_count` 个 cube；总和必须恰好等于 Cubes 中的记录数。
- 每个 cube 的 `face_count` 在 0 到 6 之间。
- position 是 xyz 序列，长度可被 3 整除且最多八个顶点；其值处于本 Schema 的模型坐标。具体应用负责把模型单位映射到自身世界坐标。
- UV 是归一化的 uv 序列，长度可被 2 整除。Schema 不要求数值截断到 `[0, 1]`，以保留纹理寻址行为。
- 每个 face 恰有四个 position index、四个 UV index 和一个三分量 normal；索引必须在各自数组范围内。
- Index 顺序就是该 quad 的顶点顺序，consumer 必须保留。Normal 是 face 的显式方向，当前 schema 不强制单位长度，也不要求从 winding 重建。
- 所有 position、UV、normal 和 transform float 都必须是有限数。

`GeoProperties` 描述 identifier、纹理参考尺寸和可见边界；实际 texture blob 尺寸仍以 Image descriptor 为准。Consumer 不得在数组长度、索引或 hierarchy 非法时自行修复模型。

## Animation 与 controller

Wire 见 [Animation Proto](proto/asset/model/data/animation.proto) 和 [Controller Proto](proto/asset/model/data/animation_controller.proto)。

- Animation name 在文件内唯一；length、keyframe start、blend point 和数值 Molang 分量必须有限。
- Length 与所有 `start_tick` 使用 20 ticks/second 的时间单位。
- Bone animation 通过 bone name 绑定 geometry；引用应可解析。Rotation、position、scale 分别保存对应轨道；`post` 省略时等于 `pre`。
- Loop、easing 与 oneof 分配以 Proto 为准；consumer 必须保留未知字段，但不得把未知 enum 解释为已知策略。
- Instruction keyframe 是在指定 tick 执行的 Molang 表达式序列。
- Controller、state name 在各自作用域唯一；default state 和 transition destination 必须可解析。

关键帧向量分量的 Bedrock/Molang 求值、旋转顺序和 controller 调度尚未冻结为 renderer 互操作标准。M2 只证明结构和引用有效。

## Common strings、settings 与保留音频

Common 的 `strings_blob_id` 引用可解析的 [StringData](proto/asset/strings/StringData.proto)；user function name 非空且唯一。Settings 中所有 image reference 遵循普通 blob image 规则。UI metadata 只描述交互表面，schema 不赋予未说明字段额外运行时行为。

[Sound Proto](proto/common/sound.proto) 和 Manifest Common 中保留了 sound/stream wire，controller 与按钮也可出现 sound 名称。当前 profile 的规则是：

- Producer 必须省略 `Common.sounds`，不得写 `stream-N`。
- M1 consumer 可以解析含这些字段的 metadata，但必须标记为 unsupported extension，不能报告可播放。
- 含 sound/stream 依赖的模型不能达到 M2；保留未知 Proto 字段但不依赖它们不影响 profile。

## 分层验证与发布

Consumer 必须 fail closed：

1. 按 Asset Container 规则验证 metadata、版本和 verification。
2. 验证并解析 Manifest logical payload，检查 schema id、字面版本 `0.1.0-unstable` 和 vendor。
3. 检查 modelHash、player target、target 唯一性、texture 与 preview 不变量。
4. 建立请求范围内的 blob、image、definition 和 strings 引用闭包，拒绝 0、缺失或类型冲突。
5. 验证 payload 的 stored size、解码长度、logical hash 和媒体格式。
6. 解析 ModelData、StringData、GeoModel、animation 与 controller，并执行本页结构、hierarchy、引用和有限数校验。

完成第 3 步只能报告明确标记为 M1 的 metadata，不能宣称完整可用。任一局部 target 只有在自身引用闭包通过第 4–6 步后才能使用；渲染还要求该 target 的 definition、texture 和 geometry 全部验证。只有整个模型的全部引用通过，才能声明 M2。
