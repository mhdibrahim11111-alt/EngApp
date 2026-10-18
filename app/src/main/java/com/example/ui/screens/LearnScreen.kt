package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.LearnModeTab
import com.example.ui.components.deckAccentColor
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(
    words: List<WordEntity>,
    stats: UserStatsEntity,
    activeMode: LearnModeTab,
    ieltsAssessment: IeltsAssessmentDto?,
    isAiLoading: Boolean,
    onSelectMode: (LearnModeTab) -> Unit,
    onStartLesson: (List<WordEntity>) -> Unit,
    onEvaluateIelts: (topic: String, response: String) -> Unit,
    onTapWordLookup: (String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val activeDecks = stats.activeDecksCsv.split(",").filter { it.isNotBlank() }.toSet()
    val filteredWords = remember(words, activeDecks) {
        words.filter { it.deck in activeDecks }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-navigation pills: Learning Path | IELTS Mode | IT Mode | Leaderboard
        ScrollableTabRow(
            selectedTabIndex = LearnModeTab.entries.indexOf(activeMode),
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            LearnModeTab.entries.forEach { tab ->
                Tab(
                    selected = activeMode == tab,
                    onClick = { onSelectMode(tab) },
                    text = {
                        Text(
                            text = if (stats.uiLanguage == "AR") tab.titleAr else tab.titleEn,
                            fontWeight = if (activeMode == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (activeMode) {
            LearnModeTab.PATH -> {
                DuolingoPathContent(
                    words = filteredWords.ifEmpty { words },
                    isArabicUi = stats.uiLanguage == "AR",
                    onStartLesson = onStartLesson
                )
            }
            LearnModeTab.IELTS -> {
                IeltsModeContent(
                    ieltsWords = words.filter { it.deck == "IELTS" },
                    assessment = ieltsAssessment,
                    isAiLoading = isAiLoading,
                    onStartQuiz = { onStartLesson(words.filter { it.deck == "IELTS" }) },
                    onEvaluate = onEvaluateIelts,
                    onInspectWord = { onTapWordLookup(it.word) }
                )
            }
            LearnModeTab.IT -> {
                ItIndustryModeContent(
                    itWords = words.filter { it.deck == "IT" },
                    onStartItQuiz = { onStartLesson(words.filter { it.deck == "IT" }) },
                    onTapWord = onTapWordLookup,
                    onSpeakText = onSpeakText
                )
            }
            LearnModeTab.LEADERBOARD -> {
                LeaderboardContent(stats = stats)
            }
        }
    }
}

@Composable
private fun DuolingoPathContent(
    words: List<WordEntity>,
    isArabicUi: Boolean,
    onStartLesson: (List<WordEntity>) -> Unit
) {
    val lessons = remember(words) {
        words.chunked(9)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabicUi) "مسار التعلّم التفاعلي (8-10 كلمات لكل درس)" else "Gamified Learning Path (8-10 words/lesson)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "تحديات اختيار من متعدد، استماع، إملاء، إكمال جمل، مطابقة، وترتيب كلمات",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        itemsIndexed(lessons) { idx, lessonWords ->
            val primaryDeck = lessonWords.firstOrNull()?.deck ?: "OXFORD"
            val accent = deckAccentColor(primaryDeck)
            val levelLabel = lessonWords.firstOrNull()?.cefrLevel ?: "B1"
            val masteredInLesson = lessonWords.count { it.masteryLevel != MasteryLevel.NEW.name }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lesson_node_${idx + 1}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Node Badge
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0F141C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الوحدة ${idx + 1}: ${DeckType.fromString(primaryDeck).titleAr} ($levelLabel)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = lessonWords.take(4).joinToString(" • ") { it.word },
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = accent
                        )
                        Text(
                            text = "التقدم: $masteredInLesson / ${lessonWords.size} كلمات متعلّمة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onStartLesson(lessonWords) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("ابدأ", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IeltsModeContent(
    ieltsWords: List<WordEntity>,
    assessment: IeltsAssessmentDto?,
    isAiLoading: Boolean,
    onStartQuiz: () -> Unit,
    onEvaluate: (String, String) -> Unit,
    onInspectWord: (WordEntity) -> Unit
) {
    val promptTopic = "Some people believe that technology has made society more isolated, while others think it connects communities globally. Discuss both views and give your opinion."
    var userEssay by remember {
        mutableStateOf("While excessive smartphone screen time can be detrimental to face-to-face relationships, ubiquitous digital platforms act as a powerful catalyst for global education and remote collaboration.")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IeltsTealSoft),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, IeltsTeal)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معمل الآيلتس الأكاديمي (IELTS Academic Lab)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = IeltsTealLight
                    )
                    Text(
                        text = "اختبارات مفردات أكاديمية + مصحح كتابة ومحادثة بالذكاء الاصطناعي مع تقدير درجة Band Score.",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = IeltsTeal)
                    ) {
                        Text("ابدأ اختبار مفردات IELTS Core (${ieltsWords.size} كلمة)", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تقييم IELTS Writing / Speaking بالذكاء الاصطناعي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = IeltsTeal
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "Task Prompt: $promptTopic",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = userEssay,
                            onValueChange = { userEssay = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            label = { Text("Your IELTS Response") }
                        )
                    }
                    Button(
                        onClick = { onEvaluate(promptTopic, userEssay) },
                        enabled = !isAiLoading && userEssay.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IeltsTeal)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("احصل على تقدير Band Score وتصحيح Gemini", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                    }

                    if (assessment != null) {
                        Surface(
                            color = Color(0xFF141821),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, IeltsTeal)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "الدرجة التقديرية: ${assessment.estimatedBand} 🎯",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = OxfordGold
                                )
                                Text("• الثروة اللغوية: ${assessment.lexicalResourceFeedbackAr}", fontSize = 13.sp, color = Color.White)
                                Text("• القواعد النحوية: ${assessment.grammarFeedbackAr}", fontSize = 13.sp, color = Color.White)
                                Text("• الترابط والمنطق: ${assessment.coherenceFeedbackAr}", fontSize = 13.sp, color = Color.White)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "Band 8.5 Upgrade: ${assessment.improvedSampleEn}",
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        color = IeltsTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItIndustryModeContent(
    itWords: List<WordEntity>,
    onStartItQuiz: () -> Unit,
    onTapWord: (String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ItMaroonSoft),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ItMaroon)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معمل البرمجة والتقنية (IT Career & Engineering)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ItMaroonLight
                    )
                    Text(
                        text = "مقالات تقنية تفاعلية قابلة للنقر على أي كلمة + أسئلة مقابلات هندسة البرمجيات.",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onStartItQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = ItMaroon)
                    ) {
                        Text("ابدأ تدريب مصطلحات IT (${itWords.size} مصطلح)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(SeedTranscriptsAndDrills.techArticles) { article ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(article.titleAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ItMaroon)
                        IconButton(onClick = { onSpeakText(article.paragraphsEn.joinToString(" ")) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "استمع للمقال")
                        }
                    }

                    Text(
                        text = "اضغط على أي كلمة إنجليزية لعرض معناها ونطقها وإضافتها لبطاقاتك:",
                        fontSize = 12.sp,
                        color = OxfordGold
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        article.paragraphsEn.forEach { para ->
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                para.split(" ").forEach { token ->
                                    val clean = token.replace(Regex("[^a-zA-Z-]"), "").lowercase()
                                    val isItTerm = itWords.any { it.word.equals(clean, ignoreCase = true) }
                                    Surface(
                                        color = if (isItTerm) ItMaroon.copy(alpha = 0.22f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onTapWord(token) }
                                    ) {
                                        Text(
                                            text = token,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isItTerm) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isItTerm) ItMaroon else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = "سؤال المقابلة التقنية (Mock Interview):",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 14.sp
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(article.interviewQuestionEn, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                    Text(article.interviewTipAr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardContent(stats: UserStatsEntity) {
    val entries = remember(stats.xp) {
        SeedTranscriptsAndDrills.weeklyLeaderboard.map {
            if (it.isCurrentUser) it.copy(xp = stats.xp) else it
        }.sortedByDescending { it.xp }.mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "لوحة الصدارة الأسبوعية (الـ الدوري الذهبي 🏆)",
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(entries) { item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isCurrentUser) EmeraldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, if (item.isCurrentUser) EmeraldPrimary else MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${item.rank}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (item.rank <= 3) OxfordGold else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = item.countryFlag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.badge, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${item.xp} XP",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
