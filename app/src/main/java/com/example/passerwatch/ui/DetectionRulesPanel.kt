package com.example.passerwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passerwatch.model.DetectionSettings
import com.example.passerwatch.model.MonitorUiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DetectionRulesPanel(
    settings: DetectionSettings,
    running: Boolean,
    ui: MonitorUiState,
    pending: Int,
    onApply: (DetectionSettings) -> Unit
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    val changed = draft != settings
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RulesCard {
            Text("DETECTION RULES", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Text("让抓拍符合你的场景", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            RuleDescription("调整识别灵敏度和去重节奏。停止检测后应用，新规则在下一次检测中生效。")
        }

        RulesCard {
            Text("抓拍流程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RuleDescription("流程说明 · 当前实际状态见下方")
            val stages = listOf(
                "未开始" to "点击开始检测，进入等待。",
                "等待有人" to "连续分析最新画面，寻找可抓拍目标。",
                "连续确认" to "满足 ${settings.confirmationFrames} 帧有效条件后触发。",
                "抓拍并锁定" to "每次出现触发一次抓拍，停留期间不重复。",
                "等待离开" to "连续有效无人 ${decimal(settings.vacantSeconds, 1)} 秒后解锁。",
                "重新待命" to "自动等待下次出现，检测继续运行。"
            )
            stages.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { index, stage ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("STAGE 0${row * 2 + index + 1}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                Text(stage.first, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium)
                                RuleDescription(stage.second)
                            }
                        }
                    }
                }
            }
            Text("当前状态 · ${ui.monitorText}", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium)
            if (ui.error != null) Text(ui.error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Text("核心检测参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        RuleSlider("分析频率", "${draft.targetFps} FPS", draft.targetFps.toFloat(), 1f..30f, 28,
            "检测频率上限。相机只保留最新帧；实际速度受设备和模型耗时影响。") {
            draft = draft.copy(targetFps = it.roundToInt())
        }
        RuleSlider("置信度阈值", decimal(draft.scoreThreshold, 2), draft.scoreThreshold, 0.3f..0.95f, 12,
            "用于人脸、人员和姿态识别。降低更容易触发，提高更严格；完整身体的可见性规则保持独立。") {
            draft = draft.copy(scoreThreshold = (it * 100).roundToInt() / 100f)
        }
        RuleSlider("连续确认帧数", "${draft.confirmationFrames} 帧", draft.confirmationFrames.toFloat(), 1f..5f, 3,
            "连续满足可抓拍条件才触发。无人、识别失败或结果间隔超过允许范围会清零；1 FPS 模式允许少量回调延迟。") {
            draft = draft.copy(confirmationFrames = it.roundToInt())
        }
        RulesCard {
            RuleHeading("单次出现抓拍数", "1 张")
            RuleDescription("抓拍触发后立即锁定。目标停留时持续检测，直到确认离开后才允许下一次触发。保存失败会显示错误，不代表已成功落盘。")
        }
        RuleSlider("离开确认时长", "${decimal(draft.vacantSeconds, 1)} 秒", draft.vacantSeconds, 1f..5f, 7,
            "锁定后连续收到有效无人结果满该时长才重新待命。识别失败不会被当作无人。") {
            draft = draft.copy(vacantSeconds = (it * 2).roundToInt() / 2f)
        }
        RulesCard {
            RuleHeading("保存队列", "1 写入 + 2 等待")
            RuleDescription("队列满时拒绝新的保存任务，并提示繁忙。检测继续进行，已经入队的任务按顺序处理。")
            Text("当前待处理 · $pending", color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
        }
        RulesCard {
            Text(if (changed) "有尚未应用的调整" else "参数与当前配置一致",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            RuleDescription(if (running) "检测正在运行。请先停止检测，再应用新参数。" else
                "应用后重新开始检测即可使用新规则。重置默认值后仍需点击应用。")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { draft = DetectionSettings() }, modifier = Modifier.weight(1f)) {
                    Text("重置默认值")
                }
                Button(onClick = { if (!running) onApply(draft) }, enabled = !running && changed,
                    modifier = Modifier.weight(1f)) {
                    Text("应用参数")
                }
            }
            RuleDescription("默认：20 FPS · 阈值 0.60 · 连续 2 帧 · 离开 2.0 秒")
        }
    }
}

@Composable
private fun RulesCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun RuleHeading(title: String, badge: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text(badge, modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RuleDescription(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RuleSlider(
    title: String,
    badge: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    description: String,
    onValueChange: (Float) -> Unit
) {
    RulesCard {
        RuleHeading(title, badge)
        RuleDescription(description)
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

private fun decimal(value: Float, places: Int): String = String.format(Locale.US, "%.${places}f", value)
