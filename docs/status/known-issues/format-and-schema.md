# 格式、Schema 与编解码已知问题

本页记录当前实现与[格式一致性要求](../../standards/conformance.md)之间的重要缺口，不修改标准本身。

## 一致性与转换

- Asset Container 与 Model Schema 均缺跨实现 portable binary golden；同一实现自产自读不能证明互操作。
- verified preamble 的精确结束、固定字符串 ASCII、图像 payload 长度及 metadata 一致性仍有严格校验偏差。
- raw 几何转换会误写 visible bounds offset，可能使转换后的模型范围信息错误。

## Codec、导入与平台

- `ZTX` 是未冻结的私有图像格式；PNG、JPEG、WebP、AVIF 与各运行平台的 encode / decode 组合尚未系统验证。
- legacy v1/v2 只有 archive 到统一 raw parser 的迁移路径，缺真实模型端到端验证；v3 加密模型尚无导入路径，兼容范围仅包括独立的单向导入边缘。
- Codec 与 archive 验证尚未覆盖全部产品平台；存在 native 构建目标或库文件不等于产品入口已经接通或验收。

Model Schema 快照保留音频字段与 `stream-N` 资源表面，但当前 Java 生产端不写入、消费端不读取，底层 Ogg / Opus 组件也未接入资产与播放生命周期；这不构成音频或流式能力。全部明确不支持项见[当前支持状态](../support-and-verification.md)。
