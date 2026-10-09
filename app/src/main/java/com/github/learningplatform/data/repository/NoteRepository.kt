package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.demo.contentPreviewUnit
import com.github.learningplatform.data.remote.NoteApi
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.UpdateNoteRequest
import javax.inject.Inject
import javax.inject.Singleton

/** 笔记仓库（接口文档 v3.0 第十章） */
@Singleton
class NoteRepository @Inject constructor(
    private val noteApi: NoteApi
) {

    suspend fun getNotes(
        pageNum: Int = 1,
        pageSize: Int = 20,
        sourceType: Int? = null,
        keyword: String? = null
    ): PageData<NoteDto> {
        contentPreview { return DemoData.notes }
        return safeApiCall {
            noteApi.getNotes(pageNum, pageSize, sourceType, keyword)
        }
    }

    /** @return 新建笔记 ID */
    suspend fun createNote(request: CreateNoteRequest): Long {
        contentPreview { return 7999L }
        return safeApiCall { noteApi.createNote(request) }.noteId
    }

    suspend fun updateNote(noteId: Long, request: UpdateNoteRequest) {
        contentPreviewUnit { return }
        safeApiCallForUnit { noteApi.updateNote(noteId, request) }
    }

    suspend fun deleteNote(noteId: Long) {
        contentPreviewUnit { return }
        safeApiCallForUnit { noteApi.deleteNote(noteId) }
    }

    /** 某内容下的我的笔记（不分页） */
    suspend fun getNotesBySource(sourceType: Int, sourceId: Long): List<NoteDto> {
        contentPreview { return DemoData.notes.list }
        return safeApiCall { noteApi.getNotesBySource(sourceType, sourceId) }
    }
}
