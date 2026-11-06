package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 首页轮播图（对齐后端 `GET /api/v1/banner/list`）。
 *
 * 后端字段（learning_banner）：
 *  - bannerId / title / imageUrl / orderNum / status
 *  - courseId / courseName：点击后跳转的课程
 *  - categoryId / categoryName：分类窗口，0 = 全部
 */
@Serializable
data class BannerDto(
    val bannerId: Long = 0,
    val title: String = "",
    /** 轮播图图片地址（可能是 OSS 绝对地址，也可能是 /profile/... 相对路径） */
    val imageUrl: String = "",
    val courseId: Long = 0,
    val courseName: String = "",
    /** 0 = 全部窗口 */
    val categoryId: Long = 0,
    val categoryName: String = "",
    val orderNum: Int = 0,
    /** 0 = 正常 */
    val status: String = "0"
)
