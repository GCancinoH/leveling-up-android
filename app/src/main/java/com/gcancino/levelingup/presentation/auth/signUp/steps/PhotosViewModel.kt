package com.gcancino.levelingup.presentation.auth.signUp.steps

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.repositories.BodyCompositionRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotosViewModel(
    private val signUpViewModel: SignUpViewModel,
    private val bodyCompositionRepository: BodyCompositionRepository
) : ViewModel() {
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState = _saveState.asStateFlow()

    fun addPhotos(newPhotos: List<Uri>) {
        viewModelScope.launch {
            _photos.emit(_photos.value + newPhotos)
        }
    }

    fun removePhoto(photo: Uri) {
        viewModelScope.launch {
            _photos.emit(_photos.value.filter { it != photo })
        }
    }

    fun saveData() {
        viewModelScope.launch {
            bodyCompositionRepository.updateInitialBodyCompositionPhotos(photos.value).collect { result ->
                _saveState.emit(result)
            }
        }
    }

    fun goToDashboard() {}
}