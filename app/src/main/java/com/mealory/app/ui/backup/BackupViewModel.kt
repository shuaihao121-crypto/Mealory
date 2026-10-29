package com.mealory.app.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealory.app.MealoryApplication
import com.mealory.app.backup.BackupPreview
import com.mealory.app.backup.PreparedRestore
import com.mealory.app.util.toUserFacingMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val pendingRestore: BackupPreview? = null,
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = (application as MealoryApplication).container.backupManager
    private val mutableState = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = mutableState.asStateFlow()
    private var preparedRestore: PreparedRestore? = null

    fun export(uri: Uri) {
        viewModelScope.launch {
            mutableState.value = BackupUiState(busy = true)
            runCatching { manager.exportTo(uri) }
                .onSuccess { result ->
                    mutableState.value = BackupUiState(
                        message = "完整备份已导出：${result.mealCount} 顿饭，${result.photoCount} 张照片。",
                    )
                }
                .onFailure { error -> mutableState.value = BackupUiState(error = error.userMessage("备份导出失败")) }
        }
    }

    fun prepareRestore(uri: Uri) {
        viewModelScope.launch {
            preparedRestore?.let(manager::discardPrepared)
            preparedRestore = null
            mutableState.value = BackupUiState(busy = true)
            runCatching { manager.prepareRestore(uri) }
                .onSuccess { prepared ->
                    preparedRestore = prepared
                    mutableState.value = BackupUiState(pendingRestore = prepared.preview)
                }
                .onFailure { error -> mutableState.value = BackupUiState(error = error.userMessage("备份验证失败")) }
        }
    }

    fun confirmRestore() {
        val prepared = preparedRestore ?: return
        preparedRestore = null
        viewModelScope.launch {
            mutableState.value = BackupUiState(busy = true)
            runCatching { manager.commitRestore(prepared) }
                .onSuccess { result ->
                    mutableState.value = BackupUiState(
                        message = "恢复完成：${result.mealCount} 顿饭，${result.photoCount} 张照片。",
                    )
                }
                .onFailure { error -> mutableState.value = BackupUiState(error = error.userMessage("恢复失败，原数据已保留")) }
        }
    }

    fun cancelRestore() {
        preparedRestore?.let(manager::discardPrepared)
        preparedRestore = null
        mutableState.update { it.copy(pendingRestore = null) }
    }

    fun clearFeedback() {
        mutableState.update { it.copy(message = null, error = null) }
    }

    override fun onCleared() {
        preparedRestore?.let(manager::discardPrepared)
    }
}

private fun Throwable.userMessage(fallback: String): String = toUserFacingMessage(fallback)
