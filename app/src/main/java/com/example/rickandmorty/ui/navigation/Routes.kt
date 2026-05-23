package com.example.rickandmorty.ui.navigation

object Routes {
    const val PROFILES = "profiles"
    const val HOME = "home"
    const val HOME_LIST = "home/list"
    const val HOME_TEAMS = "home/teams"
    const val HOME_TAGS = "home/tags"
    const val HOME_SETTINGS = "home/settings"

    const val DETAIL = "detail/{pokemonId}"
    fun detail(pokemonId: Int) = "detail/$pokemonId"

    const val TEAM_DETAIL = "teams/{teamId}"
    fun teamDetail(teamId: Long) = "teams/$teamId"

    const val TEAM_PICKER = "teams/{teamId}/pick"
    fun teamPicker(teamId: Long) = "teams/$teamId/pick"

    const val FAVORITES = "favorites"
    const val HISTORY = "history"
}
