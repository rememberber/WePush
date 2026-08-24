# WePush Classic 运营商短信协议支持设计

## 1. 目标与结论

Classic 版新增统一的“运营商协议短信”消息类型，支持 CMPP 2.0/3.0、SMGP 3.0、SGIP 1.2 和 SMPP 3.4 的 MT 短信提交。这四种协议均为长连接二进制协议，不应继续套用现有 HTTP 短信供应商的“每条消息创建客户端”模型。

结论：在 Classic 现有账号、消息和任务模型上实现，不需要修改数据库表结构。账号参数和消息内容继续分别保存于 `TAccount.accountConfig` 和 `TMsg.content` JSON 中。

## 2. 产品范围

交付范围：

- 四协议的单号码 MT 文本短信发送，长短信由协议层自动分片。
- 一个统一消息类型，账号中选择具体协议。
- 每个账号共享一个协议客户端，复用 TCP 长连接，支持断线重连、窗口控制和应答超时。
- 将 `SUBMIT_RESP` 是否成功映射为 WePush 的发送成功/失败，返回协议消息 ID 或错误码。
- 模板变量替换、测试登录、测试发送、批量任务、无限推送和模拟发送与现有 Classic 能力保持一致。
- 账号删除、重新配置或应用退出时关闭对应连接。

明确不支持：

- 状态报告（DLR）的请求、解析、落库和发送历史回填。所有协议的状态报告请求位固定为 0；`SUBMIT_RESP` 成功只表示网关已受理，不代表终端已收到。
- MO 上行短信的解析、存储或业务处理。
- SGIP 反向 TCP 监听服务。
- 群发指令中一次携带多个目标号码；Classic 现有任务粒度保持一人一次提交。

部分网关可能无视请求位，在现有双向连接上推送 `DELIVER`。客户端只发送协议要求的确认包并丢弃消息，不将其交给 WePush 业务层；这是维持连接稳定所需的传输层行为，不属于状态报告或上行业务支持。

## 3. 技术选型

使用 Maven Central 中的 `com.chinamobile.cmos:sms-client:0.1` 作为协议客户端。它在同一抽象下支持 CMPP、SMGP、SGIP 和 SMPP，并提供连接池、心跳/重连、窗口和长短信分片能力。该组件及其核心声明为 Apache-2.0。

引入前后必须通过下列门槛：

- Java 21 下编译、建连、发送和关闭均无需额外 JVM `--add-opens`。无用的 FST/持久化路径从运行时依赖中排除。
- 检查运行时依赖和安装包体积；对文本短信无用的 WAP/MMS/持久化依赖仅在确认不会触发类加载后才能排除。
- 连接不可达和应答丢失时，WePush 设置的超时必须真正结束当次等待。
- 最终分发前对全部运行时依赖做许可证和安全审计。文本编码必需的 `smsj` 声明为 MPL-1.1；WAP/MMS 才使用的 GPL-2.0 `wbxml-stream` 已从 Classic 运行时依赖排除。Netty 保持 4.1 二进制兼容线并由根 POM 统一锁定安全修复版本，不使用协议库原始锁定的 4.1.86.Final。协议库带入的 Commons Pool、BeanUtils 和 Codec 也由根 POM 锁定到仍兼容其调用方式的正式维护版本。

如上述门槛无法满足，保留 `CarrierSmsGatewayClient` 内部接口，可以替换协议库而不影响 UI、配置和任务层。

## 4. 整体设计

```text
Classic 任务工作线程
        |
CarrierSmsMsgSender
        |
CarrierSmsSessionRegistry -- accountId --> 共享 CarrierSmsGatewayClient
        |                                      |
        +--------------------------------------+
                                               |
                                  sms-client / TCP 长连接池
                                               |
                             CMPP | SMGP | SGIP | SMPP 网关
```

`CarrierSmsSessionRegistry` 是进程级单例，以账号 ID 为键。任务线程只获取共享客户端，不直接创建或关闭 TCP 连接。同一账号配置发生变化时，通过配置指纹识别并原子替换旧客户端。

模拟发送不创建网络连接。真实发送在首次使用账号时惰性建连。

## 5. 配置模型

### 5.1 账号配置

| 字段 | 作用 | 适用协议 |
| --- | --- | --- |
| `protocol` | `CMPP` / `SMGP` / `SGIP` / `SMPP` | 全部 |
| `host`, `port` | 网关地址 | 全部 |
| `username`, `password` | 鉴权信息 | 全部 |
| `version` | 协议版本 | CMPP/SMGP/SMPP |
| `maxChannels` | 账号最大 TCP 连接数，默认 1 | 全部 |
| `windowSize` | 单连接未应答窗口，默认 16 | 全部 |
| `requestTimeoutMillis` | `SUBMIT_RESP` 超时，默认 10000 ms | 全部 |
| `sourceAddress` | 企业号码/SP 号/接入号 | 全部，协议字段名不同 |
| `serviceId` | 业务代码 | CMPP/SMGP/SGIP/SMPP |
| `msgSrc` | 企业代码 | CMPP/SMGP |
| `nodeId` | SGIP 企业编号 | SGIP |
| `corpId` | SGIP 企业代码 | SGIP |
| `heartbeatIntervalSeconds` | 空闲连接心跳间隔，默认 30 秒 | 全部 |

端口默认值仅用于 UI 快速填写，不作为协议规则强制：CMPP `7890`、SMGP `8900`、SGIP `8801`、SMPP `2775`。实际以运营商/网关商提供的参数为准。

密码存储延续 Classic 现有账号机制；本功能不在日志、异常信息或 `toString()` 中输出密码。

### 5.2 消息配置

消息 JSON 只保存 `content` 文本。内容支持 Classic 现有 Velocity 变量。号码仍来自人员表第一列，发送前去除首尾空格，不擅自改写国家码或号段。

## 6. 协议映射和成功判定

| 协议 | 提交请求 | 接收人 | 内容 | 提交成功 |
| --- | --- | --- | --- | --- |
| CMPP | `CMPP_SUBMIT` | `Dest_terminal_Id` | `Msg_Content` | `CMPP_SUBMIT_RESP.Result == 0` |
| SMGP | `SMGP_SUBMIT` | `DestTermID` | `MsgContent` | `SMGP_SUBMIT_RESP.Status == 0` |
| SGIP | `SGIP_SUBMIT` | `UserNumber` | `MessageContent` | `SGIP_SUBMIT_RESP.Result == 0` |
| SMPP | `submit_sm` | `destination_addr` | `short_message` | PDU `command_status == 0` |

应答类型不符、超时、连接失败、编码/分片失败或非零结果码均返回失败。失败描述包含协议和数值错误码，但不包含凭据。

对长短信，所有分片的提交应答均为成功才认为该接收人提交成功。任一分片失败时不自动重发整条长短信，避免收件人获得重复内容。

## 7. 并发、生命周期与可观测性

- Classic 任务线程数不等于 TCP 连接数。实际协议并发上限为 `maxChannels * windowSize`，两层都需有界。
- 断线后下一次发送重新建连；协议层和 `MsgSender` 上层均不对结果不明的提交自动重试。
- 账号删除时调用 `close(accountId)`，应用关闭时调用 `shutdown()`。
- 日志记录协议、账号 ID、网关地址、连接状态、应答耗时和错误码。号码按现有 Classic 行为展示，密码绝不记录。

## 8. UI 行为

- 消息类型页新增“运营商协议短信”。
- 账号页选择协议后只显示通用字段和当前协议专属字段，切换协议不丢失已输入的其他字段。
- 保存前校验必填项、端口范围、连接数、窗口和超时。保存账号不主动连接运营商网关。
- 账号页提供“测试登录”，只验证 TCP 和协议鉴权，不提交短信。
- 消息编辑页提供多行文本内容、变量提示和字符数显示。

## 9. 测试与验收

自动化测试：

- 四协议账号配置的 JSON 序列化/反序列化、默认值、边界和协议专属校验。
- 四协议提交请求字段映射。
- 四类成功应答、非零错误、错误应答类型、异常和超时映射。
- 同账号并发只创建一个共享客户端；配置变更、账号删除和应用退出正确关闭客户端。
- 模拟发送不创建客户端或 TCP 连接。
- 本地模拟网关覆盖四协议登录、中文提交、长短信分片和提交应答。
- 现有 Classic 单元测试和打包构建不回归。

联调验收：每种协议至少需要一个测试网关账号，覆盖登录、心跳、中文短信、长短信、网关拒绝、断线重连和应答超时。无真实网关时只能完成编译、单测和本地仿真验收，不能宣称已通过运营商现网联调。

## 10. 完成边界

实现以获取并校验 `SUBMIT_RESP` 为终点。任务统计中的“成功”统一表示网关受理；产品不推断、不展示终端送达状态，也不规划 DLR、MO 或 SGIP 反向监听能力。
