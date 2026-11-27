package com.github.learningplatform.data.repository

import com.github.learningplatform.data.remote.dto.AgentRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 智能体结果解析的纯逻辑测试。
 *
 * 这些 JSON 都摘自后端 `docs/智能体接口契约.md` 与 agent-service 的真实输出，
 * 解析错一处，App 就会把 PPT / 视频 / SQL / 执行步骤展示成空白。
 */
class AgentParsingTest {

    @Test
    fun `clean answer removes legacy source tag`() {
        val answer = "循环有三个要素。\n\n[来源: 第3章 循环结构]"
        assertEquals("循环有三个要素。", AgentRepository.cleanAnswer(answer))
    }

    @Test
    fun `ppt new structure keeps structured slides`() {
        val json = """
            {
              "url": "https://oss.example.com/agent/ppt/9/58.pptx",
              "fileName": "循环结构.pptx",
              "pageCount": 6,
              "slides": [
                {"page": 1, "type": "cover", "title": "循环结构", "bullets": ["从入门到实践"]},
                {"page": 2, "type": "contents", "title": "目录", "bullets": ["第一章"]}
              ],
              "outline": {"title": "循环结构", "subtitle": "课程汇报", "sections": []}
            }
        """.trimIndent()

        val ppt = AgentRepository.parsePpt(json)
        assertEquals("https://oss.example.com/agent/ppt/9/58.pptx", ppt?.url)
        assertEquals("循环结构.pptx", ppt?.fileName)
        assertEquals(6, ppt?.pageCount)
        assertEquals(2, ppt?.slides?.size)
        assertEquals("cover", ppt?.slides?.first()?.type)
        assertEquals(listOf("从入门到实践"), ppt?.slides?.first()?.bullets)
    }

    @Test
    fun `ppt legacy numeric slides falls back to outline`() {
        val json = """
            {"url":"https://oss/1.pptx","fileName":"a.pptx","slides":6,
             "outline":{"title":"旧任务","sections":[{"title":"第一章","summary":"概述"}]}}
        """.trimIndent()

        val ppt = AgentRepository.parsePpt(json)
        assertEquals(6, ppt?.pageCount)
        assertTrue(ppt?.slides.isNullOrEmpty())
        assertEquals(1, ppt?.outline?.sections?.size)
    }

    @Test
    fun `video summary parses float duration and subtitle fields`() {
        val json = """
            {"videoId":201,"courseId":12,"videoTitle":"01-环境搭建","durationMinutes":12.5,
             "summary":"本节讲环境搭建。","transcriptChars":800,"asr":true,"fallback":false,
             "subtitleUrl":"https://oss/v-201.srt","subtitleVttUrl":"https://oss/v-201.vtt",
             "subtitleFormat":"srt","subtitleSegments":42}
        """.trimIndent()

        val summary = AgentRepository.parseVideoSummary(json)
        assertEquals(201L, summary?.videoId)
        assertEquals(12, summary?.durationMinutes)
        assertEquals(42, summary?.subtitleSegments)
        assertTrue(summary?.asr == true)
        assertFalse(summary?.fallback == true)
    }

    @Test
    fun `subtitle task result parses cache flag and urls`() {
        val json = """
            {"videoId":201,"videoTitle":"01-环境搭建","durationMinutes":12.5,
             "subtitleUrl":"https://oss/v-201.srt","subtitleVttUrl":"https://oss/v-201.vtt",
             "subtitleFormat":"srt","subtitleSegments":42,"transcriptChars":800,
             "cached":true,"asr":true}
        """.trimIndent()

        val subtitle = AgentRepository.parseSubtitle(json)
        assertEquals("https://oss/v-201.srt", subtitle?.subtitleUrl)
        assertEquals(42, subtitle?.subtitleSegments)
        assertTrue(subtitle?.cached == true)
    }

    @Test
    fun `act output splits course and post refs`() {
        val json = """
            {"answer":"已完成，PPT 地址：https://oss/1.pptx",
             "refs":[
               {"course":"MySQL 性能优化实战","chapter":"第一章 索引与执行计划"},
               {"postId":211,"postTitle":"计算机网络3天速成","category":"计算机网络","excerpt":"…"}
             ],
             "rounds":4,"maxSteps":8,"toolCallCount":5,"truncated":false,"stepCount":11,
             "status":"SUCCESS"}
        """.trimIndent()

        val output = AgentRepository.parseActOutput(json)
        assertEquals(2, output?.refs?.size)
        assertEquals(4, output?.rounds)
        assertFalse(output?.truncated == true)
        val courseRef = output?.refs?.first()
        val postRef = output?.refs?.last()
        assertTrue(courseRef?.isCourse == true)
        assertFalse(courseRef?.isPost == true)
        assertTrue(postRef?.isPost == true)
        assertEquals(211L, postRef?.postId)
        assertEquals("计算机网络", postRef?.category)
    }

    @Test
    fun `act failed output keeps degrade reason`() {
        val json = """
            {"answer":"","refs":[],"rounds":8,"maxSteps":8,"truncated":true,
             "status":"FAILED","errorCode":"STEP_LIMIT","degraded":true,
             "degradeReason":"达到推理轮次上限，结果不完整","errorMessage":"已达到最大推理轮次 8"}
        """.trimIndent()

        val output = AgentRepository.parseActOutput(json)
        assertTrue(output?.truncated == true)
        assertEquals("STEP_LIMIT", output?.errorCode)
        assertEquals("达到推理轮次上限，结果不完整", output?.degradeReason)
    }

    @Test
    fun `sql snapshot builds ordered columns and string cells`() {
        val json = """
            {"sql":"SELECT course_name, cnt FROM v_agent_course_stat LIMIT 20",
             "rows":[
               {"course_name":"Java 入门","cnt":12},
               {"course_name":"MySQL 实战","cnt":8}
             ],
             "rowCount":2,"error":false}
        """.trimIndent()

        val snapshot = AgentRepository.parseSqlSnapshot(json)
        assertEquals(listOf("course_name", "cnt"), snapshot?.headers)
        assertEquals(listOf(listOf("Java 入门", "12"), listOf("MySQL 实战", "8")), snapshot?.rows)
        assertEquals(2, snapshot?.rowCount)
        assertFalse(snapshot?.error == true)
    }

    @Test
    fun `course ref falls back to title and chapterTitle`() {
        val ref = AgentRef(courseId = 12, title = "MySQL 性能优化实战", chapterTitle = "第一章")
        assertTrue(ref.isCourse)
        assertEquals("MySQL 性能优化实战", ref.courseName)
        assertEquals("第一章", ref.chapterName)
    }

    @Test
    fun `session scene label falls back to chat`() {
        assertEquals("执行任务", com.github.learningplatform.ui.axing.sceneLabel("act"))
        assertEquals("数据查询", com.github.learningplatform.ui.axing.sceneLabel("sql"))
        assertEquals("自由对话", com.github.learningplatform.ui.axing.sceneLabel("unknown"))
    }
}
