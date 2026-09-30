package com.ayc.coloraodtrace.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.ayc.coloraodtrace.BuildConfig
import com.ayc.coloraodtrace.data.TraceConfig
import com.ayc.coloraodtrace.data.TraceConfigStore

class MainActivity : Activity() {
    private lateinit var enabledSwitch: Switch
    private lateinit var stacksSwitch: Switch
    private lateinit var diffsSwitch: Switch
    private lateinit var presetGroup: RadioGroup
    private lateinit var stackSeek: SeekBar
    private lateinit var stackValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "ColorOS AOD Trace"
        setContentView(buildUi())
        loadConfig()
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "ColorOS AOD Trace"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "v${BuildConfig.VERSION_NAME} · libxposed API 102\n纯追踪：不修改参数、返回值、Display state 或 Doze state。"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(16))
        })

        enabledSwitch = Switch(this).apply {
            text = "启用追踪"
        }
        root.addView(enabledSwitch, matchWrap())

        root.addView(sectionTitle("追踪预设"))
        presetGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        addPreset("基础：AOD 会话 / DozeService / DreamService", TraceConfig.PRESET_BASIC)
        addPreset("完整：所有已知关键链路", TraceConfig.PRESET_FULL)
        addPreset("Panoramic 深度：UI hide / Controller / Display", TraceConfig.PRESET_PANORAMIC)
        addPreset("Display/Doze 深度：DisplayUtil / DozeMachine", TraceConfig.PRESET_DISPLAY)
        addPreset("Wake/Unlock 深度：点击 / 指纹 / 唤醒", TraceConfig.PRESET_WAKE)
        root.addView(presetGroup, matchWrap())

        stacksSwitch = Switch(this).apply {
            text = "记录调用栈"
        }
        root.addView(stacksSwitch, matchWrap())

        diffsSwitch = Switch(this).apply {
            text = "记录方法前后字段快照与差异"
        }
        root.addView(diffsSwitch, matchWrap())

        stackValue = TextView(this)
        root.addView(stackValue)
        stackSeek = SeekBar(this).apply {
            max = TraceConfig.MAX_STACK_FRAMES - TraceConfig.MIN_STACK_FRAMES
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    stackValue.text = "调用栈深度：${progress + TraceConfig.MIN_STACK_FRAMES}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(stackSeek, matchWrap())

        root.addView(Button(this).apply {
            text = "保存配置"
            setOnClickListener { saveConfig() }
        }, matchWrap())

        root.addView(sectionTitle("采集方式"))
        root.addView(TextView(this).apply {
            text = "1. 在 LSPosed 中启用模块，作用域选择 系统界面 (com.android.systemui)。\n" +
                "2. 保存配置后建议重启 SystemUI；预设/栈/字段选项后续可近实时读取。\n" +
                "3. 使用 Release 附带的 capture-aod-trace.sh 开始采集。\n" +
                "4. 熄屏进入 AOD，复现问题后 Ctrl+C。\n" +
                "5. 日志默认保存到 /storage/emulated/0/Documents/ColorOS-AOD-Trace/log/。\n\n" +
                "结构化事件统一以 AODT| 开头，包含 session、elapsedMs、source、method、args、before/after/diff、stack。"
            textSize = 14f
        })

        return scroll
    }

    private fun addPreset(label: String, value: Int) {
        presetGroup.addView(RadioButton(this).apply {
            id = 1000 + value
            text = label
            tag = value
        })
    }

    private fun loadConfig() {
        val config = TraceConfigStore.read(this)
        enabledSwitch.isChecked = config.enabled
        stacksSwitch.isChecked = config.includeStacks
        diffsSwitch.isChecked = config.includeFieldDiffs
        presetGroup.check(1000 + config.preset)
        stackSeek.progress = config.maxStackFrames - TraceConfig.MIN_STACK_FRAMES
        stackValue.text = "调用栈深度：${config.maxStackFrames}"
    }

    private fun saveConfig() {
        val selected = presetGroup.findViewById<RadioButton>(presetGroup.checkedRadioButtonId)
        val preset = (selected?.tag as? Int) ?: TraceConfig.PRESET_FULL
        val config = TraceConfig(
            enabled = enabledSwitch.isChecked,
            preset = preset,
            includeStacks = stacksSwitch.isChecked,
            includeFieldDiffs = diffsSwitch.isChecked,
            maxStackFrames = stackSeek.progress + TraceConfig.MIN_STACK_FRAMES,
        )
        val ok = TraceConfigStore.write(this, config)
        Toast.makeText(
            this,
            if (ok) "已保存：${TraceConfig.presetName(preset)}" else "保存失败",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 18f
        gravity = Gravity.START
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
