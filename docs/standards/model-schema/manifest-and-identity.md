<!-- SPDX-License-Identifier: CC0-1.0 -->

# Manifest 与身份 (unstable)

Manifest 的 wire 布局以 [manifest.proto](proto/manifest/manifest.proto) 及其 import closure 为准。本页只定义跨字段不变量和消费语义。

## Manifest 与 RenderTarget

Manifest 包含 render targets、公共资源描述和模型信息。它必须至少包含一个合法 player target。

每个 target 必须满足：

- `target_id` 非空且在 Manifest 内唯一。
- `kind` 是 Proto 已分配且本标准支持的 player、projectile 或 vehicle；unspecified、reserved 和未知值不得成为可加载 target。
- 恰有一个 `target_id = "player"` 且 kind 为 player；其他 target 不得声明 player kind。
- textures map 非空，key 非空。
- `blob_id` 为正数并引用合法 `ModelData`。

Player target 使用固定 id `player`。`match` 保存重复字符串序列；schema 不赋予列表顺序以优先级、覆盖或匹配算法，这些属于 consumer 的选择策略。

`ModelSettings` 是渲染提示，`ModelStats` 是展示统计。统计值不得替代解析后的 geometry 校验。

## Info 与展示信息

Info 聚合语言文件、选择界面设置、metadata、properties、export 信息和 thumbnail source。对应 wire 结构见 [Info Proto](proto/manifest/info/info.proto)。

- locale、同一语言文件内的翻译 key、作者联系人 key、链接 key，以及各类 UI id 应在各自作用域唯一，避免实现相关覆盖。
- Metadata 的名称、提示、许可证、作者、链接和头像用于展示；头像按普通 blob image 验证。
- ExportInfo 是 producer provenance。Schema 不以它判断兼容性。
- Settings 中的默认纹理、preview animation、extra animation 和 GUI 图像属于模型行为或展示提示；具体 UI 编排不是 wire 兼容规则。

这些字节可能参与某个 producer 的来源身份计算，因此本标准不承诺编辑展示 metadata 后 modelHash 保持不变。

## Properties 与 modelHash

`hash_id` 是恰好 32 bytes 的 opaque modelHash。它可作为持久化、索引和授权身份使用；不得截断、用模型路径替代，或仅保存文本前缀。

ModelHash 与以下值不同：

| 身份 | 覆盖对象 | 用途 |
|---|---|---|
| modelHash | producer 定义的模型来源内容 | 持久、索引和授权身份 |
| chunk hash | 单个 chunk 的 logical content | payload 完整性 |
| container verification | preamble/descriptor bytes | metadata 完整性 |
| representation identity | 某次容器或 descriptor 表示 | 外部去重；不属于本 schema 字段 |

本 schema 只冻结 modelHash 的长度和职责，不冻结 raw source 到 hash 的 canonicalization。当前来源算法可能纳入原始资源的实际字节、类型和路径，因此语义相同但格式、路径或文本字节不同的输入不保证产生相同 hash。重编码或重封装工具必须保留既有 modelHash；直接创建新来源的工具必须生成稳定的 32-byte 身份，并避免把同一身份指向不同内容。

`free` 是模型声明的使用属性，不替代许可证判断。`origin_ver` 是 producer-defined provenance hint，不是可靠的原始模型版本，也不参与 compatibility 或 identity 判断。

M1 consumer 在把模型用于任何外部身份域前必须验证 `hash_id` 长度。容器 verification 或 Manifest payload hash 不能替代此检查。

## Preview 与命名图像

Thumbnail source 的语义为：

| 名称 | `thumb-button` 要求 |
|---|---|
| unspecified | 必须不存在 |
| raw | 必须存在 |
| generated | 必须存在 |

未知枚举值必须拒绝。`thumb-icon` 独立可选。两个命名图像都必须声明受支持格式和非零宽高，并按 [资产与验证](assets-and-validation.md) 校验 payload。

Settings 中的 sound 名称和 controller 中的 sound effect 名称只是动作引用表面，不证明存在可播放音频资产；当前音频规则见 [模型 Schema](README.md#标准边界)。
