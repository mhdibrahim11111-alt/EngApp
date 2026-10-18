package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class MainTab(val route: String, val labelAr: String, val labelEn: String) {
    HOME("home", "الرئيسية", "Home"),
    LEARN("learn", "تعلّم", "Learn"),
    WATCH("watch", "شاهد", "Watch"),
    SPEAK("speak", "تحدّث", "Speak"),
    PROFILE("profile", "ملفي", "Profile")
}

enum class SubScreen {
    NONE,
    SRS_REVIEW,
    VOCAB_BROWSER,
    CSV_IMPORT,
    LESSON_SESSION,
    WEAK_WORDS_PRACTICE,
    GENERATE_VOCAB
}

enum class LearnModeTab(val titleAr: String, val titleEn: String) {
    PATH("مسار التعلّم", "Learning Path"),
    IELTS("وضع الآيلتس IELTS", "IELTS Mode"),
    IT("وضع التقنية IT", "IT Career Mode"),
    LEADERBOARD("لوحة الصدارة", "Leaderboard")
}

enum class SpeakModeTab(val titleAr: String, val titleEn: String) {
    PRONUNCIATION("مدرب النطق الذكي (ELSA)", "Pronunciation Coach"),
    AI_TUTOR("محادثة المعلم الذكي", "AI Tutor Chat")
}

class LinguaBridgeViewModel(application: Application) : AndroidViewModel(application) {

    val database = LinguaDatabase.getInstance(application)
    val geminiService = GeminiService(database.linguaDao())
    val repository = LinguaRepository(database, geminiService, application)

    // Audio playback manager (Media3 ExoPlayer for audio_url + Android TTS fallback)
    val audioManager = AudioPlaybackManager(application)

    // Asset import progress
    val assetImportProgress = AssetImporter.progress

    val allWords: StateFlow<List<EntryWithProgress>> = repository.allWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStatsEntity> = repository.userStats
        .map { it ?: UserStatsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatsEntity())

    val transcripts: StateFlow<List<TranscriptLessonEntity>> = repository.allTranscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pronunciationHistory: StateFlow<List<PronunciationAttemptEntity>> = repository.pronunciationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decks: StateFlow<List<DeckEntity>> = repository.decks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation & Active SubScreen
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _subScreen = MutableStateFlow(SubScreen.NONE)
    val subScreen: StateFlow<SubScreen> = _subScreen.asStateFlow()

    private val _learnModeTab = MutableStateFlow(LearnModeTab.PATH)
    val learnModeTab: StateFlow<LearnModeTab> = _learnModeTab.asStateFlow()

    private val _speakModeTab = MutableStateFlow(SpeakModeTab.PRONUNCIATION)
    val speakModeTab: StateFlow<SpeakModeTab> = _speakModeTab.asStateFlow()

    // Inspected Word & Multi-POS Variants
    private val _inspectedWord = MutableStateFlow<EntryWithProgress?>(null)
    val inspectedWord: StateFlow<EntryWithProgress?> = _inspectedWord.asStateFlow()

    private val _wordVariants = MutableStateFlow<List<EntryWithProgress>>(emptyList())
    val wordVariants: StateFlow<List<EntryWithProgress>> = _wordVariants.asStateFlow()

    private val _isEnrichingCard = MutableStateFlow(false)
    val isEnrichingCard: StateFlow<Boolean> = _isEnrichingCard.asStateFlow()

    // Status Toast / Banner message
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // SRS Session State
    private val _srsQueue = MutableStateFlow<List<EntryWithProgress>>(emptyList())
    val srsQueue: StateFlow<List<EntryWithProgress>> = _srsQueue.asStateFlow()

    private val _srsIndex = MutableStateFlow(0)
    val srsIndex: StateFlow<Int> = _srsIndex.asStateFlow()

    private val _srsShowAnswer = MutableStateFlow(true)
    val srsShowAnswer: StateFlow<Boolean> = _srsShowAnswer.asStateFlow()

    // Duolingo-style Lesson State
    private val _activeExercises = MutableStateFlow<List<ExerciseItem>>(emptyList())
    val activeExercises: StateFlow<List<ExerciseItem>> = _activeExercises.asStateFlow()

    private val _exerciseIndex = MutableStateFlow(0)
    val exerciseIndex: StateFlow<Int> = _exerciseIndex.asStateFlow()

    private val _lessonCorrectCount = MutableStateFlow(0)
    val lessonCorrectCount: StateFlow<Int> = _lessonCorrectCount.asStateFlow()

    private val _lessonCompleted = MutableStateFlow(false)
    val lessonCompleted: StateFlow<Boolean> = _lessonCompleted.asStateFlow()

    // Watch & Learn Player State
    private val _selectedTranscript = MutableStateFlow<TranscriptLessonEntity?>(null)
    val selectedTranscript: StateFlow<TranscriptLessonEntity?> = _selectedTranscript.asStateFlow()

    private val _activeSubtitleIndex = MutableStateFlow(0)
    val activeSubtitleIndex: StateFlow<Int> = _activeSubtitleIndex.asStateFlow()

    private val _isPlayingTranscript = MutableStateFlow(false)
    val isPlayingTranscript: StateFlow<Boolean> = _isPlayingTranscript.asStateFlow()

    private val _loopCurrentSentence = MutableStateFlow(false)
    val loopCurrentSentence: StateFlow<Boolean> = _loopCurrentSentence.asStateFlow()

    private val _autoPauseAfterLine = MutableStateFlow(false)
    val autoPauseAfterLine: StateFlow<Boolean> = _autoPauseAfterLine.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _showEnglishSub = MutableStateFlow(true)
    val showEnglishSub: StateFlow<Boolean> = _showEnglishSub.asStateFlow()

    private val _showArabicSub = MutableStateFlow(true)
    val showArabicSub: StateFlow<Boolean> = _showArabicSub.asStateFlow()

    private val _subtitleFontSizeSp = MutableStateFlow(18)
    val subtitleFontSizeSp: StateFlow<Int> = _subtitleFontSizeSp.asStateFlow()

    private var playbackJob: Job? = null

    // Pronunciation Coach State
    private val _selectedPhonemeCategory = MutableStateFlow(PhonemeChallengeCategory.P_B)
    val selectedPhonemeCategory: StateFlow<PhonemeChallengeCategory> = _selectedPhonemeCategory.asStateFlow()

    private val _latestPronunciationEval = MutableStateFlow<PronunciationEvaluation?>(null)
    val latestPronunciationEval: StateFlow<PronunciationEvaluation?> = _latestPronunciationEval.asStateFlow()

    // AI Tutor Chat State
    private val _selectedScenario = MutableStateFlow(ChatScenario.IT_INTERVIEW)
    val selectedScenario: StateFlow<ChatScenario> = _selectedScenario.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // IELTS Assessment State
    private val _ieltsAssessment = MutableStateFlow<IeltsAssessmentDto?>(null)
    val ieltsAssessment: StateFlow<IeltsAssessmentDto?> = _ieltsAssessment.asStateFlow()

    // Vocabulary Browser Search & Filter
    private val _vocabBrowserDeckFilter = MutableStateFlow<String?>(null)
    val vocabBrowserDeckFilter: StateFlow<String?> = _vocabBrowserDeckFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Paged words flow
    private val _selectedDeckForPaging = MutableStateFlow<String?>(null)
    val pagedEntries: Flow<PagingData<EntryWithProgress>> = _selectedDeckForPaging
        .flatMapLatest { deck -> repository.getPagedEntries(deck) }
        .cachedIn(viewModelScope)

    // Bulk Import Preview State
    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null) // (imported, total)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    // Background Deck Enrichment Progress per Deck
    private val _deckEnrichmentProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val deckEnrichmentProgress: StateFlow<Map<String, Int>> = _deckEnrichmentProgress.asStateFlow()

    // Generate Vocabulary Tool State
    private val _generateProgress = MutableStateFlow<Pair<Int, Int>?>(null) // (currentGenerated, targetCount)
    val generateProgress: StateFlow<Pair<Int, Int>?> = _generateProgress.asStateFlow()
    private var generateJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            resetTutorChat(ChatScenario.IT_INTERVIEW)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        generateJob?.cancel()
        audioManager.release()
    }

    // Play Audio (Media3 ExoPlayer for audio_url, otherwise Android TTS)
    fun playWordAudio(word: EntryWithProgress, isBritish: Boolean = false) {
        audioManager.playAudio(
            audioUrl = word.audioUrl,
            textFallback = word.text,
            isBritish = isBritish,
            playbackSpeed = _playbackSpeed.value
        )
    }

    fun playSentenceAudio(sentence: String) {
        audioManager.playAudio(
            audioUrl = null,
            textFallback = sentence,
            playbackSpeed = _playbackSpeed.value
        )
    }

    fun speakEnglish(text: String, isBritish: Boolean = false, slow: Boolean = false) {
        val speed = if (slow) 0.72f else _playbackSpeed.value
        audioManager.playAudio(
            audioUrl = null,
            textFallback = text,
            isBritish = isBritish,
            playbackSpeed = speed
        )
    }

    fun selectTab(tab: MainTab) {
        _subScreen.value = SubScreen.NONE
        _currentTab.value = tab
    }

    fun openSubScreen(screen: SubScreen) {
        _subScreen.value = screen
    }

    fun closeSubScreen() {
        _subScreen.value = SubScreen.NONE
    }

    fun setLearnModeTab(tab: LearnModeTab) {
        _learnModeTab.value = tab
    }

    fun setSpeakModeTab(tab: SpeakModeTab) {
        _speakModeTab.value = tab
    }

    fun openVocabBrowser(deckFilter: String? = null) {
        _vocabBrowserDeckFilter.value = deckFilter
        _selectedDeckForPaging.value = deckFilter
        _subScreen.value = SubScreen.VOCAB_BROWSER
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Inspects word, queries variants with different POS, and triggers Lazy AI enrichment if needed
     */
    fun inspectWord(word: EntryWithProgress?) {
        _inspectedWord.value = word
        if (word == null) {
            _wordVariants.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Find all POS variants for the same text
            val variants = repository.getWordVariants(word.text)
            _wordVariants.value = variants

            // Lazy AI Enrichment: If fields are missing, enrich in background while showing shimmer
            if (!word.isEnriched && (word.arabic.isBlank() || word.ipa.isBlank() || word.examplesEn.isBlank())) {
                _isEnrichingCard.value = true
                val enrichedEntity = repository.lazyEnrichEntry(word.entry, userStats.value.arabicDialect)
                _isEnrichingCard.value = false
                _inspectedWord.value = word.copy(entry = enrichedEntity)
                // refresh variants
                _wordVariants.value = repository.getWordVariants(word.text)
            }
        }
    }

    fun inspectWordByText(rawWord: String) {
        val clean = rawWord.replace(Regex("[^a-zA-Z-]"), "").trim()
        if (clean.isBlank()) return
        val match = allWords.value.find { it.word.equals(clean, ignoreCase = true) }
        if (match != null) {
            inspectWord(match)
            playWordAudio(match)
        } else {
            viewModelScope.launch {
                _isAiLoading.value = true
                val added = repository.addWordFromSubtitleOrLookup(
                    wordText = clean,
                    dialect = userStats.value.arabicDialect
                )
                _isAiLoading.value = false
                inspectWord(added)
                playWordAudio(added)
            }
        }
    }

    fun selectVariant(variant: EntryWithProgress) {
        inspectWord(variant)
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
    }

    // Onboarding & Preferences
    fun completeOnboarding(goal: String, cefr: String, dailyMinutes: Int, dialect: String) {
        viewModelScope.launch {
            repository.updateStats {
                it.copy(
                    onboardingCompleted = true,
                    learningGoal = goal,
                    cefrLevel = cefr,
                    dailyGoalMinutes = dailyMinutes,
                    arabicDialect = dialect
                )
            }
            showBanner("تم إعداد خطتك التعليمية المخصصة بنجاح! 🎉")
        }
    }

    fun toggleUiLanguage() {
        viewModelScope.launch {
            repository.updateStats {
                val next = if (it.uiLanguage == "AR") "EN" else "AR"
                it.copy(uiLanguage = next)
            }
        }
    }

    fun toggleArabicDialect() {
        viewModelScope.launch {
            repository.updateStats {
                val next = if (it.arabicDialect == "MSA") "EGYPTIAN" else "MSA"
                it.copy(arabicDialect = next)
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            repository.updateStats { it.copy(darkMode = !it.darkMode) }
        }
    }

    fun toggleDeckSelection(deck: DeckType) {
        viewModelScope.launch {
            repository.updateStats { stats ->
                val currentSet = stats.activeDecksCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
                if (currentSet.contains(deck.id)) {
                    if (currentSet.size > 1) currentSet.remove(deck.id)
                } else {
                    currentSet.add(deck.id)
                }
                stats.copy(activeDecksCsv = currentSet.joinToString(","))
            }
        }
    }

    fun refillHearts() {
        viewModelScope.launch {
            repository.updateStats { it.copy(hearts = it.maxHearts) }
            showBanner("تمت استعادة القلوب الكاملة! ❤️")
        }
    }

    // SRS Review Flow
    fun startSrsReview(deckFilter: String? = null) {
        val now = System.currentTimeMillis()
        val pool = allWords.value.filter { w ->
            if (deckFilter != null) w.deck == deckFilter else true
        }
        val due = pool.filter { it.nextReviewDate <= now && it.masteryLevel != MasteryLevel.MASTERED.name }
            .ifEmpty { pool.take(20) }

        _srsQueue.value = due
        _srsIndex.value = 0
        _srsShowAnswer.value = true
        _subScreen.value = SubScreen.SRS_REVIEW
        due.firstOrNull()?.let { playWordAudio(it) }
    }

    fun navigateSrsCard(delta: Int) {
        val list = _srsQueue.value
        if (list.isEmpty()) return
        val next = (_srsIndex.value + delta).coerceIn(0, list.lastIndex)
        _srsIndex.value = next
        playWordAudio(list[next])
    }

    fun submitSrsGrade(grade: SrsGrade) {
        val list = _srsQueue.value
        val idx = _srsIndex.value
        val current = list.getOrNull(idx) ?: return
        viewModelScope.launch {
            repository.gradeWordSrs(current, grade)
            if (idx < list.lastIndex) {
                _srsIndex.value = idx + 1
                playWordAudio(list[idx + 1])
            } else {
                showBanner("أحسنت! أكملت جلسة المراجعة المتباعدة اليوم (+XP) 🌟")
                _subScreen.value = SubScreen.NONE
            }
        }
    }

    fun toggleMasteredOnWord(word: EntryWithProgress) {
        viewModelScope.launch {
            val updated = repository.toggleWordMastered(word)
            if (_inspectedWord.value?.id == word.id) {
                _inspectedWord.value = updated
            }
            val currentList = _srsQueue.value.toMutableList()
            val pos = currentList.indexOfFirst { it.id == word.id }
            if (pos >= 0) {
                currentList[pos] = updated
                _srsQueue.value = currentList
            }
        }
    }

    fun toggleFavoriteOnWord(word: EntryWithProgress) {
        viewModelScope.launch {
            val updated = repository.toggleWordFavorite(word)
            if (_inspectedWord.value?.id == word.id) {
                _inspectedWord.value = updated
            }
            val currentList = _srsQueue.value.toMutableList()
            val pos = currentList.indexOfFirst { it.id == word.id }
            if (pos >= 0) {
                currentList[pos] = updated
                _srsQueue.value = currentList
            }
        }
    }

    // Duolingo-Style Lesson
    fun startLesson(wordsSubset: List<EntryWithProgress>, isWeakPractice: Boolean = false) {
        val lessonWords = wordsSubset.take(8).ifEmpty { allWords.value.take(8) }
        val allPool = allWords.value.ifEmpty { lessonWords }
        val exercises = mutableListOf<ExerciseItem>()

        lessonWords.forEachIndexed { idx, w ->
            val distractorsAr = allPool.filter { it.id != w.id }.shuffled().take(3).map { it.arabicTranslation }
            val distractorsEn = allPool.filter { it.id != w.id }.shuffled().take(3).map { it.word }

            val item = when (idx % 7) {
                0 -> ExerciseItem(
                    type = ExerciseType.MC_EN_TO_AR,
                    targetWord = w,
                    promptText = w.word,
                    subPromptText = "${w.partOfSpeech}  ${w.ipaUs}",
                    options = (distractorsAr + w.arabicTranslation).shuffled(),
                    correctAnswer = w.arabicTranslation
                )
                1 -> ExerciseItem(
                    type = ExerciseType.MC_AR_TO_EN,
                    targetWord = w,
                    promptText = w.arabicTranslation,
                    subPromptText = w.englishDefinition,
                    options = (distractorsEn + w.word).shuffled(),
                    correctAnswer = w.word
                )
                2 -> ExerciseItem(
                    type = ExerciseType.LISTENING,
                    targetWord = w,
                    promptText = "استمع إلى نطق الكلمة واختر الإجابة الصحيحة",
                    subPromptText = w.arabicTranslation,
                    options = (distractorsEn + w.word).shuffled(),
                    correctAnswer = w.word
                )
                3 -> {
                    val blankSentence = w.exampleEn1.replace(Regex(w.word, RegexOption.IGNORE_CASE), "______")
                    ExerciseItem(
                        type = ExerciseType.FILL_BLANK,
                        targetWord = w,
                        promptText = if (blankSentence.contains("______")) blankSentence else "______: ${w.englishDefinition}",
                        subPromptText = w.exampleAr1,
                        options = (distractorsEn + w.word).shuffled(),
                        correctAnswer = w.word
                    )
                }
                4 -> {
                    val cleanWords = w.exampleEn1.split(" ").filter { it.isNotBlank() }
                    ExerciseItem(
                        type = ExerciseType.WORD_ORDER,
                        targetWord = w,
                        promptText = w.exampleAr1,
                        subPromptText = "رتّب الكلمات لتكوين الجملة الإنجليزية الصحيحة",
                        correctAnswer = cleanWords.joinToString(" "),
                        scrambledWords = cleanWords.shuffled()
                    )
                }
                5 -> ExerciseItem(
                    type = ExerciseType.SPELLING,
                    targetWord = w,
                    promptText = w.arabicTranslation,
                    subPromptText = "التعريف: ${w.englishDefinition}",
                    correctAnswer = w.word
                )
                else -> ExerciseItem(
                    type = ExerciseType.SPEAKING,
                    targetWord = w,
                    promptText = w.word,
                    subPromptText = "${w.ipaUs} — ${w.arabicTranslation}",
                    correctAnswer = w.word
                )
            }
            exercises.add(item)
        }

        val pairSample = lessonWords.take(4).map { it.word to it.arabicTranslation.split(";").first().trim() }
        if (pairSample.size >= 3) {
            exercises.add(
                ExerciseItem(
                    type = ExerciseType.MATCH_PAIRS,
                    targetWord = lessonWords.first(),
                    promptText = "طابق كل كلمة إنجليزية بمعناها العربي",
                    correctAnswer = "MATCHED",
                    matchPairs = pairSample
                )
            )
        }

        _activeExercises.value = exercises
        _exerciseIndex.value = 0
        _lessonCorrectCount.value = 0
        _lessonCompleted.value = false
        _subScreen.value = if (isWeakPractice) SubScreen.WEAK_WORDS_PRACTICE else SubScreen.LESSON_SESSION
    }

    fun submitExerciseResult(isCorrect: Boolean) {
        val currentEx = _activeExercises.value.getOrNull(_exerciseIndex.value) ?: return
        viewModelScope.launch {
            repository.recordExerciseOutcome(currentEx.targetWord, isCorrect)
            if (isCorrect) {
                _lessonCorrectCount.value += 1
            }
            val nextIdx = _exerciseIndex.value + 1
            if (nextIdx < _activeExercises.value.size) {
                _exerciseIndex.value = nextIdx
            } else {
                _lessonCompleted.value = true
                repository.updateStats {
                    it.copy(
                        xp = it.xp + 30,
                        minutesStudiedToday = it.minutesStudiedToday + 3,
                        totalMinutesStudied = it.totalMinutesStudied + 3
                    )
                }
            }
        }
    }

    // --- STEP 2: BULK IMPORT WITH PREVIEW & PROGRESS ---
    fun previewImportData(rawText: String, defaultDeck: String) {
        val preview = ImportExportManager.parseForPreview(rawText, defaultDeck)
        _importPreview.value = preview
    }

    fun executeImport(targetDeck: String) {
        val preview = _importPreview.value ?: return
        if (preview.parsedEntries.isEmpty()) return

        viewModelScope.launch {
            _importProgress.value = Pair(0, preview.parsedEntries.size)
            val insertedCount = ImportExportManager.executeBulkImport(
                database = database,
                entries = preview.parsedEntries,
                targetDeck = targetDeck
            ) { count, total ->
                _importProgress.value = Pair(count, total)
            }
            _importProgress.value = null
            _importPreview.value = null
            showBanner("تم استيراد $insertedCount عنصر جديد بنجاح إلى حزمة $targetDeck! ✨")
            _subScreen.value = SubScreen.NONE
        }
    }

    // --- STEP 3: WORKMANAGER DECK ENRICHMENT ---
    fun startDeckEnrichment(deckId: String) {
        DeckEnrichmentWorker.startEnrichment(getApplication(), deckId)
        showBanner("بدأت عملية إثراء حزمة $deckId في الخلفية عبر الذكاء الاصطناعي...")
    }

    fun pauseDeckEnrichment(deckId: String) {
        DeckEnrichmentWorker.pauseEnrichment(getApplication(), deckId)
        showBanner("تم إيقاف عملية إثراء حزمة $deckId مؤقتاً.")
    }

    // --- STEP 4: GENERATE VOCABULARY WITH AI ---
    fun startGenerateVocabulary(topic: String, cefrLevel: String, targetCount: Int, targetDeck: String) {
        generateJob?.cancel()
        _generateProgress.value = Pair(0, targetCount)
        _subScreen.value = SubScreen.GENERATE_VOCAB

        generateJob = viewModelScope.launch {
            var currentTotal = 0
            val existingLower = database.linguaDao().getAllEntryIds().let {
                // Fetch known word texts
                allWords.value.map { it.text.lowercase().trim() }.toHashSet()
            }

            while (currentTotal < targetCount) {
                val needed = (targetCount - currentTotal).coerceAtMost(50)
                val batch = geminiService.generateVocabularyBatch(
                    topic = topic,
                    cefrLevel = cefrLevel,
                    count = needed,
                    existingWordsLower = existingLower
                )

                if (batch.isEmpty()) {
                    showBanner("توقف توليد الكلمات: يرجى التحقق من الاتصال بمفتاح Gemini.")
                    break
                }

                val entities = batch.map { dto ->
                    val id = (System.currentTimeMillis() % 1_000_000_000L + (Math.random() * 10000).toInt()).toInt()
                    existingLower.add(dto.text.lowercase().trim())
                    EntryEntity(
                        id = id,
                        type = "word",
                        deck = targetDeck,
                        text = dto.text.trim(),
                        pos = dto.pos.trim(),
                        level = dto.level.trim().uppercase(),
                        definitionEn = dto.definition_en.trim(),
                        noteEn = "",
                        examplesEn = dto.examples_en.trim(),
                        definitionUrl = "",
                        audioUrl = "",
                        arabic = dto.arabic.trim(),
                        ipa = dto.ipa.trim(),
                        syllables = dto.syllables.trim(),
                        etymologyAr = dto.etymology_ar.trim(),
                        exampleAr = dto.example_ar.trim(),
                        isEnriched = true
                    )
                }

                database.linguaDao().insertEntries(entities)
                currentTotal += entities.size
                _generateProgress.value = Pair(currentTotal, targetCount)
                delay(1200L) // Rate limiting
            }

            database.linguaDao().recalculateDeckCounts()
            _generateProgress.value = null
            showBanner("اكتمل توليد $currentTotal كلمة بنجاح وحُفظت في $targetDeck! 🎉")
            _subScreen.value = SubScreen.NONE
        }
    }

    fun cancelGenerateVocabulary() {
        generateJob?.cancel()
        _generateProgress.value = null
        showBanner("تم إلغاء توليد الكلمات.")
        _subScreen.value = SubScreen.NONE
    }

    // --- STEP 6: EXPORT / BACKUP (Storage Access Framework) ---
    fun exportDeckCsv(uri: Uri, deckId: String) {
        viewModelScope.launch {
            ImportExportManager.exportDeckToCsv(getApplication(), uri, database, deckId)
            showBanner("تم تصدير حزمة $deckId كملف CSV بنجاح! 💾")
        }
    }

    fun exportDeckJson(uri: Uri, deckId: String) {
        viewModelScope.launch {
            ImportExportManager.exportDeckToJson(getApplication(), uri, database, deckId)
            showBanner("تم تصدير النسخة الاحتياطية JSON لحزمة $deckId بنجاح! 💾")
        }
    }

    fun importBackup(uri: Uri, targetDeck: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val count = ImportExportManager.importFromBackup(getApplication(), uri, database, targetDeck)
            _isAiLoading.value = false
            showBanner("تمت استعادة $count كلمة من النسخة الاحتياطية بنجاح! 🔄")
        }
    }

    // Watch & Learn Controls
    fun selectTranscript(lesson: TranscriptLessonEntity) {
        stopTranscriptPlayback()
        _selectedTranscript.value = lesson
        _activeSubtitleIndex.value = 0
    }

    fun toggleTranscriptPlayback(lines: List<SubtitleLine>) {
        if (_isPlayingTranscript.value) {
            stopTranscriptPlayback()
        } else {
            startTranscriptPlayback(lines)
        }
    }

    private fun startTranscriptPlayback(lines: List<SubtitleLine>) {
        if (lines.isEmpty()) return
        _isPlayingTranscript.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_isPlayingTranscript.value) {
                val currentIdx = _activeSubtitleIndex.value.coerceIn(0, lines.lastIndex)
                val line = lines[currentIdx]
                speakEnglish(line.textEn, slow = _playbackSpeed.value < 0.9f)
                val durationMs = (((line.endSec - line.startSec).coerceAtLeast(4) * 1000L) / _playbackSpeed.value).toLong()
                delay(durationMs)

                if (!_isPlayingTranscript.value) break

                if (_autoPauseAfterLine.value) {
                    _isPlayingTranscript.value = false
                    break
                } else if (_loopCurrentSentence.value) {
                    // repeat
                } else if (currentIdx < lines.lastIndex) {
                    _activeSubtitleIndex.value = currentIdx + 1
                } else {
                    _isPlayingTranscript.value = false
                    break
                }
            }
        }
    }

    fun stopTranscriptPlayback() {
        _isPlayingTranscript.value = false
        playbackJob?.cancel()
        audioManager.stop()
    }

    fun jumpSubtitleLine(delta: Int, totalLines: Int) {
        if (totalLines <= 0) return
        val next = (_activeSubtitleIndex.value + delta).coerceIn(0, totalLines - 1)
        _activeSubtitleIndex.value = next
    }

    fun setSubtitleLineIndex(index: Int, lineTextEn: String) {
        _activeSubtitleIndex.value = index
        speakEnglish(lineTextEn)
    }

    fun toggleLoopSentence() {
        _loopCurrentSentence.value = !_loopCurrentSentence.value
    }

    fun toggleAutoPause() {
        _autoPauseAfterLine.value = !_autoPauseAfterLine.value
    }

    fun togglePlaybackSpeed() {
        _playbackSpeed.value = if (_playbackSpeed.value == 1.0f) 0.75f else 1.0f
    }

    fun toggleEnglishSubtitles() {
        _showEnglishSub.value = !_showEnglishSub.value
    }

    fun toggleArabicSubtitles() {
        _showArabicSub.value = !_showArabicSub.value
    }

    fun adjustSubtitleFontSize(delta: Int) {
        _subtitleFontSizeSp.value = (_subtitleFontSizeSp.value + delta).coerceIn(14, 26)
    }

    fun createCustomTranscript(title: String, url: String, rawText: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val created = repository.createCustomTranscriptLesson(title, url, rawText)
            _isAiLoading.value = false
            _selectedTranscript.value = created
            _activeSubtitleIndex.value = 0
            showBanner("قام Gemini بترجمة النص واستخراج الكلمات المفتاحية بنجاح! 🎬")
        }
    }

    // Pronunciation Coach Actions
    fun selectPhonemeCategory(category: PhonemeChallengeCategory) {
        _selectedPhonemeCategory.value = category
        _latestPronunciationEval.value = null
    }

    fun analyzePronunciation(targetText: String, spokenText: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val eval = repository.savePronunciationAttempt(
                targetText = targetText,
                spokenText = spokenText,
                category = _selectedPhonemeCategory.value
            )
            _latestPronunciationEval.value = eval
            _isAiLoading.value = false
        }
    }

    // AI Tutor Chat Actions
    fun resetTutorChat(scenario: ChatScenario) {
        _selectedScenario.value = scenario
        _chatMessages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                textEn = scenario.starterEn,
                arabicExplanation = scenario.starterAr,
                vocabularySuggestions = listOf("perspective", "mitigate", "scalability")
            )
        )
    }

    fun sendUserChatMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isBlank()) return
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            textEn = clean
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            val activeDecks = userStats.value.activeDecksCsv.split(",").toSet()
            val candidateWords = allWords.value.filter { it.deck in activeDecks }.map { it.word }
            val reply = geminiService.sendTutorMessage(
                scenario = _selectedScenario.value,
                cefrLevel = userStats.value.cefrLevel,
                dialect = userStats.value.arabicDialect,
                userMessage = clean,
                activeDeckWords = candidateWords
            )
            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                textEn = reply.replyEn,
                grammarCorrection = reply.grammarCorrection,
                arabicExplanation = reply.arabicExplanation,
                vocabularySuggestions = reply.suggestedDeckWords
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiLoading.value = false
            speakEnglish(reply.replyEn)
        }
    }

    fun evaluateIeltsWritingOrSpeaking(topic: String, userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val eval = geminiService.evaluateIeltsResponse(topic, userText)
            _ieltsAssessment.value = eval
            _isAiLoading.value = false
        }
    }
}
