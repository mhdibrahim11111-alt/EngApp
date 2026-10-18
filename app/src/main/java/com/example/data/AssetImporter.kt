package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

data class AssetImportProgress(
    val isImporting: Boolean = false,
    val currentFileName: String = "",
    val importedCount: Int = 0,
    val totalEstimated: Int = 9862,
    val isCompleted: Boolean = false
)

object AssetImporter {

    private val _progress = MutableStateFlow(AssetImportProgress())
    val progress: StateFlow<AssetImportProgress> = _progress.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun importBundledAssetsIfNeeded(context: Context, database: LinguaDatabase) = withContext(Dispatchers.IO) {
        val dao = database.linguaDao()
        val existingCount = dao.getEntryCount()
        if (existingCount >= 9000) {
            // Already imported
            _progress.value = AssetImportProgress(isCompleted = true, importedCount = existingCount)
            return@withContext
        }

        // Initialize standard decks if not present
        val initialDecks = listOf(
            DeckEntity(
                id = "oxford5000",
                nameEn = "Oxford 5000",
                nameAr = "أكسفورد 5000 الأساسي",
                description = "Core 5000 vocabulary words from Oxford Learner's Dictionary",
                targetCount = 5836
            ),
            DeckEntity(
                id = "oxford_opal",
                nameEn = "Oxford OPAL",
                nameAr = "أكسفورد الأكاديمي OPAL",
                description = "Oxford Phrasal Academic Lexicon",
                targetCount = 2096
            ),
            DeckEntity(
                id = "oxford_phrases",
                nameEn = "Oxford Phrases",
                nameAr = "عبارات أكسفورد",
                description = "Common everyday phrases and idioms from Oxford",
                targetCount = 750
            ),
            DeckEntity(
                id = "everyday_expressions",
                nameEn = "Everyday Expressions",
                nameAr = "تعبيرات يومية",
                description = "Expressions for daily life and conversation",
                targetCount = 600
            ),
            DeckEntity(
                id = "essential_idioms",
                nameEn = "Essential Idioms",
                nameAr = "مصطلحات أساسية",
                description = "Essential English idioms with meanings and examples",
                targetCount = 468
            ),
            DeckEntity(
                id = "ielts_core",
                nameEn = "IELTS Core",
                nameAr = "مفردات الآيلتس الأكاديمية",
                description = "High-scoring academic vocabulary for IELTS",
                targetCount = 3575
            ),
            DeckEntity(
                id = "it_industry",
                nameEn = "IT Industry",
                nameAr = "مصطلحات البرمجة والتقنية",
                description = "Software engineering, cloud, and tech industry terms",
                targetCount = 1700
            )
        )
        dao.insertDecks(initialDecks)

        val assetFiles = listOf(
            "oxford5000_part1.json" to "oxford5000",
            "oxford5000_part2.json" to "oxford5000",
            "oxford5000_part3.json" to "oxford5000",
            "oxford_opal.json" to "oxford_opal",
            "oxford_phrases.json" to "oxford_phrases",
            "everyday_expressions.json" to "everyday_expressions",
            "essential_idioms.json" to "essential_idioms"
        )

        _progress.value = AssetImportProgress(isImporting = true, importedCount = existingCount)
        val existingIds = dao.getAllEntryIds().toHashSet()
        var totalImported = existingCount

        for ((fileName, targetDeck) in assetFiles) {
            _progress.value = _progress.value.copy(currentFileName = fileName)
            try {
                val inputStream = context.assets.open(fileName)
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val text = reader.readText()
                reader.close()
                inputStream.close()

                val dtoList = json.decodeFromString<List<EntryJsonDto>>(text)

                // Chunk into batches of 500 inside transactions
                val chunks = dtoList.chunked(500)
                for (chunk in chunks) {
                    val entitiesToInsert = mutableListOf<EntryEntity>()
                    for (dto in chunk) {
                        val entryId = dto.id ?: continue
                        if (existingIds.contains(entryId)) continue

                        entitiesToInsert.add(
                            EntryEntity(
                                id = entryId,
                                type = dto.type.ifBlank { "word" },
                                deck = targetDeck,
                                text = dto.text.trim(),
                                pos = dto.pos.trim(),
                                level = dto.level.trim().uppercase(),
                                definitionEn = dto.definition_en.trim(),
                                noteEn = dto.note_en.trim(),
                                examplesEn = dto.examples_en.trim(),
                                definitionUrl = dto.definition_url.trim(),
                                audioUrl = dto.audio_url.trim(),
                                arabic = dto.arabic.trim(),
                                ipa = "",
                                syllables = "",
                                etymologyAr = "",
                                exampleAr = "",
                                isEnriched = false
                            )
                        )
                        existingIds.add(entryId)
                    }

                    if (entitiesToInsert.isNotEmpty()) {
                        database.withTransaction {
                            dao.insertEntries(entitiesToInsert)
                        }
                        totalImported += entitiesToInsert.size
                        _progress.value = _progress.value.copy(importedCount = totalImported)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dao.recalculateDeckCounts()
        _progress.value = AssetImportProgress(
            isImporting = false,
            isCompleted = true,
            importedCount = totalImported
        )
    }
}
