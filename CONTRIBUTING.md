> 欢迎你来到这里, 我们感谢您对 Kimiroyli 的一切贡献


## 项目结构

- `build.gradle` - 根构建文件, 其中依赖版本号全位于此文件内
- `src/bootloader-javaagent` - 一级启动桥, BootLoader 的 Loader
- `src/bootloader` - 系统加载核心, 负责整个系统的加载
- `src/kimiroyli-api` - 对外开放的 API
- `src/system-core` - Kimiroyli 的核心
  - `com.kasukusakura.kimiroyli.core.Bootstrap` - 核心的启动点, 全部 JDK 桥接工作都将在在这个类里完成
- `src/testing` - 测试点入口
- `src/tech` - 一些关键技术的简单描述

## 运行系统

`./gradlew :testing:launchTest`
