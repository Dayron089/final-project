package com.example.rickandmorty

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.work.SyncCachedPokemonWorker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SyncCachedPokemonWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun doWork_withFreshCache_returnsSuccessWithoutNetwork() = runBlocking {
        val repo = mockk<PokemonCacheRepository>()
        coEvery { repo.refreshList(force = true) } returns Result.success(Unit)
        coEvery { repo.refreshStaleDetails(any()) } returns Result.success(0)

        val worker = TestListenableWorkerBuilder<SyncCachedPokemonWorker>(context)
            .setWorkerFactory(factory(repo))
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWork_apiThrowsIO_returnsRetry() = runBlocking {
        val repo = mockk<PokemonCacheRepository>()
        coEvery { repo.refreshList(force = true) } returns Result.failure(IOException("offline"))

        val worker = TestListenableWorkerBuilder<SyncCachedPokemonWorker>(context)
            .setWorkerFactory(factory(repo))
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun doWork_repoFailureNonIO_returnsFailure() = runBlocking {
        val repo = mockk<PokemonCacheRepository>()
        coEvery { repo.refreshList(force = true) } returns Result.failure(IllegalStateException("boom"))

        val worker = TestListenableWorkerBuilder<SyncCachedPokemonWorker>(context)
            .setWorkerFactory(factory(repo))
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    private fun factory(repo: PokemonCacheRepository) = object : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters
        ): ListenableWorker = SyncCachedPokemonWorker(appContext, workerParameters, repo)
    }
}
