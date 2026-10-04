# 金山云短信

WePush Classic 和 Next 均支持金山云模板短信。每条导入记录发送至一个手机号，使用已审核的短信签名和模板，模板参数按名称传入。接口提交成功表示金山云已受理，实际送达以金山云控制台状态为准。

## Classic 配置

1. 在「消息类型」选择「金山云短信」，添加账号。
2. 填写金山云 AccessKey（AK）、SecretKey（SK）、已审核的短信签名，选择账号使用的区域。
3. 在消息编辑器填写短信模板 ID，逐项添加模板参数名和参数值。参数值支持 `${var1}` 等导入数据变量，`${var0}` 为手机号。
4. 先使用试运行验证模板，再执行正式发送。试运行不会请求短信接口。

区域支持北京 `cn-beijing-6`（默认）和广州 `cn-guangzhou-1`，分别使用官方 `https://ksms.ksyun.com/` 和 `https://ksms.cn-guangzhou-1.api.ksyun.com/`，无须填写自定义接口地址。

## Next 配置

Provider ID 为 `wepush.sms.ksyun`，版本 `0.1.0`，随 `provider-standard` 内置，通过 Provider SPI 自动发现。账号与消息编辑器由对应 JSON Schema 生成。

账号配置示例（SecretKey 通过 SecretRef 解析）：

```json
{
  "accessKeyId": "YOUR_AK",
  "accessKeySecret": {"namespace": "ksyun", "name": "sms-secret", "version": "v1"},
  "region": "cn-beijing-6",
  "connectTimeout": "PT10S"
}
```

消息配置示例：

```json
{
  "signName": "已审核的签名",
  "templateId": "YOUR_TEMPLATE_ID",
  "templateParamJsonTemplate": "{\"code\":\"{{code}}\"}"
}
```

收件人包含 `phoneNumber` 和模板所需的字段，例如 `code`。可选 `extId` 作为业务扩展标识；它不保证接口幂等。试运行校验并渲染 JSON 参数，不解析密钥、不发送请求。连接测试仅检查密钥可解析及服务地址可达，不验证发送权限。

## 接口和验证依据

实现参照金山云发布的 [com.ksyun:ksc-sdk-sms-java:0.1.8 源码](https://repo.maven.apache.org/maven2/com/ksyun/ksc-sdk-sms-java/0.1.8/ksc-sdk-sms-java-0.1.8-sources.jar)：`SmsService.sendSms`、`ParamAssemble`、`UtilAll.sign`、`Consts`、`SendSmsResult`。

- HTTPS POST 表单，`Action=SendSms`、`Service=ksms`、`Version=2019-05-01`。
- 业务参数：`Mobile`、`SignName`、`TplId`、`TplParams`。
- UTC 时间戳，参数排序并按 RFC 3986 编码后计算 HMAC-SHA256，输出十六进制签名。
- HTTP 成功且响应含非空 `Sid`、无 `Error` 才计为受理成功。Next 对超时、I/O 异常、无效响应等不确定结果标为 UNKNOWN，避免自动重发产生重复短信。

本地测试覆盖独立签名向量（含中文与保留字符）、模拟 HTTP 接口、模板变量、错误/缺失响应、试运行和渠道编辑器隔离。未使用真实金山云账号执行付费发送。
