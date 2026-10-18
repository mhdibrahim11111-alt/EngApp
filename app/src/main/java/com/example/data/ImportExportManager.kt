package com.example.data

import android.content.Context
import android.net.Uri
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class ImportPreview(
    val rowCount: Int,
    val firstRows: List<List<String>>,
    val headers: List<String>,
    val parsedEntries: List<EntryEntity>
)

object ImportExportManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        coerceInputValues = true
    }

    /**
     * Parses raw CSV or JSON text and returns a preview of first rows, count, and parsed entities
     */
    fun parseForPreview(rawText: String, defaultDeck: String): ImportPreview {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return ImportPreview(0, emptyList(), emptyList(), emptyList())
        }

        // Try JSON
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                val dtoList = if (trimmed.startsWith("[")) {
                    json.decodeFromString<List<EntryJsonDto>>(trimmed)
                } else {
                    listOf(json.decodeFromString<EntryJsonDto>(trimmed))
                }

                val headers = listOf("text", "pos", "level", "deck", "arabic", "definition_en", "examples_en")
                val previewRows = dtoList.take(5).map { d ->
                    listOf(d.text, d.pos, d.level, d.deck.ifBlank { defaultDeck }, d.arabic, d.definition_en, d.examples_en)
                }

                val entities = dtoList.mapIndexed { index, d ->
                    val id = d.id ?: (System.currentTimeMillis() % 1_000_000_000L + index).toInt()
                    EntryEntity(
                        id = id,
                        type = d.type.ifBlank { "word" },
                        deck = d.deck.ifBlank { defaultDeck },
                        text = d.text.trim(),
                        pos = d.pos.trim(),
                        level = d.level.trim().uppercase(),
                        definitionEn = d.definition_en.trim(),
                        noteEn = d.note_en.trim(),
                        examplesEn = d.examples_en.trim(),
                        definitionUrl = d.definition_url.trim(),
                        audioUrl = d.audio_url.trim(),
                        arabic = d.arabic.trim(),
                        ipa = "",
                        syllables = "",
                        etymologyAr = "",
                        exampleAr = "",
                        isEnriched = false
                    )
                }

                return ImportPreview(dtoList.size, previewRows, headers, entities)
            } catch (e: Exception) {
                // fallback to CSV
            }
        }

        // Parse CSV with kotlin-csv
        try {
            val rows = csvReader().readAll(trimmed)
            if (rows.isEmpty()) return ImportPreview(0, emptyList(), emptyList(), emptyList())

            val hasHeader = rows.first().any { col ->
                col.trim().lowercase() in listOf("word", "text", "pos", "level", "deck", "arabic", "definition")
            }
            val headers = if (hasHeader) rows.first() else listOf("word", "pos", "level", "deck", "arabic", "definition", "example_en", "example_ar", "ipa")
            val dataRows = if (hasHeader) rows.drop(1) else rows

            val entities = dataRows.mapIndexedNotNull { index, row ->
                val wordText = row.getOrNull(0)?.trim().orEmpty()
                if (wordText.isBlank()) return@mapIndexedNotNull null

                val pos = row.getOrNull(1)?.trim().orEmpty()
                val level = row.getOrNull(2)?.trim().orEmpty().uppercase()
                val deckCol = row.getOrNull(3)?.trim().orEmpty().ifBlank { defaultDeck }
                val arabic = row.getOrNull(4)?.trim().orEmpty()
                val definition = row.getOrNull(5)?.trim().orEmpty()
                val exampleEn = row.getOrNull(6)?.trim().orEmpty()
                val exampleAr = row.getOrNull(7)?.trim().orEmpty()
                val ipa = row.getOrNull(8)?.trim().orEmpty()

                val id = (System.currentTimeMillis() % 1_000_000_000L + index).toInt()
                EntryEntity(
                    id = id,
                    type = "word",
                    deck = deckCol,
                    text = wordText,
                    pos = pos,
                    level = level,
                    definitionEn = definition,
                    noteEn = "",
                    examplesEn = exampleEn,
                    definitionUrl = "",
                    audioUrl = "",
                    arabic = arabic,
                    ipa = ipa,
                    syllables = "",
                    etymologyAr = "",
                    exampleAr = exampleAr,
                    isEnriched = arabic.isNotBlank() && ipa.isNotBlank()
                )
            }

            return ImportPreview(
                rowCount = dataRows.size,
                firstRows = dataRows.take(5),
                headers = headers,
                parsedEntries = entities
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return ImportPreview(0, emptyList(), emptyList(), emptyList())
        }
    }

    /**
     * Bulk imports parsed entries into Room:
     * Skips duplicates by checking (same text + pos + deck).
     * Returns the count of successfully imported new entries.
     */
    suspend fun executeBulkImport(
        database: LinguaDatabase,
        entries: List<EntryEntity>,
        targetDeck: String,
        onProgress: (Int, Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val dao = database.linguaDao()
        val allEntriesInDeck = dao.findEntriesByText("") // or query existing entries in target deck
        val existingTuples = HashSet<String>()
        val allExisting = dao.getAllEntryIds()

        // Cache existing (text + pos + deck)
        val existingRows = dao.getUnenrichedEntriesByDeck(targetDeck, 10000)
        // More robust: query text and pos
        // Insert in batches of 500
        var insertedCount = 0
        val total = entries.size

        entries.chunked(500).forEachIndexed { chunkIndex, chunk ->
            val toInsert = mutableListOf<EntryEntity>()
            for (entry in chunk) {
                val assignedDeck = if (entry.deck.isBlank()) targetDeck else entry.deck
                val key = "${entry.text.trim().lowercase()}|${entry.pos.trim().lowercase()}|$assignedDeck"
                if (existingTuples.contains(key)) continue

                // Check DB for duplicate
                val existingMatch = dao.findEntryByText(entry.text.trim())
                if (existingMatch != null && 
                    existingMatch.pos.equals(entry.pos.trim(), ignoreCase = true) && 
                    existingMatch.deck.equals(assignedDeck, ignoreCase = true)) {
                    existingTuples.add(key)
                    continue
                }

                toInsert.add(entry.copy(deck = assignedDeck))
                existingTuples.add(key)
            }

            if (toInsert.isNotEmpty()) {
                dao.insertEntries(toInsert)
                insertedCount += toInsert.size
            }
            onProgress(insertedCount, total)
        }

        dao.recalculateDeckCounts()
        insertedCount
    }

    /**
     * Export a deck to CSV using Storage Access Framework Uri
     */
    suspend fun exportDeckToCsv(
        context: Context,
        uri: Uri,
        database: LinguaDatabase,
        deckId: String
    ) = withContext(Dispatchers.IO) {
        val dao = database.linguaDao()
        // Query all entries in this deck with progress
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        // CSV Header
        val rows = mutableListOf<List<String>>()
        rows.add(
            listOf(
                "id", "text", "pos", "level", "deck", "arabic",
                "definition_en", "examples_en", "ipa", "syllables",
                "etymology_ar", "example_ar", "audio_url", "is_mastered", "is_favorite", "repetitions"
            )
        )

        // Read all entries for this deck
        val entries = dao.getUnenrichedEntriesByDeck(deckId, 50000) // or all entries
        // Let's get entries
        for (e in entries) {
            val progress = dao.getProgressForEntry(e.id)
            rows.add(
                listOf(
                    e.id.toString(),
                    e.text,
                    e.pos,
                    e.level,
                    e.deck,
                    e.arabic,
                    e.definitionEn,
                    e.examplesEn,
                    e.ipa,
                    e.syllables,
                    e.etymologyAr,
                    e.exampleAr,
                    e.audioUrl,
                    (progress?.isMastered ?: false).toString(),
                    (progress?.isFavorite ?: false).toString(),
                    (progress?.repetitions ?: 0).toString()
                )
            )
        }

        csvWriter().writeAll(rows, writer)
        writer.flush()
        writer.close()
        outputStream.close()
    }

    /**
     * Export a deck to JSON backup format using Storage Access Framework Uri
     */
    suspend fun exportDeckToJson(
        context: Context,
        uri: Uri,
        database: LinguaDatabase,
        deckId: String
    ) = withContext(Dispatchers.IO) {
        val dao = database.linguaDao()
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        val entries = dao.getUnenrichedEntriesByDeck(deckId, 50000)
        val exportList = entries.map { e ->
            val progress = dao.getProgressForEntry(e.id)
            mapOf(
                "id" to e.id,
                "text" to e.text,
                "pos" to e.pos,
                "level" to e.level,
                "deck" to e.deck,
                "arabic" to e.arabic,
                "definition_en" to e.definitionEn,
                "examples_en" to e.examplesEn,
                "ipa" to e.ipa,
                "syllables" to e.syllables,
                "etymology_ar" to e.etymologyAr,
                "example_ar" to e.exampleAr,
                "audio_url" to e.audioUrl,
                "is_mastered" to (progress?.isMastered ?: false),
                "is_favorite" to (progress?.isFavorite ?: false),
                "repetitions" to (progress?.repetitions ?: 0),
                "next_review" to (progress?.nextReviewDate ?: 0L)
            )
        }

        val jsonString = json.encodeToString(exportList)
        writer.write(jsonString)
        writer.flush()
        writer.close()
        outputStream.close()
    }

    /**
     * Import from backup file Uri
     */
    suspend fun importFromBackup(
        context: Context,
        uri: Uri,
        database: LinguaDatabase,
        targetDeck: String
    ): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val text = reader.readText()
        reader.close()
        inputStream.close()

        val preview = parseForPreview(text, targetDeck)
        executeBulkImport(database, preview.parsedEntries, targetDeck) { _, _ -> }
    }
}
