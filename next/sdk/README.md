# WePush Next Java SDKs

WePush Next 提供两种边界不同、彼此独立的 Java SDK：

| SDK | Maven Artifact | 执行位置 | 依赖边界 | 适用场景 |
| --- | --- | --- | --- | --- |
| Remote Java SDK | `sdk-java` | WePush Service / Agent | 只依赖公开 `service-api`，不依赖 Core、Engine 或 Provider | 应用通过 HTTP 调用已部署的 WePush |
| Embedded Java SDK | `embedded-java` | 调用方 JVM 进程内 | 依赖 Core API、Engine、Provider SPI；Provider 由应用显式选择 | 不启动 Service，直接把 Engine 嵌入 Java 应用 |

两者不是上下级关系，也不会合并成一个 Artifact。Remote SDK 保持稳定的网络契约边界；Embedded SDK 提供进程内执行能力。

> `0.1.0-alpha.2` 发行包和独立 Java SDK 附件同时提供 Remote Java SDK 与 Embedded Java SDK。

## 从源码安装

需要 Java 21 或更高版本。在仓库根目录执行：

```bash
cd next
./mvnw -pl sdk/sdk-java,sdk/embedded-java,providers/provider-http -am install
```

## 从预览发行附件安装

进入包含 POM/JAR 的 `sdk/` 目录（独立 Java SDK 压缩包则进入其根目录），先安装 Parent，再按需要安装 Remote 或 Embedded 依赖闭包：

```bash
VERSION=0.1.0-alpha.2

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

# Embedded Java SDK 与内置 HTTP Provider
install_jar core-api
install_jar provider-spi
install_jar engine
install_jar embedded-java
install_jar provider-http
```

Windows PowerShell 可以对相同文件逐条执行 `mvn install:install-file`。

## Remote Java SDK

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>sdk-java</artifactId>
    <version>0.1.0-alpha.2</version>
</dependency>
```

Remote SDK 使用 `WePushClient` 连接 Service，Service 和客户端应使用相同预览版本：

```java
try (var client = WePushClient.builder()
        .endpoint(URI.create("http://127.0.0.1:18990"))
        .build()) {
    var system = client.system().info();
    var workspace = client.workspace("ws_default");
    var runs = workspace.runs();
}
```

## Embedded Java SDK

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>embedded-java</artifactId>
    <version>0.1.0-alpha.2</version>
</dependency>
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>provider-http</artifactId>
    <version>0.1.0-alpha.2</version>
</dependency>
```

Embedded SDK 不创建数据库、不启动 Web Server、不读取 Service 配置，也不自动扫描第三方 Provider。应用必须显式注册 Provider，并负责持久化结果、事件、Artifact 和 Secret 的适配器。

完整示例、资源所有权和生产接入说明见 [Embedded Java SDK README](embedded-java/README.md)。

预览期 API 可能发生不兼容变化。
