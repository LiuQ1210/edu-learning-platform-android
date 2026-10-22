package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.NoteApi
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.UpdateNoteRequest
import javax.inject.Inject
import javax.inject.Singleton

/** 笔记仓库（对接后端 ApiV1 接口） */
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
        // 后端接口还没完全对接好，返回假数据
        val filtered = if (sourceType == null) DemoData.notes
        else DemoData.notes.copy(list = DemoData.notes.list.filter { it.sourceType == sourceType })
        return filtered
    }

    /** @return 新建笔记 ID */
    suspend fun createNote(request: CreateNoteRequest): Long {
        // 后端接口还没完全对接好，返回假数据
        return 7999L
    }

    suspend fun updateNote(noteId: Long, request: UpdateNoteRequest) {
        // 后端接口还没完全对接好
    }

    suspend fun deleteNote(noteId: Long) {
        // 后端接口还没完全对接好
    }

    /** 某内容下的我的笔记（不分页） */
    suspend fun getNotesBySource(sourceType: Int, sourceId: Long): List<NoteDto> {
        // 后端接口还没完全对接好，返回假数据
        return DemoData.notes.list
    }
}
