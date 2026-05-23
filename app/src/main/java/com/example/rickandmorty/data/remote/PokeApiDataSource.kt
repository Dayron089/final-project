package com.example.rickandmorty.data.remote

import com.example.rickandmorty.api.PokeApi
import com.example.rickandmorty.data.model.PokemonDetail
import com.example.rickandmorty.data.model.PokemonResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PokeApiDataSource @Inject constructor(
    private val api: PokeApi
) {
    suspend fun fetchListPage(limit: Int, offset: Int = 0): Result<List<PokemonResult>> = runCatching {
        api.getPokemonList(limit = limit, offset = offset).results
    }

    suspend fun fetchDetail(name: String): Result<PokemonDetail> = runCatching {
        api.getPokemonDetail(name)
    }

    suspend fun fetchDetailById(id: Int): Result<PokemonDetail> = fetchDetail(id.toString())
}
