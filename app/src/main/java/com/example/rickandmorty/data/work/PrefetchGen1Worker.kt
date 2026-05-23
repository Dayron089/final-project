package com.example.rickandmorty.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import java.io.IOException

@HiltWorker
class PrefetchGen1Worker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cacheRepository: PokemonCacheRepository,
    private val settings: SettingsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (settings.hasPrefetchedGen1.first()) {
            return Result.success(workDataOf(KEY_SKIPPED to true))
        }

        // Ensure list cache is filled so the list screen shows 151 rows.
        val listResult = cacheRepository.refreshList(force = true)
        if (listResult.isFailure) {
            val err = listResult.exceptionOrNull()
            return if (err is IOException) Result.retry() else Result.failure()
        }

        var prefetched = 0
        var failed = 0
        for (id in 1..151) {
            yield()
            val result = cacheRepository.ensureDetailFresh(id)
            if (result.isSuccess) prefetched++ else failed++
        }
        return when {
            failed == 0 -> {
                settings.setHasPrefetchedGen1(true)
                Result.success(workDataOf(KEY_PREFETCHED to prefetched))
            }
            // Bounded retry: after MAX_ATTEMPTS, accept whatever we have and mark done
            // so the worker doesn't loop forever on a stable per-id failure (e.g. 404).
            runAttemptCount >= MAX_ATTEMPTS && prefetched > 0 -> {
                settings.setHasPrefetchedGen1(true)
                Result.success(workDataOf(KEY_PREFETCHED to prefetched, KEY_PARTIAL to true))
            }
            prefetched > 0 -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val KEY_PREFETCHED = "prefetched"
        const val KEY_SKIPPED = "skipped"
        const val KEY_PARTIAL = "partial"
        const val MAX_ATTEMPTS = 3
    }
}
