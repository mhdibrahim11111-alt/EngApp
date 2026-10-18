package com.example.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.delay

class DeckEnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val deck = inputData.getString(KEY_DECK) ?: return Result.failure()
        val database = LinguaDatabase.getInstance(applicationContext)
        val dao = database.linguaDao()
        val geminiService = GeminiService(dao)

        val totalToEnrich = dao.getUnenrichedCountByDeck(deck)
        if (totalToEnrich == 0) {
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_PROCESSED to 0, KEY_TOTAL to 0))
            return Result.success()
        }

        var processed = 0
        while (processed < totalToEnrich) {
            if (isStopped) {
                return Result.retry()
            }

            // Fetch up to 25 unenriched entries
            val batch = dao.getUnenrichedEntriesByDeck(deck, 25)
            if (batch.isEmpty()) break

            try {
                val enriched = geminiService.enrichBatchEntries(batch)
                enriched.forEach { entry ->
                    dao.updateEntry(entry)
                }
                processed += batch.size

                val percent = ((processed.toFloat() / totalToEnrich) * 100).toInt().coerceIn(0, 100)
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to percent,
                        KEY_PROCESSED to processed,
                        KEY_TOTAL to totalToEnrich
                    )
                )

                // Rate limiting delay between batches
                delay(1200L)
            } catch (e: Exception) {
                e.printStackTrace()
                // Retry with exponential backoff handled by WorkManager
                return Result.retry()
            }
        }

        return Result.success(
            workDataOf(
                KEY_PROGRESS to 100,
                KEY_PROCESSED to processed,
                KEY_TOTAL to totalToEnrich
            )
        )
    }

    companion object {
        const val KEY_DECK = "key_deck"
        const val KEY_PROGRESS = "key_progress"
        const val KEY_PROCESSED = "key_processed"
        const val KEY_TOTAL = "key_total"

        fun startEnrichment(context: Context, deck: String): androidx.work.Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DeckEnrichmentWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_DECK to deck))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .addTag("enrich_deck_$deck")
                .build()

            return WorkManager.getInstance(context).enqueueUniqueWork(
                "enrich_deck_$deck",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun pauseEnrichment(context: Context, deck: String) {
            WorkManager.getInstance(context).cancelUniqueWork("enrich_deck_$deck")
        }
    }
}
