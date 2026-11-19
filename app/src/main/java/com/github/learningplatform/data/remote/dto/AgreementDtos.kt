package com.github.learningplatform.data.remote.dto

/**
 * 平台协议（后端 Agreement 裁剪字段）。
 *
 * 三种固定类型：
 *  - user_agreement 用户协议
 *  - privacy_policy 隐私政策
 *  - about_us       关于我们
 *
 * [content] 为后端 summernote 编辑的 HTML 正文，详情页用 WebView 渲染。
 */
data class AgreementDto(
    val agreementId: Long = 0L,
    val agreementType: String = "",
    val typeLabel: String = "",
    val title: String = "",
    val content: String = "",
    val updateTime: String = ""
)
