package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.components.TrancyWordCard
import com.example.ui.components.deckAccentColor
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    words: List<WordEntity>,
    stats: UserStatsEntity,
    decks: List<DeckEntity>,
    assetImportProgress: AssetImportProgress,
    onStartSrsReview: (String?) -> Unit,
    onOpenVocabBrowser: (String?) -> Unit,
    onOpenCsvImport: () -> Unit,
    onOpenGenerateVocab: () -> Unit,
    onStartLesson: (List<WordEntity>, Boolean) -> Unit,
    onToggleDeck: (DeckType) -> Unit,
    onInspectWord: (WordEntity) -> Unit,
    onToggleMastered: (WordEntity) -> Unit,
    onSpeakWord: (String, Boolean) -> Unit,
    onToggleUiLang: () -> Unit,
    onToggleDialect: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onStartDeckEnrichment: (String) -> Unit = {},
    posVariants: List<WordEntity> = emptyList(),
    onSelectVariant: (WordEntity) -> Unit = {},
    isEnrichingCard: Boolean = false
) {
    val isArabicUi = stats.uiLanguage == "AR"
    val now = System.currentTimeMillis()
    val activeDeckSet = stats.activeDecksCsv.split(",").filter { it.isNotBlank() }.toSet()
    val dueWords = remember(words, activeDeckSet) {
        words.filter { it.deck in activeDeckSet && it.nextReviewDate <= now && it.masteryLevel != MasteryLevel.MASTERED.name }
    }
    val weakWords = remember(words) {
        words.filter { it.isWeakWord || it.mistakeCount > 0 }
    }
    val featuredWord = remember(words) {
        words.find { it.word.equals("cattle", ignoreCase = true) } ?: words.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Status Bar: Streak, Freeze, XP, Hearts & Quick Language/Dialect Toggles
        item {
            TopGamifiedHeader(
                stats = stats,
                onToggleUiLang = onToggleUiLang,
                onToggleDialect = onToggleDialect,
                onToggleDarkMode = onToggleDarkMode
            )
        }

        // 2. Mascot Welcome + Daily Goal Progress Bar + SRS "Review Due Today" & Trancy Word List CTA
        item {
            HeroWelcomeAndSrsCard(
                stats = stats,
                dueCount = dueWords.size.coerceAtLeast(12),
                weakCount = weakWords.size,
                isArabicUi = isArabicUi,
                onStartSrs = { onStartSrsReview(null) },
                onOpenVocabList = { onOpenVocabBrowser(null) },
                onStartWeakPractice = { onStartLesson(weakWords.ifEmpty { words.take(8) }, true) }
            )
        }

        // 3. Three Color-Coded Deck Progress Cards (with 3D Book-Cover Thumbnails & Word List Table launcher)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabicUi) "حزم المفردات والكتب الثلاث" else "3 Vocabulary Decks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onOpenGenerateVocab,
                        modifier = Modifier.testTag("open_generate_vocab_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = OxfordGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabicUi) "توليد AI" else "Generate", color = OxfordGold)
                    }
                    TextButton(
                        onClick = { onOpenVocabBrowser(null) },
                        modifier = Modifier.testTag("open_vocab_browser_button")
                    ) {
                        Icon(Icons.Default.TableRows, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabicUi) "جدول الكلمات" else "Word List")
                    }
                    TextButton(
                        onClick = onOpenCsvImport,
                        modifier = Modifier.testTag("open_csv_import_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV/JSON")
                    }
                }
            }
        }

        // Background Asset Import Progress Bar
        if (assetImportProgress.isImporting) {
            item {
                Surface(
                    color = Color(0xFF13202C),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "جاري تهيئة واستيراد الكلمات الأصلية (${assetImportProgress.currentFileName})...",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${assetImportProgress.importedCount} / ${assetImportProgress.totalEstimated}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (assetImportProgress.importedCount.toFloat() / assetImportProgress.totalEstimated).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = EmeraldPrimary,
                            trackColor = Color(0xFF042F2E)
                        )
                    }
                }
            }
        }

        items(DeckType.entries) { deck ->
            val deckWords = words.filter { it.deck == deck.id }
            val masteredCount = deckWords.count { it.isMastered || it.masteryLevel == MasteryLevel.MASTERED.name }
            val totalInDb = deckWords.size.coerceAtLeast(deck.totalCatalogTarget)
            val isSelected = deck.id in activeDeckSet

            DeckProgressCard(
                deck = deck,
                learnedCount = masteredCount,
                demoCount = deckWords.size,
                isSelected = isSelected,
                isArabicUi = isArabicUi,
                onToggleSelect = { onToggleDeck(deck) },
                onOpenDeckWordList = { onOpenVocabBrowser(deck.id) },
                onReviewDeck = { onStartSrsReview(deck.id) },
                onPracticeDeckLesson = { onStartLesson(deckWords, false) },
                onEnrichDeck = { onStartDeckEnrichment(deck.id) }
            )
        }

        // 4. Trancy-Style Featured Word Card ("cattle" with Detailed AI Definition & Mastered toggle)
        if (featuredWord != null) {
            item {
                val realIndex = words.indexOfFirst { it.id == featuredWord.id } + 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabicUi) "بطاقة اليوم التفاعلية (إشعار الكلمة اليومية 🔔)" else "Featured Daily Word Card 🔔",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Surface(
                        color = OxfordGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$realIndex / ${words.size.coerceAtLeast(1)}",
                            color = OxfordGold,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TrancyWordCard(
                    word = featuredWord,
                    arabicDialect = stats.arabicDialect,
                    onSpeakUs = { onSpeakWord(featuredWord.word, false) },
                    onSpeakGb = { onSpeakWord(featuredWord.word, true) },
                    onSpeakSentence1 = { onSpeakWord(featuredWord.exampleEn1, false) },
                    onSpeakSentence2 = { onSpeakWord(featuredWord.exampleEn2, false) },
                    onOpenAiDefinition = { onInspectWord(featuredWord) },
                    onToggleMastered = { onToggleMastered(featuredWord) },
                    posVariants = posVariants,
                    onSelectVariant = onSelectVariant,
                    isEnriching = isEnrichingCard
                )
            }
        }

        // 5. Quick Browse / Search Active Deck Vocabulary Chips
        item {
            Text(
                text = if (isArabicUi) "استكشف كلمات الحزم النشطة (اضغط لعرض تعريف AI)" else "Tap Any Word for AI Definition",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(words.filter { it.deck in activeDeckSet }.take(18)) { w ->
                    val accent = deckAccentColor(w.deck)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onInspectWord(w) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = w.word,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = w.arabicTranslation.split(";").first().trim(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopGamifiedHeader(
    stats: UserStatsEntity,
    onToggleUiLang: () -> Unit,
    onToggleDialect: () -> Unit,
    onToggleDarkMode: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(
                    icon = Icons.Default.LocalFireDepartment,
                    tint = StreakFlameOrange,
                    text = "${stats.streakDays} يوم"
                )
                StatPill(
                    icon = Icons.Default.AcUnit,
                    tint = FreezeIceBlue,
                    text = if (stats.streakFreezeAvailable) "تجميد متاح" else "بدون تجميد"
                )
                StatPill(
                    icon = Icons.Default.Bolt,
                    tint = OxfordGold,
                    text = "${stats.xp} XP"
                )
                StatPill(
                    icon = Icons.Default.Favorite,
                    tint = HeartCrimson,
                    text = "${stats.hearts}/${stats.maxHearts}"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onToggleUiLang,
                    label = {
                        Text(
                            text = if (stats.uiLanguage == "AR") "الواجهة: العربية (RTL)" else "UI: English (LTR)",
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("toggle_ui_language_chip")
                )

                AssistChip(
                    onClick = onToggleDialect,
                    label = {
                        Text(
                            text = if (stats.arabicDialect == "MSA") "الشرح: فصحى" else "الشرح: مصري 🇪🇬",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("toggle_dialect_chip")
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (stats.darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "تبديل المظهر",
                        tint = OxfordGold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = text, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HeroWelcomeAndSrsCard(
    stats: UserStatsEntity,
    dueCount: Int,
    weakCount: Int,
    isArabicUi: Boolean,
    onStartSrs: () -> Unit,
    onOpenVocabList: () -> Unit,
    onStartWeakPractice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102226)),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mascot_noor),
                    contentDescription = "نور",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabicUi) "مرحباً! مستواك ${stats.cefrLevel} • المستوى ${stats.level}" else "Welcome! Level ${stats.level} (${stats.cefrLevel})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isArabicUi) {
                            "هدف اليوم: ${stats.minutesStudiedToday} / ${stats.dailyGoalMinutes} دقائق مكتملة"
                        } else {
                            "Daily Goal: ${stats.minutesStudiedToday} / ${stats.dailyGoalMinutes} min studied"
                        },
                        fontSize = 13.sp,
                        color = Color(0xFFA7F3D0)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (stats.minutesStudiedToday.toFloat() / stats.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldPrimary,
                        trackColor = Color(0xFF064E3B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartSrs,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("review_due_today_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabicUi) "بطاقات اليوم ($dueCount)" else "SRS ($dueCount)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF042F2E)
                    )
                }

                OutlinedButton(
                    onClick = onOpenVocabList,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_vocab_table_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OxfordGold),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = OxfordGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabicUi) "جدول المفردات" else "Word List",
                        color = OxfordGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onStartWeakPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("practice_weak_words_home_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabicUi) "تدريب ذكي على الكلمات الضعيفة ($weakCount كلمة)" else "Practice Weak Words ($weakCount)",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DeckProgressCard(
    deck: DeckType,
    learnedCount: Int,
    demoCount: Int,
    isSelected: Boolean,
    isArabicUi: Boolean,
    onToggleSelect: () -> Unit,
    onOpenDeckWordList: () -> Unit,
    onReviewDeck: () -> Unit,
    onPracticeDeckLesson: () -> Unit,
    onEnrichDeck: () -> Unit = {}
) {
    val accent = deckAccentColor(deck.id)
    val bookGradient = when (deck) {
        DeckType.IELTS -> listOf(Color(0xFF155E63), Color(0xFF0A363A))
        DeckType.IT -> listOf(Color(0xFF5B4582), Color(0xFF362752))
        DeckType.OXFORD -> listOf(Color(0xFF7C3228), Color(0xFF4B1B15))
        DeckType.OPAL -> listOf(Color(0xFF1E3A8A), Color(0xFF172554))
        DeckType.PHRASES -> listOf(Color(0xFF065F46), Color(0xFF064E3B))
        DeckType.EVERYDAY -> listOf(Color(0xFFB45309), Color(0xFF78350F))
        DeckType.IDIOMS -> listOf(Color(0xFF831843), Color(0xFF500724))
    }
    val progress = (learnedCount.toFloat() / deck.totalCatalogTarget.toFloat().coerceAtLeast(1f)).coerceIn(0.04f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deck_card_${deck.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // 3D Trancy Book Cover Spine Icon (matching Screenshot 2)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                                .background(Brush.verticalGradient(bookGradient))
                                .clickable { onOpenDeckWordList() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            Text(
                                text = deck.titleEn.replace(" ", "\n"),
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                modifier = Modifier.padding(start = 7.dp, top = 7.dp, end = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isArabicUi) "${deck.titleAr} (${deck.titleEn})" else deck.titleEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "${deck.titleEn}: $learnedCount / ${deck.totalCatalogTarget} learned",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = accent)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDeckWordList,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (isArabicUi) "القائمة (${deck.totalCatalogTarget})" else "Word List",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onReviewDeck,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (isArabicUi) "بطاقات SRS" else "Flashcards",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onPracticeDeckLesson,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (isArabicUi) "درس تفاعلي" else "Lesson",
                        color = Color(0xFF0F141C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onEnrichDeck,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Enrich deck with AI",
                        tint = OxfordGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
