package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinTaskDto
import com.github.learningplatform.ui.readableMessage
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.data.repository.FileRepository
import com.github.learningplatform.data.remote.dto.UploadScene
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CheckinUiState(
    val isLoading: Boolean = false,
    val tasks: List<CheckinTaskDto> = emptyList(),
    val error: String? = null,
    val activeTask: CheckinTaskDto? = null,
    val submitting: Boolean = false,
    val message: String? = null,
    val recordsLoading: Boolean = false,
    val records: List<CheckinRecordDto> = emptyList(),
    val recordsError: String? = null,
    /** 拍照签到：照片上传中 */
    val uploadingPhoto: Boolean = false,
    /** 已上传照片的 URL（上传成功即可提交） */
    val uploadedPhotoUrl: String? = null,
    /** 本地已选中的照片，用于预览 */
    val localPhotoPath: String? = null
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckinUiState())
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    init { loadTasks() }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val tasks = checkinRepository.getTodayTasks()
                _uiState.value = _uiState.value.copy(isLoading = false, tasks = tasks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readable())
            }
        }
    }

    fun openPanel(task: CheckinTaskDto) {
        _uiState.value = _uiState.value.copy(activeTask = task, message = null)
    }

    fun closePanel() {
        if (_uiState.value.submitting) return
        _uiState.value = _uiState.value.copy(activeTask = null)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** 正常 / 位置 / 手势签到 */
    fun submit(
        task: CheckinTaskDto,
        longitude: Double? = null,
        latitude: Double? = null,
        gesturePattern: String? = null
    ) {
        if (_uiState.value.submitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, message = null)
            try {
                val result = checkinRepository.submit(
                    taskId = task.taskId,
                    longitude = longitude,
                    latitude = latitude,
                    gesturePattern = gesturePattern
                )
                finish(result.success, result.message.ifBlank { "签到失败" })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(submitting = false, message = e.readable())
            }
        }
    }

    /**
     * 拍照签到第一步：上传照片（接口 v3.1 增补 15.1，scene=checkin_photo）。
     *
     * 上传与签到是两步：先拿 url，再用 url 调 9.2 提交。
     * 上传成功后不立刻提交，等用户点「确认签到」，避免误触直接签到。
     */
    fun uploadPhoto(file: File) {
        if (_uiState.value.uploadingPhoto) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                uploadingPhoto = true,
                message = null,
                localPhotoPath = file.absolutePath,
                uploadedPhotoUrl = null
            )
            try {
                val result = fileRepository.upload(file, UploadScene.CHECKIN_PHOTO)
                _uiState.value = _uiState.value.copy(
                    uploadingPhoto = false,
                    uploadedPhotoUrl = result.url
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    uploadingPhoto = false,
                    localPhotoPath = null,
                    message = e.readableMessage()
                )
            }
        }
    }

    /** 拍照签到第二步：带 photoUrl 提交（接口 9.2） */
    fun submitPhoto(task: CheckinTaskDto) {
        val photoUrl = _uiState.value.uploadedPhotoUrl
        if (photoUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(message = "请先选择并上传照片")
            return
        }
        if (_uiState.value.submitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, message = null)
            try {
                val result = checkinRepository.submit(taskId = task.taskId, photoUrl = photoUrl)
                _uiState.value = _uiState.value.copy(
                    uploadedPhotoUrl = null,
                    localPhotoPath = null
                )
                finish(result.success, result.message.ifBlank { "签到失败" })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(submitting = false, message = e.readableMessage())
            }
        }
    }

    /** 清空已选照片（重选） */
    fun clearPhoto() {
        if (_uiState.value.uploadingPhoto) return
        _uiState.value = _uiState.value.copy(uploadedPhotoUrl = null, localPhotoPath = null)
    }

    private fun finish(success: Boolean, text: String) {
        _uiState.value = _uiState.value.copy(
            submitting = false,
            activeTask = null,
            message = if (success) "签到成功" else text
        )
        if (success) loadTasks()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recordsLoading = true, recordsError = null)
            try {
                val page = checkinRepository.getRecords()
                _uiState.value = _uiState.value.copy(recordsLoading = false, records = page.list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(recordsLoading = false, recordsError = e.readable())
            }
        }
    }

    private fun Throwable.readable(): String = readableMessage()
}