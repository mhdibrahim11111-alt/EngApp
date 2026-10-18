package com.example

import com.example.data.MasteryLevel
import com.example.data.SeedData
import com.example.data.SrsEngine
import com.example.data.SrsGrade
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun seedData_containsAllThreeDecksWith30WordsEach() {
        val words = SeedData.getInitialWords()
        assertEquals(90, words.size)
        assertEquals(30, words.count { it.deck == "OXFORD" })
        assertEquals(30, words.count { it.deck == "IELTS" })
        assertEquals(30, words.count { it.deck == "IT" })
        assertTrue(words.any { it.word == "cattle" })
    }

    @Test
    fun srsEngine_updatesIntervalsAndMasteryCorrectly() {
        val sample = SeedData.getInitialWords().first()
        val afterGood = SrsEngine.applyReview(sample.copy(repetitions = 0, intervalDays = 0), SrsGrade.GOOD, 1000L)
        assertEquals(1, afterGood.repetitions)
        assertEquals(1, afterGood.intervalDays)

        val afterAgain = SrsEngine.applyReview(afterGood, SrsGrade.AGAIN, 2000L)
        assertEquals(0, afterAgain.repetitions)
        assertEquals(0, afterAgain.intervalDays)
        assertTrue(afterAgain.isWeakWord)

        val mastered = SrsEngine.toggleMastered(afterGood, 3000L)
        assertEquals(MasteryLevel.MASTERED.name, mastered.masteryLevel)
    }
}
