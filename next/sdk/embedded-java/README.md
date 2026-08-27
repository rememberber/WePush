# WePush Next Embedded Java SDK

Embedded Java SDK 让 Java 应用在自己的 JVM 进程内直接运行 WePush Core Engine，无需安装或启动 Service、Agent、数据库和 WebUI。

当前实现提供：

- `WePushEngine.builder()` 显式注册一个或多个 `ProviderFactory`。
- 从 `List<RecipientRecord>` 或流式 `RecipientSource` 启动 Run。
- 返回 Core `RunHandle`，支持查询状态、提交暂停/恢复/取消/并发调整命令以及等待 `RunSummary`。
- 共享的 Engine 级 Sink，或由工厂创建的 Run 级 Result/Event/Artifact Sink。
- 可替换 `SecretResolver` 与 `ExecutionClock`。
- 面向测试和小任务的线程安全 `InMemoryExecutionStore`。

它刻意不包含 Service API、数据库、Spring、Agent、调度器、多租户控制面和 Provider 插件发现。应用只获得执行内核，其他生命周期由应用自己管理。

> `0.1.0-alpha.3` 发行包和独立 Java SDK 附件包含 Embedded SDK 及所需 POM/JAR。

## 要求与依赖

需要 Java 21 或更高版本。从源码安装当前模块和内置 HTTP Provider：

```bash
cd next
./mvnw -pl sdk/embedded-java,providers/provider-http -am install
```

调用方声明 SDK，并只加入自己允许使用的 Provider：

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>embedded-java</artifactId>
    <version>0.1.0-alpha.3</version>
</dependency>
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>provider-http</artifactId>
    <version>0.1.0-alpha.3</version>
</dependency>
```

## 最小可运行示例

下面使用 HTTP Provider 的 Dry Run，因此不会访问 `example.com`：

```java
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.embedded.InMemoryExecutionStore;
import com.fangxuele.wepush.next.embedded.WePushEngine;
import com.fangxuele.wepush.next.provider.http.HttpProviderFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

static ConfigDocument json(String schema, String value) {
    return new ConfigDocument(schema, "1", value.getBytes(StandardCharsets.UTF_8));
}

var account = json("http-account", """
        {"baseUrl":"https://example.com","auth":{"type":"NONE"}}
        """);
var message = json("http-message", """
        {
          "method":"POST",
          "path":"/notify",
          "headers":{"Content-Type":"application/json"},
          "bodyTemplate":"{\"name\":\"{{name}}\"}",
          "successStatuses":[200,202,204]
        }
        """);
var spec = new RunExecutionSpec(
        "embedded-example-1",
        new ProviderRef(HttpProviderFactory.PROVIDER_ID, HttpProviderFactory.VERSION),
        account,
        message,
        ExecutionPolicies.defaults(),
        Map.of("source", "example"),
        true,
        Instant.now());
var recipients = List.of(
        new RecipientRecord("alice", 0,
                Map.of("name", new RecipientValue.TextValue("Alice"))),
        new RecipientRecord("bob", 1,
                Map.of("name", new RecipientValue.TextValue("Bob"))));

var store = new InMemoryExecutionStore();
try (var engine = WePushEngine.builder()
        .provider(new HttpProviderFactory())
        .resultSink(store)
        .eventSink(store)
        .build()) {
    var handle = engine.start(spec, recipients);
    var summary = handle.completion().toCompletableFuture().join();
    System.out.println(summary.finalState());
    store.results(spec.runId()).forEach(System.out::println);
}
```

正式发送只应在完成小范围测试、目标服务限流和幂等校验后把 `dryRun` 改为 `false`。HTTP Provider 默认阻止私网目标；只有明确受信的内部地址才应开启 `allowPrivateAddresses`。

## Secret

默认 `SecretResolver` 只在 Provider 实际请求 Secret 时失败。需要认证的 Provider 必须显式配置：

```java
var engine = WePushEngine.builder()
        .provider(new HttpProviderFactory())
        .secretResolver(ref -> loadSecretFromYourStore(ref))
        .resultSink(resultSink)
        .build();
```

`SecretValue` 是可关闭资源。自定义 Resolver 不应记录明文；Secret Store、轮换和访问控制由宿主应用负责。

## Sink 与资源所有权

共享 Sink 的生命周期与 Engine 一致，必须支持多个 Run 并发调用：

```java
WePushEngine.builder()
        .resultSink(sharedResultSink)
        .eventSink(sharedEventSink)
        .artifactSink(sharedArtifactSink);
```

按 Run 隔离时使用工厂；工厂返回的资源在 Run 完成或启动被拒绝后由 Engine 关闭：

```java
WePushEngine.builder()
        .resultSinkFactory(spec -> openResultSink(spec.runId()))
        .eventSinkFactory(spec -> openEventSink(spec.runId()))
        .artifactSinkFactory(spec -> openArtifactSink(spec.runId()));
```

`start(spec, RecipientSource)` 会接管 `RecipientSource`，完成或拒绝时关闭它。`start(spec, List)` 会先复制列表。`WePushEngine.close()` 会取消仍在运行的 Run、等待执行器退出并关闭共享 Sink；建议始终使用 try-with-resources。

`InMemoryExecutionStore` 不限容量，只适合测试和可控的小任务。长期进程和大批量发送应实现有界或持久化的 `ResultSink` / `RunEventSink`，并根据需要实现 `ArtifactSink`。

## Provider 边界

SDK 不通过 Classpath 或 PF4J 自动发现 Provider。每个允许使用的实现都必须通过 `.provider(...)` 注册，`RunExecutionSpec.provider` 的 ID 和实现版本必须精确匹配。自定义渠道只需实现 `provider-spi` 的 `ProviderFactory`，无需依赖 Embedded SDK。
