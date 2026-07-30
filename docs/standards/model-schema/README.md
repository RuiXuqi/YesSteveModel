<!-- SPDX-License-Identifier: CC0-1.0 -->

# Model Schema (unstable)

Model Schema 定义 `.mxc` 模型的逻辑内容与 Protobuf wire。本标准独立于 YSM、Minecraft 及任何具体实现，按 [CC0 1.0 Universal](../../../LICENSES/CC0-1.0.txt) 发布。

## 容器绑定

一个模型必须使用 [Asset Container](../asset-container.md)，并满足：

| 项目 | 规则 |
|---|---|
| schema id | `mixel/character` |
| property 0 | schema version |
| property 1 | 非空白 producer/vendor 标识 |
| 必需 chunk | `manifest` |
| 可选命名图像 | `thumb-button`、`thumb-icon` |
| 编号 blob | `blob-N`，`N` 从 1 开始 |
| 编号 stream | `stream-N`；只保留 wire 名字，不属于当前支持 profile |

`manifest` 是非空 Proto3 `Manifest` payload。Proto 语法遵循 [Proto3 Language Guide](https://protobuf.dev/programming-guides/proto3/)，二进制表示遵循 [Protocol Buffer Encoding](https://protobuf.dev/programming-guides/encoding/)；随本标准发布的 `.proto` 快照进一步冻结本 schema 的实际 wire。生产者直接存储 Manifest；consumer 仍须先完成容器级 payload 验证再解析。容器 summary 可以复制模型显示名，但不是 schema 的权威数据。

## Normative Proto 快照

以下文件是 wire 布局、字段编号、字段类型、枚举数值和 import 关系的唯一规范来源；Markdown 不重复这些信息：

- [Manifest 入口](proto/manifest/manifest.proto)
- [ModelData 入口](proto/asset/model/ModelData.proto)
- [StringData 入口](proto/asset/strings/StringData.proto)

`proto/` 包含三个入口的完整 import closure，使用独立的 `mixel.common`、`mixel.asset.*` 与 `mixel.manifest.*` package。修改 wire 必须先裁决版本，再同步更新 Proto、语义规则和 fixtures。

## 逻辑组成

```text
Manifest
├── render_targets[] ──> blob-N: ModelData
│   └── textures{} ────> blob-N: Image payload
├── common_behavior
│   ├── strings_blob_id -> blob-N: StringData
│   └── sounds[] ───────> stream-N (reserved, unsupported)
└── info
    ├── metadata / language / settings / properties
    └── image references -> blob-N
```

- Manifest 是可先行读取的 descriptor，声明身份、展示信息、render target 和资源引用。
- Blob 是按正整数 ID 命名的不可变 payload，可存放 definition、字符串或图像。
- 命名图像用于模型列表的 thumbnail 与 icon；格式和尺寸由 chunk descriptor 描述。

M2 完整模型的全部引用必须形成闭包：每个正 blob ID 都解析到 `blob-N`，引用双方对 payload 类型和用途一致，资源通过容器 hash 与 schema 校验。

## 文档划分

- [Manifest 与身份](manifest-and-identity.md)：descriptor、target、metadata、modelHash 和 preview 语义。
- [资产与验证](assets-and-validation.md)：definition、几何、动画、纹理、字符串、保留音频表面和验证顺序。
- [格式一致性](../conformance.md)：conformance profile 与 fixture 要求。

## 标准边界

Schema 包含 player、projectile、vehicle target，以及几何、动画、控制器、PBR texture descriptor、展示图像、语言条目和 Molang user function 的 wire 表面。这些 target 名称是标准自身的逻辑类别，不要求 Minecraft 或 YSM 运行时。尚未冻结的渲染语义不属于本标准。

模型音频目前是保留字段：当前 profile 的 producer 必须省略 `Common.sounds` 和 `stream-N`；M1 consumer 可以解析并报告 unsupported extension，依赖这些资源的模型不能达到 M2。未来启用音频必须另行冻结 encoding、采样、流式传输、生命周期和播放契约。
