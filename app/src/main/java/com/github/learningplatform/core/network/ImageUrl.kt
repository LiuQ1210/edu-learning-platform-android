package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import java.net.URI

/**
 * 图片地址补全。
 *
 * 后端对图片字段（课程封面 `coverUrl`、用户头像 `avatar`、文章/历史封面等）
 * 会返回两种形态：
 *
 *  1. 完整可访问地址 —— 开了 OSS 时是 `https://bucket.oss-xxx/upload/x.png`；
 *     `/api/v1/upload` 上传成功返回的也是「服务器地址 + /profile/upload/xxx.png」。
 *  2. **相对路径** —— 数据库里直接存的是 `/profile/upload/2026/10/26/xxx.jpg`，
 *     课程列表、文章列表、个人资料等接口都是原样回传，不做拼接。
 *
 * Coil 不认相对路径（会当成非法 URL 直接失败），所以统一在这里按
 * [Constants.BASE_URL] 的 origin（scheme + host + port）补全。
 *
 * 注意 origin 要取 BASE_URL 的 origin 而不是整串：BASE_URL 可能带路径段
 * （例如 `http://host/api/v1/`），直接拼会得到 `/api/v1/profile/upload/...`。
 */
object ImageUrl {

    private val origin: String = runCatching {
        val uri = URI(Constants.BASE_URL)
        "${uri.scheme}://${uri.authority}"
    }.getOrElse {
        Constants.BASE_URL.trimEnd('/')
    }

    fun resolve(raw: String?): String {
        val url = raw?.trim().orEmpty()
        if (url.isEmpty()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return if (url.startsWith("/")) origin + url else "$origin/$url"
    }
}
