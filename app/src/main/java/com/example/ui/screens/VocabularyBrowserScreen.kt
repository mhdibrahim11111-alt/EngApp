package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

/**
 * Replicates the exact Trancy Vocabulary Explorer & Book-Cover Deck Manager from Screenshots 2 & 3:
 * - Left / Top 3D Book-Cover Deck Cards (`IELTS Core 3575`, `IT Industry 1700`, `Oxford 5000 5836`)
 * - Top Filter Pills (`All`, `Mastered <count>`, `Favorites <count>`) with gold outline on active pill
 * - Top Right Toolbar:
 *   - `A+` Flashcard Study Carousel launcher (switches to Screenshot 1 Flashcard view)
 *   - `[|=]` Book-Cover Sidebar toggle
 *   - `👁️` Eye Mask toggle (hides/blurs Arabic translations for self-testing; tap to reveal)
 *   - `By date ↓` / `A → Z` / `CEFR Level` Sort selector
 * - Rich Word List Table Rows (`able`, `abandon`, `descriptor`, `depict`, `intrinsic`, `volatile`, `professed`, `discrepancy`, `compliance`, `supplement`, `jumble`, `cattle`, etc.)
 *   with `[US] 🔊 /ipa/`, inline multi-POS Arabic definitions (`v.`, `n.`, `adj.`, `web.`), and right action icons:
 *   - Gold Mastered checkbox (`☑` / `☐`)
 *   - Red Favorite heart (`❤️`)
 *   - Detailed AI Definition trigger (`≣🔍`)
 */
@Composable
fun VocabularyBrowserScreen(
    words: List<WordEntity>,
    initialDeckFilter: String? = null,
    onInspectWord: (WordEntity) -> Unit,
    onToggleMastered: (WordEntity) -> Unit,
    onToggleFavorite: (WordEntity) -> Unit,
    onSpeakWord: (String) -> Unit,
    onLaunchFlashcardMode: (String?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedDeckId by remember { mutableStateOf(initialDeckFilter) } // null = All Decks
    var filterTab by remember { mutableStateOf(VocabFilterTab.ALL) }
    var sortOrder by remember { mutableStateOf(VocabSortOrder.BY_DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var maskMeanings by remember { mutableStateOf(false) }
    var showBookSidebar by remember { mutableStateOf(false) }
    var revealedWordIds by remember { mutableStateOf(setOf<Int>()) }
    var highlightedWordId by remember { mutableStateOf<Int?>(null) }

    val deckFilteredWords = remember(words, selectedDeckId) {
        if (selectedDeckId == null) words
        else words.filter { it.deck == selectedDeckId }
    }

    val masteredCount = remember(deckFilteredWords) {
        deckFilteredWords.count { it.masteryLevel == MasteryLevel.MASTERED.name }
    }
    val favoritesCount = remember(deckFilteredWords) {
        deckFilteredWords.count { it.isFavorite }
    }

    val displayedWords = remember(deckFilteredWords, filterTab, sortOrder) {
        val tabFiltered = when (filterTab) {
            VocabFilterTab.ALL -> deckFilteredWords
            VocabFilterTab.MASTERED -> deckFilteredWords.filter { it.masteryLevel == MasteryLevel.MASTERED.name }
            VocabFilterTab.FAVORITES -> deckFilteredWords.filter { it.isFavorite }
        }
        when (sortOrder) {
            VocabSortOrder.BY_DATE_DESC -> tabFiltered.sortedByDescending { it.createdAt }
            VocabSortOrder.ALPHABETICAL -> tabFiltered.sortedBy { it.word.lowercase() }
            VocabSortOrder.CEFR_LEVEL -> tabFiltered.sortedByDescending { it.cefrLevel }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0D12)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Filter & Action Bar (Screenshot 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button + Filter Pills (All | Mastered 3 | Favorites)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1A1E29),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        TrancyFilterPill(
                            text = "All",
                            selected = filterTab == VocabFilterTab.ALL,
                            onClick = { filterTab = VocabFilterTab.ALL }
                        )

                        TrancyFilterPill(
                            text = "Mastered $masteredCount",
                            selected = filterTab == VocabFilterTab.MASTERED,
                            onClick = { filterTab = VocabFilterTab.MASTERED }
                        )

                        TrancyFilterPill(
                            text = "♥ $favoritesCount",
                            selected = filterTab == VocabFilterTab.FAVORITES,
                            onClick = { filterTab = VocabFilterTab.FAVORITES }
                        )
                    }

                    // Right Toolbar: [A+ Flashcards] [|= Sidebar] [👁️ Eye Mask] [By date ↓]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // A+ Flashcard Study Mode Button
                        TrancyCircleToolButton(
                            icon = Icons.Default.FontDownload,
                            tooltip = "Flashcard Study Mode",
                            isActive = false,
                            onClick = { onLaunchFlashcardMode(selectedDeckId) }
                        )

                        // Book-Cover Sidebar Toggle
                        TrancyCircleToolButton(
                            icon = Icons.Default.ViewSidebar,
                            tooltip = "Toggle Deck Book Covers",
                            isActive = showBookSidebar,
                            onClick = { showBookSidebar = !showBookSidebar }
                        )

                        // Eye Mask Toggle (Hide/Show Arabic Meanings)
                        TrancyCircleToolButton(
                            icon = if (maskMeanings) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            tooltip = "Mask Arabic Meanings",
                            isActive = maskMeanings,
                            onClick = {
                                maskMeanings = !maskMeanings
                                revealedWordIds = emptySet()
                            }
                        )

                        // Sort Dropdown Pill ("By date ↓")
                        Box {
                            Surface(
                                color = Color(0xFF1A1E29),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF2B3242)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showSortMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sortOrder.labelEn,
                                        color = Color(0xFFE2E8F0),
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Sort",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor = Color(0xFF1A1E29)
                            ) {
                                VocabSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${order.labelEn} (${order.labelAr})",
                                                color = if (sortOrder == order) OxfordGold else Color.White,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Book-Cover Deck Selector Row (Always accessible at top when sidebar is collapsed)
                AnimatedVisibility(visible = !showBookSidebar) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            BookCoverDeckItem(
                                title = "All Decks",
                                countText = "11111",
                                coverColors = listOf(Color(0xFF065F46), Color(0xFF042F2E)),
                                selected = selectedDeckId == null,
                                onClick = { selectedDeckId = null }
                            )
                        }
                        items(DeckType.entries) { deck ->
                            val colors = when (deck) {
                                DeckType.IELTS -> listOf(Color(0xFF155E63), Color(0xFF0A363A))
                                DeckType.IT -> listOf(Color(0xFF5B4582), Color(0xFF362752))
                                DeckType.OXFORD -> listOf(Color(0xFF7C3228), Color(0xFF4B1B15))
                            }
                            BookCoverDeckItem(
                                title = deck.titleEn,
                                countText = "${deck.totalCatalogTarget}",
                                coverColors = colors,
                                selected = selectedDeckId == deck.id,
                                onClick = {
                                    selectedDeckId = if (selectedDeckId == deck.id) null else deck.id
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1B202C))

                // 3. Main Body: Optional Left Book Sidebar (Screenshot 2) + Trancy Word Table List
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showBookSidebar) {
                        Column(
                            modifier = Modifier
                                .width(155.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF090B10))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BookCoverSidebarItem(
                                title = "IELTS Core",
                                countText = "3575",
                                coverColors = listOf(Color(0xFF155E63), Color(0xFF0A363A)),
                                selected = selectedDeckId == "IELTS",
                                onClick = { selectedDeckId = if (selectedDeckId == "IELTS") null else "IELTS" }
                            )
                            BookCoverSidebarItem(
                                title = "IT Industry",
                                countText = "1700",
                                coverColors = listOf(Color(0xFF5B4582), Color(0xFF362752)),
                                selected = selectedDeckId == "IT",
                                onClick = { selectedDeckId = if (selectedDeckId == "IT") null else "IT" }
                            )
                            BookCoverSidebarItem(
                                title = "Oxford 5000",
                                countText = "5836",
                                coverColors = listOf(Color(0xFF7C3228), Color(0xFF4B1B15)),
                                selected = selectedDeckId == "OXFORD",
                                onClick = { selectedDeckId = if (selectedDeckId == "OXFORD") null else "OXFORD" }
                            )
                        }
                        VerticalDivider(color = Color(0xFF1B202C))
                    }

                    // Trancy Word List Table
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("trancy_vocab_table")
                    ) {
                        items(displayedWords, key = { it.id }) { word ->
                            val isHighlighted = highlightedWordId == word.id
                            val isMeaningsHidden = maskMeanings && (word.id !in revealedWordIds)
                            TrancyWordListRow(
                                word = word,
                                isHighlighted = isHighlighted,
                                isMeaningsMasked = isMeaningsHidden,
                                onRowClick = {
                                    highlightedWordId = word.id
                                    if (isMeaningsHidden) {
                                        revealedWordIds = revealedWordIds + word.id
                                    } else {
                                        onSpeakWord(word.word)
                                    }
                                },
                                onSpeak = { onSpeakWord(word.word) },
                                onToggleMastered = { onToggleMastered(word) },
                                onToggleFavorite = { onToggleFavorite(word) },
                                onOpenAiDefinition = {
                                    highlightedWordId = word.id
                                    onInspectWord(word)
                                }
                            )
                            HorizontalDivider(color = Color(0xFF171B24), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrancyFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF171B24),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) OxfordGold else Color(0xFF282F3E)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontFamily = InterFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TrancyCircleToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) OxfordGold.copy(alpha = 0.2f) else Color(0xFF1A1E29),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isActive) OxfordGold else Color(0xFF2B3242)),
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (isActive) OxfordGold else Color(0xFFCBD5E1),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * 3D Book-Cover Deck Item (Horizontal Shelf Style)
 */
@Composable
private fun BookCoverDeckItem(
    title: String,
    countText: String,
    coverColors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF1B2230) else Color(0xFF121620),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) OxfordGold else Color(0xFF232A3A)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature 3D Book Cover with Spine
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Brush.verticalGradient(coverColors))
            ) {
                // Left spine groove
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
                Text(
                    text = title.replace(" ", "\n"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    modifier = Modifier.padding(start = 7.dp, top = 6.dp, end = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = countText,
                    color = Color(0xFF94A3B8),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 3D Book-Cover Sidebar Item (Exact Left Column from Screenshot 2)
 */
@Composable
private fun BookCoverSidebarItem(
    title: String,
    countText: String,
    coverColors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF181D28) else Color.Transparent)
            .clickable { onClick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                .background(Brush.verticalGradient(coverColors))
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Text(
                text = title.replace(" ", "\n"),
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = if (selected) OxfordGold else Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = countText,
                color = Color(0xFF64748B),
                fontFamily = InterFontFamily,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Exact Trancy Word List Table Row (Screenshots 2 & 3):
 * [English Word]   [US] 🔊 /ipa/   [v. تَرَكَ ; هَجَرَ  n. تخلٍّ ; هجران  web. تخلّى عن]   [☑] [❤️] [≣🔍]
 */
@Composable
private fun TrancyWordListRow(
    word: WordEntity,
    isHighlighted: Boolean,
    isMeaningsMasked: Boolean,
    onRowClick: () -> Unit,
    onSpeak: () -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAiDefinition: () -> Unit
) {
    val isMastered = word.masteryLevel == MasteryLevel.MASTERED.name

    val formattedDefinitions = remember(word) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                append("${word.partOfSpeech}  ")
            }
            withStyle(SpanStyle(color = Color(0xFFE2E8F0), fontSize = 14.sp)) {
                append(word.arabicTranslation)
            }
            if (word.secondaryPos.isNotBlank() && word.secondaryArabic.isNotBlank()) {
                withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                    append("   ${word.secondaryPos}  ")
                }
                withStyle(SpanStyle(color = Color(0xFFCBD5E1), fontSize = 14.sp)) {
                    append(word.secondaryArabic)
                }
            }
            if (word.webArabicSynonyms.isNotBlank()) {
                withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                    append("   web.  ")
                }
                withStyle(SpanStyle(color = Color(0xFF94A3B8), fontSize = 14.sp)) {
                    append(word.webArabicSynonyms)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlighted) Color(0xFF161A23) else Color.Transparent)
            .clickable { onRowClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left: English Word + US Phonetic Audio Row
        Column(
            modifier = Modifier.width(138.dp)
        ) {
            Text(
                text = word.word,
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSpeak() }
            ) {
                Surface(
                    color = Color(0xFF1F2430),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "US",
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speak ${word.word}",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = word.ipaUs,
                    color = Color(0xFF64748B),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Middle: Multi-POS Arabic Definitions (or Masked Blur Pill when Eye Mask is active)
        Box(modifier = Modifier.weight(1f)) {
            if (isMeaningsMasked) {
                Surface(
                    color = Color(0xFF1C2230),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "••• اضغط لإظهار المعنى العربي •••",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    text = formattedDefinitions,
                    fontFamily = CairoFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 3. Right Action Icons: [☑ Mastered] [❤️ Favorite] [≣🔍 AI Definition]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onToggleMastered,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isMastered) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Toggle Mastered",
                    tint = if (isMastered) OxfordGold else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (word.isFavorite) Color(0xFFEF4444) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenAiDefinition,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("row_ai_def_${word.word}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                    contentDescription = "AI Definition",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
