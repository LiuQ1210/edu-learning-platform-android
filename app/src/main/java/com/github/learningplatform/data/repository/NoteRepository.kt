package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.ApiTargetType
import com.github.learningplatform.data.remote.NoteApi
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.UpdateNoteRequest
import kotlinx.serialization.json.JsonObject
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
        val table = noteApi.getNoteList(
            targetType = ApiTargetType.ofNote(sourceType),
            keyword = keyword,
            pageNum = pageNum,
            pageSize = pageSize
        ).okTable()
        return PageData(total = table.total, list = table.rows.map { it.toNote() })
    }

    /** @return 新建笔记 ID */
    suspend fun createNote(request: CreateNoteRequest): Long =
        noteApi.addNote(
            targetType = ApiTargetType.ofNote(request.sourceType) ?: ApiTargetType.POST,
            targetId = request.sourceId,
            noteTitle = request.title,
            noteContent = request.content,
            progress = request.videoTimestamp
        ).okBody().payload().long("noteId")

    suspend fun updateNote(noteId: Long, request: UpdateNoteRequest) {
        noteApi.editNote(
            noteId = noteId,
            noteTitle = request.title.orEmpty(),
            noteContent = request.content.orEmpty()
        ).okBody()
    }

    suspend fun deleteNote(noteId: Long) {
        noteApi.removeNote(noteId).okBody()
    }

    /** 某内容下的我的笔记（不分页） */
    suspend fun getNotesBySource(sourceType: Int, sourceId: Long): List<NoteDto> {
        val table = noteApi.getNoteList(
            targetType = ApiTargetType.ofNote(sourceType),
            targetId = sourceId,
            pageNum = 1,
            pageSize = 100
        ).okTable()
        return table.rows.map { it.toNote() }
    }
}

/** 后端 Note -> 客户端 NoteDto（字段名不同：noteTitle/noteContent/progress） */
private fun JsonObject.toNote(): NoteDto = NoteDto(
    noteId = long("noteId"),
    title = str("noteTitle"),
    content = str("noteContent"),
    sourceType = ApiTargetType.toInt(str("targetType")),
    sourceId = long("targetId"),
    sourceTitle = str("sourceTitle"),
    videoTimestamp = int("progress").takeIf { it > 0 },
    updateTime = str("updateTime").ifEmpty { str("createTime") }
)
