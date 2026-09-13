# 构建配置说明（已验证可用）

本文记录**当前实际跑通**的构建配置。这里是"事实记录"，不是愿望清单 ——
每一项都对应仓库里真实存在的文件，并已由 CI（GitHub Actions）实际构建验证过。

> **最后验证**：CI `Build APK` 在 `main` 分支连续成功（含新增定位功能后的构建）。
> 只要下表的版本组合不被改动，构建就是可复现的。

---

## 1. 一行摘要

| 项 | 值 |
|---|---|
| Gradle | **9.7.1**（**不在仓库里，由 CI 生成**，见第 3 节） |
| Android Gradle Plugin (AGP) | **9.4.0** |
| Kotlin | **2.2.10**（AGP 9.4 **内置**，未使用独立 Kotlin 插件） |
| JDK | **17**（Temurin） |
| compileSdk | **37**（Android 17）+ `compileSdkMinor = 0` |
| targetSdk | 37 |
| minSdk | 26 |
| Java 源码/目标兼容 | 17 |
| Compose BOM | 2026.09.00 |
| 构建产物 | `app/build/outputs/apk/debug/app-debug.apk` |

---

## 2. 版本目录（`gradle/libs.versions.toml`）

`libs.versions.toml` 是全部依赖版本的**唯一来源**，`build.gradle.kts` 里
不写死版本号。当前锁定值：

```toml
[versions]
agp = "9.4.0"                    # 支持 API 37
kotlin = "2.2.10"                # 与 AGP 9.4 内置 Kotlin 保持一致
coreKtx = "1.17.0"
lifecycleRuntimeKtx = "2.9.4"
activityCompose = "1.11.0"
composeBom = "2026.09.00"
navigationCompose = "2.9.5"
retrofit = "3.0.0"
okhttp = "5.2.1"
kotlinxSerialization = "1.9.0"
datastore = "1.1.7"
coroutines = "1.10.2"
```

---

## 3. ⚠️ Gradle Wrapper 不在仓库里（重要）

**仓库中没有 `gradle/wrapper/gradle-wrapper.properties`，也没有 `gradlew`。**
这是**有意为之**，不是遗漏。

CI 构建前会先执行：

```yaml
- name: Set up Gradle
  uses: gradle/actions/setup-gradle@v4
- name: Generate Gradle wrapper
  run: gradle wrapper --gradle-version 9.7.1
```

也就是说 **Gradle 版本由 CI 在运行时生成，不提交进仓库**。

### 这么做的影响

- ✅ 好处：不需要把 `gradle-wrapper.jar`（二进制）放进仓库，也不用担心它和 Gradle 版本漂移。
- ⚠️ 代价：**本地直接执行 `./gradlew` 会失败**，因为文件不存在。
  本地要构建，必须自己先跑一次 `gradle wrapper --gradle-version 9.7.1`，
  或直接用已安装的 `gradle` 命令。

### 如果要改 Gradle 版本

**只改一处**：`.github/workflows/build.yml` 里那行
`gradle wrapper --gradle-version 9.7.1`。改完推上去，CI 会用新版本重建。

---

## 4. AGP 9.x 的两个特殊点

这两个点**很容易踩坑**，所以单独说明。

### 4.1 不再需要 `org.jetbrains.kotlin.android` 插件

AGP 9.0 起**内置** Kotlin 支持。根 `build.gradle.kts` 里只有三个插件：

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false      // Compose 编译器仍需显式应用
    alias(libs.plugins.kotlin.serialization) apply false  // kotlinx.serialization 仍需显式应用
}
```

**没有** `org.jetbrains.kotlin.android`。如果按旧习惯加上它，可能引发冲突。

同时，官方明确：使用内置 Kotlin 时**无需**设置
`kotlin.compilerOptions.jvmTarget`，其值默认取
`android.compileOptions.targetCompatibility`（本项目为 17）。
所以 `build.gradle.kts` 里**故意没有**这段配置。

### 4.2 `compileSdkMinor` —— Android 17 的平台目录名

Android 17（API 37）引入了次要版本，SDK 平台的**真实包名/目录是 `android-37.0`**
（而不是 `android-37`）。因此必须**同时**指定：

```kotlin
compileSdk = 37
compileSdkMinor = 0
```

**只写 `compileSdk = 37` 会让 AGP 去找不存在的 `android-37` 目录而失败。**

CI 侧对应地安装了正确的包：

```yaml
sdkmanager --install "platforms;android-37.0" "platform-tools" "build-tools;36.0.0"
```

---

## 5. 固定签名（解决「每次都要卸载重装」）

默认 debug 构建用的是 Gradle **每次临时生成**的 debug keystore，
导致 CI 产出的每个 APK 签名都不同，Android 判定为不同应用 —— 只能卸载重装，数据全丢。

现在的做法：**签名信息从环境变量读取**，keystore 不变则签名不变，
APK 可以**直接覆盖安装**。

### 5.1 Gradle 侧（`app/build.gradle.kts`）

```kotlin
val keystorePath = System.getenv("UILAB_KEYSTORE_PATH")
val hasFixedSigning = keystorePath != null && file(keystorePath).exists()
signingConfigs {
    if (hasFixedSigning) {
        create("fixed") {
            storeFile = file(keystorePath!!)
            storePassword = System.getenv("UILAB_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("UILAB_KEY_ALIAS")
            keyPassword = System.getenv("UILAB_KEY_PASSWORD")
        }
    }
}
```

**关键设计**：环境变量缺失时**自动回退**到 AGP 内置 debug 签名，不影响本地开发。
（刻意不写 `signingConfig = getByName("debug")`，避免时序问题。）

### 5.2 CI 侧（`.github/workflows/build.yml`）

Secret 里存的是 **PKCS12 的 base64**，CI 解码后用 `keytool` 转成 JKS 再喂给 Gradle：

```bash
echo "$KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/ui-lab.p12"
keytool -importkeystore \
  -srckeystore "$RUNNER_TEMP/ui-lab.p12" -srcstoretype PKCS12 \
  -srcstorepass "$KEYSTORE_PASSWORD" \
  -destkeystore "$RUNNER_TEMP/ui-lab.jks" -deststoretype JKS \
  -deststorepass "$KEYSTORE_PASSWORD" -destkeypass "$KEYSTORE_PASSWORD" \
  -alias "$KEY_ALIAS" -noprompt
```

**为什么转 JKS**：JKS 是 Gradle/Android 的默认 keystore 类型，兼容性最好。

### 5.3 需要的 GitHub Secrets

| Secret | 含义 |
|---|---|
| `KEYSTORE_BASE64` | PKCS12 keystore 的 base64 编码（**注意：不是文件本身**） |
| `KEYSTORE_PASSWORD` | keystore 与 key 的密码 |
| `KEY_ALIAS` | 别名 |
| `KEY_PASSWORD` | key 密码 |

缺失 `KEYSTORE_BASE64` 时 CI 会**主动报错退出**，而不是静默产出一个签名不稳定的 APK。

### 5.4 🔒 keystore 绝不进仓库

`.gitignore` 已包含：

```
*.jks
*.keystore
*.p12
```

**keystore 一旦进入公开仓库，任何人都能用它伪造这个 App 的更新包。**
务必只通过 GitHub Secrets 传递。

### 5.5 验证签名是否生效

CI 最后一步会自动校验，输出中应出现 `CN=ui-lab`：

```bash
APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
"$APKSIGNER" verify --print-certs out/ui-lab-debug.apk
```

---

## 6. 项目级 Gradle 配置（`gradle.properties`）

```properties
org.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- `-Xmx3072m`：CI runner 内存有限，3GB 是**实测够用且稳定**的值。
- `file.encoding=UTF-8`：源码与注释里有大量中文，不设会出问题。
- `nonTransitiveRClass=true`：非传递 R 类，加快构建。

---

## 7. 仓库与仓库源（`settings.gradle.kts`）

```kotlin
pluginManagement {
    repositories {
        google { content { /* 只对 android/google/androidx 走 Google 源 */ } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
```

- Google 源加了 `content { includeGroupByRegex(...) }` 过滤，**避免把所有依赖都拿去 Google 源查**，加快解析。
- `FAIL_ON_PROJECT_REPOS`：禁止在模块里另开仓库，保证依赖来源统一。

---

## 8. 构建产物与版本号

| 项 | 值 |
|---|---|
| applicationId | `com.oopnv70.uilab` |
| namespace | `com.oopnv70.uilab` |
| versionName | `0.5-fixed-signing` |
| versionCode | `1` |
| debug 产物路径 | `app/build/outputs/apk/debug/app-debug.apk` |
| CI 重命名 | `out/ui-lab-debug.apk`（artifact 名 `ui-lab-debug-apk`） |

`versionName` 每次 UI 调整**手动递增**，方便确认手机上装的是哪一版。

---

## 9. debug 与 release 的差别

| | debug | release |
|---|---|---|
| 混淆 | 否 | 是（`isMinifyEnabled = true`） |
| 资源压缩 | 否 | 是（`isShrinkResources = true`） |
| 签名 | 固定签名（有 Secret 时） | 未配置 |

**CI 目前只产出 debug APK。** release 的混淆规则见 `app/proguard-rules.pro`。

---

## 10. 本地构建（需要注意）

本地**没有** wrapper，所以不能直接 `./gradlew`。可行做法：

```bash
# 方式一：用已安装的 gradle
gradle :app:assembleDebug

# 方式二：先生成 wrapper（版本与 CI 保持一致）
gradle wrapper --gradle-version 9.7.1
./gradlew :app:assembleDebug
```

另外还需要 `local.properties` 指向 Android SDK：

```properties
sdk.dir=/path/to/android-sdk
```

> ⚠️ `local.properties` **不应提交**（与机器相关），已在 `.gitignore` 中。

---

## 11. CI 排错提示

- **构建失败只显示 "exit code 1"**？
  workflow 里有一段专门把 Kotlin 报错行（`e: ` 开头）提取成 GitHub annotation：

  ```bash
  grep '^e: ' build.log | head -40 | while IFS= read -r line; do
    echo "::error::$line"
  done
  ```

  所以去 Actions 页面的 **Annotations** 区域看，比翻日志快得多。

- **找不到 `android-37`**？→ 检查 `compileSdkMinor = 0` 是否被误删（见 4.2）。

- **签名每次都变**？→ 检查 `KEYSTORE_BASE64` Secret 是否存在（见 5.3）。

---

## 12. 变更本文件的原则

改动构建配置时，**请同步更新本文**，尤其是：

- 第 2 节的版本表
- 第 3 节的 Gradle 版本（如果动了 CI 里那行）
- 第 5 节的签名流程

本文的价值在于"记录已验证的状态"。如果它和实际配置不一致，
**它的存在反而会误导人** —— 那比没有这份文档更糟。
