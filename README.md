# ColorOS AOD Trace

`ColorOS-AOD-Trace` 是一个面向 ColorOS / OPlus SystemUI 的 **AOD/Doze 全链路只读追踪模块**。

它与 `ColorOS-AOD` 功能模块分离：本项目负责诊断和逆向运行链路，不负责修改 AOD 行为。

## 当前版本

`v0.1.0-alpha1`

- Modern libxposed API 102。
- 作用域仅 `com.android.systemui`。
- 默认纯追踪：不修改 Hook 参数、返回值、Display state 或 Doze state。
- 统一结构化日志 TAG：`AOD_Trace`。
- 统一事件前缀：`AODT|`。
- 每次 AOD 会话分配独立 `session`，所有事件记录 `elapsedMs`。
- 支持方法 ENTER / EXIT / THROW。
- 支持调用参数、返回值、关键字段快照和前后差异。
- 支持调用栈。
- UI 可切换追踪预设。

## 追踪预设

### 基础

- `DozeService`
- `DreamService.finish`
- `OplusDozeServiceExImpl`
- AOD session start / end

### 完整

包含所有已实现追踪点，适合一次性完整复现。日志量最大。

### Panoramic 深度

重点覆盖：

- `PanoramicAodController`
- `BaseAodController`
- `AodLifecycle`
- `AodBlackLayout`
- `SmoothTransitionController`
- `AODDisplayUtil`
- `BaseDisplayUtil`
- `AODVirtualDozeClient`
- `AodUpdateManager`
- Doze / Display 状态链

用于定位：

`动画结束 -> UI hide -> View invalidation -> Display request -> Doze -> OFF`

### Display / Doze 深度

重点覆盖：

- `AODDisplayUtil`
- `BaseDisplayUtil`
- `AODVirtualDozeClient`
- `DozeMachine`
- `DozeScreenState`
- `DozeService`

### Wake / Unlock 深度

重点覆盖：

- `OplusWakeUpController`
- `AodSingleClickWakeUpCallback`
- `OnScreenFingerprintUiMech`
- `OplusBiometricAuthController`
- `DozeService.onWakeUp`

## 结构化事件

示例：

```text
AODT|event=18|session=3|elapsedMs=7284|thread=main|phase=ENTER|source=PanoramicAodController|method=updatePanoramicAodHideStatus(...)|args=[...]|before={...}|stack=...
AODT|event=19|session=3|elapsedMs=7288|thread=main|phase=EXIT|source=PanoramicAodController|method=updatePanoramicAodHideStatus(...)|result=null|after={...}|diff={mIsShowing:true->false}
```

关键字段候选包括：

- `mReason`
- `mRequestState`
- `mRequestedDisplayState`
- `mDeviceDisplayState`
- `mPendingScreenState`
- `mState`
- `mWakefulness`
- `mPerformAodType`
- `mAODProcessType`
- `mKgShowingWhileGoingToSleep`
- `currentUiState`
- `gotoDozeWithOff`
- `mIsHideBySpecialRule`
- `mAodIsInShow`
- `mIsShowing`
- View `alpha / visibility / isShown`

不同 ColorOS 版本不存在的字段会自动跳过，不会因此中断追踪。

## 使用

1. 安装 APK。
2. 在 LSPosed 中启用模块。
3. 作用域选择 **系统界面 / com.android.systemui**。
4. 打开 App，选择追踪预设并保存。
5. 首次启用或更新模块后重启 SystemUI/手机。
6. 将 Release 附带的 `capture-aod-trace.sh` 放到下载目录。
7. Termux 执行：

```sh
su -c 'sh /storage/emulated/0/Download/capture-aod-trace.sh'
```

8. 熄屏进入 AOD，完整复现一次。
9. 回到 Termux 按 `Ctrl+C`。
10. 日志位于：

```text
/storage/emulated/0/Documents/ColorOS-AOD-Trace/log/
```

## 辅助摘要

```sh
su -c 'sh /storage/emulated/0/Download/summarize-aod-trace.sh /storage/emulated/0/Documents/ColorOS-AOD-Trace/log/aod-full-trace-XXXX.log'
```

## 设计原则

- 追踪器默认 **只读**。
- 不拦截 OFF。
- 不改变 DOZE / DOZE_SUSPEND。
- 不阻止 `DreamService.finish()`。
- 不替换返回值。
- 不修改方法参数。
- 不在模块中直接写公共存储。
- 文件采集由用户主动运行的 root shell 脚本完成。

这保证追踪器尽量不改变被观察的 ColorOS AOD 状态机。

## 兼容性

第一阶段针对已经实测的 OPPO / ColorOS 16 AOD 类名构建。OEM 内部类在 ColorOS 版本间可能变化；不存在的类或方法会记录 `TRACE_REGISTER ... unavailable`，不会阻止其他追踪点工作。

后续计划加入运行时类/方法发现、会话自动报告、JSONL 导出和 ColorOS 版本适配数据库。

## License

MIT
