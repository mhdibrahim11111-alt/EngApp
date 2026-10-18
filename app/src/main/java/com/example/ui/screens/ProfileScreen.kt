package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.deckAccentColor
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    words: List<WordEntity>,
    stats: UserStatsEntity,
    pronunciationHistory: List<PronunciationAttemptEntity>,
    onStartWeakWordsSession: (List<WordEntity>) -> Unit,
    onInspectWord: (WordEntity) -> Unit,
    onOpenOnboardingReset: () -> Unit
) {
    val accuracy = remember(stats) {
        if (stats.totalAnswers == 0) 88
        else ((stats.correctAnswers.toFloat() / stats.totalAnswers) * 100).toInt().coerceIn(0, 100)
    }
    val weakWords = remember(words) {
        words.filter { it.isWeakWord || it.mistakeCount > 0 }.ifEmpty { words.take(4) }
    }
    val scoresTrend = remember(pronunciationHistory) {
        val fromDb = pronunciationHistory.map { it.overallScore }.reversed()
        if (fromDb.size >= 4) fromDb else listOf(68, 74, 79, 84, 88) + fromDb
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Summary & Analytics KPI Grid
        item {
            Text(
                text = "لوحة التقدم والإحصائيات الشاملة 📊",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "نسبة الدقة",
                    value = "$accuracy%",
                    subtitle = "${stats.correctAnswers}/${stats.totalAnswers} إجابة",
                    accent = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "وقت الدراسة",
                    value = "${stats.totalMinutesStudied} د",
                    subtitle = "اليوم: ${stats.minutesStudiedToday}/${stats.dailyGoalMinutes} د",
                    accent = OxfordGold,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "الحماسة الحالية",
                    value = "${stats.streakDays} أيام 🔥",
                    subtitle = "المستوى ${stats.level} (${stats.xp} XP)",
                    accent = StreakFlameOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Streak Calendar & Badges Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تقويم الاستمرارية والأوسمة المكتسبة 🏅", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val daysLabels = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
                    val activeFlags = listOf(true, true, true, false, true, true, true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysLabels.forEachIndexed { idx, day ->
                            val active = activeFlags.getOrElse(idx) { true }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) EmeraldPrimary else Color(0xFF2D3748),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (active) Icons.Default.Check else Icons.Default.AcUnit,
                                            contentDescription = day,
                                            tint = if (active) Color(0xFF042F2E) else FreezeIceBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BadgeItem("🔥 بطل الأسبوع", "5 أيام متتالية")
                        BadgeItem("🎙️ متقن الأصوات", "تحدي P/B & V/F")
                        BadgeItem("🎓 باحث IELTS", "مفردات C1")
                    }
                }
            }
        }

        // 3. Pronunciation Score Trend Chart (Canvas)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("منحنى تطور دقة النطق (ELSA Score Trend)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "${scoresTrend.lastOrNull() ?: 85}%",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PronunciationTrendCanvas(scores = scoresTrend)
                }
            }
        }

        // 4. Mastery Breakdown per Deck
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إحصائيات الإتقان حسب الحزمة (New → Learning → Review → Mastered)", fontWeight = FontWeight.Bold)
                    DeckType.entries.forEach { d ->
                        val dWords = words.filter { it.deck == d.id }
                        val mastered = dWords.count { it.masteryLevel == MasteryLevel.MASTERED.name }
                        val learning = dWords.count { it.masteryLevel == MasteryLevel.LEARNING.name || it.masteryLevel == MasteryLevel.REVIEW.name }
                        val accent = deckAccentColor(d.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(d.titleAr, color = accent, fontWeight = FontWeight.Bold)
                            Text("متقنة: $mastered | قيد المراجعة: $learning | المجموع: ${dWords.size}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Weak Words Diagnostic List + Smart "Practice Weak Words" Session
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("قائمة الكلمات الضعيفة التي تحتاج تركيزاً (${weakWords.size})", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { onStartWeakWordsSession(weakWords) },
                    colors = ButtonDefaults.buttonColors(containerColor = OxfordGold),
                    modifier = Modifier.testTag("profile_practice_weak_words_button")
                ) {
                    Text("تدرّب عليها الآن", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        items(weakWords) { w ->
            val accent = deckAccentColor(w.deck)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectWord(w) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${w.word}  ${w.ipaUs}", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = accent)
                        Text(w.arabicTranslation, fontSize = 14.sp)
                    }
                    Surface(
                        color = PhonemeRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "أخطاء: ${w.mistakeCount.coerceAtLeast(1)}",
                            color = PhonemeRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenOnboardingReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة ضبط الهدف واختبار تحديد المستوى")
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accent)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BadgeItem(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OxfordGold)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PronunciationTrendCanvas(scores: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (scores.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = if (scores.size > 1) w / (scores.size - 1) else w
        val path = Path()

        scores.forEachIndexed { idx, score ->
            val x = idx * stepX
            val normalized = (score.coerceIn(40, 100) - 40) / 60f
            val y = h - (normalized * h)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(
                color = EmeraldPrimary,
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
        }
        drawPath(
            path = path,
            color = EmeraldPrimary,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
