package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

private data class PlacementQuestion(
    val questionAr: String,
    val sentenceEn: String,
    val options: List<String>,
    val correctIndex: Int,
    val levelTag: String
)

private val placementQuestions = listOf(
    PlacementQuestion(
        questionAr = "1. اختر الكلمة الصحيحة لإكمال الجملة اليومية (مستوى A2):",
        sentenceEn = "It would be ______ to save some money every month.",
        options = listOf("sensible", "sensitive", "reluctant", "ephemeral"),
        correctIndex = 0,
        levelTag = "B1"
    ),
    PlacementQuestion(
        questionAr = "2. ما معنى كلمة (mitigate) في سياق اختبارات الآيلتس؟ (مستوى B2):",
        sentenceEn = "Planting trees helps mitigate the effects of air pollution.",
        options = listOf("يخفف من حدة", "يفاقم المشكلة", "يتجاهل تماماً", "يؤجل مؤقتاً"),
        correctIndex = 0,
        levelTag = "B2"
    ),
    PlacementQuestion(
        questionAr = "3. في هندسة البرمجيات، ماذا نعني بـ (reducing API latency)؟ (مستوى C1):",
        sentenceEn = "Caching frequently accessed rows reduced API latency by 70%.",
        options = listOf("تقليل زمن الاستجابة والتأخر", "زيادة حجم قاعدة البيانات", "حذف واجهة المستخدم", "تغيير لغة البرمجة"),
        correctIndex = 0,
        levelTag = "C1"
    )
)

@Composable
fun OnboardingFlowScreen(
    onComplete: (goal: String, cefr: String, dailyMinutes: Int, dialect: String) -> Unit,
    onSkipWithDefaults: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0 = Goal & Dialect, 1 = Quick Level Test, 2 = Personalized Plan
    var selectedGoal by remember { mutableStateOf("GENERAL") }
    var selectedMinutes by remember { mutableIntStateOf(10) }
    var selectedDialect by remember { mutableStateOf("MSA") }

    var qIndex by remember { mutableIntStateOf(0) }
    var correctAnswers by remember { mutableIntStateOf(0) }
    val computedCefr = when (correctAnswers) {
        0 -> "A2"
        1 -> "B1"
        2 -> "B2"
        else -> "C1"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LinguaBridge • جسر اللغات",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = onSkipWithDefaults,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("تخطي للرئيسية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mascot Hero Header
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.14f),
                border = BorderStroke(2.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.size(92.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mascot_noor),
                    contentDescription = "نور - تميمة التطبيق",
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (step) {
                0 -> {
                    Text(
                        text = "أهلاً بك! أنا «نور» مرافقك الذكي لتعلّم الإنجليزية",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "مصمم خصيصاً للمتحدثين بالعربية: يجمع بين أسلوب Duolingo التفاعلي، وترجمة فيديوهات Trancy، ومدرب نطق ELSA بالذكاء الاصطناعي.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "اختر هدفك الرئيسي:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GoalOptionCard(
                        title = "اجتياز اختبار الآيلتس (IELTS Core)",
                        subtitle = "3,575 كلمة أكاديمية + تقييم كتابة ومحادثة بدرجة Band متوقعة",
                        accent = IeltsTeal,
                        selected = selectedGoal == "IELTS",
                        onClick = { selectedGoal = "IELTS" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GoalOptionCard(
                        title = "العمل في البرمجة والتقنية (IT Career)",
                        subtitle = "1,700 مصطلح تقني + اجتماعات Standup ومقابلات عمل برمجية",
                        accent = ItMaroon,
                        selected = selectedGoal == "IT",
                        onClick = { selectedGoal = "IT" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GoalOptionCard(
                        title = "الطلاقة الشاملة (Oxford 5000 + الشامل)",
                        subtitle = "5,836 كلمة أساسية للحياة اليومية والسفر والتواصل بثقة",
                        accent = OxfordGold,
                        selected = selectedGoal == "GENERAL",
                        onClick = { selectedGoal = "GENERAL" }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "هدفك اليومي وأسلوب الشرح العربي:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(5 to "5 دقائق", 10 to "10 دقائق", 20 to "20 دقيقة").forEach { (mins, label) ->
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedDialect == "MSA",
                            onClick = { selectedDialect = "MSA" },
                            label = { Text("العربية الفصحى (الافتراضي)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedDialect == "EGYPTIAN",
                            onClick = { selectedDialect = "EGYPTIAN" },
                            label = { Text("تلميحات بالعامية المصرية 🇪🇬") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_placement_test_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(
                            text = "ابدأ اختبار تحديد المستوى السريع (3 أسئلة)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF042F2E)
                        )
                    }
                }

                1 -> {
                    val q = placementQuestions[qIndex]
                    Text(
                        text = "اختبار تحديد المستوى السريع (${qIndex + 1} / ${placementQuestions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = OxfordGold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = q.questionAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = q.sentenceEn,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            q.options.forEachIndexed { idx, opt ->
                                OutlinedButton(
                                    onClick = {
                                        if (idx == q.correctIndex) correctAnswers++
                                        if (qIndex < placementQuestions.lastIndex) {
                                            qIndex++
                                        } else {
                                            step = 2
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = "خطتك الشخصية جاهزة! 🎯",
                        style = MaterialTheme.typography.headlineMedium,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("• مستواك الحالي المقدّر: CEFR $computedCefr", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("• المسار المختار: ${if (selectedGoal == "IELTS") "إعداد الآيلتس الأكاديمي" else if (selectedGoal == "IT") "مسار هندسة البرمجيات والتقنية" else "الطلاقة الشاملة (Oxford + IELTS + IT)"}")
                            Text("• الالتزام اليومي: $selectedMinutes دقائق يومياً مع نظام التكرار المتباعد SM-2")
                            Text("• تركيز النطق الذكي: أصوات P/B، V/F، الثاء والذال، وتتابع السواكن")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onComplete(selectedGoal, computedCefr, selectedMinutes, selectedDialect)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_onboarding_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(
                            text = "انطلق إلى LinguaBridge الآن 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF042F2E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalOptionCard(
    title: String,
    subtitle: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
