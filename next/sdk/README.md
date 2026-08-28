# WePush Next Java SDKs

WePush Next 提供两种边界不同、彼此独立的 Java SDK：

| SDK | Maven Artifact | 执行位置 | 依赖边界 | 适用场景 |
| --- | --- | --- | --- | --- |
| Remote Java SDK | `sdk-java` | WePush Service / Agent | 只依赖公开 `service-api`，不依赖 Core、Engine 或 Provider | 应用通过 HTTP 调用已部署的 WePush |
| Embedded Java SDK | `embedded-java` | 调用方 JVM 进程内 | 依赖 Core API、Engine、Provider SPI；Provider 由应用显式选择 | 不启动 Service，直接把 Engine 嵌入 Java 应用 |

两者不是上下级关系，也不会合并成一个 Artifact。Remote SDK 保持稳定的网络契约边界；Embedded SDK 提供进程内执行能力。

> `1.0.0` 发行包和独立 Java SDK 附件同时提供 Remote Java SDK 与 Embedded Java SDK。

## 从源码安装

需要 Java 21 或更高版本。在仓库根目录执行：

```bash
cd next
./mvnw -pl sdk/sdk-java,sdk/embedded-java,providers/provider-http,providers/provider-standard -am install
```

## 从预览发行附件安装

进入包含 POM/JAR 的 `sdk/` 目录（独立 Java SDK 压缩包则进入其根目录），先安装 Parent，再按需要安装 Remote 或 Embedded 依赖闭包：

```bash
VERSION=1.0.0

mvn install:install-file \
  -Dfile="wepush-next-parent-$VERSION.pom" \
  -DpomFile="wepush-next-parent-$VERSION.pom"

install_jar() {
  artifact="$1"
  mvn install:install-file \
    -Dfile="$artifact-$VERSION.jar" \
    -DpomFile="$artifact-$VERSION.pom"
}

# Remote Java SDK
install_jar service-api
install_jar sdk-java

# Embedded Java SDK 与内置 Provider
install_jar core-api
install_jar provider-spi
install_jar engine
install_jar embedded-java
install_jar provider-http
install_jar provider-standard
```

Windows PowerShell 可以对相同文件逐条执行 `mvn install:install-file`。

## Remote Java SDK

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>sdk-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Remote SDK 使用 `WePushClient` 连接 Service；建议 Service 和客户端使用相同 `1.x` Minor，兼容范围见[《兼容性策略》](../docs/compatibility-policy.md)：

```java
try (var client = WePushClient.builder()
        .endpoint(URI.create("http://127.0.0.1:18990"))
        .build()) {
    var system = client.system().info();
    var workspace = client.workspace("ws_default");
    var runs = workspace.runs();
}
```

`beta.1` 的 Remote SDK 同步支持资源分页/编辑、CSV/TXT 文件上传、正式发送确认、筛选重发和真实总览。例如上传文件不会先把完整内容读入 SDK 内存：

```java
var workspace = client.workspace("ws_default");
var preview = workspace.uploadAudience(
        Path.of("recipients.csv"), "Imported", null, "CSV", "itemId",
        Map.of("mobile", "mobile"), null);
var audience = workspace.commitAudienceImport(preview.id());

var confirmation = workspace.confirmRun("job-id");
var run = workspace.createRun("job-id", UUID.randomUUID().toString(),
        new ControlPlaneApi.CreateRunRequest(false, Map.of(), "manual",
                confirmation.confirmationToken()));
```

每个 Cursor Page 最多 100 条；正式发送和失败重发的确认令牌只短期有效，并绑定当前资源或来源 Run 状态。

## Embedded Java SDK

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>embedded-java</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>provider-http</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>provider-standard</artifactId>
    <version>1.0.0</version>
</dependency>
```

`provider-standard` 提供 SMTP Email、飞书/钉钉/企微机器人、阿里云短信、微信公众号、小程序和企业微信应用消息。Embedded SDK 不创建数据库、不启动 Web Server、不读取 Service 配置，也不自动扫描第三方 Provider。应用必须通过 `.provider(new SmtpProviderFactory())` 等方式显式注册允许的实现，并负责持久化结果、事件、Artifact 和 Secret 的适配器。渠道配置见[内置 Provider 指南](../docs/provider-guide.md)。

完整示例、资源所有权和生产接入说明见 [Embedded Java SDK README](embedded-java/README.md)。

预览期 API 可能发生不兼容变化。
