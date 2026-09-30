# ColorOS AOD Trace

ColorOS / OPlus AOD 与 Android Doze 的**只读全链路追踪模块**。

该项目用于分析 AOD 从进入、展示、动画、UI 隐藏、Display 状态请求，到 Doze/Dream 退出的完整因果链。它与 `ColorOS-AOD` 功能模块分离：本项目负责诊断，正式功能模块只保留已经验证过的 Hook。

## v0.1.0 目标

首版基于已验证的 ColorOS AOD 调用链构建，重点回答：

- 谁最先触发 AOD UI 隐藏？
- 哪个方法首先请求 `OFF` / `DOZE_SUSPEND`？
- `PanoramicAodController`、`AodBlackLayout`、`AODDisplayUtil`、`BaseDisplayUtil`、`OplusDozeServiceExImpl` 和 `DozeService` 的调用先后顺序是什么？
- 某方法执行前后关键字段发生了什么变化？
- AOD 是因为普通超时、用户唤醒、指纹、低光规则还是其它状态机事件结束？

## 安全设计

**默认且当前版本完全只读。**

所有 Hook 都保持：

```text
ENTER
→ 记录参数 / 字段 / 调用栈
→ chain.proceed()
→ 记录返回值 / 字段差异
→ EXIT
```

不会主动：

- 修改参数
- 替换返回值
- 阻止 `OFF`
- 修改 View alpha / visibility
- 修改 DozeMachine 状态
- 延长或缩短 AOD
- 调用 `DreamService.finish()`

因此追踪器本身尽量不改变被观察的 AOD 状态机。

## 技术栈

- Modern libxposed API 102
- LSPosed / compatible libxposed framework
- Android minSdk 33
- targetSdk 36
- compileSdk 37
- Kotlin / Jetpack Compose
- ARM64 Release

模块作用域：

```text
com.android.systemui
```

## 追踪档位

应用界面可以实时配置以下档位：

- **基础**：AOD Session + Display / Doze
- **全量**：所有已知 AOD 路径
- **全景 AOD**：Panoramic + UI + Display
- **Display**：屏幕状态、AODDisplayUtil、Doze
- **唤醒/退出**：Wake + Doze + Display

还可控制：

- 是否记录关键调用栈
- 是否生成 `FIELD_DIFF`
- 调用栈最大深度（4–32）

配置通过 libxposed Remote Preferences 传给 SystemUI Hook 进程。

## 当前追踪范围

### AOSP / SystemUI

- `DozeService`
- `DreamService.finish`
- `DozeMachine`
- `DozeScreenState`

### ColorOS / OPlus

- `AODDisplayUtil`
- `BaseDisplayUtil`
- `AODVirtualDozeClient`
- `OplusDozeServiceExImpl`
- `PanoramicAodController`
- Panoramic show animation listener
- `AodBlackLayout`
- `AodUpdateManager`
- low-light sensor callback
- `SmoothTransitionController`
- AOD single-click wake callback
- on-screen fingerprint touch path

不存在的 OEM 类/方法只会记录 `REGISTER_MISSING`，不会导致整个安装失败。

## 日志结构

模块日志 TAG：

```text
AOD_Trace
```

主要事件：

```text
MODULE_LOADED
PACKAGE_READY
REGISTER
REGISTER_MISSING
INSTALL_COMPLETE
SESSION_START
TRACE_ENTER
TRACE_EXIT
FIELD_DIFF
TRACE_THROW
SESSION_END
```

## 使用方法

1. 安装 APK。
2. 在 LSPosed 中启用模块。
3. 作用域选择 **系统界面 / com.android.systemui**。
4. 打开 ColorOS AOD Trace，建议先选择 **全量**。
5. 重启 SystemUI 或重启手机，使新的 Hook 集合加载。
6. 将 Release 中的 `capture-aod-trace.sh` 放到手机 Download 目录。
7. Termux 执行：

```sh
su -c 'sh /storage/emulated/0/Download/capture-aod-trace.sh'
```

8. 熄屏进入 AOD，测试期间不要操作。
9. AOD 自然结束后按 `Ctrl+C` 停止。
10. 日志位于：

```text
/storage/emulated/0/Documents/ColorOS-AOD-Trace/
```

可用摘要脚本：

```sh
sh /storage/emulated/0/Download/summarize-aod-trace.sh \
  /storage/emulated/0/Documents/ColorOS-AOD-Trace/aod-trace-YYYYMMDD-HHMMSS.log
```

## 已验证的目标链路

当前调试基线上已经观察到典型链路：

```text
PanoramicAodController
→ animation onAnimationEnd
→ updatePanoramicAodHideStatus
→ AodBlackLayout / View invalidation
→ AODDisplayUtil.requestScreenState(...)
→ BaseDisplayUtil
→ OplusDozeServiceExImpl
→ DozeService
→ Display OFF
→ onDreamingStopped
```

## 后续计划

- 运行时动态发现更多 `com.oplus.systemui.aod.*` 候选方法
- Session 自动报告生成
- 首个 UI hide / 首个 OFF / 物理 OFF / Session end 自动归因
- JSON Lines 日志格式
- 环境快照与兼容性指纹
- 可选实验 Hook 页面（默认关闭，并与只读追踪严格隔离）

## License

MIT
