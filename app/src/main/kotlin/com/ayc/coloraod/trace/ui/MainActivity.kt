package com.ayc.coloraod.trace.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayc.coloraod.trace.TraceApplication
import com.ayc.coloraod.trace.config.TraceConfig
import com.ayc.coloraod.trace.config.TraceConfigStore
import com.ayc.coloraod.trace.config.TraceProfile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TraceScreen(
                    initial = TraceConfigStore.read(this),
                    serviceConnected = TraceApplication.isServiceConnected(),
                    onSave = { TraceConfigStore.write(this, it) },
                )
            }
        }
    }
}

@Composable
private fun TraceScreen(
    initial: TraceConfig,
    serviceConnected: Boolean,
    onSave: (TraceConfig) -> Boolean,
) {
    var config by remember { mutableStateOf(initial) }
    var saveState by remember { mutableStateOf("未修改") }

    fun update(next: TraceConfig) {
        config = next
        saveState = if (onSave(next)) "已保存" else "保存失败"
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ColorOS AOD Trace") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("只读诊断模块", style = MaterialTheme.typography.titleMedium)
                        Text("不会修改 AOD 参数、返回值、Display state 或 View 状态。")
                        Text("XposedService：${if (serviceConnected) "已连接" else "等待连接"}")
                        Text("配置状态：$saveState")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("追踪总开关", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(if (config.enabled) "已启用" else "已停用")
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { update(config.copy(enabled = it)) },
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("追踪档位：${config.profile.title}", style = MaterialTheme.typography.titleMedium)
                        TraceProfile.entries.forEach { profile ->
                            Button(
                                onClick = { update(config.copy(profile = profile)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    when (profile) {
                                        TraceProfile.BASIC -> "基础：生命周期 + Display/Doze"
                                        TraceProfile.FULL -> "全量：全部已知 AOD 链路"
                                        TraceProfile.PANORAMIC -> "全景：Panoramic + UI + Display"
                                        TraceProfile.DISPLAY -> "Display：屏幕状态与 Doze"
                                        TraceProfile.WAKE -> "唤醒/退出：Wake + Doze + Display"
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("诊断细节", style = MaterialTheme.typography.titleMedium)
                        ToggleRow(
                            title = "关键调用栈",
                            checked = config.captureStacks,
                            onChange = { update(config.copy(captureStacks = it)) },
                        )
                        ToggleRow(
                            title = "字段前后差异 FIELD_DIFF",
                            checked = config.captureFieldDiffs,
                            onChange = { update(config.copy(captureFieldDiffs = it)) },
                        )
                        Text("调用栈深度：${config.maxStackDepth}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                update(config.copy(maxStackDepth = (config.maxStackDepth - 2).coerceAtLeast(4)))
                            }) { Text("-2") }
                            Button(onClick = {
                                update(config.copy(maxStackDepth = (config.maxStackDepth + 2).coerceAtMost(32)))
                            }) { Text("+2") }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("推荐使用方式", style = MaterialTheme.typography.titleMedium)
                        Text("1. LSPosed 作用域勾选 SystemUI。")
                        Text("2. 选择“全量”并重启 SystemUI。")
                        Text("3. 在 Termux 运行 Release 附带的 capture-aod-trace.sh。")
                        Text("4. 熄屏后不要操作，等待 AOD 自然结束。")
                        Text("5. 用 summarize-aod-trace.sh 生成时间线摘要，或直接提交 raw 日志。")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
