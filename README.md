# Yes Steve Model

## ⚠️ 警告

当前公开版本还未完成，不保证稳定性、数据安全、跨平台行为、API、代码结构或后续版本兼容性。请勿用于生产环境或重要存档，测试前务必备份游戏目录、世界和模型。

所有公开格式、Schema、内部协议和缓存布局均尚未冻结，在正式发布前大概率会有 break change，并且不做向后兼容。

## 项目状态

本项目正在进行大规模重构，目前公开代码主要用于审阅、协作和验证设计。

目前模型格式、动画、渲染、gui 已趋于稳定，但网络协议和模型管理的代码还未提交。当前仓库中已有的模型管理和网络相关代码是为了让开发环境能跑起来而临时生成的代码，未经严格验证，仅提供最低限度的可用性，并且近期会有大幅度零兼容替换。贡献者不应向这部分贡献代码，下游项目也不应视其为可依赖接口。

目前仅协作者可贡献代码，如有贡献意愿可加入 YSM 开发者交流群了解详情。须知当前代码结构还未稳定，贡献者的本地开发进程可能得跟着主线一起重构。待模型管理和网络协议完成开发、旧版能力完成迁移，将会开放贡献。

更多信息见 [迁移概览](docs/migration-overview.md) 

## 与旧版相比

| 类别     | 重点                                                                                                                                                    |
| ------ |---------------------------------------------------------------------------------------------------------------------------------------------------------|
| 新增能力   | 公开的模型资产标准；细粒度资产分发；动态资源管理；更多平台支持。                                                                                        |
| 既有能力改进 | 模型业务回归 Java，native 收缩为能力层；内容身份、连接、资源所有权、失效和恢复边界显式化。                                                              |
| 暂缺旧能力  | 模型音频和 v3 加密模型暂未完成迁移；第一人称、附着 layer 和部分模组联动也未完全恢复。                                                                   |
| 迁移重点   | 完成模型管理和网络协议，接通模型音频、补充 v3 加密模型的独立导入、将 x64 基线降至 x86-64-v1，并适配 Windows 7。模组联动、手臂模型、layer 等旧代码迁移。 |
|        |                                                                                                                                                         |
| 未来方向   | 扩展 API、模型签名、通用外部模型源、GPU Compute Pipeline、独立 Backend。                                                                                |

## 其他文档

- [术语表](docs/glossary.md) / [文档政策](docs/governance/documentation-policy.md)：统一名称与信息取舍规则。
- 独立格式标准：
    - [Asset Container](docs/standards/asset-container.md)
    - [Model Schema](docs/standards/model-schema/README.md)：[Manifest 与身份](docs/standards/model-schema/manifest-and-identity.md)、[资产与验证](docs/standards/model-schema/assets-and-validation.md)
    - [一致性要求](docs/standards/conformance.md)
- 顶层设计与架构：
    - [动画系统](docs/concepts/animation.md) / [动画架构](docs/architecture/animation/README.md)
    - [渲染系统](docs/concepts/rendering.md) / [渲染架构](docs/architecture/rendering/README.md)

## 许可证

- 除另有声明的内容外，本仓库的原创代码按 [Apache License 2.0](LICENSE) 开源。
- Asset Container Spec、Model Schema、规范性 Proto 快照及一致性要求是独立于 YSM 和 Minecraft 的标准，按 [CC0 1.0 Universal](LICENSES/CC0-1.0.txt) 发布。
- 内置模型资产不属于 Apache-2.0；每个资产目录中的 `ysm.json` 是其许可证的权威清单。
- 项目包含直接拷贝或修改的第三方代码以及随包依赖，详见[NOTICE.md](NOTICE.md)。

Apache-2.0 不覆盖上述独立标准、内置资产或第三方作品；对应文件中的单独声明优先。
