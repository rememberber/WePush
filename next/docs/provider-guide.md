# WePush Next 内置 Provider 指南

本文适用于 `0.1.0-alpha.4`，说明内置 Provider 的最小配置、Recipient 字段、发送语义和故障处理。所有渠道都连接用户自己的服务或渠道账号；WePush 不代注册账号、不转售消息额度、不代收款，也不把凭据上传到官方公共平台。

## 1. 共同行为

Service 和 Agent 随发行包内置以下 Provider：

| Provider ID | 渠道 | 实现版本 |
|---|---|---|
| `wepush.http` | 通用 HTTP | `0.1.0` |
| `wepush.email.smtp` | SMTP Email | `0.1.0` |
| `wepush.bot.feishu` | 飞书 / Lark 群机器人 | `0.1.0` |
| `wepush.bot.dingtalk` | 钉钉群机器人 | `0.1.0` |
| `wepush.bot.wecom` | 企业微信群机器人 | `0.1.0` |
| `wepush.sms.aliyun` | 阿里云短信 | `0.1.0` |
| `wepush.wechat.official` | 微信公众号 | `0.1.0` |
| `wepush.wechat.mini` | 微信小程序 | `0.1.0` |
| `wepush.wecom.app` | 企业微信应用消息 | `0.1.0` |

Provider 实现版本与 WePush 产品版本独立。创建 Account、Message 或 Run 时，应使用 Provider Catalog 返回的精确版本。

### 1.1 SecretRef

密码、Webhook URL、签名密钥、AccessKey Secret、AppSecret 和 CorpSecret 必须保存到当前自部署实例的 Secret Store，并在 Account 中只写引用：

```json
{"namespace":"smtp","name":"password","version":"v1"}
```

本地 Secret API 路径为 `/api/v1/workspaces/{workspaceId}/secrets/{namespace}/{name}/versions/{version}`。响应、日志、Provider metadata 和错误诊断都不返回 Secret 明文或带令牌的完整远端响应。

### 1.2 模板、Dry Run 与重试

- 文本字段使用 `{{fieldName}}` 引用 Recipient 字段；JSON 模板中的变量应放在 JSON 字符串内。
- 所有本页渠道支持 Dry Run。Dry Run 会完成配置、Recipient、模板和 Payload 校验，但不会解析 Secret、获取 Access Token 或访问网络。
- 除明确声明外，外部渠道不提供 Exactly Once。Engine 只重试 Provider 明确标记为 retryable 的确定失败；提交后发生超时、连接中断或服务端 `5xx` 时结果记为 `UNKNOWN`，不会自动重发，以避免重复消息。
- `idempotencyKey` 只是运行追踪信息。阿里云短信会把最多 64 个字符写入 `OutId`；它不等同于服务端去重。企业微信应用消息默认启用渠道自身的短窗口重复检查。
- Provider Descriptor 的最大并发是安全上限，Job 的并发和速率策略仍应根据用户自己的渠道配额进一步收紧。

## 2. SMTP Email

最小 Account：

```json
{
  "host":"smtp.example.com",
  "port":587,
  "security":"STARTTLS",
  "username":"sender@example.com",
  "password":{"namespace":"smtp","name":"password","version":"v1"},
  "fromAddress":"sender@example.com",
  "fromName":"WePush"
}
```

不需要认证的内网 SMTP 必须同时省略 `username` 和 `password`。`security` 可取 `NONE`、`STARTTLS`、`TLS`；不要在不受信网络使用 `NONE`。

Message 与 Recipient：

```json
{"subjectTemplate":"Hello {{name}}","textBodyTemplate":"Welcome {{name}}","htmlBodyTemplate":"<strong>Welcome {{name}}</strong>"}
```

```json
{"email":"alice@example.com","name":"Alice"}
```

同时提供文本和 HTML 时发送 `multipart/alternative`。连接测试会执行 SMTP 握手和认证，但不发送邮件。Provider 使用单一串行 Transport，避免同一 SMTP 会话被并发破坏；服务端拒绝 Recipient 归类为 `RECIPIENT_INVALID`，认证失败不重试，连接建立前失败可重试，提交后 I/O 不确定记为 `UNKNOWN`。SMTP 不提供幂等保证。

## 3. 群机器人

三个机器人都把完整 Webhook URL 存为 SecretRef，生产实现只接受各厂商官方 HTTPS 域名和路径，从而避免把 Secret 或消息转发到任意主机。

### 3.1 飞书 / Lark

```json
{
  "webhook":{"namespace":"feishu","name":"webhook","version":"v1"},
  "signingSecret":{"namespace":"feishu","name":"signing-secret","version":"v1"},
  "keyword":"optional-keyword"
}
```

```json
{"type":"TEXT","contentTemplate":"Build {{build}} completed","mentionField":"openId"}
```

支持 `TEXT`、`POST`、`INTERACTIVE` 和 `RAW`；`INTERACTIVE` / `RAW` 使用 `rawJsonTemplate`。Recipient 可提供 `openId`。本地安全限流为每秒 5 次且每分钟 100 次，Payload 上限 20 KiB。签名按飞书自定义机器人协议生成。参见[飞书自定义机器人文档](https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot)。

### 3.2 钉钉

```json
{
  "webhook":{"namespace":"dingtalk","name":"webhook","version":"v1"},
  "signingSecret":{"namespace":"dingtalk","name":"signing-secret","version":"v1"}
}
```

```json
{"type":"MARKDOWN","titleTemplate":"Build {{build}}","contentTemplate":"Deployment completed","mentionField":"mobile"}
```

支持 `TEXT`、`MARKDOWN`、`LINK`、`ACTION_CARD` 和 `RAW`。Recipient 可提供 `mobile`。本地安全限流为每分钟 20 次，Payload 上限 20 KiB。启用加签时，时间戳和 HMAC-SHA256 签名只在请求时生成。

### 3.3 企业微信群机器人

```json
{"webhook":{"namespace":"wecom-bot","name":"webhook","version":"v1"}}
```

```json
{"type":"TEXT","contentTemplate":"Hello {{name}}","mentionField":"mobile"}
```

支持 `TEXT`、`MARKDOWN` 和 `RAW`，Recipient 可提供 `mobile`。本地安全限流为每分钟 20 次，Payload 上限 20 KiB。企业微信群机器人不使用独立 `signingSecret`。

机器人 HTTP `429` 或业务响应中的频率限制归类为 `RATE_LIMITED`；签名、Token 或 Key 被拒绝归类为认证错误；提交后超时、I/O 或 `5xx` 记为 `UNKNOWN`。机器人渠道不提供幂等保证。

## 4. 阿里云短信

Account：

```json
{
  "accessKeyId":"LTAI...",
  "accessKeySecret":{"namespace":"aliyun","name":"access-key-secret","version":"v1"},
  "regionId":"cn-hangzhou"
}
```

建议为自建 WePush 实例创建最小权限 RAM 用户，不要使用主账号 AccessKey。生产端点固定为 `https://dysmsapi.aliyuncs.com/`。

Message 与 Recipient：

```json
{
  "signName":"你的短信签名",
  "templateCode":"SMS_123456789",
  "templateParamJsonTemplate":"{\"name\":\"{{name}}\",\"code\":\"{{code}}\"}"
}
```

```json
{"phoneNumber":"13800138000","name":"Alice","code":"7312"}
```

Provider 使用 `SendSms` `2017-05-25` RPC API 和 HMAC-SHA1 POP 签名。连接测试只验证 Secret 存在和官方端点可达，不消耗短信，也不代表签名、模板或余额一定可用；首次正式发送前必须对测试号码做小范围验证。

`MOBILE_*` 等号码错误归类为 `RECIPIENT_INVALID`，模板/签名/参数错误归类为 `INVALID_REQUEST`，AccessKey/签名错误归类为认证错误，权限错误归类为授权错误，业务频控归类为 `RATE_LIMITED`。`OutId` 只用于关联追踪，不承诺去重。参见[阿里云发送短信 API](https://help.aliyun.com/document_detail/101414.html)和[阿里云 RPC 签名机制](https://help.aliyun.com/document_detail/66384.html)。

## 5. 微信公众号与小程序

两者 Account 结构相同，`appSecret` 必须使用 SecretRef：

```json
{
  "appId":"wx...",
  "appSecret":{"namespace":"wechat","name":"app-secret","version":"v1"}
}
```

公众号支持：

- `TEMPLATE`：模板消息；
- `SUBSCRIBE`：订阅通知；
- `CUSTOM`：客服消息。

小程序支持：

- `SUBSCRIBE`：订阅消息；
- `UNIFORM`：统一服务消息。

Message 的 `payloadJsonTemplate` 是各微信接口的业务 JSON，Provider 会覆盖 `touser`，因此不要把 Access Token 或目标 OpenID 写进模板：

```json
{
  "type":"TEMPLATE",
  "payloadJsonTemplate":"{\"template_id\":\"TEMPLATE_ID\",\"data\":{\"name\":{\"value\":\"{{name}}\"}}}"
}
```

Recipient：

```json
{"openId":"openid...","name":"Alice"}
```

Provider 在 Session 内缓存 Access Token，并提前 5 分钟刷新。只有远端明确返回 Token 无效或过期代码时，才刷新 Token 并安全重放一次；普通超时或 `5xx` 不重放消息。无效 OpenID、未关注或未订阅归类为 `RECIPIENT_INVALID`，API 权限/IP 白名单问题归类为授权错误，模板数据问题归类为 `INVALID_REQUEST`，配额限制归类为 `RATE_LIMITED`。连接测试会真实获取一次 Access Token，但不会发送消息。参见[微信公众号 Access Token](https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html)、[公众号模板消息](https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Template_Message_Interface.html)和[小程序订阅消息](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-message-management/subscribe-message/sendMessage.html)。

## 6. 企业微信应用消息

Account：

```json
{
  "corpId":"ww...",
  "corpSecret":{"namespace":"wecom","name":"corp-secret","version":"v1"},
  "agentId":100001
}
```

`payloadJsonTemplate` 可使用企业微信应用消息接口支持的任意 `msgtype` 和对应内容；Provider 负责覆盖目标、`agentid` 和重复检查字段：

```json
{
  "type":"APP",
  "payloadJsonTemplate":"{\"msgtype\":\"text\",\"text\":{\"content\":\"Hello {{name}}\"}}",
  "enableDuplicateCheck":true,
  "duplicateCheckInterval":1800
}
```

每条 Recipient 至少包含 `userId`、`partyId` 或 `tagId` 之一，分别映射为 `touser`、`toparty`、`totag`：

```json
{"userId":"alice","name":"Alice"}
```

Token 缓存、单次安全刷新和错误策略与微信 Provider 相同。默认开启企业微信 30 分钟重复检查；该机制由企业微信定义，不等同于跨时间、跨模板的 Exactly Once。参见[企业微信发送应用消息](https://developer.work.weixin.qq.com/document/path/90236)。

## 7. 验证顺序

对每个新账号按以下顺序操作：

1. 在 Secret Store 写入凭据，只在 Account 中保存 SecretRef。
2. 在 Provider 页面保存 Account，执行连接测试并阅读返回代码；阿里云短信连接测试不验证模板和额度。
3. 在 Message 页面从 Schema 默认示例开始，替换真实模板 ID、签名和内容。
4. 导入 1 至 3 个自有测试 Recipient，先运行 Dry Run。
5. 将并发和速率设置为渠道配额以内，核对正式发送确认页后小范围发送。
6. 在 Run Item 中按 `providerCode`、`category`、`retryable` 和 `UNKNOWN` 语义处理失败；不要盲目重发 `UNKNOWN`。

本地 mock、协议签名、错误映射、Secret 不解析 Dry Run、Service/Agent 发现和 Engine 纵向测试均随源码测试套件执行。Classic 只作为需求与验收经验来源，Next Provider 不依赖 Classic 源码。
