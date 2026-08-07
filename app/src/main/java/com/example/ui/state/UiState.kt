package com.example.ui.state

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
}

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Processing : BackupUiState
    data class Success(val message: String, val jsonContent: String? = null) : BackupUiState
    data class Error(val errorMessage: String) : BackupUiState
}
