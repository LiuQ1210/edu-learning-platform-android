package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 文件上传（接口文档 v3.1 增补 15.1）。
 *
 * ### 请求
 * `POST /api/v1/files/upload`，`multipart/form-data`：
 *  - `file`：文件本体，字段名固定为 `file`
 *  - `scene`：业务场景，决定归档目录与大小上限（见 [UploadScene]）
 *
 * ### 响应
 * 返回公网可访问的 `url`，客户端直接把它落库到对应的 `*_url` 字段，
 * **不要**自己拼 CDN 前缀。
 */
@Serializable
data class UploadFileDto(
    /** 文件唯一标识，删除时使用 */
    val fileId: String = "",
    /** 公网可访问的完整 URL */
    val url: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    /** 服务端识别出的真实 MIME（不信任客户端声明） */
    val mimeType: String = "",
    /** 图片宽（px），非图片为 0 */
    val width: Int = 0,
    /** 图片高（px），非图片为 0 */
    val height: Int = 0,
    val scene: String = "",
    val createTime: String = ""
)

/**
 * 上传场景（接口文档 v3.1 增补 15.1 的 scene 取值表）。
 *
 * @param code 传给后端的 scene 字符串
 * @param label 给用户看的文案
 * @param maxBytes 客户端预校验的大小上限，避免白跑一次上传
 */
enum class UploadScene(
    val code: String,
    val label: String,
    val maxBytes: Long
) {
    AVATAR("avatar", "头像", 2L * 1024 * 1024),
    ARTICLE_COVER("article_cover", "文章封面", 5L * 1024 * 1024),
    ARTICLE_IMAGE("article_image", "文章配图", 5L * 1024 * 1024),
    NOTE_IMAGE("note_image", "笔记插图", 5L * 1024 * 1024),
    CHECKIN_PHOTO("checkin_photo", "签到照片", 5L * 1024 * 1024);

    companion object {
        fun from(code: String): UploadScene =
            entries.firstOrNull { it.code == code } ?: ARTICLE_COVER
    }
}
