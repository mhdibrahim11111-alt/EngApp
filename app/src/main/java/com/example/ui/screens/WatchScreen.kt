package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.components.deckAccentColor
import com.example.ui.theme.*
import kotlinx.serialization.json.Json

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatchScreen(
    transcripts: List<TranscriptLessonEntity>,
    selectedTranscript: TranscriptLessonEntity?,
    allWords: List<WordEntity>,
    activeSubtitleIndex: Int,
    isPlaying: Boolean,
    loopSentence: Boolean,
    autoPause: Boolean,
    playbackSpeed: Float,
    showEnglish: Boolean,
    showArabic: Boolean,
    fontSizeSp: Int,
    isAiLoading: Boolean,
    onSelectTranscript: (TranscriptLessonEntity) -> Unit,
    onTogglePlay: (List<SubtitleLine>) -> Unit,
    onJumpLine: (Int, Int) -> Unit,
    onSelectLine: (Int, String) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleAutoPause: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleEnglish: () -> Unit,
    onToggleArabic: () -> Unit,
    onAdjustFontSize: (Int) -> Unit,
    onTapWord: (String) -> Unit,
    onCreateCustomTranscript: (title: String, url: String, rawText: String) -> Unit
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val activeLesson = selectedTranscript ?: transcripts.firstOrNull()
    val lines = remember(activeLesson) {
        if (activeLesson == null) emptyList()
        else try {
            json.decodeFromString<List<SubtitleLine>>(activeLesson.linesJson)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val keyVocab = remember(activeLesson) {
        if (activeLesson == null) emptyList()
        else try {
            json.decodeFromString<List<KeyVocabItem>>(activeLesson.keyVocabJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    var showImporterSheet by remember { mutableStateOf(false) }
    val wordDeckMap = remember(allWords) {
        allWords.associate { it.word.lowercase() to it.deck }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("watch_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Lesson Selector + Custom YouTube / SRT Import Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شاهد وتعلّم (Trancy Dual Subtitles)",
                    style = MaterialTheme.typography.titleLarge
                )
                FilledTonalButton(
                    onClick = { showImporterSheet = !showImporterSheet },
                    modifier = Modifier.testTag("open_srt_importer_button")
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رابط يوتيوب / SRT")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(transcripts) { item ->
                    val isSelected = item.id == activeLesson?.id
                    val accent = deckAccentColor(item.category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTranscript(item) },
                        label = { Text("${item.titleAr} (${item.cefrLevel})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.2f),
                            selectedLabelColor = accent
                        )
                    )
                }
            }
        }

        // Custom YouTube / SRT Importer Card
        if (showImporterSheet) {
            item {
                CustomTranscriptImporterCard(
                    isAiLoading = isAiLoading,
                    onSubmit = { title, url, text ->
                        onCreateCustomTranscript(title, url, text)
                        showImporterSheet = false
                    }
                )
            }
        }

        // 2. Interactive Video / Audio Stage Card + Active Dual Subtitle Display
        if (activeLesson != null && lines.isNotEmpty()) {
            val currentLine = lines[activeSubtitleIndex.coerceIn(0, lines.lastIndex)]

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                    border = BorderStroke(1.dp, Color(0xFF2B3548))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Video Stage Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeLesson.titleAr,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = activeLesson.speakerOrSource,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "جملة ${activeSubtitleIndex + 1}/${lines.size}",
                                    color = EmeraldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Active Interactive Dual Subtitle Stage
                        Surface(
                            color = Color(0xFF192130),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, OxfordGold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (showEnglish) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            currentLine.textEn.split(" ").forEach { rawWord ->
                                                val clean = rawWord.replace(Regex("[^a-zA-Z-]"), "").lowercase()
                                                val deck = wordDeckMap[clean]
                                                val highlightColor = if (deck != null) deckAccentColor(deck) else Color.White

                                                Surface(
                                                    color = if (deck != null) highlightColor.copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .padding(horizontal = 2.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { onTapWord(rawWord) }
                                                ) {
                                                    Text(
                                                        text = rawWord,
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = if (deck != null) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        color = highlightColor,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showArabic) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentLine.textAr,
                                        fontFamily = CairoFontFamily,
                                        fontSize = (fontSizeSp - 2).sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Legend for Deck Word Highlighting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendDot(color = OxfordGold, text = "Oxford 5000")
                            LegendDot(color = IeltsTeal, text = "IELTS Core")
                            LegendDot(color = ItMaroon, text = "IT Industry")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transport Controls: Prev | Play/Pause | Next | Loop | Slow | Auto-Pause
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onJumpLine(-1, lines.size) }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "الجملة السابقة", tint = Color.White)
                            }

                            FilledIconButton(
                                onClick = { onTogglePlay(lines) },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("watch_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color(0xFF042F2E)
                                )
                            }

                            IconButton(onClick = { onJumpLine(1, lines.size) }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "الجملة التالية", tint = Color.White)
                            }

                            FilterChip(
                                selected = loopSentence,
                                onClick = onToggleLoop,
                                label = { Text("تكرار 🔁", fontSize = 11.sp) }
                            )

                            FilterChip(
                                selected = playbackSpeed < 1.0f,
                                onClick = onToggleSpeed,
                                label = { Text("${playbackSpeed}x 🐢", fontSize = 11.sp) }
                            )
                        }

                        // Subtitle Display Controls: EN | AR | Auto-Pause | Font +/-
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = showEnglish,
                                onClick = onToggleEnglish,
                                label = { Text("EN", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = showArabic,
                                onClick = onToggleArabic,
                                label = { Text("عربي", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = autoPause,
                                onClick = onToggleAutoPause,
                                label = { Text("توقف تلقائي", fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedIconButton(
                                onClick = { onAdjustFontSize(-2) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("A-", fontSize = 11.sp, color = Color.White)
                            }
                            OutlinedIconButton(
                                onClick = { onAdjustFontSize(2) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("A+", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. Key Vocabulary Extracted by Gemini for this Video
            if (keyVocab.isNotEmpty()) {
                item {
                    Text(
                        text = "المفردات المفتاحية في هذا الفيديو (اضغط لعرض بطاقة الكلمة):",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(keyVocab) { kv ->
                            val accent = deckAccentColor(kv.deck)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, accent),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTapWord(kv.word) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(kv.word, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = accent)
                                    Text(kv.ipa, fontFamily = InterFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text(kv.arabic, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Full Interactive Transcript Lines
            item {
                Text(
                    text = "النص التفاعلي الكامل (اضغط على أي جملة للتشغيل أو أي كلمة للترجمة):",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            itemsIndexed(lines) { index, line ->
                val isCurrent = index == activeSubtitleIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLine(index, line.textEn) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) EmeraldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = line.textEn,
                                fontFamily = InterFontFamily,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = line.textAr,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 11.sp, color = Color(0xFFCBD5E1), fontFamily = InterFontFamily)
    }
}

@Composable
private fun CustomTranscriptImporterCard(
    isAiLoading: Boolean,
    onSubmit: (title: String, url: String, rawText: String) -> Unit
) {
    var title by remember { mutableStateOf("درس يوتيوب / SRT مخصص") }
    var url by remember { mutableStateOf("https://youtube.com/watch?v=custom") }
    var rawText by remember {
        mutableStateOf(
            """
            1
            00:00:01,000 --> 00:00:05,000
            Building a scalable architecture requires reducing latency and bottlenecks.
            2
            00:00:05,000 --> 00:00:10,000
            Empirical data helps engineers make sensible decisions under pressure.
            """.trimIndent()
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EmeraldPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "استيراد رابط يوتيوب أو ملف ترجمة SRT (مع ترجمة Gemini التلقائية)",
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الفيديو / الدرس") },
                modifier = Modifier.fillMaxWidth()
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("YouTube URL or Video Tag") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste SRT or English Transcript Lines") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Button(
                onClick = { onSubmit(title, url, rawText) },
                enabled = !isAiLoading && rawText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("توليد الترجمة المزدوجة والكلمات المفتاحية عبر Gemini", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            }
        }
    }
}
