package com.example.data

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SuperMemo-2 (SM-2) Spaced Repetition System (SRS) Engine.
 *
 * Scheduling Logic:
 * 1. Quality grade `q` is mapped from the user's button choice:
 *    - AGAIN (q = 1): Complete lapse. Reset repetitions to 0, interval to 0 (due immediately/today),
 *      decrease ease factor by 0.20 (floored at 1.3), and mark mastery as LEARNING.
 *    - HARD  (q = 3): Recalled with significant effort. Increment repetitions, interval grows conservatively
 *      (max(1, interval * 1.2)), slight ease penalty (-0.15, min 1.3).
 *    - GOOD  (q = 4): Standard successful recall.
 *      - rep 1 -> 1 day
 *      - rep 2 -> 3 days
 *      - rep >= 3 -> round(previousInterval * easeFactor)
 *    - EASY  (q = 5): Effortless recall.
 *      - rep 1 -> 4 days
 *      - rep >= 2 -> round(previousInterval * easeFactor * 1.3)
 *      - Boost ease factor by +0.15.
 *
 * 2. Mastery State Machine (New -> Learning -> Review -> Mastered):
 *    - NEW: repetitions == 0 && correctCount == 0
 *    - LEARNING: intervalDays in 0..2
 *    - REVIEW: intervalDays in 3..13
 *    - MASTERED: intervalDays >= 14 or explicit user "Mastered" toggle
 */
object SrsEngine {

    private const val MIN_EASE_FACTOR = 1.3f
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun applyReview(
        word: WordEntity,
        grade: SrsGrade,
        nowMillis: Long = System.currentTimeMillis()
    ): WordEntity {
        val q = grade.quality

        // Standard SM-2 Ease Factor adjustment formula:
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val easeDelta = (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        val newEaseFactor = max(MIN_EASE_FACTOR, word.easeFactor + easeDelta)

        val newRepetitions: Int
        val newIntervalDays: Int

        if (q < 3) {
            // Lapse (AGAIN): reset repetition chain and review again in the current session / today
            newRepetitions = 0
            newIntervalDays = 0
        } else {
            newRepetitions = word.repetitions + 1
            newIntervalDays = when (grade) {
                SrsGrade.HARD -> {
                    if (word.intervalDays <= 1) 1
                    else max(2, (word.intervalDays * 1.2f).roundToInt())
                }
                SrsGrade.GOOD -> {
                    when (newRepetitions) {
                        1 -> 1
                        2 -> 3
                        else -> max(4, (word.intervalDays * newEaseFactor).roundToInt())
                    }
                }
                SrsGrade.EASY -> {
                    when (newRepetitions) {
                        1 -> 4
                        2 -> 7
                        else -> max(8, (word.intervalDays * newEaseFactor * 1.3f).roundToInt())
                    }
                }
                SrsGrade.AGAIN -> 0
            }
        }

        val newMastery = when {
            newIntervalDays >= 14 || (grade == SrsGrade.EASY && newRepetitions >= 3) -> MasteryLevel.MASTERED
            newIntervalDays >= 3 -> MasteryLevel.REVIEW
            else -> MasteryLevel.LEARNING
        }

        val nextReview = if (newIntervalDays == 0) {
            // Due immediately (within 5 minutes)
            nowMillis
        } else {
            nowMillis + (newIntervalDays * DAY_MILLIS)
        }

        val updatedCorrect = if (q >= 3) word.correctCount + 1 else word.correctCount
        val updatedMistake = if (q < 3) word.mistakeCount + 1 else word.mistakeCount
        val isWeak = (updatedMistake >= 2 && updatedMistake >= updatedCorrect) || grade == SrsGrade.AGAIN

        return word.copy(
            masteryLevel = newMastery.name,
            repetitions = newRepetitions,
            easeFactor = newEaseFactor,
            intervalDays = newIntervalDays,
            nextReviewDate = nextReview,
            correctCount = updatedCorrect,
            mistakeCount = updatedMistake,
            isWeakWord = isWeak
        )
    }

    fun toggleMastered(word: WordEntity, nowMillis: Long = System.currentTimeMillis()): WordEntity {
        val isCurrentlyMastered = word.masteryLevel == MasteryLevel.MASTERED.name
        return if (isCurrentlyMastered) {
            word.copy(
                masteryLevel = MasteryLevel.REVIEW.name,
                intervalDays = 3,
                nextReviewDate = nowMillis + 3 * DAY_MILLIS
            )
        } else {
            word.copy(
                masteryLevel = MasteryLevel.MASTERED.name,
                repetitions = max(4, word.repetitions),
                intervalDays = max(21, word.intervalDays),
                nextReviewDate = nowMillis + 21 * DAY_MILLIS,
                isWeakWord = false
            )
        }
    }
}
