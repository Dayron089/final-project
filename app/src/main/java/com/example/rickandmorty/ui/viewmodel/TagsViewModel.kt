package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.TagRepository
import com.example.rickandmorty.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> =
        profileRepository.activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList()) else tagRepository.observeTags(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun createTag(name: String, colorArgb: Int) {
        viewModelScope.launch {
            try {
                val profileId = profileRepository.activeProfileId.first()
                if (profileId == -1L) return@launch
                tagRepository.createTag(profileId, name, colorArgb)
                _error.value = null
            } catch (e: Throwable) {
                _error.value = e.message
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepository.updateTag(tag)
            } catch (e: Throwable) {
                _error.value = e.message
            }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch { tagRepository.deleteTag(tagId) }
    }

    fun clearError() { _error.value = null }
}
