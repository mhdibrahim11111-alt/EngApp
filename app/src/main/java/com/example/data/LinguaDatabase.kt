package com.example.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinguaDao {

    // --- ENTRIES ---
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Int): EntryEntity?

    @Query("SELECT * FROM entries WHERE LOWER(text) = LOWER(:text) LIMIT 1")
    suspend fun findEntryByText(text: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE LOWER(text) = LOWER(:text)")
    suspend fun findEntriesByText(text: String): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY id ASC")
    fun observeAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deck = :deck")
    suspend fun getEntryCountByDeck(deck: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<EntryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("SELECT id FROM entries")
    suspend fun getAllEntryIds(): List<Int>

    @Query("SELECT * FROM entries WHERE isEnriched = 0 AND (arabic = '' OR ipa = '' OR examplesEn = '') LIMIT :limit")
    suspend fun getUnenrichedEntries(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE deck = :deck AND isEnriched = 0 AND (arabic = '' OR ipa = '' OR examplesEn = '') LIMIT :limit")
    suspend fun getUnenrichedEntriesByDeck(deck: String, limit: Int): List<EntryEntity>

    @Query("SELECT COUNT(*) FROM entries WHERE deck = :deck AND isEnriched = 0 AND (arabic = '' OR ipa = '')")
    suspend fun getUnenrichedCountByDeck(deck: String): Int

    // --- JOIN ENTRIES WITH PROGRESS ---
    @Query("""
        SELECT e.*, p.masteryLevel, p.isMastered, p.isFavorite, p.repetitions, 
               p.easeFactor, p.intervalDays, p.nextReviewDate, p.correctCount, 
               p.mistakeCount, p.isWeakWord, p.lastReviewedAt, p.createdAt AS progressCreatedAt
        FROM entries e
        LEFT JOIN progress p ON e.id = p.entryId
        ORDER BY e.id ASC
    """)
    fun observeAllEntriesWithProgress(): Flow<List<EntryWithProgressRaw>>

    @Query("""
        SELECT e.*, p.masteryLevel, p.isMastered, p.isFavorite, p.repetitions, 
               p.easeFactor, p.intervalDays, p.nextReviewDate, p.correctCount, 
               p.mistakeCount, p.isWeakWord, p.lastReviewedAt, p.createdAt AS progressCreatedAt
        FROM entries e
        LEFT JOIN progress p ON e.id = p.entryId
        WHERE (:deck IS NULL OR e.deck = :deck)
        ORDER BY e.id ASC
    """)
    fun pagingSourceEntries(deck: String?): PagingSource<Int, EntryWithProgressRaw>

    @Query("""
        SELECT e.*, p.masteryLevel, p.isMastered, p.isFavorite, p.repetitions, 
               p.easeFactor, p.intervalDays, p.nextReviewDate, p.correctCount, 
               p.mistakeCount, p.isWeakWord, p.lastReviewedAt, p.createdAt AS progressCreatedAt
        FROM entries e
        LEFT JOIN progress p ON e.id = p.entryId
        WHERE e.id IN (
            SELECT rowid FROM entries_fts WHERE entries_fts MATCH :query
        )
        ORDER BY e.id ASC
    """)
    fun searchEntriesFts(query: String): Flow<List<EntryWithProgressRaw>>

    @Query("""
        SELECT e.*, p.masteryLevel, p.isMastered, p.isFavorite, p.repetitions, 
               p.easeFactor, p.intervalDays, p.nextReviewDate, p.correctCount, 
               p.mistakeCount, p.isWeakWord, p.lastReviewedAt, p.createdAt AS progressCreatedAt
        FROM entries e
        LEFT JOIN progress p ON e.id = p.entryId
        WHERE e.id = :id
        LIMIT 1
    """)
    suspend fun getEntryWithProgressById(id: Int): EntryWithProgressRaw?

    @Query("""
        SELECT e.*, p.masteryLevel, p.isMastered, p.isFavorite, p.repetitions, 
               p.easeFactor, p.intervalDays, p.nextReviewDate, p.correctCount, 
               p.mistakeCount, p.isWeakWord, p.lastReviewedAt, p.createdAt AS progressCreatedAt
        FROM entries e
        LEFT JOIN progress p ON e.id = p.entryId
        WHERE LOWER(e.text) = LOWER(:text)
        ORDER BY e.id ASC
    """)
    suspend fun getEntriesWithProgressByText(text: String): List<EntryWithProgressRaw>

    // --- DECKS ---
    @Query("SELECT * FROM decks ORDER BY id ASC")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getDeckById(id: String): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Query("UPDATE decks SET totalCount = (SELECT COUNT(*) FROM entries WHERE deck = decks.id)")
    suspend fun recalculateDeckCounts()

    // --- PROGRESS ---
    @Query("SELECT * FROM progress WHERE entryId = :entryId LIMIT 1")
    suspend fun getProgressForEntry(entryId: Int): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressList(progressList: List<ProgressEntity>)

    @Query("SELECT COUNT(*) FROM progress WHERE isMastered = 1")
    suspend fun getMasteredCount(): Int

    @Query("SELECT COUNT(*) FROM progress WHERE isFavorite = 1")
    suspend fun getFavoritesCount(): Int

    @Query("SELECT COUNT(*) FROM progress p JOIN entries e ON p.entryId = e.id WHERE e.deck = :deck AND p.isMastered = 1")
    suspend fun getMasteredCountByDeck(deck: String): Int

    // --- AI CACHE ---
    @Query("SELECT * FROM ai_cache WHERE entryId = :entryId LIMIT 1")
    suspend fun getAiCache(entryId: String): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAiCache(cache: AiCacheEntity)

    // --- USER STATS ---
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsOnce(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserStats(stats: UserStatsEntity)

    // --- TRANSCRIPTS ---
    @Query("SELECT * FROM transcripts ORDER BY id DESC")
    fun observeTranscripts(): Flow<List<TranscriptLessonEntity>>

    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTranscriptCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscripts(list: List<TranscriptLessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(item: TranscriptLessonEntity): Long

    // --- PRONUNCIATION ATTEMPTS ---
    @Query("SELECT * FROM pronunciation_attempts ORDER BY timestamp DESC LIMIT 30")
    fun observePronunciationAttempts(): Flow<List<PronunciationAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPronunciationAttempt(attempt: PronunciationAttemptEntity)
}

/**
 * Raw data class matching the SQL join between entries and progress
 */
data class EntryWithProgressRaw(
    @Embedded val entry: EntryEntity,
    val masteryLevel: String?,
    val isMastered: Boolean?,
    val isFavorite: Boolean?,
    val repetitions: Int?,
    val easeFactor: Float?,
    val intervalDays: Int?,
    val nextReviewDate: Long?,
    val correctCount: Int?,
    val mistakeCount: Int?,
    val isWeakWord: Boolean?,
    val lastReviewedAt: Long?,
    val progressCreatedAt: Long?
) {
    fun toEntryWithProgress(): EntryWithProgress {
        val progress = if (masteryLevel != null) {
            ProgressEntity(
                entryId = entry.id,
                masteryLevel = masteryLevel,
                isMastered = isMastered ?: false,
                isFavorite = isFavorite ?: false,
                repetitions = repetitions ?: 0,
                easeFactor = easeFactor ?: 2.5f,
                intervalDays = intervalDays ?: 0,
                nextReviewDate = nextReviewDate ?: 0L,
                correctCount = correctCount ?: 0,
                mistakeCount = mistakeCount ?: 0,
                isWeakWord = isWeakWord ?: false,
                lastReviewedAt = lastReviewedAt ?: 0L,
                createdAt = progressCreatedAt ?: System.currentTimeMillis()
            )
        } else null
        return EntryWithProgress(entry = entry, progress = progress)
    }
}

@Database(
    entities = [
        EntryEntity::class,
        EntryFtsEntity::class,
        DeckEntity::class,
        ProgressEntity::class,
        AiCacheEntity::class,
        UserStatsEntity::class,
        TranscriptLessonEntity::class,
        PronunciationAttemptEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LinguaDatabase : RoomDatabase() {
    abstract fun linguaDao(): LinguaDao

    companion object {
        @Volatile
        private var INSTANCE: LinguaDatabase? = null

        fun getInstance(context: Context): LinguaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinguaDatabase::class.java,
                    "linguabridge_room.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
