# WePush Next Java SDK — 0.1.0-alpha.1

Java SDK 是远程 Service API 客户端，只依赖公开 `service-api` 和通用 JSON 组件，不依赖 Core、Engine、Agent 或具体 Provider。

预览期尚未发布到 Maven Central。发行包的 `sdk/` 目录提供 Parent POM、`service-api` 与 `sdk-java` 的 POM/JAR，可按下面顺序安装到本机 Maven Repository：

```bash
cd sdk
mvn install:install-file \
  -Dfile=wepush-next-parent-0.1.0-alpha.1.pom \
  -DpomFile=wepush-next-parent-0.1.0-alpha.1.pom
mvn install:install-file \
  -Dfile=service-api-0.1.0-alpha.1.jar \
  -DpomFile=service-api-0.1.0-alpha.1.pom
mvn install:install-file \
  -Dfile=sdk-java-0.1.0-alpha.1.jar \
  -DpomFile=sdk-java-0.1.0-alpha.1.pom
```

随后在应用中声明：

```xml
<dependency>
    <groupId>com.fangxuele.wepush.next</groupId>
    <artifactId>sdk-java</artifactId>
    <version>0.1.0-alpha.1</version>
</dependency>
```

预览期 API 可能发生不兼容变化，客户端与 Service 应使用相同的预览版本。
