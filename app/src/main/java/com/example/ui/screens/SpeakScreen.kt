package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.SpeakModeTab
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakScreen(
    stats: UserStatsEntity,
    activeTab: SpeakModeTab,
    selectedCategory: PhonemeChallengeCategory,
    latestEvaluation: PronunciationEvaluation?,
    selectedScenario: ChatScenario,
    chatMessages: List<ChatMessage>,
    isAiLoading: Boolean,
    onSelectTab: (SpeakModeTab) -> Unit,
    onSelectCategory: (PhonemeChallengeCategory) -> Unit,
    onAnalyzePronunciation: (targetText: String, spokenText: String) -> Unit,
    onSelectScenario: (ChatScenario) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSpeakText: (text: String, slow: Boolean) -> Unit,
    onTapWordLookup: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = SpeakModeTab.entries.indexOf(activeTab),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SpeakModeTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = if (stats.uiLanguage == "AR") tab.titleAr else tab.titleEn,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (activeTab) {
            SpeakModeTab.PRONUNCIATION -> {
                ElsaPronunciationCoachContent(
                    stats = stats,
                    selectedCategory = selectedCategory,
                    evaluation = latestEvaluation,
                    isAiLoading = isAiLoading,
                    onSelectCategory = onSelectCategory,
                    onAnalyze = onAnalyzePronunciation,
                    onSpeakText = onSpeakText
                )
            }
            SpeakModeTab.AI_TUTOR -> {
                AiTutorChatContent(
                    stats = stats,
                    selectedScenario = selectedScenario,
                    messages = chatMessages,
                    isAiLoading = isAiLoading,
                    onSelectScenario = onSelectScenario,
                    onSendMessage = onSendChatMessage,
                    onSpeakText = { onSpeakText(it, false) },
                    onTapWord = onTapWordLookup
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElsaPronunciationCoachContent(
    stats: UserStatsEntity,
    selectedCategory: PhonemeChallengeCategory,
    evaluation: PronunciationEvaluation?,
    isAiLoading: Boolean,
    onSelectCategory: (PhonemeChallengeCategory) -> Unit,
    onAnalyze: (String, String) -> Unit,
    onSpeakText: (String, Boolean) -> Unit
) {
    val drillsForCategory = remember(selectedCategory) {
        SeedTranscriptsAndDrills.minimalPairDrills.filter { it.category == selectedCategory }
            .ifEmpty { SeedTranscriptsAndDrills.minimalPairDrills.take(2) }
    }
    var selectedDrill by remember(selectedCategory) { mutableStateOf(drillsForCategory.first()) }
    var spokenCapture by remember(selectedDrill) { mutableStateOf(selectedDrill.sentenceEn) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) {
                spokenCapture = recognized
                onAnalyze(selectedDrill.sentenceEn, recognized)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, selectedDrill.sentenceEn)
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (_: Exception) {
                onAnalyze(selectedDrill.sentenceEn, spokenCapture)
            }
        } else {
            onAnalyze(selectedDrill.sentenceEn, spokenCapture)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pronunciation_coach_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Arabic Speaker Phoneme Challenge Categories
        item {
            Text(
                text = "مدرب النطق الذكي (ELSA) — أصوات يصعب نطقها على المتحدث العربي:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PhonemeChallengeCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text("${cat.titleAr} (${cat.symbols})") }
                    )
                }
            }
        }

        // 2. Mouth & Tongue Position Coaching Card in MSA / Egyptian Arabic
        item {
            val tip = if (stats.arabicDialect == "EGYPTIAN") selectedCategory.tipEgyptian else selectedCategory.tipMsa
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15222B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FreezeIceBlue.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = FreezeIceBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "وضعية الفم واللسان (${if (stats.arabicDialect == "EGYPTIAN") "شرح بالمصري 🇪🇬" else "شرح بالفصحى"}):",
                            fontWeight = FontWeight.Bold,
                            color = FreezeIceBlue,
                            fontSize = 14.sp
                        )
                        Text(text = tip, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. Minimal-Pair Drills (Ship/Sheep, Pen/Ben, Very/Ferry, Think/Sink, Street/Spring)
        item {
            Text(
                text = "تدريبات الأزواج المتشابهة (Minimal Pairs):",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                drillsForCategory.forEach { drill ->
                    FilterChip(
                        selected = selectedDrill.id == drill.id,
                        onClick = { selectedDrill = drill },
                        label = { Text("${drill.wordA} vs ${drill.wordB}", fontFamily = InterFontFamily) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinimalPairWordBox(
                    word = selectedDrill.wordA,
                    ipa = selectedDrill.ipaA,
                    arabic = selectedDrill.arabicA,
                    accent = EmeraldPrimary,
                    onSpeak = { onSpeakText(selectedDrill.wordA, false) },
                    modifier = Modifier.weight(1f)
                )
                MinimalPairWordBox(
                    word = selectedDrill.wordB,
                    ipa = selectedDrill.ipaB,
                    arabic = selectedDrill.arabicB,
                    accent = OxfordGold,
                    onSpeak = { onSpeakText(selectedDrill.wordB, false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Target Sentence Recording & Gemini Phoneme Diagnostics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "جملة التدريب الصوتي:",
                        fontWeight = FontWeight.Bold,
                        color = OxfordGold
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = selectedDrill.sentenceEn,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = selectedDrill.sentenceAr,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { onSpeakText(selectedDrill.sentenceEn, false) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نطق طبيعي")
                        }
                        OutlinedButton(onClick = { onSpeakText(selectedDrill.sentenceEn, true) }) {
                            Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نطق بطيء 🐢")
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = spokenCapture,
                            onValueChange = { spokenCapture = it },
                            label = { Text("Spoken / Recorded Transcript for Gemini Analysis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("record_pronunciation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ItMaroon),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سجّل صوتك", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAnalyze(selectedDrill.sentenceEn, spokenCapture) },
                            enabled = !isAiLoading,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(52.dp)
                                .testTag("analyze_pronunciation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحليل النطق بالذكاء الاصطناعي", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Color-Coded Phoneme Feedback Card (Green / Yellow / Red)
        if (evaluation != null) {
            item {
                val scoreColor = when {
                    evaluation.overallScore >= 80 -> PhonemeGreen
                    evaluation.overallScore >= 60 -> PhonemeYellow
                    else -> PhonemeRed
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, scoreColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نتيجة تشخيص النطق التفصيلي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Surface(
                                color = scoreColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, scoreColor)
                            ) {
                                Text(
                                    text = "${evaluation.overallScore}%",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = scoreColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Color-coded words (Green / Yellow / Red)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                evaluation.words.forEach { wf ->
                                    val wColor = when {
                                        wf.score >= 80 -> PhonemeGreen
                                        wf.score >= 60 -> PhonemeYellow
                                        else -> PhonemeRed
                                    }
                                    Surface(
                                        color = wColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, wColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(wf.word, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = wColor)
                                            Text("${wf.ipaTarget} • ${wf.score}%", fontFamily = InterFontFamily, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                        }
                                    }
                                }
                            }
                        }

                        // Per-word Arabic notes
                        evaluation.words.forEach { wf ->
                            if (wf.issueAr.isNotBlank()) {
                                Text(
                                    text = "• ${wf.word}: ${wf.issueAr}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF262D3D))

                        Text(
                            text = "نصيحة حركة الفم واللسان (${if (stats.arabicDialect == "EGYPTIAN") "عامية مصرية" else "فصحى"}):",
                            fontWeight = FontWeight.Bold,
                            color = OxfordGold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (stats.arabicDialect == "EGYPTIAN") evaluation.mouthTipEgyptian else evaluation.mouthTipMsa,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalPairWordBox(
    word: String,
    ipa: String,
    arabic: String,
    accent: Color,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSpeak() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accent)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "نطق $word", tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(ipa, fontFamily = InterFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(arabic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AiTutorChatContent(
    stats: UserStatsEntity,
    selectedScenario: ChatScenario,
    messages: List<ChatMessage>,
    isAiLoading: Boolean,
    onSelectScenario: (ChatScenario) -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onTapWord: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Scenario Picker Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ChatScenario.entries) { sc ->
                FilterChip(
                    selected = selectedScenario == sc,
                    onClick = { onSelectScenario(sc) },
                    label = { Text(sc.titleAr) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeak = { onSpeakText(msg.textEn) },
                    onTapSuggestedWord = onTapWord
                )
            }
            if (isAiLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المعلم الذكي يكتب رداً وتصحيحاً لغوياً...", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Reply in English (e.g. We refactored the database...)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutor_chat_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 3
                )
            }

            FilledIconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("send_tutor_message_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color(0xFF042F2E))
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onTapSuggestedWord: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUser) EmeraldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (message.isUser) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.isUser) "أنت (المتعلم)" else "المعلم الذكي (Noor AI Tutor)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (message.isUser) EmeraldPrimary else OxfordGold
                )
                IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "نطق الرسالة", modifier = Modifier.size(18.dp))
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = message.textEn,
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (message.grammarCorrection.isNotBlank()) {
                Surface(
                    color = OxfordGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "✏️ ${message.grammarCorrection}",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = OxfordGold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            if (message.arabicExplanation.isNotBlank()) {
                Text(
                    text = "💡 ${message.arabicExplanation}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (message.vocabularySuggestions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("كلمات مقترحة من حزمك:", fontSize = 11.sp, color = EmeraldPrimary)
                    message.vocabularySuggestions.forEach { w ->
                        SuggestionChip(
                            onClick = { onTapSuggestedWord(w) },
                            label = { Text(w, fontFamily = InterFontFamily, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
