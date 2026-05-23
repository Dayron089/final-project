package com.example.rickandmorty.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class SyncCachedPokemonWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cacheRepository: PokemonCacheRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val listRes = cacheRepository.refreshList(force = true)
            if (listRes.isFailure) {
                val err = listRes.exceptionOrNull()
                return if (err is IOException) Result.retry() else Result.failure()
            }
            val refreshed = cacheRepository.refreshStaleDetails(limit = 20).getOrDefault(0)
            Result.success(workDataOf(KEY_REFRESHED to refreshed))
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Throwable) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "")))
        }
    }

    companion object {
        const val KEY_REFRESHED = "refreshed"
        const val KEY_ERROR = "error"
    }
}
