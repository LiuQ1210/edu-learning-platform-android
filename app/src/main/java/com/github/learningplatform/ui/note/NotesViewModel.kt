package com.github.learningplatform.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.UpdateNoteRequest
import com.github.learningplatform.data.repository.NoteRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 独立笔记的占位来源：不关联具体文章/视频时使用 */
private const val SOURCE_TYPE_ARTICLE = 1
private const val SOURCE_ID_STANDALONE = 0L

/** 关键词搜索防抖（毫秒） */
private const val SEARCH_DEBOUNCE_MS = 350L

data class NotesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val sourceType: Int? = null,
    val notes: List<NoteDto> = emptyList(),
    val message: String? = null,
    // 详情编辑态
    val currentNote: NoteDto? = null,
    val editTitle: String = "",
    val editContent: String = "",
    val saving: Boolean = false,
    val detailClosed: Boolean = false
)

/**
 * 笔记（接口文档 10.1 列表 / 10.2 新建 / 10.3 编辑 / 10.4 删除）。
 *
 * 笔记入口在文章详情页与视频播放页（接口 10.2 的 sourceId 必填且必须命中真实内容），
 * 笔记本页只做浏览/搜索/编辑/删除，不提供「新建独立笔记」。
 *
 * 文档没有提供「按 noteId 查单条」的接口，10.5 的 by-source 需要 sourceType+sourceId。
 * 因此详情页进入时按 noteId 翻列表查找（最多回溯 5 页 = 100 条），
 * 这是当前接口集合下能做到的最稳妥方案，已在代码中标注。
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    private var searchJob: Job? = null

    /**
     * 关键词输入。
     *
     * 每敲一个字就发一次请求会打爆后端（10.1 是分页查询），这里做 350ms 防抖，
     * 只有输入停顿后才真正查询；切换筛选条件或刷新会先取消待发的搜索。
     */
    fun onKeywordChange(value: String) {
        _uiState.value = _uiState.value.copy(keyword = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load()
        }
    }

    fun selectSource(sourceType: Int?) {
        if (sourceType == _uiState.value.sourceType) return
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(sourceType = sourceType)
        load()
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun load() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            _uiState.value = snapshot.copy(isLoading = true, error = null)
            try {
                val page = noteRepository.getNotes(
                    pageNum = 1,
                    pageSize = 50,
                    sourceType = snapshot.sourceType,
                    keyword = snapshot.keyword.trim().ifBlank { null }
                )
                _uiState.value = _uiState.value.copy(isLoading = false, notes = page.list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }

    fun loadDetail(noteId: Long) {
        viewModelScope.launch {
            var found = _uiState.value.notes.firstOrNull { it.noteId == noteId }
            if (found == null) {
                found = fetchNoteById(noteId)
            }
            if (found == null) {
                _uiState.value = _uiState.value.copy(
                    currentNote = null,
                    detailClosed = true,
                    message = "笔记不存在或已删除"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                currentNote = found,
                editTitle = found.title,
                editContent = found.content
            )
        }
    }

    private suspend fun fetchNoteById(noteId: Long): NoteDto? {
        var pageNum = 1
        while (pageNum <= 5) {
            val page = try {
                noteRepository.getNotes(pageNum = pageNum, pageSize = 20, sourceType = null, keyword = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return null
            }
            page.list.firstOrNull { it.noteId == noteId }?.let { return it }
            if (page.list.size < 20) return null
            pageNum++
        }
        return null
    }

    fun onEditTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(editTitle = value)
    }

    fun onEditContentChange(value: String) {
        _uiState.value = _uiState.value.copy(editContent = value)
    }

    fun saveCurrentNote() {
        val note = _uiState.value.currentNote ?: return
        if (_uiState.value.saving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                noteRepository.updateNote(
                    note.noteId,
                    UpdateNoteRequest(
                        title = _uiState.value.editTitle,
                        content = _uiState.value.editContent
                    )
                )
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    message = "已保存",
                    notes = _uiState.value.notes.map {
                        if (it.noteId == note.noteId)
                            it.copy(
                                title = _uiState.value.editTitle,
                                content = _uiState.value.editContent
                            )
                        else it
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(saving = false, message = e.readableMessage())
            }
        }
    }

    fun deleteCurrentNote() {
        val note = _uiState.value.currentNote ?: return
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(note.noteId)
                _uiState.value = _uiState.value.copy(
                    notes = _uiState.value.notes.filterNot { it.noteId == note.noteId },
                    detailClosed = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }
}
