package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinguaDao {
    @Query("SELECT * FROM words ORDER BY id ASC")
    fun observeAllWords(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("SELECT * FROM words WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun findWordByText(word: String): WordEntity?

    // User Stats
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsOnce(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserStats(stats: UserStatsEntity)

    // Transcripts
    @Query("SELECT * FROM transcripts ORDER BY id DESC")
    fun observeTranscripts(): Flow<List<TranscriptLessonEntity>>

    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTranscriptCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscripts(list: List<TranscriptLessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(item: TranscriptLessonEntity): Long

    // Pronunciation Attempts
    @Query("SELECT * FROM pronunciation_attempts ORDER BY timestamp DESC LIMIT 30")
    fun observePronunciationAttempts(): Flow<List<PronunciationAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPronunciationAttempt(attempt: PronunciationAttemptEntity)

    // AI Cache
    @Query("SELECT * FROM ai_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCachedResponse(key: String): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCachedResponse(entry: AiCacheEntity)
}

@Database(
    entities = [
        WordEntity::class,
        UserStatsEntity::class,
        TranscriptLessonEntity::class,
        PronunciationAttemptEntity::class,
        AiCacheEntity::class
    ],
    version = 2,
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
