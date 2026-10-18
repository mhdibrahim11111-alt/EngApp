package com.example.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LinguaRepository(
    val database: LinguaDatabase,
    val geminiService: GeminiService,
    private val context: Context
) {
    private val dao = database.linguaDao()

    val allWords: Flow<List<EntryWithProgress>> = dao.observeAllEntriesWithProgress().map { list ->
        list.map { it.toEntryWithProgress() }
    }

    val userStats: Flow<UserStatsEntity?> = dao.observeUserStats()
    val allTranscripts: Flow<List<TranscriptLessonEntity>> = dao.observeTranscripts()
    val pronunciationHistory: Flow<List<PronunciationAttemptEntity>> = dao.observePronunciationAttempts()
    val decks: Flow<List<DeckEntity>> = dao.observeDecks()

    fun getPagedEntries(deck: String?): Flow<PagingData<EntryWithProgress>> {
        return Pager(
            config = PagingConfig(pageSize = 40, prefetchDistance = 10, enablePlaceholders = false),
            pagingSourceFactory = { dao.pagingSourceEntries(deck) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEntryWithProgress() }
        }
    }

    fun searchEntriesFts(query: String): Flow<List<EntryWithProgress>> {
        val cleanQuery = query.trim().replace("'", "").replace("\"", "")
        if (cleanQuery.isBlank()) return allWords
        // FTS match query with prefix
        val ftsQuery = "$cleanQuery*"
        return dao.searchEntriesFts(ftsQuery).map { list ->
            list.map { it.toEntryWithProgress() }
        }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getUserStatsOnce() == null) {
            dao.saveUserStats(UserStatsEntity())
        }
        if (dao.getTranscriptCount() == 0) {
            dao.insertTranscripts(SeedTranscriptsAndDrills.getInitialTranscripts())
        }
        // Background import bundled assets if needed
        AssetImporter.importBundledAssetsIfNeeded(context, database)
    }

    suspend fun updateStats(transform: (UserStatsEntity) -> UserStatsEntity) {
        val current = dao.getUserStatsOnce() ?: UserStatsEntity()
        val updated = transform(current)
        dao.saveUserStats(updated)
    }

    suspend fun gradeWordSrs(word: EntryWithProgress, grade: SrsGrade) {
        val currentProgress = dao.getProgressForEntry(word.id) ?: ProgressEntity(entryId = word.id)
        val updated = SrsEngine.applyReview(currentProgress, grade)
        dao.saveProgress(updated)

        updateStats { stats ->
            val xpGain = when (grade) {
                SrsGrade.EASY -> 15
                SrsGrade.GOOD -> 10
                SrsGrade.HARD -> 6
                SrsGrade.AGAIN -> 2
            }
            val newXp = stats.xp + xpGain
            stats.copy(
                xp = newXp,
                level = 1 + (newXp / 150),
                totalAnswers = stats.totalAnswers + 1,
                correctAnswers = if (grade != SrsGrade.AGAIN) stats.correctAnswers + 1 else stats.correctAnswers
            )
        }
    }

    suspend fun toggleWordMastered(word: EntryWithProgress): EntryWithProgress {
        val currentProgress = dao.getProgressForEntry(word.id) ?: ProgressEntity(entryId = word.id)
        val updated = SrsEngine.toggleMastered(currentProgress)
        dao.saveProgress(updated)
        return word.copy(progress = updated)
    }

    suspend fun toggleWordFavorite(word: EntryWithProgress): EntryWithProgress {
        val currentProgress = dao.getProgressForEntry(word.id) ?: ProgressEntity(entryId = word.id)
        val updated = currentProgress.copy(isFavorite = !currentProgress.isFavorite)
        dao.saveProgress(updated)
        return word.copy(progress = updated)
    }

    suspend fun recordExerciseOutcome(word: EntryWithProgress, isCorrect: Boolean) {
        val currentProgress = dao.getProgressForEntry(word.id) ?: ProgressEntity(entryId = word.id)
        val updated = if (isCorrect) {
            currentProgress.copy(
                correctCount = currentProgress.correctCount + 1,
                masteryLevel = if (currentProgress.masteryLevel == MasteryLevel.NEW.name) MasteryLevel.LEARNING.name else currentProgress.masteryLevel,
                isWeakWord = if (currentProgress.correctCount + 1 > currentProgress.mistakeCount + 1) false else currentProgress.isWeakWord
            )
        } else {
            currentProgress.copy(
                mistakeCount = currentProgress.mistakeCount + 1,
                isWeakWord = true,
                nextReviewDate = System.currentTimeMillis()
            )
        }
        dao.saveProgress(updated)

        updateStats { stats ->
            val newXp = if (isCorrect) stats.xp + 12 else stats.xp + 2
            val newHearts = if (!isCorrect && stats.hearts > 1) stats.hearts - 1 else stats.hearts
            stats.copy(
                xp = newXp,
                level = 1 + (newXp / 150),
                hearts = newHearts,
                totalAnswers = stats.totalAnswers + 1,
                correctAnswers = if (isCorrect) stats.correctAnswers + 1 else stats.correctAnswers
            )
        }
    }

    /**
     * Finds all POS variants for the same text
     */
    suspend fun getWordVariants(text: String): List<EntryWithProgress> {
        return dao.getEntriesWithProgressByText(text).map { it.toEntryWithProgress() }
    }

    /**
     * Lazy AI enrichment for a single card opened by the user
     */
    suspend fun lazyEnrichEntry(entry: EntryEntity, dialect: String = "MSA"): EntryEntity {
        if (entry.isEnriched && entry.arabic.isNotBlank() && entry.ipa.isNotBlank()) {
            return entry
        }
        val enriched = geminiService.enrichSingleEntry(entry, dialect)
        dao.updateEntry(enriched)
        return enriched
    }

    suspend fun addWordFromSubtitleOrLookup(
        wordText: String,
        deck: String = "oxford5000",
        dialect: String = "MSA"
    ): EntryWithProgress {
        val clean = wordText.replace(Regex("[^a-zA-Z-]"), "").trim()
        val existing = dao.findEntryByText(clean)
        if (existing != null) {
            val progress = dao.getProgressForEntry(existing.id) ?: ProgressEntity(entryId = existing.id)
            val updatedProgress = progress.copy(
                masteryLevel = MasteryLevel.LEARNING.name,
                nextReviewDate = System.currentTimeMillis()
            )
            dao.saveProgress(updatedProgress)
            return EntryWithProgress(existing, updatedProgress)
        }

        val newId = (System.currentTimeMillis() % 1_000_000_000L).toInt()
        val partial = EntryEntity(
            id = newId,
            type = "word",
            deck = deck,
            text = clean.ifBlank { wordText },
            pos = "n.",
            level = "B2",
            definitionEn = "",
            noteEn = "",
            examplesEn = "",
            definitionUrl = "",
            audioUrl = "",
            arabic = "",
            ipa = "",
            syllables = "",
            etymologyAr = "",
            exampleAr = "",
            isEnriched = false
        )
        val enriched = geminiService.enrichSingleEntry(partial, dialect)
        dao.insertOrUpdateEntry(enriched)
        val progress = ProgressEntity(
            entryId = enriched.id,
            masteryLevel = MasteryLevel.LEARNING.name,
            nextReviewDate = System.currentTimeMillis()
        )
        dao.saveProgress(progress)
        dao.recalculateDeckCounts()
        return EntryWithProgress(enriched, progress)
    }

    suspend fun createCustomTranscriptLesson(
        title: String,
        sourceUrlOrTag: String,
        rawTextOrSrt: String
    ): TranscriptLessonEntity {
        val pack = geminiService.generateSubtitlesAndVocab(rawTextOrSrt)
        val entity = TranscriptLessonEntity(
            titleEn = title.ifBlank { "Custom Interactive Video Lesson" },
            titleAr = if (title.isBlank()) "درس فيديو تفاعلي مخصص" else title,
            speakerOrSource = sourceUrlOrTag.ifBlank { "YouTube / SRT Import" },
            category = "CUSTOM",
            cefrLevel = "B1-C1",
            durationSec = (pack.lines.size * 6).coerceAtLeast(18),
            videoUrlOrTag = sourceUrlOrTag.ifBlank { "Custom SRT" },
            linesJson = geminiService.json.encodeToString(pack.lines),
            keyVocabJson = geminiService.json.encodeToString(pack.keyVocab)
        )
        val id = dao.insertTranscript(entity)
        return entity.copy(id = id.toInt())
    }

    suspend fun savePronunciationAttempt(
        targetText: String,
        spokenText: String,
        category: PhonemeChallengeCategory
    ): PronunciationEvaluation {
        val eval = geminiService.evaluatePronunciation(targetText, spokenText, category)
        dao.insertPronunciationAttempt(
            PronunciationAttemptEntity(
                targetText = targetText,
                spokenText = spokenText,
                focusCategory = category.id,
                overallScore = eval.overallScore,
                evaluationJson = geminiService.json.encodeToString(eval)
            )
        )
        updateStats { stats ->
            val newXp = stats.xp + 15
            stats.copy(xp = newXp, level = 1 + (newXp / 150), minutesStudiedToday = stats.minutesStudiedToday + 1)
        }
        return eval
    }
}
