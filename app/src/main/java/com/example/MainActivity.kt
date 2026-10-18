package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.ui.*
import com.example.ui.components.AiDefinitionBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LinguaBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinguaBridgeApp()
        }
    }
}

@Composable
fun LinguaBridgeApp(vm: LinguaBridgeViewModel = viewModel()) {
    val words by vm.allWords.collectAsStateWithLifecycle()
    val stats by vm.userStats.collectAsStateWithLifecycle()
    val transcripts by vm.transcripts.collectAsStateWithLifecycle()
    val pronunciationHistory by vm.pronunciationHistory.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val assetImportProgress by vm.assetImportProgress.collectAsStateWithLifecycle()

    val currentTab by vm.currentTab.collectAsStateWithLifecycle()
    val subScreen by vm.subScreen.collectAsStateWithLifecycle()
    val learnModeTab by vm.learnModeTab.collectAsStateWithLifecycle()
    val speakModeTab by vm.speakModeTab.collectAsStateWithLifecycle()
    val inspectedWord by vm.inspectedWord.collectAsStateWithLifecycle()
    val wordVariants by vm.wordVariants.collectAsStateWithLifecycle()
    val isEnrichingCard by vm.isEnrichingCard.collectAsStateWithLifecycle()
    val bannerMessage by vm.bannerMessage.collectAsStateWithLifecycle()
    val isAiLoading by vm.isAiLoading.collectAsStateWithLifecycle()

    // SRS & Vocab Browser State
    val srsQueue by vm.srsQueue.collectAsStateWithLifecycle()
    val srsIndex by vm.srsIndex.collectAsStateWithLifecycle()
    val vocabBrowserDeckFilter by vm.vocabBrowserDeckFilter.collectAsStateWithLifecycle()
    val pagedEntries = vm.pagedEntries.collectAsLazyPagingItems()

    // Bulk Import Preview & Progress
    val importPreview by vm.importPreview.collectAsStateWithLifecycle()
    val importProgress by vm.importProgress.collectAsStateWithLifecycle()

    // Generate Vocabulary Progress
    val generateProgress by vm.generateProgress.collectAsStateWithLifecycle()

    // Lesson State
    val activeExercises by vm.activeExercises.collectAsStateWithLifecycle()
    val exerciseIndex by vm.exerciseIndex.collectAsStateWithLifecycle()
    val lessonCorrectCount by vm.lessonCorrectCount.collectAsStateWithLifecycle()
    val lessonCompleted by vm.lessonCompleted.collectAsStateWithLifecycle()

    // Watch & Learn State
    val selectedTranscript by vm.selectedTranscript.collectAsStateWithLifecycle()
    val activeSubtitleIndex by vm.activeSubtitleIndex.collectAsStateWithLifecycle()
    val isPlayingTranscript by vm.isPlayingTranscript.collectAsStateWithLifecycle()
    val loopSentence by vm.loopCurrentSentence.collectAsStateWithLifecycle()
    val autoPause by vm.autoPauseAfterLine.collectAsStateWithLifecycle()
    val playbackSpeed by vm.playbackSpeed.collectAsStateWithLifecycle()
    val showEnglishSub by vm.showEnglishSub.collectAsStateWithLifecycle()
    val showArabicSub by vm.showArabicSub.collectAsStateWithLifecycle()
    val subtitleFontSizeSp by vm.subtitleFontSizeSp.collectAsStateWithLifecycle()

    // Speak & Tutor State
    val selectedPhonemeCategory by vm.selectedPhonemeCategory.collectAsStateWithLifecycle()
    val latestPronunciationEval by vm.latestPronunciationEval.collectAsStateWithLifecycle()
    val selectedScenario by vm.selectedScenario.collectAsStateWithLifecycle()
    val chatMessages by vm.chatMessages.collectAsStateWithLifecycle()
    val ieltsAssessment by vm.ieltsAssessment.collectAsStateWithLifecycle()

    var showOnboardingModal by remember { mutableStateOf(false) }

    val isRtl = stats.uiLanguage == "AR"

    LinguaBridgeTheme(
        darkTheme = stats.darkMode,
        isRtl = isRtl
    ) {
        if (showOnboardingModal) {
            OnboardingFlowScreen(
                onComplete = { goal, cefr, mins, dialect ->
                    vm.completeOnboarding(goal, cefr, mins, dialect)
                    showOnboardingModal = false
                },
                onSkipWithDefaults = {
                    showOnboardingModal = false
                }
            )
            return@LinguaBridgeTheme
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (subScreen == SubScreen.NONE) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { vm.selectTab(tab) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                icon = {
                                    val icon = when (tab) {
                                        MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                        MainTab.LEARN -> if (selected) Icons.Filled.School else Icons.Outlined.School
                                        MainTab.WATCH -> if (selected) Icons.Filled.OndemandVideo else Icons.Outlined.OndemandVideo
                                        MainTab.SPEAK -> if (selected) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver
                                        MainTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                    }
                                    Icon(imageVector = icon, contentDescription = tab.labelAr)
                                },
                                label = {
                                    Text(
                                        text = if (isRtl) tab.labelAr else tab.labelEn,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF042F2E),
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Dismissible Status Banner
                AnimatedVisibility(visible = bannerMessage != null) {
                    bannerMessage?.let { msg ->
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = msg, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { vm.clearBanner() }) {
                                    Text("إغلاق")
                                }
                            }
                        }
                    }
                }

                // Active SubScreen or Main Tab Content
                when (subScreen) {
                    SubScreen.SRS_REVIEW -> {
                        val currentDeckId = srsQueue.getOrNull(srsIndex)?.deck
                        val totalForDeck = words.count { it.deck == currentDeckId }
                        SrsReviewScreen(
                            queue = srsQueue,
                            currentIndex = srsIndex,
                            arabicDialect = stats.arabicDialect,
                            totalDeckCount = totalForDeck,
                            onNavigateCard = { vm.navigateSrsCard(it) },
                            onGrade = { vm.submitSrsGrade(it) },
                            onInspectWord = { vm.inspectWord(it) },
                            onToggleMastered = { vm.toggleMasteredOnWord(it) },
                            onSpeakWord = { w, gb -> vm.playWordAudio(w, isBritish = gb) },
                            onSwitchToListView = { vm.openVocabBrowser(null) },
                            posVariants = wordVariants,
                            onSelectVariant = { vm.selectVariant(it) },
                            isEnriching = isEnrichingCard,
                            onBack = { vm.closeSubScreen() }
                        )
                    }

                    SubScreen.VOCAB_BROWSER -> {
                        VocabularyBrowserScreen(
                            words = words,
                            pagedWords = pagedEntries,
                            initialDeckFilter = vocabBrowserDeckFilter,
                            onInspectWord = { vm.inspectWord(it) },
                            onToggleMastered = { vm.toggleMasteredOnWord(it) },
                            onToggleFavorite = { vm.toggleFavoriteOnWord(it) },
                            onSpeakWord = { vm.playWordAudio(it) },
                            onLaunchFlashcardMode = { deck -> vm.startSrsReview(deck) },
                            onExportDeckCsv = { uri, deck -> vm.exportDeckCsv(uri, deck) },
                            onExportDeckJson = { uri, deck -> vm.exportDeckJson(uri, deck) },
                            onImportBackup = { uri, deck -> vm.importBackup(uri, deck) },
                            onBack = { vm.closeSubScreen() }
                        )
                    }

                    SubScreen.CSV_IMPORT -> {
                        CsvJsonImportScreen(
                            preview = importPreview,
                            importProgress = importProgress,
                            onPreview = { raw, deck -> vm.previewImportData(raw, deck) },
                            onConfirmImport = { targetDeck -> vm.executeImport(targetDeck) },
                            onBack = { vm.closeSubScreen() }
                        )
                    }

                    SubScreen.GENERATE_VOCAB -> {
                        GenerateVocabularyScreen(
                            progress = generateProgress,
                            onStartGenerate = { topic, lvl, cnt, dck ->
                                vm.startGenerateVocabulary(topic, lvl, cnt, dck)
                            },
                            onCancel = { vm.cancelGenerateVocabulary() },
                            onBack = { vm.closeSubScreen() }
                        )
                    }

                    SubScreen.LESSON_SESSION,
                    SubScreen.WEAK_WORDS_PRACTICE -> {
                        LessonSessionScreen(
                            exercises = activeExercises,
                            currentIndex = exerciseIndex,
                            correctCount = lessonCorrectCount,
                            isCompleted = lessonCompleted,
                            hearts = stats.hearts,
                            arabicDialect = stats.arabicDialect,
                            onSubmitAnswer = { vm.submitExerciseResult(it) },
                            onSpeakWord = { vm.speakEnglish(it) },
                            onRefillHearts = { vm.refillHearts() },
                            onClose = { vm.closeSubScreen() }
                        )
                    }

                    SubScreen.NONE -> {
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    words = words,
                                    stats = stats,
                                    decks = decks,
                                    assetImportProgress = assetImportProgress,
                                    onStartSrsReview = { deck -> vm.startSrsReview(deck) },
                                    onOpenVocabBrowser = { deck -> vm.openVocabBrowser(deck) },
                                    onOpenCsvImport = { vm.openSubScreen(SubScreen.CSV_IMPORT) },
                                    onOpenGenerateVocab = { vm.openSubScreen(SubScreen.GENERATE_VOCAB) },
                                    onStartLesson = { subset, weak -> vm.startLesson(subset, weak) },
                                    onToggleDeck = { vm.toggleDeckSelection(it) },
                                    onInspectWord = { vm.inspectWord(it) },
                                    onToggleMastered = { vm.toggleMasteredOnWord(it) },
                                    onSpeakWord = { txt, gb -> vm.speakEnglish(txt, isBritish = gb) },
                                    onToggleUiLang = { vm.toggleUiLanguage() },
                                    onToggleDialect = { vm.toggleArabicDialect() },
                                    onToggleDarkMode = { vm.toggleDarkMode() },
                                    onStartDeckEnrichment = { deckId -> vm.startDeckEnrichment(deckId) },
                                    posVariants = wordVariants,
                                    onSelectVariant = { vm.selectVariant(it) },
                                    isEnrichingCard = isEnrichingCard
                                )
                            }

                            MainTab.LEARN -> {
                                LearnScreen(
                                    words = words,
                                    stats = stats,
                                    activeMode = learnModeTab,
                                    ieltsAssessment = ieltsAssessment,
                                    isAiLoading = isAiLoading,
                                    onSelectMode = { vm.setLearnModeTab(it) },
                                    onStartLesson = { vm.startLesson(it) },
                                    onEvaluateIelts = { topic, resp -> vm.evaluateIeltsWritingOrSpeaking(topic, resp) },
                                    onTapWordLookup = { vm.inspectWordByText(it) },
                                    onSpeakText = { vm.speakEnglish(it) }
                                )
                            }

                            MainTab.WATCH -> {
                                WatchScreen(
                                    transcripts = transcripts,
                                    selectedTranscript = selectedTranscript,
                                    allWords = words,
                                    activeSubtitleIndex = activeSubtitleIndex,
                                    isPlaying = isPlayingTranscript,
                                    loopSentence = loopSentence,
                                    autoPause = autoPause,
                                    playbackSpeed = playbackSpeed,
                                    showEnglish = showEnglishSub,
                                    showArabic = showArabicSub,
                                    fontSizeSp = subtitleFontSizeSp,
                                    isAiLoading = isAiLoading,
                                    onSelectTranscript = { vm.selectTranscript(it) },
                                    onTogglePlay = { vm.toggleTranscriptPlayback(it) },
                                    onJumpLine = { d, total -> vm.jumpSubtitleLine(d, total) },
                                    onSelectLine = { idx, txt -> vm.setSubtitleLineIndex(idx, txt) },
                                    onToggleLoop = { vm.toggleLoopSentence() },
                                    onToggleAutoPause = { vm.toggleAutoPause() },
                                    onToggleSpeed = { vm.togglePlaybackSpeed() },
                                    onToggleEnglish = { vm.toggleEnglishSubtitles() },
                                    onToggleArabic = { vm.toggleArabicSubtitles() },
                                    onAdjustFontSize = { vm.adjustSubtitleFontSize(it) },
                                    onTapWord = { vm.inspectWordByText(it) },
                                    onCreateCustomTranscript = { title, url, text ->
                                        vm.createCustomTranscript(title, url, text)
                                    }
                                )
                            }

                            MainTab.SPEAK -> {
                                SpeakScreen(
                                    stats = stats,
                                    activeTab = speakModeTab,
                                    selectedCategory = selectedPhonemeCategory,
                                    latestEvaluation = latestPronunciationEval,
                                    selectedScenario = selectedScenario,
                                    chatMessages = chatMessages,
                                    isAiLoading = isAiLoading,
                                    onSelectTab = { vm.setSpeakModeTab(it) },
                                    onSelectCategory = { vm.selectPhonemeCategory(it) },
                                    onAnalyzePronunciation = { target, spoken ->
                                        vm.analyzePronunciation(target, spoken)
                                    },
                                    onSelectScenario = { vm.resetTutorChat(it) },
                                    onSendChatMessage = { vm.sendUserChatMessage(it) },
                                    onSpeakText = { txt, slow -> vm.speakEnglish(txt, slow = slow) },
                                    onTapWordLookup = { vm.inspectWordByText(it) }
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    words = words,
                                    stats = stats,
                                    pronunciationHistory = pronunciationHistory,
                                    onStartWeakWordsSession = { vm.startLesson(it, isWeakPractice = true) },
                                    onInspectWord = { vm.inspectWord(it) },
                                    onOpenOnboardingReset = { showOnboardingModal = true }
                                )
                            }
                        }
                    }
                }
            }

            // Global Trancy-style "AI Definition" Detailed Sheet
            inspectedWord?.let { word ->
                AiDefinitionBottomSheet(
                    word = word,
                    arabicDialect = stats.arabicDialect,
                    onDismiss = { vm.inspectWord(null) },
                    onSpeakUs = { vm.playWordAudio(word, isBritish = false) },
                    onSpeakGb = { vm.playWordAudio(word, isBritish = true) },
                    onSpeakText = { vm.playSentenceAudio(it) },
                    onToggleMastered = { vm.toggleMasteredOnWord(word) },
                    onToggleFavorite = { vm.toggleFavoriteOnWord(word) }
                )
            }
        }
    }
}
