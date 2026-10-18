package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonSessionScreen(
    exercises: List<ExerciseItem>,
    currentIndex: Int,
    correctCount: Int,
    isCompleted: Boolean,
    hearts: Int,
    arabicDialect: String,
    onSubmitAnswer: (Boolean) -> Unit,
    onSpeakWord: (String) -> Unit,
    onRefillHearts: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    if (isCompleted || exercises.isEmpty()) {
        LessonCelebrationView(
            total = exercises.size,
            correct = correctCount,
            onFinish = onClose
        )
        return
    }

    val currentEx = exercises[currentIndex.coerceIn(0, exercises.lastIndex)]
    var userInput by remember(currentIndex) { mutableStateOf("") }
    var assembledWords by remember(currentIndex) { mutableStateOf(listOf<String>()) }
    var matchedIndices by remember(currentIndex) { mutableStateOf(setOf<Int>()) }
    var selectedLeftIdx by remember(currentIndex) { mutableStateOf<Int?>(null) }
    var feedbackBanner by remember(currentIndex) { mutableStateOf<Pair<Boolean, String>?>(null) }

    val tipText = if (arabicDialect == "EGYPTIAN" && currentEx.targetWord.arabicSpeakerTipEgyptian.isNotBlank()) {
        currentEx.targetWord.arabicSpeakerTipEgyptian
    } else {
        currentEx.targetWord.arabicSpeakerTipMsa
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Progress & Hearts Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق الدرس")
            }
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / exercises.size.coerceAtLeast(1) },
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp),
                color = EmeraldPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartCrimson)
            Spacer(modifier = Modifier.width(4.dp))
            Text("$hearts", fontWeight = FontWeight.Bold)
            if (hearts <= 1) {
                TextButton(onClick = onRefillHearts) {
                    Text("+قلوب", color = HeartCrimson, fontSize = 12.sp)
                }
            }
        }

        // Exercise Type Badge
        Surface(
            color = EmeraldPrimary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "${currentEx.type.labelAr} (${currentIndex + 1}/${exercises.size})",
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Prompt Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { onSpeakWord(currentEx.targetWord.word) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "استمع للنطق",
                        tint = OxfordGold,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = currentEx.promptText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                if (currentEx.subPromptText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentEx.subPromptText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Arabic-specific Hint Box
        if (tipText.isNotBlank()) {
            Surface(
                color = OxfordGold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OxfordGold.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = OxfordGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = tipText, fontSize = 13.sp)
                }
            }
        }

        // Dynamic Exercise Body
        when (currentEx.type) {
            ExerciseType.MC_EN_TO_AR,
            ExerciseType.MC_AR_TO_EN,
            ExerciseType.LISTENING,
            ExerciseType.FILL_BLANK -> {
                currentEx.options.forEach { option ->
                    OutlinedButton(
                        onClick = {
                            val ok = option.equals(currentEx.correctAnswer, ignoreCase = true)
                            feedbackBanner = ok to if (ok) "إجابة صحيحة! أحسنت 🎉" else "الإجابة الصحيحة: ${currentEx.correctAnswer}"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(option, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            ExerciseType.SPELLING,
            ExerciseType.SPEAKING -> {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (currentEx.type == ExerciseType.SPEAKING)
                                    "انطق الكلمة أو اكتبها للتحقق السريع (${currentEx.targetWord.word})"
                                else "اكتب الكلمة بالإنجليزية"
                            )
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val ok = userInput.trim().equals(currentEx.correctAnswer, ignoreCase = true)
                            feedbackBanner = ok to if (ok) "ممتاز! إملاء ونطق متقن 🌟" else "الكلمة الصحيحة هي: ${currentEx.correctAnswer}"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("تحقق", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            onSpeakWord(currentEx.targetWord.word)
                            userInput = currentEx.targetWord.word
                        }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("محاكاة النطق الصوتي")
                    }
                }
            }

            ExerciseType.WORD_ORDER -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = assembledWords.joinToString(" ").ifBlank { "اضغط على الكلمات بالأسفل لترتيب الجملة..." },
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(14.dp),
                            color = EmeraldPrimary
                        )
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentEx.scrambledWords.forEach { w ->
                            AssistChip(
                                onClick = { assembledWords = assembledWords + w },
                                label = { Text(w, fontFamily = InterFontFamily) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { assembledWords = emptyList() }) {
                        Text("إعادة الترتيب")
                    }
                    Button(
                        onClick = {
                            val built = assembledWords.joinToString(" ")
                            val ok = built.equals(currentEx.correctAnswer, ignoreCase = true)
                            feedbackBanner = ok to if (ok) "ترتيب الجملة صحيح! 🎯" else "الترتيب الصحيح: ${currentEx.correctAnswer}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("تحقق من الجملة", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                    }
                }
            }

            ExerciseType.MATCH_PAIRS -> {
                currentEx.matchPairs.forEachIndexed { idx, (en, ar) ->
                    val isMatched = idx in matchedIndices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedLeftIdx = idx },
                            enabled = !isMatched,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(en, fontFamily = InterFontFamily)
                        }
                        OutlinedButton(
                            onClick = {
                                if (selectedLeftIdx == idx) {
                                    val nextSet = matchedIndices + idx
                                    matchedIndices = nextSet
                                    selectedLeftIdx = null
                                    if (nextSet.size == currentEx.matchPairs.size) {
                                        feedbackBanner = true to "طابقت جميع الأزواج بنجاح! 🎉"
                                    }
                                }
                            },
                            enabled = !isMatched,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isMatched) "✓ $ar" else ar)
                        }
                    }
                }
            }
        }

        // Feedback Banner + Next Button
        feedbackBanner?.let { (isCorrect, message) ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) PhonemeGreen.copy(alpha = 0.2f) else PhonemeRed.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, if (isCorrect) PhonemeGreen else PhonemeRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) PhonemeGreen else PhonemeRed,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onSubmitAnswer(isCorrect) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCorrect) PhonemeGreen else PhonemeRed
                        )
                    ) {
                        Text("متابعة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCelebrationView(
    total: Int,
    correct: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "إنجاز الدرس",
            tint = OxfordGold,
            modifier = Modifier.size(88.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "أحسنت! أكملت الدرس بنجاح 🎉",
            style = MaterialTheme.typography.headlineLarge,
            color = EmeraldPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "النتيجة: $correct من $total إجابات صحيحة • +30 XP",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("العودة إلى المسار", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
