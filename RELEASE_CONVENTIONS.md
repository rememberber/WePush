# WePush 发布约定

## 版本号对齐

发布前请保证以下来源一致：

| 来源 | 示例 |
|------|------|
| `pom.xml` `<version>` | `5.0.5` |
| Git tag | `v5.0.5` |
| `UiConsts.APP_VERSION` | `v_5.0.5` |
| `version_summary.json` `currentVersion` / `versionIndex` / `versionDetailList` | `v_5.0.5` |

说明：运行时版本历史沿用 `v_` 前缀；GitHub Release tag 使用标准 `v{semver}`。

## 自动更新通道

客户端检查：

- `https://raw.githubusercontent.com/rememberber/WePush/master/src/main/resources/version_summary.json`

客户端下载链接：

- `https://raw.githubusercontent.com/rememberber/WePush/master/download_links.json`

`download_links.json` 字段：

- `windows`
- `macSilicon`
- `mac`
- `linux`

## 发布流程

1. 更新 `pom.xml`、`UiConsts.APP_VERSION`、`version_summary.json`
2. 推送 tag：`git tag v5.0.5 && git push origin v5.0.5`
3. GitHub Actions `Build installers` 会：
   - 校验版本元数据
   - 构建 macOS Apple Silicon / macOS Intel / Windows x64 / Linux x64 安装包
   - 创建 GitHub Release 并上传资产
   - 回写 `master` 上的 `download_links.json`

也可在 Actions 页面手动运行 `workflow_dispatch`，用于补传某一平台资产。

## 本地打包

```bash
python3 scripts/prepare_jdks.py --targets mac-arm64
mvn clean package -Pmac-apple-silicon -Dmaven.test.skip=true
```

支持的 profile：`mac-universal`（默认）、`mac-intel`、`mac-apple-silicon`、`windows-x64`、`linux-x64`。
