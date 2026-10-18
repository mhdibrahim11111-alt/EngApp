package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun VocabularyBrowserScreen(
    words: List<WordEntity>,
    pagedWords: LazyPagingItems<WordEntity>?,
    initialDeckFilter: String? = null,
    onInspectWord: (WordEntity) -> Unit,
    onToggleMastered: (WordEntity) -> Unit,
    onToggleFavorite: (WordEntity) -> Unit,
    onSpeakWord: (WordEntity) -> Unit,
    onLaunchFlashcardMode: (String?) -> Unit,
    onExportDeckCsv: (Uri, String) -> Unit = { _, _ -> },
    onExportDeckJson: (Uri, String) -> Unit = { _, _ -> },
    onImportBackup: (Uri, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedDeckId by remember { mutableStateOf(initialDeckFilter) }
    var filterTab by remember { mutableStateOf(VocabFilterTab.ALL) }
    var sortOrder by remember { mutableStateOf(VocabSortOrder.BY_DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var maskMeanings by remember { mutableStateOf(false) }
    var showBookSidebar by remember { mutableStateOf(false) }
    var revealedWordIds by remember { mutableStateOf(setOf<Int>()) }
    var highlightedWordId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val activeDeck = selectedDeckId ?: "oxford5000"

    // SAF Launchers for Export & Backup
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportDeckCsv(uri, activeDeck)
    }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportDeckJson(uri, activeDeck)
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri, activeDeck)
    }

    val deckFilteredWords = remember(words, selectedDeckId) {
        if (selectedDeckId == null) words
        else words.filter { it.deck == selectedDeckId }
    }

    val masteredCount = remember(deckFilteredWords) {
        deckFilteredWords.count { it.isMastered || it.masteryLevel == MasteryLevel.MASTERED.name }
    }
    val favoritesCount = remember(deckFilteredWords) {
        deckFilteredWords.count { it.isFavorite }
    }

    val displayedWords = remember(deckFilteredWords, filterTab, sortOrder, searchQuery) {
        val searched = if (searchQuery.isBlank()) {
            deckFilteredWords
        } else {
            val q = searchQuery.trim().lowercase()
            deckFilteredWords.filter {
                it.text.lowercase().contains(q) ||
                it.arabic.contains(q) ||
                it.definitionEn.lowercase().contains(q)
            }
        }
        val tabFiltered = when (filterTab) {
            VocabFilterTab.ALL -> searched
            VocabFilterTab.MASTERED -> searched.filter { it.isMastered || it.masteryLevel == MasteryLevel.MASTERED.name }
            VocabFilterTab.FAVORITES -> searched.filter { it.isFavorite }
        }
        when (sortOrder) {
            VocabSortOrder.BY_DATE_DESC -> tabFiltered.sortedByDescending { it.createdAt }
            VocabSortOrder.ALPHABETICAL -> tabFiltered.sortedBy { it.text.lowercase() }
            VocabSortOrder.CEFR_LEVEL -> tabFiltered.sortedByDescending { it.level }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0D12)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Filter & Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button + Filter Pills
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
                            text = "All (${deckFilteredWords.size})",
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

                    // Right Toolbar: [A+ Flashcards] [|= Sidebar] [👁️ Eye Mask] [💾 Export/Backup] [By date ↓]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Flashcard Mode
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

                        // Eye Mask Toggle
                        TrancyCircleToolButton(
                            icon = if (maskMeanings) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            tooltip = "Mask Arabic Meanings",
                            isActive = maskMeanings,
                            onClick = {
                                maskMeanings = !maskMeanings
                                revealedWordIds = emptySet()
                            }
                        )

                        // Export / Backup Menu Button
                        Box {
                            TrancyCircleToolButton(
                                icon = Icons.Default.SaveAlt,
                                tooltip = "Export / Backup",
                                isActive = showExportMenu,
                                onClick = { showExportMenu = true }
                            )

                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                                containerColor = Color(0xFF1A1E29)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تصدير الحزمة CSV", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = OxfordGold) },
                                    onClick = {
                                        showExportMenu = false
                                        csvExportLauncher.launch("${activeDeck}_export.csv")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("نسخة احتياطية JSON", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, tint = EmeraldPrimary) },
                                    onClick = {
                                        showExportMenu = false
                                        jsonBackupLauncher.launch("${activeDeck}_backup.json")
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF282F3E))
                                DropdownMenuItem(
                                    text = { Text("استعادة من نسخة احتياطية", color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = FreezeIceBlue) },
                                    onClick = {
                                        showExportMenu = false
                                        backupImportLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*"))
                                    }
                                )
                            }
                        }

                        // Sort Menu Dropdown
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sortOrder.labelEn,
                                        color = Color(0xFFE2E8F0),
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Sort",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
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

                // Search Bar with FTS Filter
                Surface(
                    color = Color(0xFF121620),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        placeholder = { Text("Search word, Arabic meaning, or definition...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OxfordGold, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OxfordGold,
                            unfocusedBorderColor = Color(0xFF262D3D),
                            focusedContainerColor = Color(0xFF141822),
                            unfocusedContainerColor = Color(0xFF141822)
                        ),
                        singleLine = true
                    )
                }

                // 2. Book-Cover Deck Selector Row
                AnimatedVisibility(visible = !showBookSidebar) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            BookCoverDeckItem(
                                title = "All Decks",
                                countText = "${words.size}",
                                coverColors = listOf(Color(0xFF065F46), Color(0xFF042F2E)),
                                selected = selectedDeckId == null,
                                onClick = { selectedDeckId = null }
                            )
                        }
                        items(DeckType.entries) { deck ->
                            val colors = when (deck) {
                                DeckType.OXFORD -> listOf(Color(0xFF7C3228), Color(0xFF4B1B15))
                                DeckType.OPAL -> listOf(Color(0xFF1E3A8A), Color(0xFF172554))
                                DeckType.PHRASES -> listOf(Color(0xFF065F46), Color(0xFF064E3B))
                                DeckType.EVERYDAY -> listOf(Color(0xFFB45309), Color(0xFF78350F))
                                DeckType.IDIOMS -> listOf(Color(0xFF831843), Color(0xFF500724))
                                DeckType.IELTS -> listOf(Color(0xFF155E63), Color(0xFF0A363A))
                                DeckType.IT -> listOf(Color(0xFF5B4582), Color(0xFF362752))
                            }
                            val count = words.count { it.deck == deck.id }.coerceAtLeast(deck.totalCatalogTarget)
                            BookCoverDeckItem(
                                title = deck.titleEn,
                                countText = "$count",
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

                // 3. Main Body: Optional Left Book Sidebar + Paging/List Table
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showBookSidebar) {
                        Column(
                            modifier = Modifier
                                .width(155.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF090B10))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DeckType.entries.forEach { deck ->
                                val colors = when (deck) {
                                    DeckType.OXFORD -> listOf(Color(0xFF7C3228), Color(0xFF4B1B15))
                                    DeckType.OPAL -> listOf(Color(0xFF1E3A8A), Color(0xFF172554))
                                    DeckType.PHRASES -> listOf(Color(0xFF065F46), Color(0xFF064E3B))
                                    DeckType.EVERYDAY -> listOf(Color(0xFFB45309), Color(0xFF78350F))
                                    DeckType.IDIOMS -> listOf(Color(0xFF831843), Color(0xFF500724))
                                    DeckType.IELTS -> listOf(Color(0xFF155E63), Color(0xFF0A363A))
                                    DeckType.IT -> listOf(Color(0xFF5B4582), Color(0xFF362752))
                                }
                                val count = words.count { it.deck == deck.id }.coerceAtLeast(deck.totalCatalogTarget)
                                BookCoverSidebarItem(
                                    title = deck.titleEn,
                                    countText = "$count",
                                    coverColors = colors,
                                    selected = selectedDeckId == deck.id,
                                    onClick = { selectedDeckId = if (selectedDeckId == deck.id) null else deck.id }
                                )
                            }
                        }
                        VerticalDivider(color = Color(0xFF1B202C))
                    }

                    // Paging 3 Lazy List Table
                    if (searchQuery.isBlank() && filterTab == VocabFilterTab.ALL && pagedWords != null) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("trancy_vocab_table")
                        ) {
                            items(
                                count = pagedWords.itemCount,
                                key = pagedWords.itemKey { it.id }
                            ) { index ->
                                val word = pagedWords[index]
                                if (word != null) {
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
                                                onSpeakWord(word)
                                            }
                                        },
                                        onSpeak = { onSpeakWord(word) },
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
                    } else {
                        // Standard filtered/sorted list
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("trancy_vocab_table")
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
                                            onSpeakWord(word)
                                        }
                                    },
                                    onSpeak = { onSpeakWord(word) },
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
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Brush.verticalGradient(coverColors))
            ) {
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
    val isMastered = word.isMastered || word.masteryLevel == MasteryLevel.MASTERED.name

    val formattedDefinitions = remember(word) {
        buildAnnotatedString {
            if (word.pos.isNotBlank()) {
                withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                    append("${word.pos}  ")
                }
            }
            withStyle(SpanStyle(color = Color(0xFFE2E8F0), fontSize = 14.sp)) {
                append(word.arabic.ifBlank { word.definitionEn })
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
        // Left: Word + US/Audio
        Column(modifier = Modifier.width(138.dp)) {
            Text(
                text = word.text,
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
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onSpeak() }
            ) {
                Surface(
                    color = Color(0xFF1F2430),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = if (word.audioUrl.isNotBlank()) "AUDIO" else "US",
                        color = if (word.audioUrl.isNotBlank()) OxfordGold else Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speak ${word.text}",
                    tint = if (word.audioUrl.isNotBlank()) OxfordGold else Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = word.ipa.ifBlank { "/${word.text}/" },
                    color = Color(0xFF64748B),
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Middle: Arabic/Definition
        Box(modifier = Modifier.weight(1f)) {
            if (isMeaningsMasked) {
                Surface(
                    color = Color(0xFF1C2230),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "••• اضغط لإظهار المعنى •••",
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

        // Right Action Icons: Mastered, Favorite, AI Definition
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
                modifier = Modifier.size(32.dp).testTag("row_ai_def_${word.text}")
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
