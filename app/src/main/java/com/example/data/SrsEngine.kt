package com.example.data

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SuperMemo-2 (SM-2) Spaced Repetition System (SRS) Engine.
 */
object SrsEngine {

    private const val MIN_EASE_FACTOR = 1.3f
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun applyReview(
        progress: ProgressEntity,
        grade: SrsGrade,
        nowMillis: Long = System.currentTimeMillis()
    ): ProgressEntity {
        val q = grade.quality
        val easeDelta = (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        val newEaseFactor = max(MIN_EASE_FACTOR, progress.easeFactor + easeDelta)

        val newRepetitions: Int
        val newIntervalDays: Int

        if (q < 3) {
            newRepetitions = 0
            newIntervalDays = 0
        } else {
            newRepetitions = progress.repetitions + 1
            newIntervalDays = when (grade) {
                SrsGrade.HARD -> {
                    if (progress.intervalDays <= 1) 1
                    else max(2, (progress.intervalDays * 1.2f).roundToInt())
                }
                SrsGrade.GOOD -> {
                    when (newRepetitions) {
                        1 -> 1
                        2 -> 3
                        else -> max(4, (progress.intervalDays * newEaseFactor).roundToInt())
                    }
                }
                SrsGrade.EASY -> {
                    when (newRepetitions) {
                        1 -> 4
                        2 -> 7
                        else -> max(8, (progress.intervalDays * newEaseFactor * 1.3f).roundToInt())
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
            nowMillis
        } else {
            nowMillis + (newIntervalDays * DAY_MILLIS)
        }

        val updatedCorrect = if (q >= 3) progress.correctCount + 1 else progress.correctCount
        val updatedMistake = if (q < 3) progress.mistakeCount + 1 else progress.mistakeCount
        val isWeak = (updatedMistake >= 2 && updatedMistake >= updatedCorrect) || grade == SrsGrade.AGAIN

        return progress.copy(
            masteryLevel = newMastery.name,
            isMastered = newMastery == MasteryLevel.MASTERED,
            repetitions = newRepetitions,
            easeFactor = newEaseFactor,
            intervalDays = newIntervalDays,
            nextReviewDate = nextReview,
            correctCount = updatedCorrect,
            mistakeCount = updatedMistake,
            isWeakWord = isWeak,
            lastReviewedAt = nowMillis
        )
    }

    fun toggleMastered(progress: ProgressEntity, nowMillis: Long = System.currentTimeMillis()): ProgressEntity {
        val isCurrentlyMastered = progress.isMastered || progress.masteryLevel == MasteryLevel.MASTERED.name
        return if (isCurrentlyMastered) {
            progress.copy(
                isMastered = false,
                masteryLevel = MasteryLevel.REVIEW.name,
                intervalDays = 3,
                nextReviewDate = nowMillis + 3 * DAY_MILLIS
            )
        } else {
            progress.copy(
                isMastered = true,
                masteryLevel = MasteryLevel.MASTERED.name,
                repetitions = max(4, progress.repetitions),
                intervalDays = max(21, progress.intervalDays),
                nextReviewDate = nowMillis + 21 * DAY_MILLIS,
                isWeakWord = false
            )
        }
    }
}
