package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.TrancyWordCard
import com.example.ui.theme.*

/**
 * Trancy + SM-2 Spaced Repetition Flashcard Screen.
 * Directly implements the visual layout from the user's uploaded screenshot:
 * - Header: `< CEFR_independent` + `1 / 3575` counter in gold/white
 * - Previous `<` / Next `>` circular navigation buttons
 * - Center `TrancyWordCard` with `Detailed Definition` drawer button & `Mastered [M]`
 * - Bottom SM-2 Scheduling Bar: Again / Hard / Good / Easy with interval hints
 */
@Composable
fun SrsReviewScreen(
    queue: List<WordEntity>,
    currentIndex: Int,
    arabicDialect: String,
    onNavigateCard: (Int) -> Unit,
    onGrade: (SrsGrade) -> Unit,
    onInspectWord: (WordEntity) -> Unit,
    onToggleMastered: (WordEntity) -> Unit,
    onSpeakWord: (String, Boolean) -> Unit,
    onSwitchToListView: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val currentWord = queue.getOrNull(currentIndex)
    val totalCatalog = when (currentWord?.deck) {
        "OXFORD" -> 5836
        "IELTS" -> 3575
        "IT" -> 1700
        else -> 3571
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0E14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Trancy-style Header Bar
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1A202C),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "رجوع")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CEFR_${currentWord?.cefrLevel ?: "independent"}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    }

                    // Gold index / total counter: "1 / 3571" + Switch to Word List Table button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentIndex + 1}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OxfordGold
                        )
                        Text(
                            text = " / $totalCatalog",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalIconButton(
                            onClick = onSwitchToListView,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1A202C),
                                contentColor = OxfordGold
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Word List Table")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentWord == null) {
                Text(
                    text = "لا توجد بطاقات مستحقة حالياً!",
                    color = Color.White,
                    fontSize = 18.sp
                )
            } else {
                // Card Navigation Row (< Prev | Next >)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = { onNavigateCard(-1) },
                            enabled = currentIndex > 0,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF2D3748))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "السابق",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "بطاقة ${currentIndex + 1} من ${queue.size} في جلسة اليوم",
                            fontFamily = CairoFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )

                        OutlinedIconButton(
                            onClick = { onNavigateCard(1) },
                            enabled = currentIndex < queue.lastIndex,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF2D3748))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "التالي",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Trancy Word Card
                TrancyWordCard(
                    word = currentWord,
                    arabicDialect = arabicDialect,
                    onSpeakUs = { onSpeakWord(currentWord.word, false) },
                    onSpeakGb = { onSpeakWord(currentWord.word, true) },
                    onSpeakSentence1 = { onSpeakWord(currentWord.exampleEn1, false) },
                    onSpeakSentence2 = { onSpeakWord(currentWord.exampleEn2, false) },
                    onOpenAiDefinition = { onInspectWord(currentWord) },
                    onToggleMastered = { onToggleMastered(currentWord) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // SM-2 Spaced Repetition Grading Buttons (Again / Hard / Good / Easy)
                Text(
                    text = "جدولة التكرار المتباعد (SM-2) — كيف كان تذكرك للكلمة؟",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SrsGrade.entries.forEach { grade ->
                        val btnColor = when (grade) {
                            SrsGrade.AGAIN -> PhonemeRed
                            SrsGrade.HARD -> PhonemeYellow
                            SrsGrade.GOOD -> EmeraldPrimary
                            SrsGrade.EASY -> FreezeIceBlue
                        }
                        OutlinedButton(
                            onClick = { onGrade(grade) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("srs_grade_${grade.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, btnColor.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = btnColor.copy(alpha = 0.12f)
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = grade.labelAr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = btnColor
                                )
                                Text(
                                    text = "${grade.labelEn} • ${grade.intervalHintAr}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CSV / JSON Custom Deck Import Screen with Gemini Batch Auto-Fill.
 */
@Composable
fun CsvJsonImportScreen(
    isAiLoading: Boolean,
    onImport: (rawText: String, deck: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedDeck by remember { mutableStateOf("OXFORD") }
    var rawInput by remember {
        mutableStateOf(
            """
            word,pos,level,deck,arabic,definition,example
            microservice,n.,C1,IT,,,We split the monolith into independent microservices.
            meticulous,adj.,C1,IELTS,دقيق للغاية ; شديد العناية,showing great attention to detail,The researcher kept meticulous notes.
            thrive,v.,B2,OXFORD,,,Small businesses thrive when supported by technology.
            """.trimIndent()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text(
                text = "استيراد بطاقات CSV / JSON مع إكمال Gemini التلقائي",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الأعمدة المدعومة (CSV أو JSON):",
                    fontWeight = FontWeight.Bold,
                    color = OxfordGold
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "word, pos, level, deck, arabic, definition, example",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = EmeraldPrimary
                    )
                }
                Text(
                    text = "اترك أي حقل فارغاً (مثل المعنى العربي أو النطق الصوتي IPA أو الأمثلة) وسيقوم Gemini بملئه تلقائياً على دفعات!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckType.entries.forEach { d ->
                FilterChip(
                    selected = selectedDeck == d.id,
                    onClick = { selectedDeck = d.id },
                    label = { Text(d.titleEn) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    rawInput = """
                        word,pos,level,deck,arabic,definition,example
                        microservice,n.,C1,IT,,,We split the monolith into independent microservices.
                        meticulous,adj.,C1,IELTS,دقيق للغاية,showing great attention to detail,
                        thrive,v.,B2,OXFORD,,,Small businesses thrive when supported by technology.
                    """.trimIndent()
                }
            ) {
                Text("قالب CSV تجريبي")
            }
            OutlinedButton(
                onClick = {
                    rawInput = """
                        [
                          {"word": "observability", "partOfSpeech": "n.", "cefrLevel": "C1"},
                          {"word": "pragmatic", "partOfSpeech": "adj.", "cefrLevel": "C1"}
                        ]
                    """.trimIndent()
                }
            ) {
                Text("قالب JSON تجريبي")
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("csv_json_input_field"),
                label = { Text("CSV / JSON Data") },
                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontSize = 13.sp)
            )
        }

        Button(
            onClick = { onImport(rawInput, selectedDeck) },
            enabled = !isAiLoading && rawInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_import_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF042F2E), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("جاري إكمال الحقول عبر Gemini...", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF042F2E))
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد وإكمال الحقول الناقصة بالذكاء الاصطناعي", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            }
        }
    }
}
