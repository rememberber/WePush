# WePush Next Carrier SMS Provider Plugins

`provider-cmpp`、`provider-smgp`、`provider-sgip` 和 `provider-smpp` 是四个独立 PF4J Provider 插件。它们只通过公开 Provider SPI 和 Agent Plugin API 接入，不是 Service 或 Agent 的内置依赖。共同的协议适配代码位于 `provider-carrier-common`，与 Classic 不存在源码或构建依赖。

每个插件支持协议登录、单号码 MT 文本提交、长短信分片、`SUBMIT_RESP` 判定、连接测试和无网络 Dry Run。网关受理不代表终端送达；本版本不处理 DLR、MO 或 SGIP 反向监听。

## 构建签名包

打包脚本不会内置或生成发布私钥。提供 Ed25519 PKCS#8 DER 的 Base64 和可信 Key ID 后分别构建：

首次配置时，可用 JDK 21+ 生成 Agent 所需的 PKCS#8 私钥和 X.509 公钥（Base64）：

```bash
java scripts/GeneratePluginSigningKey.java private-key.b64 public-key.b64
```

私钥文件只提供给离线构建/发布环境；Agent 仅配置 `public-key.b64` 的内容。

```bash
export WEPUSH_PLUGIN_SIGNING_KEY_ID=release-2026
export WEPUSH_PLUGIN_SIGNING_KEY_PKCS8_BASE64='...'
./scripts/package-carrier-provider.sh cmpp ./target/provider-plugins
./scripts/package-carrier-provider.sh smgp ./target/provider-plugins
./scripts/package-carrier-provider.sh sgip ./target/provider-plugins
./scripts/package-carrier-provider.sh smpp ./target/provider-plugins
```

Agent/Service 使用对应 Ed25519 X.509 公钥配置 `WEPUSH_PLUGIN_TRUSTED_KEYS`。插件包先经过 SHA-256 清单、Ed25519 签名、共享 API、Zip Slip 和压缩大小校验，再由 PF4J 加载。
