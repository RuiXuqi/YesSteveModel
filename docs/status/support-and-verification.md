# 当前支持状态

> [!WARNING]
> “已接线”只表示主要代码链路存在，不代表稳定、完整或通过实机、跨平台及跨实现验证。总体使用风险与未完成模块边界见[项目 README](../../README.md)。

| 能力 | 当前边界                                                                                                                        |
|---|---------------------------------------------------------------------------------------------------------------------------------|
| Asset Container / Model Schema `0.1.0-unstable` | Java reader、writer 与主要 schema 已接线；portable golden、外部 payload 和严格校验仍有[缺口](known-issues/format-and-schema.md) |
| `.mxc` 模型容器 | 当前后缀；格式未冻结，后续版本可以不兼容                                                                                        |
| 历史 `.ysm` | V1/V2 文件头进入 raw archive 导入；V3 只识别后拒绝；未知或损坏文件头拒绝                                                        |
| 模型导出 | `/ysm export` 已禁用，直到格式冻结                                                                                              |
| Java 动画运行时 | animation、coded / Bedrock / hybrid controller、Molang 与骨骼输出主链已接线；见[动画问题](known-issues/animation.md)            |
| Native CPU renderer | bake / extract / render、SIMD 与多线程顶点生成已接线；自动化基线和视觉验收未收敛，见[渲染问题](known-issues/rendering.md)       |
| 图像 codec | Native 解码覆盖 PNG、JPEG、WebP、AVIF、ZTX，编码仅覆盖 WebP、AVIF、ZTX；平台组合未完整验证，JPEG XL 不支持                      |
| 当前产品运行入口 | Java 入口覆盖 Windows x64、GNU/Linux x64 和专用环境下的 Android arm64；平台组合尚未完整验收                                     |
| 模型音频 | 暂不支持                                                                                                                        |
| v3 加密模型 | 暂不支持；当前没有导入器                                                                                                        |

格式本身的要求以[独立标准](../standards/README.md)为准；实现状态不能反向修改标准。
