package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString

class LinguaRepository(
    private val dao: LinguaDao,
    val geminiService: GeminiService
) {
    val allWords: Flow<List<WordEntity>> = dao.observeAllWords()
    val userStats: Flow<UserStatsEntity?> = dao.observeUserStats()
    val allTranscripts: Flow<List<TranscriptLessonEntity>> = dao.observeTranscripts()
    val pronunciationHistory: Flow<List<PronunciationAttemptEntity>> = dao.observePronunciationAttempts()

    suspend fun ensureSeeded() {
        if (dao.getUserStatsOnce() == null) {
            dao.saveUserStats(UserStatsEntity())
        }
        if (dao.getWordCount() == 0) {
            dao.insertWords(SeedData.getInitialWords())
        }
        if (dao.getTranscriptCount() == 0) {
            dao.insertTranscripts(SeedTranscriptsAndDrills.getInitialTranscripts())
        }
    }

    suspend fun updateStats(transform: (UserStatsEntity) -> UserStatsEntity) {
        val current = dao.getUserStatsOnce() ?: UserStatsEntity()
        val updated = transform(current)
        dao.saveUserStats(updated)
    }

    suspend fun gradeWordSrs(word: WordEntity, grade: SrsGrade) {
        val updated = SrsEngine.applyReview(word, grade)
        dao.updateWord(updated)
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

    suspend fun toggleWordMastered(word: WordEntity): WordEntity {
        val updated = SrsEngine.toggleMastered(word)
        dao.updateWord(updated)
        return updated
    }

    suspend fun toggleWordFavorite(word: WordEntity): WordEntity {
        val updated = word.copy(isFavorite = !word.isFavorite)
        dao.updateWord(updated)
        return updated
    }

    suspend fun recordExerciseOutcome(word: WordEntity, isCorrect: Boolean) {
        val updated = if (isCorrect) {
            word.copy(
                correctCount = word.correctCount + 1,
                masteryLevel = if (word.masteryLevel == MasteryLevel.NEW.name) MasteryLevel.LEARNING.name else word.masteryLevel,
                isWeakWord = if (word.correctCount + 1 > word.mistakeCount + 1) false else word.isWeakWord
            )
        } else {
            word.copy(
                mistakeCount = word.mistakeCount + 1,
                isWeakWord = true,
                nextReviewDate = System.currentTimeMillis()
            )
        }
        dao.updateWord(updated)

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
     * Parses CSV or JSON input (columns: word, pos, level, deck, arabic, definition, example)
     * and uses Gemini to batch auto-fill missing Arabic translation, IPA, etymology, and examples.
     */
    suspend fun importCustomDeckData(rawInput: String, defaultDeck: String, dialect: String): Int {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return 0

        val parsedPartials = mutableListOf<WordEntity>()
        if (trimmed.startsWith("[")) {
            // JSON array format
            try {
                val dtos = geminiService.json.decodeFromString<List<AutoFillWordDto>>(trimmed)
                dtos.forEach { d ->
                    if (d.word.isNotBlank()) {
                        parsedPartials.add(
                            WordEntity(
                                word = d.word.trim(),
                                partOfSpeech = d.partOfSpeech.ifBlank { "n." },
                                cefrLevel = d.cefrLevel.ifBlank { "B1" },
                                deck = defaultDeck,
                                arabicTranslation = d.arabicTranslation,
                                englishDefinition = d.englishDefinition,
                                exampleEn1 = d.exampleEn1,
                                exampleAr1 = d.exampleAr1,
                                exampleEn2 = d.exampleEn2,
                                exampleAr2 = d.exampleAr2,
                                ipaUs = d.ipaUs,
                                synonyms = d.synonyms,
                                nextReviewDate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback to CSV line parsing
            }
        }

        if (parsedPartials.isEmpty()) {
            // CSV parsing: word, pos, level, deck, arabic, definition, example
            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
            lines.forEachIndexed { index, line ->
                val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                if (index == 0 && cols.firstOrNull()?.lowercase() == "word") return@forEachIndexed
                val wordText = cols.getOrNull(0).orEmpty()
                if (wordText.isNotBlank()) {
                    val pos = cols.getOrNull(1).orEmpty().ifBlank { "n." }
                    val level = cols.getOrNull(2).orEmpty().uppercase().ifBlank { "B1" }
                    val deckCol = cols.getOrNull(3).orEmpty().ifBlank { defaultDeck }
                    val deck = DeckType.fromString(deckCol).id
                    val arabic = cols.getOrNull(4).orEmpty()
                    val definition = cols.getOrNull(5).orEmpty()
                    val example = cols.getOrNull(6).orEmpty()

                    parsedPartials.add(
                        WordEntity(
                            word = wordText,
                            partOfSpeech = pos,
                            cefrLevel = if (level in listOf("A1", "A2", "B1", "B2", "C1")) level else "B1",
                            deck = deck,
                            arabicTranslation = arabic,
                            englishDefinition = definition,
                            exampleEn1 = example,
                            exampleAr1 = "",
                            exampleEn2 = "",
                            exampleAr2 = "",
                            ipaUs = "",
                            synonyms = "",
                            nextReviewDate = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        if (parsedPartials.isEmpty()) return 0

        // Batch auto-fill in chunks of up to 10 via Gemini
        val enriched = parsedPartials.chunked(10).flatMap { batch ->
            val needsAutoFill = batch.any { it.arabicTranslation.isBlank() || it.ipaUs.isBlank() || it.exampleAr1.isBlank() }
            if (needsAutoFill) {
                geminiService.autoFillImportedWords(batch, dialect)
            } else {
                batch
            }
        }

        dao.insertWords(enriched)
        updateStats { it.copy(xp = it.xp + enriched.size * 5) }
        return enriched.size
    }

    suspend fun addWordFromSubtitleOrLookup(
        wordText: String,
        deck: String = "OXFORD",
        dialect: String = "MSA"
    ): WordEntity {
        val clean = wordText.replace(Regex("[^a-zA-Z-]"), "").trim()
        val existing = dao.findWordByText(clean)
        if (existing != null) {
            val updated = existing.copy(
                masteryLevel = MasteryLevel.LEARNING.name,
                nextReviewDate = System.currentTimeMillis()
            )
            dao.updateWord(updated)
            return updated
        }
        val partial = WordEntity(
            word = clean.ifBlank { wordText },
            partOfSpeech = "n.",
            cefrLevel = "B2",
            deck = deck,
            arabicTranslation = "",
            englishDefinition = "",
            exampleEn1 = "",
            exampleAr1 = "",
            exampleEn2 = "",
            exampleAr2 = "",
            ipaUs = "",
            synonyms = "",
            masteryLevel = MasteryLevel.LEARNING.name,
            nextReviewDate = System.currentTimeMillis()
        )
        val enriched = geminiService.autoFillImportedWords(listOf(partial), dialect).first()
        val id = dao.insertWord(enriched)
        return enriched.copy(id = id.toInt())
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
