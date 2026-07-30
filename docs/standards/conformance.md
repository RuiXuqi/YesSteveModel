<!-- SPDX-License-Identifier: CC0-1.0 -->

# 一致性 (unstable)

本页定义 Asset Container 与 Model Schema 的一致性等级和 fixture 要求。它与两项标准共同按 [CC0 1.0 Universal](../../LICENSES/CC0-1.0.txt) 发布。通过较低等级不代表通过较高等级。

## Conformance profile

| Profile | 必须证明 | 不证明 |
|---|---|---|
| C1 Container Metadata | magic、summary、header、properties、chunk table、version、verification | ordinary payload 存在或可用 |
| C2 Verified Preamble | C1，且输入恰好结束于 verification payload | 外部 ordinary payload 可取得 |
| C3 Complete Container | C1，所有 inline payload、padding、decode size、hash 与精确 EOF | 上层 schema 语义 |
| M1 Model Metadata | C1 或 C2、Manifest payload、schema/version/vendor、modelHash、target 与 preview 不变量；列出所有 unsupported extension | 引用 payload 可加载或 extension 可消费 |
| M2 Complete Model | M1、完整引用闭包、全部资源与结构校验，且不依赖本标准未冻结的 extension | renderer 输出或平台设备行为 |

实现必须在结果中携带实际 profile。Metadata-first、延迟加载和外部 payload source 是执行策略，不能把 M1 标成 M2。

## 必需 golden fixtures

格式冻结前必须提交可跨实现复用的二进制 fixture；运行时临时生成的 round-trip 不能替代它们。最小集合为：

1. 一个 M1 metadata-only descriptor 与对应 verified preamble；其普通引用可以尚未物化，但必须明确列出。
2. 一个最小 M2 player 模型，包含 player definition、`main`/`arm` GeoModel 和至少一个 UV texture。
3. 一个含直接媒体 payload、非零 alignment 和多个 blob 的 C3/M2 文件。
4. 一个含 zstd definition/string blob 的 C3/M2 文件。
5. 与同一完整文件对应的 C2 preamble 和逐 chunk 外部 payload。
6. 每个 fixture 的 container version、preamble 长度、chunk offset/size/decodeSize/hash、modelHash、profile 和期望解析摘要。
7. 固定 BLAKE3 空输入/短输入向量、zstd logical-content hash 向量，以及含 packed scalar 和嵌套 GeoModel/Cubes bytes 的 Proto fixture。

至少两个独立实现必须能消费同一组 portable fixtures。Golden bytes 只能通过显式格式版本裁决替换，不得由被测 serializer 每次重建后自我比较。

## 必需负例

测试至少覆盖：

- 错 magic、无 summary 终止符、过长或非法 UTF-8 summary、非法或非 ASCII 固定/短字符串。
- 不支持的 major/minor、非法 header size、负计数/长度、offset 加法或对齐溢出。
- property/chunk 重名、区域剩余字节、verification 非首项、非法 alignment。
- verification size/encoding/hash 错误、普通 chunk hash 错误、zstd dictionary 依赖、decodeSize 错误、截断或非 zstd 尾随垃圾。
- alignment padding 中含非零字节、payload 截断、C3 尾随数据、C2 多余数据。
- schema id、字面版本、vendor 错误，Manifest 缺失、空或非法 Proto3。
- modelHash 非 32 bytes、player target 缺失或错配、target id 重复、texture 为空。
- preview source 与 thumbnail presence 冲突，blob/image/definition/string 引用缺失或类型冲突。
- RGBA 长度或尺寸溢出、外部图像 probe 与 descriptor 不一致、多帧输入进入 portable profile。
- bone name、parent、cycle、vector、finite-number、cube-count，以及 cube 数组长度或索引错误。
- sound/stream 和 ZTX extension 不得被误报为 M2；ED25519 与未知 image token 不得被静默降级。

每个负例应只破坏一个不变量，并断言失败层级；不能只断言“最终某处抛错”。

## 边界 fixture

实现存在语言、进程、JNI、IPC 或其他边界时，必须用正式 producer 生成的 bytes 经真实 packing 和消费入口验证，不能只让同一侧 parser 自产自读。至少证明：

- ModelData 中的 GeoModel bytes 能二次解析，GeoModel 中的 Cubes bytes 能继续解析。
- Packed scalar、未知 Proto 字段和 enum 拒绝/保留策略符合 profile。
- 非法 hierarchy、cube count、数组长度和索引在进入 renderer 前 fail closed。
- 每种媒体格式分别验证 decode/encode direction、平台和最终 RGBA8 结果。

具体实现可以另行记录当前支持和测试缺口，但实现状态不能反向修改本标准。
