package com.example.rickandmorty.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val profileRepository: ProfileRepository
) {

    private val workManager get() = WorkManager.getInstance(context)

    suspend fun bootstrap() {
        // Ensure there is a default profile and active id.
        profileRepository.getOrCreateDefault()

        if (settings.syncEnabled.first()) {
            schedulePeriodicSync()
        } else {
            cancelPeriodicSync()
        }

        if (!settings.hasPrefetchedGen1.first()) {
            enqueuePrefetchGen1()
        }
    }

    suspend fun schedulePeriodicSync() {
        val intervalHours = settings.syncIntervalHours.first().toLong().coerceAtLeast(1L)
        val wifiOnly = settings.wifiOnlySync.first()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncCachedPokemonWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC)
    }

    fun enqueueOneTimeSync() {
        val request = OneTimeWorkRequestBuilder<SyncCachedPokemonWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_SYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueuePrefetchGen1() {
        val request = OneTimeWorkRequestBuilder<PrefetchGen1Worker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_PREFETCH_GEN1,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_PERIODIC_SYNC = "work.sync.cached_pokemon"
        const val UNIQUE_ONE_TIME_SYNC = "work.sync.cached_pokemon.now"
        const val UNIQUE_PREFETCH_GEN1 = "work.prefetch.gen1"
    }
}
